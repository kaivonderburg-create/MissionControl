package com.dynamicos.sfd2trust;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Iterator;
import java.util.TreeMap;

public final class LabEngine {
    private final SharedPreferences prefs;
    private final KeyStore keyStore;
    private JSONObject state;
    private final SecureRandom random = new SecureRandom();
    public static final String DISCLAIMER = "LOKALES KRYPTO-LABOR · KEIN VW-SFD2-UNLOCK · KEINE FAHRZEUGVERBINDUNG";

    public LabEngine(Context c) throws Exception {
        prefs = c.getSharedPreferences("dynamicOS_lab_v8", Context.MODE_PRIVATE);
        keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias("dynamicOS_lab_envelope")) {
            KeyGenerator kg = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder("dynamicOS_lab_envelope",
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
            kg.generateKey();
        }
        state = new JSONObject(prefs.getString("state", "{}"));
        keys("tester"); keys("signer");
        event("SYSTEM", "Android Trust Signer gestartet");
    }

    private String b64(byte[] b) { return Base64.encodeToString(b, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING); }
    private byte[] ub64(String s) { return Base64.decode(s, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING); }
    private String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format(Locale.ROOT, "%02x", x & 255));
        return sb.toString();
    }
    private String sha(byte[] b) throws Exception { return hex(MessageDigest.getInstance("SHA-256").digest(b)); }
    public String hash(String txt) throws Exception { return sha(txt.getBytes(StandardCharsets.UTF_8)); }
    private String nonce() { byte[] b = new byte[24]; random.nextBytes(b); return hex(b); }
    private String newId() { return nonce().substring(0, 32); }
    private byte[] encrypt(byte[] raw) throws Exception {
        SecretKey k = (SecretKey) keyStore.getKey("dynamicOS_lab_envelope", null);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, k);
        byte[] iv = c.getIV(); byte[] cipher = c.doFinal(raw);
        byte[] out = new byte[iv.length + cipher.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(cipher, 0, out, iv.length, cipher.length);
        return out;
    }
    private byte[] decrypt(byte[] raw) throws Exception {
        if (raw.length < 29) throw new SecurityException("Schlüsseldatei ungültig");
        byte[] iv = java.util.Arrays.copyOfRange(raw, 0, 12);
        byte[] encrypted = java.util.Arrays.copyOfRange(raw, 12, raw.length);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, (SecretKey) keyStore.getKey("dynamicOS_lab_envelope", null),
                new GCMParameterSpec(128, iv));
        return c.doFinal(encrypted);
    }
    private KeyPair keys(String slot) throws Exception {
        String entry = prefs.getString("key_" + slot, "");
        if (!entry.isEmpty()) {
            String json = new String(decrypt(ub64(entry)), StandardCharsets.UTF_8);
            JSONObject j = new JSONObject(json);
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(ub64(j.getString("private"))));
            PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(ub64(j.getString("public"))));
            return new KeyPair(pub, priv);
        }
        KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        JSONObject j = new JSONObject();
        j.put("private", b64(pair.getPrivate().getEncoded()));
        j.put("public", b64(pair.getPublic().getEncoded()));
        prefs.edit().putString("key_" + slot, b64(encrypt(j.toString().getBytes(StandardCharsets.UTF_8)))).commit();
        return pair;
    }

    private byte[] canonical(JSONObject j) throws Exception {
        TreeMap<String, Object> sorted = new TreeMap<>();
        Iterator<String> names = j.keys();
        while (names.hasNext()) {
            String name = names.next();
            Object v = j.get(name);
            if (v instanceof JSONObject || v instanceof JSONArray) throw new SecurityException("Verschachtelte Datensätze nicht zugelassen");
            sorted.put(name, v);
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (java.util.Map.Entry<String, Object> e : sorted.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(JSONObject.quote(e.getKey())).append(":");
            Object v = e.getValue();
            if (v instanceof String) sb.append(JSONObject.quote((String)v));
            else if (v == JSONObject.NULL) sb.append("null");
            else sb.append(v.toString());
        }
        return sb.append("}").toString().getBytes(StandardCharsets.UTF_8);
    }
    private String sign(KeyPair pair, JSONObject j) throws Exception {
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(pair.getPrivate()); s.update(canonical(j));
        return b64(s.sign());
    }
    private boolean verify(KeyPair pair, JSONObject j, String sig) throws Exception {
        if (sig == null || sig.isEmpty()) return false;
        Signature s = Signature.getInstance("Ed25519");
        s.initVerify(pair.getPublic()); s.update(canonical(j));
        try { return s.verify(ub64(sig)); } catch (IllegalArgumentException ex) { return false; }
    }
    private void save() { prefs.edit().putString("state", state.toString()).commit(); }
    private void require(boolean condition, String error) {
        if (!condition) throw new IllegalStateException(error);
    }
    private void checkScope(String vin, String ecu) {
        require(vin != null && vin.toUpperCase(Locale.ROOT).matches("[A-HJ-NPR-Z0-9]{17}"), "FIN muss 17 gültige Zeichen haben");
        require(ecu != null && ecu.toUpperCase(Locale.ROOT).matches("[0-9A-F]{2}"), "ECU-Adresse: zwei Hex-Zeichen erforderlich");
    }
    public synchronized void event(String category, String msg) {
        JSONArray entries;
        try { entries = new JSONArray(prefs.getString("journal", "[]")); }
        catch(Exception e){ entries = new JSONArray(); }
        JSONArray next = new JSONArray();
        String when = new SimpleDateFormat("dd.MM. HH:mm:ss", Locale.GERMANY).format(new Date());
        next.put(when + "  " + category + "  " + msg);
        for (int i=0; i < Math.min(entries.length(), 79); i++) next.put(entries.optString(i));
        prefs.edit().putString("journal", next.toString()).apply();
    }
    public synchronized String journal() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("journal", "[]"));
            StringBuilder b = new StringBuilder();
            for (int i=0;i<arr.length();i++) b.append(arr.optString(i)).append("\n\n");
            return b.toString();
        } catch(Exception e) { return "Noch keine Ereignisse"; }
    }
    public synchronized void provision(String vin, String ecu) throws Exception {
        vin = vin.trim().toUpperCase(Locale.ROOT);
        ecu = ecu.trim().toUpperCase(Locale.ROOT);
        checkScope(vin, ecu);
        String previous = state.optString("vin", "") + ":" + state.optString("ecu", "");
        if (!previous.equals(vin + ":" + ecu)) {
            state = new JSONObject();
            prefs.edit().remove("key_ecu").commit();
        }
        KeyPair ecuKey = keys("ecu");
        state.put("vin", vin); state.put("ecu", ecu);
        state.put("fingerprint", sha(ecuKey.getPublic().getEncoded()));
        state.remove("challenge"); state.remove("ecu_signature"); state.remove("grant");
        state.remove("grant_signature"); state.remove("package"); state.remove("package_signature");
        state.remove("grant_used"); state.remove("virtual_value"); state.remove("challenge_used");
        save();
        event("ECU", "Virtuelle ECU " + ecu + " registriert · " + vin);
    }
    public synchronized String challenge(String vin, String ecu, String payload) throws Exception {
        checkScope(vin, ecu);
        require(vin.equalsIgnoreCase(state.optString("vin")) && ecu.equalsIgnoreCase(state.optString("ecu")), "Zuerst passende Labor-ECU registrieren");
        require(payload != null && !payload.isEmpty(), "Codierungswert fehlt");
        JSONObject c = new JSONObject();
        c.put("format", "dynamicOS-lab-challenge/8");
        c.put("challenge_id", newId());
        c.put("vin", state.getString("vin")); c.put("ecu", state.getString("ecu"));
        c.put("job_hash", hash(payload)); c.put("client_nonce", nonce()); c.put("ecu_nonce", nonce());
        c.put("expires", System.currentTimeMillis() / 1000L + 120L);
        String signature = sign(keys("ecu"), c);
        require(verify(keys("ecu"), c, signature), "ECU-Signaturprüfung gescheitert");
        state.put("challenge", c); state.put("ecu_signature", signature);
        state.put("payload", payload);
        state.put("challenge_used", false);
        state.remove("grant"); state.remove("package"); state.remove("grant_used");
        save();
        event("CHALLENGE", "Signierte ECU-Challenge mit zwei Zufallsnonces");
        return c.getString("challenge_id");
    }
    public synchronized String issue() throws Exception {
        JSONObject c = state.optJSONObject("challenge");
        require(c != null, "Zuerst Challenge erstellen");
        require(!state.optBoolean("challenge_used"), "Challenge bereits verwendet");
        require(System.currentTimeMillis() / 1000L <= c.getLong("expires"), "Challenge abgelaufen");
        require(verify(keys("ecu"), c, state.optString("ecu_signature")), "ECU-Identität ungültig");
        JSONObject proof = new JSONObject(c.toString());
        proof.put("format", "dynamicOS-lab-tester-proof/8");
        String testerSig = sign(keys("tester"), proof);
        require(verify(keys("tester"), proof, testerSig), "Testerprüfung gescheitert");
        JSONObject grant = new JSONObject();
        grant.put("format", "dynamicOS-lab-grant/8");
        grant.put("grant_id", newId());
        grant.put("challenge_id", c.getString("challenge_id"));
        grant.put("vin", c.getString("vin")); grant.put("ecu", c.getString("ecu"));
        grant.put("job_hash", c.getString("job_hash"));
        grant.put("expires", System.currentTimeMillis() / 1000L + 180L);
        String sig = sign(keys("ecu"), grant);
        require(verify(keys("ecu"), grant, sig), "Laborfreigabe ungültig");
        state.put("grant", grant); state.put("grant_signature", sig);
        state.put("challenge_used", true); state.put("grant_used", false);
        save();
        event("TOKEN", "ED25519-Laborfreigabe an Auftrag und ECU gebunden");
        return grant.getString("grant_id");
    }
    public synchronized String signJob() throws Exception {
        JSONObject g = state.optJSONObject("grant");
        require(g != null, "Zuerst Token ausstellen");
        require(!state.optBoolean("grant_used"), "Token bereits verwendet");
        require(System.currentTimeMillis() / 1000L <= g.getLong("expires"), "Token abgelaufen");
        require(verify(keys("ecu"), g, state.optString("grant_signature")), "ECU-Token ungültig");
        require(hash(state.optString("payload")).equals(g.getString("job_hash")), "Auftragsbindung ungültig");
        JSONObject pkg = new JSONObject();
        pkg.put("format", "dynamicOS-lab-signed-coding/8");
        pkg.put("package_id", newId()); pkg.put("vin", g.getString("vin"));
        pkg.put("ecu", g.getString("ecu")); pkg.put("payload", state.getString("payload"));
        pkg.put("job_hash", g.getString("job_hash"));
        pkg.put("grant_id", g.getString("grant_id"));
        pkg.put("expires", Math.min(System.currentTimeMillis() / 1000L + 180L, g.getLong("expires")));
        String sig = sign(keys("signer"), pkg);
        state.put("package", pkg); state.put("package_signature", sig);
        save();
        event("SIGN", "Codierungsauftrag digital signiert");
        return pkg.getString("package_id");
    }
    private boolean verifyPackage(JSONObject pkg, String sig) throws Exception {
        JSONObject grant = state.optJSONObject("grant");
        return pkg != null && grant != null &&
            verify(keys("signer"), pkg, sig) &&
            verify(keys("ecu"), grant, state.optString("grant_signature")) &&
            pkg.optString("grant_id").equals(grant.optString("grant_id")) &&
            pkg.optString("job_hash").equals(grant.optString("job_hash")) &&
            pkg.optString("job_hash").equals(hash(pkg.optString("payload"))) &&
            pkg.optString("vin").equals(grant.optString("vin")) &&
            pkg.optString("ecu").equals(grant.optString("ecu")) &&
            System.currentTimeMillis() / 1000L <= pkg.optLong("expires") &&
            System.currentTimeMillis() / 1000L <= grant.optLong("expires");
    }
    public synchronized boolean verifyJob() throws Exception {
        require(state.optJSONObject("package") != null, "Zuerst Codierungsauftrag signieren");
        boolean ok = verifyPackage(state.getJSONObject("package"), state.optString("package_signature"));
        event("VERIFY", ok ? "Signatur, Scope, Ablaufzeit gültig" : "Prüfung abgewiesen");
        return ok;
    }
    public synchronized void apply() throws Exception {
        require(!state.optBoolean("grant_used"), "Replay blockiert: Token bereits verbraucht");
        JSONObject pkg = state.optJSONObject("package");
        require(verifyPackage(pkg, state.optString("package_signature")), "Paketprüfung oder Freigabe fehlgeschlagen");
        state.put("grant_used", true);
        state.put("virtual_value", pkg.getString("payload"));
        state.put("virtual_applied_at", System.currentTimeMillis());
        save();
        event("APPLY", "Nur virtuelle ECU hat Codierung gespeichert");
    }
    public synchronized boolean tamperTest() throws Exception {
        JSONObject pkg = state.optJSONObject("package");
        require(pkg != null, "Zuerst Auftrag signieren");
        JSONObject broken = new JSONObject(pkg.toString());
        broken.put("payload", pkg.getString("payload") + "-MANIPULIERT");
        boolean rejected = !verifyPackage(broken, state.optString("package_signature"));
        event("SECURITY", rejected ? "Manipulation korrekt abgewiesen" : "WARNUNG: Manipulation nicht erkannt");
        return rejected;
    }
    public synchronized boolean replayTest() {
        try {
            apply();
            event("SECURITY", "WARNUNG: Replay wurde angenommen");
            return false;
        } catch (Exception ex) {
            event("SECURITY", "Wiederverwendung korrekt abgewiesen");
            return true;
        }
    }
    public synchronized String fullTest(String vin, String ecu, String payload) throws Exception {
        provision(vin, ecu); challenge(vin, ecu, payload); issue(); signJob();
        require(verifyJob(), "Signaturprüfung fehlgeschlagen");
        require(tamperTest(), "Manipulation nicht erkannt");
        apply(); require(replayTest(), "Replay nicht erkannt");
        event("TEST", "8/8 Labor-Checks erfolgreich");
        return "8/8 Tests bestanden · Virtuelle ECU codiert";
    }
    public synchronized void reset() {
        state = new JSONObject();
        prefs.edit().remove("state").remove("key_ecu").commit();
        event("RESET", "Virtuelle ECU und Sitzung zurückgesetzt");
    }
    public synchronized String status() {
        StringBuilder s = new StringBuilder();
        s.append("Fahrzeug: ").append(state.optString("vin", "nicht registriert")).append("\n");
        s.append("Steuergerät: ").append(state.optString("ecu", "–")).append("\n");
        s.append("ECU-Schlüssel: ").append(state.has("fingerprint") ? state.optString("fingerprint").substring(0, 18) + "…" : "–").append("\n");
        s.append("Challenge: ").append(state.has("challenge") ? (state.optBoolean("challenge_used") ? "verwendet" : "ausgestellt") : "–").append("\n");
        s.append("Labor-Token: ").append(state.has("grant") ? (state.optBoolean("grant_used") ? "verbraucht" : "ausgestellt") : "–").append("\n");
        s.append("Signaturpaket: ").append(state.has("package") ? "vorhanden" : "–").append("\n");
        s.append("Virtuelle Codierung: ").append(state.optString("virtual_value", "–")).append("\n");
        s.append("VW-SFD2-Freigabe: NEIN\nFahrzeugverbindung: NEIN");
        return s.toString();
    }
    public synchronized String artifact() {
        JSONObject pkg = state.optJSONObject("package");
        if (pkg == null) return "Noch kein Signaturpaket vorhanden";
        return "{\n  \"package\": " + pkg.toString() + ",\n  \"signature\": \"" + state.optString("package_signature") + "\",\n  \"lab_only\": true,\n  \"vw_sfd2_unlocked\": false\n}";
    }
    public synchronized int progress() {
        int p = 0; if(state.has("fingerprint"))p++;
        if(state.has("challenge"))p++; if(state.has("grant"))p++;
        if(state.has("package"))p++; if(state.has("virtual_value"))p++;
        return p;
    }
    public synchronized String fingerprint() { return state.optString("fingerprint","–"); }
}

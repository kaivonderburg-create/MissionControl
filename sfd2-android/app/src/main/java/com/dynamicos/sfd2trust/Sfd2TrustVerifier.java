package com.dynamicos.sfd2trust;

import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Local, generic X.509 and JWS verifier for research.
 * Not the proprietary SFD2 VW token format, not an OEM signer,
 * and never evidence of an ECU accepting an unlock.
 */
public final class Sfd2TrustVerifier {
    private X509Certificate trustedAnchor;
    private final List<X509Certificate> certificates = new ArrayList<>();
    private X509Certificate signer;
    private String compactJws = "";
    private String rootFingerprint = "nicht geladen";
    private String leafFingerprint = "nicht geladen";
    private String parsedClaims = "–";
    private String lastVerdict = "Nicht geprüft";
    private String pathSummary = "–";
    private boolean pathValid = false;
    private boolean signatureValid = false;
    private boolean claimsValid = false;
    private String expectedVin = "";
    private String expectedEcu = "";
    private String expectedChallenge = "";

    private static byte[] decode(String s) { return Base64.decode(s, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING); }
    private static String fingerprint(byte[] bytes) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b: hash) sb.append(String.format(Locale.ROOT,"%02X",b & 255));
        return sb.toString();
    }
    private static void check(boolean valid,String why) throws Exception {
        if (!valid) throw new SecurityException(why);
    }
    @SuppressWarnings("unchecked")
    public String loadRoot(byte[] data) throws Exception {
        CertificateFactory f = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> all = f.generateCertificates(new ByteArrayInputStream(data));
        check(all.size() == 1,"Nur ein Trust-Anchor-Zertifikat pro Datei zulässig.");
        Certificate one = all.iterator().next();
        check(one instanceof X509Certificate,"Kein X.509-Zertifikat.");
        trustedAnchor=(X509Certificate)one;
        trustedAnchor.checkValidity(new Date());
        check(trustedAnchor.getBasicConstraints() >= 0,"Das ausgewählte Zertifikat ist keine CA.");
        boolean[] usages=trustedAnchor.getKeyUsage();
        if(usages!=null)check(usages.length>5 && usages[5],"CA hat keine keyCertSign-Berechtigung.");
        rootFingerprint=fingerprint(trustedAnchor.getEncoded());
        resetResults();
        return "Benutzerdefinierter Vertrauensanker gespeichert · SHA256 "+rootFingerprint.substring(0,19)+"…";
    }
    public String loadChain(byte[] data) throws Exception {
        CertificateFactory f=CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> all=f.generateCertificates(new ByteArrayInputStream(data));
        check(!all.isEmpty(),"Zertifikatsdatei leer.");
        check(all.size()<=12,"Maximal 12 Zertifikate im Paket.");
        certificates.clear();
        for(Certificate cert: all) {
            check(cert instanceof X509Certificate,"Ungültiges X.509-Zertifikat.");
            certificates.add((X509Certificate)cert);
        }
        signer=null;
        for(X509Certificate c:certificates) {
            if(c.getBasicConstraints() < 0){signer=c;break;}
        }
        check(signer!=null,"Kein Endentitäts-/Signaturzertifikat erkannt.");
        leafFingerprint=fingerprint(signer.getEncoded());
        resetResults();
        return "Signaturzertifikat geladen · "+certificates.size()+" Zertifikat(e)";
    }
    public String loadJws(byte[] bytes) throws Exception {
        check(bytes.length>0 && bytes.length <=256000,"JWS-Datei leer oder zu groß.");
        compactJws=new String(bytes,StandardCharsets.UTF_8).trim();
        String[] parts=compactJws.split("\\.",-1);
        check(parts.length==3,"Erwartet wird eine kompakte JWS (Header.Payload.Signatur).");
        check(parts[0].length()<32000 && parts[1].length()<128000 && parts[2].length()<128000,
            "JWS-Segment zu groß.");
        JSONObject header=new JSONObject(new String(decode(parts[0]),StandardCharsets.UTF_8));
        String alg=header.optString("alg","");
        check(alg.equals("RS256") || alg.equals("PS256") || alg.equals("ES256") || alg.equals("EdDSA"),
            "Nur RS256, PS256, ES256 und EdDSA zugelassen.");
        check(!header.has("jku") && !header.has("jwk") && !header.has("x5u") &&
              !header.has("crit") && !header.has("b64"),
            "Unsichere externe Schlüsselreferenzen und kritische Erweiterungen sind gesperrt.");
        parsedClaims=new String(decode(parts[1]),StandardCharsets.UTF_8);
        new JSONObject(parsedClaims);
        resetResults();
        return "Signierter Token-Beleg geladen · "+alg+" · generisches JWS-Format";
    }
    public void setExpected(String vin,String ecu,String challenge){
        expectedVin=vin==null?"":vin.trim().toUpperCase(Locale.ROOT);
        expectedEcu=ecu==null?"":ecu.trim().toUpperCase(Locale.ROOT);
        expectedChallenge=challenge==null?"":challenge.trim();
    }
    public String verify() {
        resetResults();
        try{
            check(trustedAnchor!=null,"Vertrauensanker fehlt.");
            check(signer!=null,"Signaturzertifikat fehlt.");
            check(!compactJws.isEmpty(),"Token-Beleg fehlt.");
            trustedAnchor.checkValidity();
            signer.checkValidity();
            boolean[] usage=signer.getKeyUsage();
            if(usage!=null)check(usage.length>0 && usage[0],"Zertifikat erlaubt keine digitalen Signaturen.");
            PKIXBuilderParameters params=new PKIXBuilderParameters(
                Collections.singleton(new TrustAnchor(trustedAnchor,null)),
                new X509CertSelector(){{
                    setCertificate(signer);
                }}
            );
            params.setMaxPathLength(8);
            params.addCertStore(CertStore.getInstance("Collection",
                new CollectionCertStoreParameters(certificates)));
            // Offline: CRL/OCSP not queried. This is deliberately NOT full OEM assurance.
            params.setRevocationEnabled(false);
            PKIXCertPathBuilderResult result=(PKIXCertPathBuilderResult)CertPathBuilder
                .getInstance("PKIX").build(params);
            pathValid=true;
            pathSummary="PKIX-Pfad gültig ("+result.getCertPath().getCertificates().size()+
                " Zertifikate) · Sperrstatus offline NICHT geprüft.";
            String[] parts=compactJws.split("\\.",-1);
            JSONObject header=new JSONObject(new String(decode(parts[0]),StandardCharsets.UTF_8));
            String alg=header.getString("alg");
            byte[] message=(parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII);
            byte[] sig=decode(parts[2]);
            PublicKey key=signer.getPublicKey();
            String algorithm;
            if(alg.equals("RS256")){
                check("RSA".equalsIgnoreCase(key.getAlgorithm()),"Signatur/Zertifikat-Algorithmus passt nicht.");
                algorithm="SHA256withRSA";
            }else if(alg.equals("PS256")){
                check("RSA".equalsIgnoreCase(key.getAlgorithm()),"Signatur/Zertifikat-Algorithmus passt nicht.");
                algorithm="RSASSA-PSS";
            }else if(alg.equals("ES256")){
                check("EC".equalsIgnoreCase(key.getAlgorithm()),"Signatur/Zertifikat-Algorithmus passt nicht.");
                algorithm="SHA256withECDSA";
                sig=joseToDer(sig);
            }else{
                check("Ed25519".equalsIgnoreCase(key.getAlgorithm()) ||
                    "EdDSA".equalsIgnoreCase(key.getAlgorithm()),
                    "Signatur/Zertifikat-Algorithmus passt nicht.");
                algorithm="Ed25519";
            }
            Signature verifier=Signature.getInstance(algorithm);
            if(alg.equals("PS256")){
                verifier.setParameter(new java.security.spec.PSSParameterSpec(
                    "SHA-256","MGF1",java.security.spec.MGF1ParameterSpec.SHA256,32,1));
            }
            verifier.initVerify(key);
            verifier.update(message);
            check(verifier.verify(sig),"Kryptografische JWS-Signatur ungültig.");
            signatureValid=true;
            JSONObject claims=new JSONObject(new String(decode(parts[1]),StandardCharsets.UTF_8));
            long now=System.currentTimeMillis()/1000;
            check(claims.has("exp"),"Ablaufzeit (exp) fehlt.");
            check(claims.getLong("exp")>=now,"Token abgelaufen.");
            check(claims.getLong("exp")<=now+24*3600,"Token-Gültigkeit über 24 Stunden nicht zulässig.");
            if(claims.has("nbf"))check(claims.getLong("nbf")<=now,"Token noch nicht gültig.");
            check(!expectedVin.isEmpty()&&!expectedEcu.isEmpty()&&!expectedChallenge.isEmpty(),
                "FIN, ECU und Challenge müssen für die Bindungsprüfung vorgegeben sein.");
            check(expectedVin.equalsIgnoreCase(claims.optString("vin","")),"FIN-Bindung ungültig.");
            check(expectedEcu.equalsIgnoreCase(claims.optString("ecu","")),"ECU-Bindung ungültig.");
            check(expectedChallenge.equals(claims.optString("challenge","")),"Challenge-Bindung ungültig.");
            claimsValid=true;
            lastVerdict="LOKALE KRYPTOPRÜFUNG BESTANDEN. Keine VW-OEM- oder ECU-Freigabe nachgewiesen.";
        }catch(Exception ex){
            lastVerdict="ABGELEHNT: "+ex.getMessage();
        }
        return lastVerdict;
    }
    private void resetResults(){
        pathValid=false;signatureValid=false;claimsValid=false;pathSummary="–";lastVerdict="Nicht geprüft";
    }
    private static byte[] derInt(byte[] src,int start) {
        int end=start+32;
        while(start<end-1 && src[start]==0)start++;
        int len=end-start;
        boolean prefix=(src[start]&0x80)!=0;
        byte[] out=new byte[2+len+(prefix?1:0)];
        out[0]=0x02;out[1]=(byte)(len+(prefix?1:0));
        if(prefix)System.arraycopy(src,start,out,3,len);
        else System.arraycopy(src,start,out,2,len);
        return out;
    }
    private static byte[] joseToDer(byte[] signature) throws Exception {
        check(signature.length==64,"ES256 erwartet genau 64 Byte JWS-Signatur.");
        byte[] r=derInt(signature,0),s=derInt(signature,32);
        int length=r.length+s.length;
        check(length<128,"Ungültige ECDSA-Signaturlänge.");
        byte[] der=new byte[length+2];
        der[0]=0x30;der[1]=(byte)length;
        System.arraycopy(r,0,der,2,r.length);
        System.arraycopy(s,0,der,2+r.length,s.length);
        return der;
    }
    public String summary(){
        return "VERTRAUENSANKER (vom Benutzer importiert)\n"+rootFingerprint+"\n\n"+
          "SIGNATURZERTIFIKAT\n"+leafFingerprint+"\n\n"+
          "KETTENPRÜFUNG: "+(pathValid?"OK (ohne CRL/OCSP)":"NICHT BESTÄTIGT")+"\n"+
          "TOKEN-SIGNATUR: "+(signatureValid?"OK":"NICHT BESTÄTIGT")+"\n"+
          "FIN/ECU/CHALLENGE: "+(claimsValid?"OK":"NICHT BESTÄTIGT")+"\n"+
          "VW-OEM-VERTRAUEN: NICHT BESTÄTIGT\n"+
          "ECU-ENTSPERRUNG: NICHT BESTÄTIGT\n\n"+
          "HINWEIS: Der importierte Trust-Anchor ist kein nachgewiesener VW-Schlüssel.\n"+
          pathSummary+"\n\n"+lastVerdict;
    }
    public String payloadPreview(){
        if(parsedClaims.length()>3000)return parsedClaims.substring(0,3000)+"…";
        return parsedClaims;
    }
}

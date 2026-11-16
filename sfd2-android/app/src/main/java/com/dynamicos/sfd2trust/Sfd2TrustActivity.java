package com.dynamicos.sfd2trust;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Native, SFD2-only trust research UI. No gateway/OBD transport and no OEM keys. */
public final class Sfd2TrustActivity extends Activity {
    private static final int PICK_ROOT=201, PICK_CHAIN=202, PICK_JWS=203;
    private final int bg=Color.rgb(8,13,22);
    private final int panel=Color.rgb(17,27,42);
    private final int fieldBg=Color.rgb(26,41,61);
    private final int edge=Color.rgb(45,67,87);
    private final int fg=Color.rgb(237,243,255);
    private final int muted=Color.rgb(152,168,189);
    private final int cyan=Color.rgb(92,189,250);
    private final int amber=Color.rgb(255,193,109);
    private final int good=Color.rgb(101,229,176);
    private LinearLayout root,content,bottom;
    private Sfd2TrustVerifier trust;
    private SharedPreferences prefs;
    private EditText vin,ecu,challenge;
    private String expectedVin="",expectedEcu="17",expectedChallenge="";
    private String message="SFD2-Vertrauensnachweis nicht geladen";
    private int screen=0;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        prefs=getSharedPreferences("sfd2_trust_v9",MODE_PRIVATE);
        trust=new Sfd2TrustVerifier();
        expectedVin=prefs.getString("vin","");
        expectedEcu=prefs.getString("ecu","17");
        expectedChallenge=prefs.getString("challenge","");
        restoreCerts();
        draw();
    }
    private void restoreCerts(){
        try {
            String anchor=prefs.getString("anchor","");
            if(!anchor.isEmpty())trust.loadRoot(Base64.decode(anchor,Base64.DEFAULT));
        }catch(Exception e){message="Gespeicherte CA ungültig: "+e.getMessage();}
        try {
            String cert=prefs.getString("chain","");
            if(!cert.isEmpty())trust.loadChain(Base64.decode(cert,Base64.DEFAULT));
        }catch(Exception e){message="Gespeicherte Zertifikatskette ungültig: "+e.getMessage();}
    }
    private int d(float v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
    private GradientDrawable shape(int color,int radius,int outline){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(d(radius));
        if(outline!=0)g.setStroke(d(1),outline);return g;
    }
    private LinearLayout box(boolean row){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(row?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        if(row)l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private TextView txt(String v,int sp,int color,boolean bold){
        TextView t=new TextView(this);t.setText(v);t.setTextSize(sp);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);return t;
    }
    private void add(LinearLayout parent,View v,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=d(top);parent.addView(v,p);
    }
    private void setPadding(View v,int h,int vert){v.setPadding(d(h),d(vert),d(h),d(vert));}
    private LinearLayout block(String head,String body){
        LinearLayout l=box(false);
        setPadding(l,16,13);l.setBackground(shape(panel,17,edge));
        add(content,l,12);
        if(head!=null)add(l,txt(head,15,fg,true),0);
        if(body!=null)add(l,txt(body,12,muted,false),5);
        return l;
    }
    private TextView button(String value,boolean primary,Runnable action){
        TextView b=txt(value,13,primary?bg:fg,true);
        b.setGravity(Gravity.CENTER);
        setPadding(b,12,11);b.setBackground(shape(primary?cyan:fieldBg,11,primary?0:edge));
        b.setMinHeight(d(44));b.setOnClickListener(v->action.run());
        return b;
    }
    private void buttons(LinearLayout parent,String a,Runnable ra,String b,Runnable rb){
        LinearLayout r=box(true);LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,d(47),1);
        r.addView(button(a,true,ra),p1);
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,d(47),1);p2.leftMargin=d(8);
        r.addView(button(b,false,rb),p2);add(parent,r,11);
    }
    private EditText input(String value,String hint){
        EditText e=new EditText(this);
        e.setText(value);e.setHint(hint);e.setSingleLine(true);
        e.setTextSize(13);e.setTextColor(fg);e.setHintTextColor(muted);
        setPadding(e,11,8);e.setBackground(shape(fieldBg,9,edge));
        return e;
    }
    private void capture(){
        if(vin!=null)expectedVin=vin.getText().toString().trim().toUpperCase(Locale.ROOT);
        if(ecu!=null)expectedEcu=ecu.getText().toString().trim().toUpperCase(Locale.ROOT);
        if(challenge!=null)expectedChallenge=challenge.getText().toString().trim();
        prefs.edit().putString("vin",expectedVin).putString("ecu",expectedEcu)
            .putString("challenge",expectedChallenge).apply();
    }
    private void draw(){
        vin=null;ecu=null;challenge=null;
        root=box(false);root.setBackgroundColor(bg);setContentView(root);
        LinearLayout h=box(true);setPadding(h,17,15);
        LinearLayout title=box(false);
        add(title,txt("dynamicOS",22,fg,true),0);
        add(title,txt("VW SERVICE  /  SFD2 TRUST CENTER 9.0",10,cyan,true),2);
        h.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=txt("●  LOCAL",11,good,true);
        setPadding(badge,9,8);badge.setBackground(shape(fieldBg,99,edge));
        h.addView(badge,new LinearLayout.LayoutParams(-2,-2));
        root.addView(h,new LinearLayout.LayoutParams(-1,-2));
        TextView caution=txt("KEINE VW-OEM-FREIGABE · ECU NICHT VERBUNDEN",10,amber,true);
        caution.setGravity(Gravity.CENTER);setPadding(caution,5,9);
        caution.setBackgroundColor(Color.rgb(51,36,29));root.addView(caution,new LinearLayout.LayoutParams(-1,-2));
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        content=box(false);setPadding(content,14,10);
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        bottom=box(true);setPadding(bottom,8,8);bottom.setBackgroundColor(panel);
        String[] tabs={"VERTRAUEN","TOKEN","ECU","LABOR"};
        String[] icons={"◇","⌁","◉","⌘"};
        for(int i=0;i<tabs.length;i++){
            final int target=i;
            LinearLayout entry=box(false);entry.setGravity(Gravity.CENTER);
            setPadding(entry,4,7);
            if(screen==i)entry.setBackground(shape(fieldBg,11,0));
            TextView icon=txt(icons[i],22,screen==i?cyan:muted,true);icon.setGravity(Gravity.CENTER);
            add(entry,icon,0);
            TextView t=txt(tabs[i],10,screen==i?fg:muted,true);t.setGravity(Gravity.CENTER);
            add(entry,t,0);
            entry.setOnClickListener(v->{capture();screen=target;draw();});
            bottom.addView(entry,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(bottom,new LinearLayout.LayoutParams(-1,-2));
        if(screen==0)trustScreen();
        else if(screen==1)tokenScreen();
        else if(screen==2)ecuScreen();
        else labScreen();
    }
    private void headline(String chapter,String h,String explanation){
        add(content,txt(chapter,11,cyan,true),12);
        add(content,txt(h,23,fg,true),4);
        add(content,txt(explanation,12,muted,false),7);
    }
    private void trustScreen(){
        headline("SFD2 / TRUST ENGINE","Vertrauenskette","X.509-Zertifikate importieren und kryptografisch nach PKIX prüfen.");
        LinearLayout status=block("Sicherheitsstatus",message);
        add(status,txt("Eigener Vertrauensanker ≠ Volkswagen-Vertrauensnachweis",12,amber,true),10);
        LinearLayout rootCard=block("01  Vertrauensanker","Ausgewähltes CA-Zertifikat (PEM oder DER). Nur für lokale Prüfungen.");
        add(rootCard,button("CA-Zertifikat importieren",true,()->pick(PICK_ROOT)),10);
        LinearLayout certCard=block("02  Zertifikatskette","Signaturzertifikat und gegebenenfalls Zwischenzertifikate (PEM/DER).");
        add(certCard,button("Zertifikatspaket importieren",true,()->pick(PICK_CHAIN)),10);
        LinearLayout checks=block("Vertrauensprüfung","Zertifikatspfad, Gültigkeit, Key-Usage und Signaturprüfung getrennt.");
        TextView outcome=txt(trust.summary(),11,fg,false);outcome.setTextIsSelectable(true);
        add(checks,outcome,10);
    }
    private void tokenScreen(){
        headline("SFD2 / SIGNATURE","Token-Prüfstand","Signatur und Bindung eines importierten Nachweises prüfen.");
        LinearLayout note=block("Unterstütztes Forschungsformat",
            "Generische kompakte JWS mit RS256, PS256, ES256 oder EdDSA. Das ist NICHT das dokumentierte VW-SFD2-Tokenformat.");
        add(note,button("Signierten Token-Beleg importieren",true,()->pick(PICK_JWS)),12);
        LinearLayout bind=block("03  Challenge-Bindung","Diese Werte müssen mit den signierten Claims vin, ecu und challenge übereinstimmen.");
        vin=input(expectedVin,"FIN (17 Zeichen)");add(bind,txt("FIN",11,muted,true),10);add(bind,vin,5);
        ecu=input(expectedEcu,"Steuergerät, z. B. 17");add(bind,txt("ECU-ADRESSE",11,muted,true),9);add(bind,ecu,5);
        challenge=input(expectedChallenge,"Original-Challenge");
        add(bind,txt("ECU-CHALLENGE",11,muted,true),9);add(bind,challenge,5);
        add(bind,button("Kette + Signatur + Bindung prüfen",true,()->{
            capture();trust.setExpected(expectedVin,expectedEcu,expectedChallenge);
            message=trust.verify();draw();
        }),12);
        LinearLayout result=block("Prüfprotokoll",message);
        TextView v=txt(trust.summary(),11,fg,false);
        v.setTextIsSelectable(true);add(result,v,10);
        LinearLayout claims=block("Importierte Token-Nutzdaten","Nur Anzeige, keine automatische Freigabe.");
        TextView p=txt(trust.payloadPreview(),11,muted,false);
        p.setTextIsSelectable(true);add(claims,p,9);
    }
    private void ecuScreen(){
        headline("SFD2 / ECU ACCEPTANCE","Freigabestatus",
            "Eine lokale erfolgreiche Signaturprüfung beweist keine Freigabe im Fahrzeug.");
        LinearLayout indicators=block("Entscheidungskette","Nur ein positiver, überprüfbarer ECU-Nachweis darf den Freigabestatus ändern.");
        add(indicators,txt("① Zertifikatskette: lokal prüfbar\n② Token-Signatur: lokal prüfbar\n③ ECU-Challenge: lokal vergleichbar\n④ OEM-Schlüsselvertrauen: NICHT BELEGT\n⑤ Fahrzeugantwort: NICHT VORHANDEN",13,fg,false),13);
        LinearLayout limitation=block("ECU-Ergebnis","Keine SFD2-geschützte Codierung wurde an ein Fahrzeug gesendet.");
        add(limitation,txt("SFD2 ENTSPERRT: NEIN\nAUTO-UNLOCK: NICHT VERFÜGBAR\nFAHRZEUGKOMMUNIKATION: NICHT INTEGRIERT",12,amber,true),12);
        add(limitation,txt("Für eine reale Freigabe benötigt die ECU einen eigenen gültigen Vertrauensnachweis. Die App besitzt keine VW-Hersteller-Signaturschlüssel.",12,muted,false),12);
    }
    private void labScreen(){
        headline("SFD2 / CRYPTO LAB","Token-Labor",
            "Lokaler Signierer für selbst kontrollierte virtuelle ECUs.");
        LinearLayout cards=block("Eigener Signierer","Ed25519, Einmal-Nonces, Challenge-Response, Auftragsbindung und Replay-Schutz sind in der vorhandenen v8-Engine implementiert.");
        add(cards,button("Kryptografie-Labor öffnen",true,()->startActivity(new Intent(this,MainActivity.class))),12);
        LinearLayout warning=block("Vertrauensgrenze","Ein selbst signierter Labor-Token funktioniert nur, wenn die Test-ECU unserem Schlüssel vertraut.");
        add(warning,txt("Keine echte VW-SFD2-Entsperrung. Kein Herstellersignatur-Generator.",12,amber,true),9);
    }
    private void pick(int code){
        capture();
        try{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,code);
        }catch(Exception e){message="Dateiauswahl nicht verfügbar: "+e.getMessage();draw();}
    }
    @Override protected void onActivityResult(int code,int result,Intent data){
        super.onActivityResult(code,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null)return;
        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=getContentResolver().openInputStream(data.getData())){
                if(in==null)throw new IllegalStateException("Datei nicht lesbar");
                byte[] b=new byte[8192];int n;int max=1024*1024;
                while((n=in.read(b))!=-1){
                    if(out.size()+n>max)throw new SecurityException("Maximale Dateigröße 1 MB");
                    out.write(b,0,n);
                }
            }
            byte[] bytes=out.toByteArray();
            if(code==PICK_ROOT){
                message=trust.loadRoot(bytes);
                prefs.edit().putString("anchor",Base64.encodeToString(bytes,Base64.NO_WRAP)).apply();
            }else if(code==PICK_CHAIN){
                message=trust.loadChain(bytes);
                prefs.edit().putString("chain",Base64.encodeToString(bytes,Base64.NO_WRAP)).apply();
            }else if(code==PICK_JWS){
                message=trust.loadJws(bytes);
            }
        }catch(Exception e){message="Import abgelehnt: "+e.getMessage();}
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();draw();
    }
}

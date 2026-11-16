package com.dynamicos.sfd2trust;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(9,14,19);
    private final int PANEL = Color.rgb(20,29,37);
    private final int SUB = Color.rgb(26,37,46);
    private final int BLUE = Color.rgb(151,207,225);
    private final int MUTE = Color.rgb(155,174,184);
    private final int WHITE = Color.rgb(236,243,246);
    private final int GREEN = Color.rgb(145,207,181);
    private final int BORDER = Color.rgb(44,58,68);
    private LabEngine lab;
    private LinearLayout root, work, nav;
    private TextView statusPill;
    private EditText vinInput, ecuInput, payloadInput;
    private int page = 0;
    private String vin = "WVWZZZ1KZ6W000001", ecu = "17", payload = "GURTWARNUNG=AKTIV";
    private String headline = "Bereit zur lokalen Laborprüfung";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        try { lab = new LabEngine(this); } catch(Exception e) {
            TextView fatal=new TextView(this); fatal.setText("Krypto-Engine konnte nicht starten:\n"+e.getMessage());
            fatal.setTextColor(Color.WHITE); fatal.setPadding(30,70,30,30); fatal.setBackgroundColor(BG);
            setContentView(fatal); return;
        }
        render();
    }
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private GradientDrawable shape(int fill, int radius, int stroke) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fill);gd.setCornerRadius(dp(radius));
        if(stroke != 0) gd.setStroke(dp(1), stroke);
        return gd;
    }
    private TextView label(String value,int sp,int color,boolean bold) {
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL)); t.setIncludeFontPadding(false);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }
    private LinearLayout vertical() {
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private LinearLayout horizontal() {
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;
    }
    private void pad(View v,int x,int y) {v.setPadding(dp(x),dp(y),dp(x),dp(y));}
    private LinearLayout.LayoutParams lp(int w,int h) {return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void add(LinearLayout l,View v,int top) {
        LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(top);l.addView(v,p);
    }
    private LinearLayout card(LinearLayout parent,String title,String subtitle) {
        LinearLayout c=vertical();pad(c,16,15);c.setBackground(shape(PANEL,17,BORDER));
        add(parent,c,12);
        if(title!=null){add(c,label(title,15,WHITE,true),0);}
        if(subtitle!=null){add(c,label(subtitle,11,MUTE,false),4);}
        return c;
    }
    private TextView action(String text,boolean primary,Runnable run) {
        TextView t=label(text,13,primary?BG:WHITE,true);
        t.setGravity(Gravity.CENTER);t.setMinHeight(dp(45));pad(t,12,9);
        t.setBackground(shape(primary?BLUE:SUB,11,primary?0:BORDER));
        t.setOnClickListener(v->run.run());
        return t;
    }
    private void buttonRow(LinearLayout c,String a,Runnable ra,String b,Runnable rb){
        LinearLayout line=horizontal();
        TextView aa=action(a,true,ra),bb=action(b,false,rb);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);
        line.addView(aa,p);
        LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,dp(44),1);p2.leftMargin=dp(8);
        line.addView(bb,p2);add(c,line,10);
    }
    private EditText field(String value,String hint,boolean multiline) {
        EditText e=new EditText(this);
        e.setText(value);e.setTextColor(WHITE);e.setHintTextColor(MUTE);
        e.setTextSize(14);e.setHint(hint);e.setSingleLine(!multiline);
        if(multiline){e.setMinLines(2);e.setMaxLines(4);}
        e.setBackground(shape(SUB,10,BORDER));
        e.setPadding(dp(12),dp(9),dp(12),dp(9));e.setSelectAllOnFocus(false);
        return e;
    }
    private void formField(LinearLayout panel,String caption,EditText input){
        add(panel,label(caption,11,MUTE,true),11);add(panel,input,5);
    }
    private void remember(){
        if(vinInput!=null)vin=vinInput.getText().toString().trim().toUpperCase();
        if(ecuInput!=null)ecu=ecuInput.getText().toString().trim().toUpperCase();
        if(payloadInput!=null)payload=payloadInput.getText().toString();
    }
    private void process(String success,Throwing action){
        remember();
        try { action.exec();headline=success;Toast.makeText(this,success,Toast.LENGTH_SHORT).show(); }
        catch(Exception e){headline="FEHLER: "+e.getMessage();lab.event("REJECT",e.getMessage());
            Toast.makeText(this,headline,Toast.LENGTH_LONG).show();}
        render();
    }
    private interface Throwing { void exec() throws Exception; }
    private void switchPage(int p){remember();page=p;render();}
    private void render(){
        vinInput=ecuInput=payloadInput=null;
        root=vertical();root.setBackgroundColor(BG);
        setContentView(root);
        LinearLayout header=horizontal();pad(header,18,13);
        LinearLayout title=vertical();
        add(title,label("dynamicOS",22,WHITE,true),0);
        TextView sub=label("VW SERVICE  /  TOKEN LAB 9.1",10,BLUE,true);sub.setLetterSpacing(.11f);add(title,sub,5);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        statusPill=label("ISOLIERT",11,GREEN,true);
        statusPill.setGravity(Gravity.CENTER);pad(statusPill,11,8);statusPill.setBackground(shape(SUB,99,BORDER));
        header.addView(statusPill,lp(-2,-2));root.addView(header,lp(-1,-2));

        LinearLayout banner=horizontal();pad(banner,13,7);banner.setBackgroundColor(Color.rgb(24,33,40));
        TextView info=label("VIRTUELLE ECU     /     KEINE HERSTELLERFREIGABE",10,MUTE,true);
        info.setGravity(Gravity.CENTER);banner.addView(info,lp(-1,-2));root.addView(banner,lp(-1,-2));

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        work=vertical();pad(work,18,9);scroll.addView(work);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        nav=horizontal();pad(nav,8,9);nav.setBackgroundColor(PANEL);
        String[] names={"Übersicht","Signieren","Prüfen","Journal"};
        String[] ico={"◇","⌁","✓","≡"};
        for(int i=0;i<names.length;i++){
            final int to=i;
            LinearLayout item=vertical();item.setGravity(Gravity.CENTER);pad(item,4,8);
            if(page==i)item.setBackground(shape(SUB,11,0));
            TextView glyph=label(ico[i],21,page==i?BLUE:MUTE,true);glyph.setGravity(Gravity.CENTER);
            item.addView(glyph,lp(-1,-2));
            TextView t=label(names[i],10,page==i?WHITE:MUTE,page==i);t.setGravity(Gravity.CENTER);item.addView(t,lp(-1,-2));
            item.setOnClickListener(v->switchPage(to));
            nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(nav,lp(-1,-2));
        if(page==0)dashboard();
        else if(page==1)signer();
        else if(page==2)verifyPage();
        else journal();
    }
    private void sectionHead(String eyebrow,String title,String lead){
        TextView eyebrowView=label(eyebrow,10,BLUE,true);eyebrowView.setLetterSpacing(.13f);add(work,eyebrowView,16);
        add(work,label(title,26,WHITE,true),8);
        add(work,label(lead,13,MUTE,false),9);
    }
    private void dashboard(){
        sectionHead("DYNAMICOS  /  LABOR","Trust Workspace","Kryptografische Authentifizierung für virtuelle Steuergeräte.");
        LinearLayout status=card(work,"Systemstatus","Der Signaturdienst wird direkt auf deinem Gerät ausgeführt.");
        TextView ok=label("●  Ed25519 Engine aktiv    ·    Android Keystore aktiv",12,GREEN,true);
        add(status,ok,12);
        TextView note=label(headline,12,WHITE,false);add(status,note,9);
        LinearLayout progress=card(work,"Workflow","Fünf sichere Schritte vom Teststeuergerät bis zur gespeicherten Laborcodierung.");
        TextView count=label(lab.progress()+"/5  Schritte abgeschlossen",25,BLUE,true);
        add(progress,count,9);
        TextView bar=label("ECU   /   Challenge   /   Token   /   Signatur   /   Anwendung",11,MUTE,false);add(progress,bar,8);
        buttonRow(progress,"Kompletten Test",()->process("Alle 8 Laborprüfungen erfolgreich",()->lab.fullTest(vin,ecu,payload)),
                "Signaturstudio",()->switchPage(1));
        LinearLayout state=card(work,"Virtuelles Steuergerät","Lokale Sitzungsdaten, keine Verbindung mit einem Fahrzeug.");
        TextView data=label(lab.status(),12,WHITE,false);data.setTextIsSelectable(true);data.setLineSpacing(dp(3),1);
        add(state,data,10);
        LinearLayout warn=card(work,"Vertrauensgrenze","Eigene Testschlüssel werden NICHT von realen VW-Steuergeräten anerkannt.");
        add(warn,label("SFD2 Fahrzeugfreigabe: NICHT VORHANDEN",12,Color.rgb(224,187,137),true),7);
    }
    private void signer(){
        sectionHead("LABOR  /  SIGNATURE","Signaturstudio","Alle Schritte laufen lokal – ohne Browser und ohne Server.");
        LinearLayout fields=card(work,"Auftragsdaten","FIN und ECU sind ausschließlich Kennungen für die virtuelle Labor-ECU.");
        vinInput=field(vin,"17-stellige FIN",false);
        ecuInput=field(ecu,"ECU z. B. 17",false);
        payloadInput=field(payload,"Codierungswert",true);
        formField(fields,"FIN / TESTFAHRZEUG",vinInput);
        formField(fields,"STEUERGERÄT (HEX)",ecuInput);
        formField(fields,"CODIERUNG / AUFTRAG",payloadInput);
        LinearLayout stages=card(work,"Freigabeablauf","Einmal-Token, Nonces, Scope- und Signaturprüfung.");
        buttonRow(stages,"1  ECU registrieren",()->process("Virtuelle ECU registriert",()->lab.provision(vin,ecu)),
                  "2  Challenge",()->process("ECU-Challenge signiert",()->lab.challenge(vin,ecu,payload)));
        buttonRow(stages,"3  Token ausstellen",()->process("Labor-Token ausgestellt",()->lab.issue()),
                  "4  Auftrag signieren",()->process("Codierung signiert",()->lab.signJob()));
        buttonRow(stages,"5  Virtuell anwenden",()->process("Virtuelle ECU hat Auftrag angenommen",()->lab.apply()),
                  "Gesamttest  8/8",()->process("Alle 8 Laborprüfungen erfolgreich",()->lab.fullTest(vin,ecu,payload)));
        add(stages,label("Jede Freigabe gilt nur einmal und ist an den SHA-256-Auftrag gebunden.",11,MUTE,false),11);
        LinearLayout result=card(work,"Aktueller Zustand",null);
        TextView v=label(lab.status(),12,WHITE,false);v.setTextIsSelectable(true);add(result,v,9);
    }
    private void verifyPage(){
        sectionHead("SICHERHEITSPRÜFUNG","Prüfstand","Kryptografische Prüfung, Manipulation und Replay.");
        LinearLayout test=card(work,"Sicherheitsprüfungen","Die beiden negativen Tests müssen ausdrücklich abgelehnt werden.");
        buttonRow(test,"Signatur prüfen",()->process("Signaturprüfung abgeschlossen",()->{
                     if(!lab.verifyJob())throw new IllegalStateException("Signatur ungültig");}),
                  "Manipulationstest",()->process("Manipulation blockiert",()->{
                     if(!lab.tamperTest())throw new IllegalStateException("Manipulation nicht erkannt");}));
        buttonRow(test,"Replay-Test",()->process("Replay blockiert",()->{
                     if(!lab.replayTest())throw new IllegalStateException("Replay wurde angenommen");}),
                  "JSON exportieren",this::shareArtifact);
        LinearLayout pkg=card(work,"Signiertes Auftragspaket","Nur das Laborformat dynamicOS-lab-signed-coding/8.");
        TextView v=label(lab.artifact(),11,WHITE,false);v.setTextIsSelectable(true);add(pkg,v,10);
        LinearLayout trust=card(work,"Gültigkeitsbereich","Kryptografisch gültig bedeutet nicht OEM-autorisiert.");
        add(trust,label("LOKALES TESTZERTIFIKAT · KEINE VW-VERTRAUENSKETTE",11,Color.rgb(224,187,137),true),8);
        add(trust,label("Hardware-ECU: nicht verbunden\nSFD2-Unlock: nein\nReale Codierung: gesperrt",12,MUTE,false),9);
    }
    private void journal(){
        sectionHead("LOKALES PROTOKOLL","Ereignisjournal","Chronologische Übersicht der lokalen Signatur- und Prüfereignisse.");
        LinearLayout controls=card(work,"Sitzung",null);
        buttonRow(controls,"Protokoll teilen",this::shareLog,
                  "Labor zurücksetzen",()->process("Laborzustand zurückgesetzt",()->lab.reset()));
        LinearLayout entries=card(work,"Aktivitäten",null);
        TextView journal=label(lab.journal(),11,WHITE,false);journal.setTextIsSelectable(true);
        journal.setLineSpacing(dp(2),1);add(entries,journal,8);
        add(work,label("Keine Internetberechtigung · keine Fahrzeug-Schreibzugriffe",11,MUTE,false),10);
    }
    private void shareArtifact(){
        try {
            Intent i=new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,"dynamicOS Lab Signaturpaket");
            i.putExtra(Intent.EXTRA_TEXT,lab.artifact());
            startActivity(Intent.createChooser(i,"Paket exportieren"));
        }catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void shareLog(){
        try {
            Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT,"dynamicOS SFD2 Laborjournal");
            i.putExtra(Intent.EXTRA_TEXT,LabEngine.DISCLAIMER+"\n\n"+lab.journal());
            startActivity(Intent.createChooser(i,"Journal teilen"));
        }catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
}

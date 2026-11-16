package com.dynamicos.sfd2trust;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

/**
 * dynamicOS / SFD2 Trust Center 9.1
 * Native, privacy-first inspection interface. This app does NOT issue VW
 * authorizations; the locally trusted CA is user-supplied and not OEM-verified.
 */
public final class Sfd2TrustActivity extends Activity {
    private static final int PICK_ROOT=201, PICK_CHAIN=202, PICK_JWS=203;
    private static final int BG=Color.rgb(9,14,19);
    private static final int SURFACE=Color.rgb(20,29,37);
    private static final int SURFACE_2=Color.rgb(26,37,46);
    private static final int STROKE=Color.rgb(44,58,68);
    private static final int WHITE=Color.rgb(236,243,246);
    private static final int SECONDARY=Color.rgb(155,174,184);
    private static final int DIM=Color.rgb(110,132,143);
    private static final int ACCENT=Color.rgb(151,207,225);
    private static final int AMBER=Color.rgb(224,187,137);
    private static final int GREEN=Color.rgb(145,207,181);
    private LinearLayout root, content;
    private EditText vinField, ecuField, challengeField;
    private Sfd2TrustVerifier verifier;
    private SharedPreferences prefs;
    private String vin="",ecu="17",challenge="";
    private String message="Noch kein Nachweis geprüft";
    private int tab=0;
    private boolean tokenImported=false;
    private int stageResult=0;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        prefs=getSharedPreferences("sfd2_trust_v9",MODE_PRIVATE);
        verifier=new Sfd2TrustVerifier();
        vin=prefs.getString("vin","");
        ecu=prefs.getString("ecu","17");
        challenge=prefs.getString("challenge","");
        restoreCerts();
        showPage();
    }
    private void restoreCerts() {
        try {
            String s=prefs.getString("anchor","");
            if(!s.isEmpty())verifier.loadRoot(Base64.decode(s,Base64.DEFAULT));
        } catch(Exception e) { message="CA-Datei konnte nicht geladen werden"; }
        try {
            String s=prefs.getString("chain","");
            if(!s.isEmpty())verifier.loadChain(Base64.decode(s,Base64.DEFAULT));
        } catch(Exception e) { message="Zertifikatskette konnte nicht geladen werden"; }
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius,int outline){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));
        if(outline!=0)d.setStroke(dp(1),outline);
        return d;
    }
    private GradientDrawable gradient(){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(34,51,60),Color.rgb(22,34,42),Color.rgb(17,27,34)});
        d.setCornerRadius(dp(19));d.setStroke(dp(1),STROKE);return d;
    }
    private LinearLayout layout(boolean horizontal) {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(horizontal?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        if(horizontal)l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private void padding(View v,int left,int top,int right,int bottom){
        v.setPadding(dp(left),dp(top),dp(right),dp(bottom));
    }
    private LinearLayout.LayoutParams params(int w,int h){
        return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));
    }
    private FrameLayout.LayoutParams frameParams(int w,int h,int gravity){
        return new FrameLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h),gravity);
    }
    private TextView text(String val,int sp,int color,boolean weight){
        TextView t=new TextView(this);
        t.setText(val);t.setTextColor(color);t.setTextSize(sp);
        t.setTypeface(Typeface.create(weight?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false);
        return t;
    }
    private TextView overline(String v){
        TextView t=text(v.toUpperCase(Locale.ROOT),10,DIM,true);
        t.setLetterSpacing(.13f);return t;
    }
    private void add(LinearLayout box,View view,int top){
        LinearLayout.LayoutParams p=params(-1,-2);p.topMargin=dp(top);box.addView(view,p);
    }
    private void rule(LinearLayout container,int top){
        View line=new View(this);line.setBackgroundColor(STROKE);
        LinearLayout.LayoutParams p=params(-1,1);p.topMargin=dp(top);
        container.addView(line,p);
    }
    private LinearLayout surface(int top) {
        LinearLayout l=layout(false);
        l.setBackground(bg(SURFACE,16,STROKE));
        padding(l,15,14,15,14);
        add(content,l,top);
        return l;
    }
    private LinearLayout row() {return layout(true);}
    private TextView action(String label,boolean primary,Runnable click) {
        TextView t=text(label,13,primary?BG:WHITE,true);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setMinHeight(dp(45));
        padding(t,14,10,14,10);
        t.setBackground(bg(primary?ACCENT:SURFACE_2,11,primary?0:STROKE));
        t.setOnClickListener(v->{v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);click.run();});
        t.setContentDescription(label);return t;
    }
    private LinearLayout header() {
        LinearLayout h=row();padding(h,20,15,20,13);
        LinearLayout brand=layout(false);
        TextView t=text("dynamicOS",22,WHITE,true);
        t.setLetterSpacing(-.04f);
        add(brand,t,0);
        TextView s=overline("VW SERVICE   /   SFD2");
        s.setTextColor(ACCENT);add(brand,s,5);
        h.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView version=text("9.1  /  CORE",10,SECONDARY,true);
        version.setLetterSpacing(.05f);padding(version,11,8,11,8);
        version.setBackground(bg(SURFACE,10,STROKE));
        h.addView(version,params(-2,-2));
        return h;
    }
    private void showPage() {
        vinField=ecuField=challengeField=null;
        root=layout(false);root.setBackgroundColor(BG);setContentView(root);
        root.addView(header(),params(-1,-2));
        View rule=new View(this);rule.setBackgroundColor(STROKE);root.addView(rule,params(-1,1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        content=layout(false);padding(content,18,21,18,18);
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(bottomBar(),params(-1,-2));
        switch(tab){
            case 0: trustPage();break;
            case 1: tokenPage();break;
            case 2: ecuPage();break;
            default: labPage();break;
        }
    }
    private View bottomBar() {
        LinearLayout holder=layout(false);holder.setBackgroundColor(SURFACE);
        View line=new View(this);line.setBackgroundColor(STROKE);holder.addView(line,params(-1,1));
        LinearLayout nav=row();padding(nav,10,9,10,11);
        final String[] titles={"Trust","Token","ECU","Labor"};
        for(int i=0;i<4;i++){
            final int selected=i;
            LinearLayout item=layout(false);item.setGravity(Gravity.CENTER);
            padding(item,4,5,4,5);
            IconView icon=new IconView(i,tab==i?ACCENT:SECONDARY);
            item.addView(icon,params(24,24));
            TextView title=text(titles[i],11,tab==i?WHITE:SECONDARY,tab==i);
            title.setGravity(Gravity.CENTER);LinearLayout.LayoutParams p=params(-1,-2);p.topMargin=dp(7);
            item.addView(title,p);
            item.setContentDescription(titles[i]);
            if(tab==i)item.setBackground(bg(SURFACE_2,12,0));
            item.setOnClickListener(v->{capture();tab=selected;showPage();});
            nav.addView(item,new LinearLayout.LayoutParams(0,dp(62),1));
        }
        holder.addView(nav,params(-1,-2));return holder;
    }
    private void intro(String chapter,String title,String subtitle) {
        TextView cap=overline(chapter);cap.setTextColor(ACCENT);add(content,cap,0);
        TextView h=text(title,27,WHITE,true);h.setLetterSpacing(-.025f);add(content,h,9);
        TextView small=text(subtitle,13,SECONDARY,false);
        small.setLineSpacing(dp(2),1f);add(content,small,9);
    }
    private void capRow(LinearLayout s,String head,String detail) {
        add(s,overline(head),0);
        add(s,text(detail,13,WHITE,false),8);
    }
    private void sectionTitle(String title,int gap) {
        TextView t=overline(title);add(content,t,gap);
    }
    private void twoLine(LinearLayout to,String first,String second,int color) {
        LinearLayout c=layout(false);
        add(c,text(first,14,color,true),0);
        TextView sub=text(second,12,SECONDARY,false);
        sub.setMaxLines(2);add(c,sub,5);
        to.addView(c,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void importRow(LinearLayout card,String number,String title,String desc,
                           boolean imported,String importLabel,Runnable onClick) {
        LinearLayout row=row();
        TextView numberText=text(number,12,ACCENT,true);
        numberText.setGravity(Gravity.TOP);
        row.addView(numberText,params(31,-1));
        LinearLayout descCol=layout(false);
        add(descCol,text(title,15,WHITE,true),0);
        add(descCol,text(desc,11,SECONDARY,false),5);
        row.addView(descCol,new LinearLayout.LayoutParams(0,-2,1));
        TextView status=text(imported?"GELADEN":"OFFEN",10,imported?GREEN:DIM,true);
        status.setLetterSpacing(.08f);status.setGravity(Gravity.CENTER);
        row.addView(status,params(63,27));
        add(card,row,0);
        TextView btn=action(importLabel,false,onClick);
        LinearLayout.LayoutParams p=params(-1,42);p.topMargin=dp(12);
        card.addView(btn,p);
    }
    private void trustPage() {
        intro("SICHERHEITSARCHITEKTUR  /  01","Vertrauenskette",
              "Importierte Zertifikate prüfen. Kein künstlicher Entsperrstatus.");
        LinearLayout hero=layout(false);hero.setBackground(gradient());
        padding(hero,17,17,17,16);add(content,hero,20);
        LinearLayout heading=row();
        LinearLayout words=layout(false);
        add(words,overline("VERIFIKATION"),0);
        add(words,text("Nachweis ausstehend",21,WHITE,true),7);
        add(words,text("Nur lokale kryptografische Prüfung",12,SECONDARY,false),6);
        heading.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        IconView symbol=new IconView(0,ACCENT);
        FrameLayout iconSlot=new FrameLayout(this);
        iconSlot.setBackground(bg(SURFACE_2,14,STROKE));
        iconSlot.addView(symbol,frameParams(30,30,Gravity.CENTER));
        heading.addView(iconSlot,params(55,55));
        hero.addView(heading,params(-1,-2));
        rule(hero,15);
        LinearLayout micro=row();
        addMetric(micro,hasRoot()?"Importiert":"Offen","Trust Anchor",0);
        addMetric(micro,hasChain()?"Importiert":"Offen","Zertifikate",1);
        hero.addView(micro,params(-1,-2));
        sectionTitle("ZERTIFIKATSMATERIAL",23);
        LinearLayout one=surface(10);
        importRow(one,"01","Vertrauensanker","CA-Zertifikat · PEM / DER",hasRoot(),
                  "CA-Zertifikat auswählen",()->pick(PICK_ROOT));
        LinearLayout two=surface(10);
        importRow(two,"02","Signaturzertifikat","Kette mit Endzertifikat",hasChain(),
                  "Zertifikatskette auswählen",()->pick(PICK_CHAIN));
        LinearLayout footer=surface(12);
        LinearLayout info=row();
        IconView i=new IconView(0,AMBER);
        info.addView(i,params(23,23));
        LinearLayout note=layout(false);
        add(note,text("Vertrauen ist nicht gleich Freigabe",13,WHITE,true),0);
        add(note,text("Eigene CA-Zertifikate werden nicht automatisch von VW akzeptiert.",11,SECONDARY,false),5);
        LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(0,-2,1);nlp.leftMargin=dp(12);
        info.addView(note,nlp);add(footer,info,0);
        TextView detail=text("Prüfdetails ansehen   →",12,ACCENT,true);
        add(footer,detail,13);
        footer.setOnClickListener(v->dialog("Vertrauensprüfung",verifier.summary()));
    }
    private boolean hasRoot(){return !prefs.getString("anchor","").isEmpty();}
    private boolean hasChain(){return !prefs.getString("chain","").isEmpty();}
    private void addMetric(LinearLayout metrics,String status,String label,int index){
        LinearLayout holder=layout(false);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        if(index==1)p.leftMargin=dp(16);
        metrics.addView(holder,p);
        TextView large=text(status,15,status.equals("Offen")?SECONDARY:GREEN,true);add(holder,large,12);
        TextView tiny=overline(label);add(holder,tiny,5);
    }
    private void tokenPage() {
        intro("SIGNATURPRÜFUNG  /  02","Token-Verifikation",
              "Import, Gültigkeit und Bindung eines signierten Nachweises.");
        LinearLayout upload=surface(18);
        LinearLayout top=row();
        IconView icon=new IconView(1,ACCENT);top.addView(icon,params(29,29));
        LinearLayout desc=layout(false);
        add(desc,text("Signaturbeleg",15,WHITE,true),0);
        add(desc,text("Kompaktes JWS · generisches Forschungsformat",11,SECONDARY,false),5);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(12);
        top.addView(desc,p);add(upload,top,0);
        add(upload,action(tokenImported?"Beleg ersetzen":"Signierten Beleg importieren",false,()->pick(PICK_JWS)),13);
        sectionTitle("BINDUNG ZUM AUFTRAG",22);
        LinearLayout form=surface(10);
        field(form,"FAHRZEUGIDENTIFIKATION",vin=input(vin,"FIN – 17 Zeichen"));
        field(form,"STEUERGERÄT",ecuField=input(ecu,"ECU, z. B. 17"));
        field(form,"CHALLENGE",challengeField=input(challenge,"Challenge aus Aufzeichnung"));
        add(form,action("Vertrauensnachweis prüfen  →",true,()->{
            capture();verifier.setExpected(vin,ecu,challenge);
            message=verifier.verify();stageResult=message.startsWith("LOKALE")?1:2;showPage();
        }),17);
        LinearLayout result=surface(13);
        LinearLayout info=row();
        TextView dot=text("●",13,stageResult==1?GREEN:stageResult==2?AMBER:DIM,true);
        info.addView(dot,params(20,-2));
        twoLine(info,stageResult==0?"Noch nicht geprüft":stageResult==1?
                "Lokale Prüfung bestanden":"Prüfung abgewiesen",
                stageResult==1?"Keine VW-ECU-Freigabe daraus ableitbar":
                stageResult==2?limit(message,90):"Importiere einen Beleg und führe die Prüfung aus.",
                WHITE);
        add(result,info,0);
        TextView inspect=text("Prüfprotokoll und Claims ansehen  →",12,ACCENT,true);
        add(result,inspect,15);
        result.setOnClickListener(v->dialog("Token-Prüfprotokoll",
                verifier.summary()+"\n\nTOKEN-INHALT\n"+verifier.payloadPreview()));
    }
    private EditText input(String text,String placeholder) {
        EditText e=new EditText(this);
        e.setText(text);e.setHint(placeholder);e.setSingleLine(true);
        e.setTextSize(14);e.setTextColor(WHITE);e.setHintTextColor(DIM);
        e.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        e.setBackground(bg(SURFACE_2,10,STROKE));
        padding(e,12,10,12,10);
        return e;
    }
    private void field(LinearLayout card,String label,EditText edit){
        add(card,overline(label),11);
        add(card,edit,7);
    }
    private void ecuPage() {
        intro("ECU-STATUS  /  03","Freigabestatus",
              "Getrennte Prüfung von Signaturvertrauen und ECU-Akzeptanz.");
        LinearLayout summary=layout(false);summary.setBackground(gradient());
        padding(summary,17,18,17,17);add(content,summary,19);
        LinearLayout r=row();
        LinearLayout left=layout(false);
        TextView x=overline("SFD2-SICHERHEITSSTATUS");x.setTextColor(AMBER);add(left,x,0);
        add(left,text("Nicht freigegeben",23,WHITE,true),8);
        add(left,text("Keine bestätigte Antwort eines VW-Steuergeräts",12,SECONDARY,false),9);
        r.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        IconView ic=new IconView(2,AMBER);r.addView(ic,params(36,36));
        summary.addView(r,params(-1,-2));
        sectionTitle("VERIFIKATIONSEBENEN",23);
        LinearLayout levels=surface(10);
        statusRow(levels,"01","Zertifikatskette",stageResult==1?"Lokal validiert":"Nicht nachgewiesen",
                stageResult==1?GREEN:SECONDARY);
        rule(levels,13);
        statusRow(levels,"02","Signatur und Challenge",stageResult==1?"Lokal validiert":"Nicht nachgewiesen",
                stageResult==1?GREEN:SECONDARY);
        rule(levels,13);
        statusRow(levels,"03","VW-Herstellervertrauen","Nicht nachgewiesen",AMBER);
        rule(levels,13);
        statusRow(levels,"04","Akzeptanz durch Fahrzeug","Keine ECU-Verbindung",AMBER);
        LinearLayout explain=surface(13);
        add(explain,overline("VERTRAUENSNACHWEIS"),0);
        TextView t=text("Eine lokale Signaturprüfung kann keine Herstellerfreigabe ersetzen. Erst eine vom Fahrzeug bestätigte Autorisierung würde den SFD2-Status ändern.",13,SECONDARY,false);
        t.setLineSpacing(dp(3),1f);add(explain,t,10);
        TextView report=text("Technischen Bericht öffnen  →",12,ACCENT,true);
        add(explain,report,13);
        explain.setOnClickListener(v->dialog("ECU-Vertrauensbericht",verifier.summary()));
    }
    private void statusRow(LinearLayout into,String index,String name,String status,int color){
        LinearLayout line=row();
        TextView n=text(index,12,DIM,true);
        line.addView(n,params(30,-2));
        LinearLayout details=layout(false);
        add(details,text(name,14,WHITE,true),0);
        add(details,text(status,12,color,false),5);
        line.addView(details,new LinearLayout.LayoutParams(0,-2,1));
        add(into,line,0);
    }
    private void labPage() {
        intro("ENTWICKLUNG  /  04","Trust Laboratory",
              "Isolierte Signatur- und Authentifizierungsprüfungen.");
        LinearLayout main=layout(false);main.setBackground(gradient());
        padding(main,18,18,18,18);add(content,main,19);
        IconView flask=new IconView(3,ACCENT);main.addView(flask,params(34,34));
        TextView heading=text("Kryptografischer Prüfstand",19,WHITE,true);
        add(main,heading,13);
        TextView body=text("Ed25519 · ECU-Challenge · Einmal-Nonces · SHA-256-Auftragsbindung · Replay-Schutz",13,SECONDARY,false);
        body.setLineSpacing(dp(3),1f);add(main,body,9);
        add(main,action("Lokales Labor öffnen  →",true,
            ()->startActivity(new Intent(this,MainActivity.class))),17);
        LinearLayout boundary=surface(16);
        add(boundary,overline("SICHERHEITSGRENZE"),0);
        add(boundary,text("Nur virtuelle ECU",16,WHITE,true),9);
        TextView caution=text("Die selbst erzeugte Test-Vertrauenskette ist unabhängig von Volkswagen. Sie kann keine SFD2-Freigabe für echte Fahrzeugsteuergeräte ausstellen.",13,SECONDARY,false);
        caution.setLineSpacing(dp(3),1f);add(boundary,caution,9);
    }
    private String limit(String v,int max){
        return v.length()>max?v.substring(0,max)+"…":v;
    }
    private void dialog(String title,String body) {
        TextView t=text(body,12,WHITE,false);
        t.setTextIsSelectable(true);t.setLineSpacing(dp(4),1.03f);
        padding(t,21,16,21,16);
        ScrollView scroll=new ScrollView(this);
        scroll.addView(t);
        new AlertDialog.Builder(this)
            .setTitle(title).setView(scroll).setPositiveButton("Schließen",(a,b)->{}).show();
    }
    private void capture(){
        if(vinField!=null)vin=vinField.getText().toString().trim().toUpperCase(Locale.ROOT);
        if(ecuField!=null)ecu=ecuField.getText().toString().trim().toUpperCase(Locale.ROOT);
        if(challengeField!=null)challenge=challengeField.getText().toString().trim();
        prefs.edit().putString("vin",vin).putString("ecu",ecu).putString("challenge",challenge).apply();
    }
    private void pick(int mode) {
        capture();
        try {
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
            startActivityForResult(i,mode);
        } catch(Exception e) {
            message="Dateiauswahl fehlgeschlagen: "+e.getMessage();stageResult=2;showPage();
        }
    }
    @Override protected void onActivityResult(int code,int result,Intent data){
        super.onActivityResult(code,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=getContentResolver().openInputStream(data.getData())){
                if(in==null)throw new IllegalStateException("Datei nicht lesbar");
                byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1){
                    if(out.size()+n>1024*1024)throw new SecurityException("Datei größer als 1 MB");
                    out.write(buffer,0,n);
                }
            }
            byte[] bytes=out.toByteArray();
            if(code==PICK_ROOT){
                message=verifier.loadRoot(bytes);
                prefs.edit().putString("anchor",Base64.encodeToString(bytes,Base64.NO_WRAP)).apply();
            }else if(code==PICK_CHAIN){
                message=verifier.loadChain(bytes);
                prefs.edit().putString("chain",Base64.encodeToString(bytes,Base64.NO_WRAP)).apply();
            }else if(code==PICK_JWS){
                message=verifier.loadJws(bytes);tokenImported=true;
            }
            stageResult=0;
        }catch(Exception e){
            message="Import abgelehnt: "+e.getMessage();
            stageResult=2;
        }
        Toast.makeText(this,limit(message,100),Toast.LENGTH_LONG).show();
        showPage();
    }
    private static final class IconView extends View {
        final int type; final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final int ink;
        IconView(int type,int ink){ super(Sfd2TrustActivityHolder.context); this.type=type;this.ink=ink; }
        @Override protected void onDraw(Canvas original){
            super.onDraw(original);
            int save=original.save();
            float size=Math.min(getWidth(),getHeight());
            float s=size/28f;
            original.translate((getWidth()-28*s)/2,(getHeight()-28*s)/2);
            original.scale(s,s);
            p.setColor(ink);p.setStrokeWidth(1.65f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            if(type==0){
                Path shield=new Path();
                shield.moveTo(14,3);shield.lineTo(23,7);shield.lineTo(23,14);
                shield.cubicTo(23,19,19,23,14,25);
                shield.cubicTo(9,23,5,19,5,14);
                shield.lineTo(5,7);shield.close();original.drawPath(shield,p);
                original.drawLine(10,14,13,17,p);original.drawLine(13,17,19,11,p);
            }else if(type==1){
                original.drawRoundRect(6,4,22,24,2,2,p);
                original.drawLine(10,10,18,10,p);
                original.drawLine(10,14,18,14,p);
                original.drawLine(10,18,16,18,p);
            }else if(type==2){
                original.drawRoundRect(7,7,21,21,3,3,p);
                original.drawRoundRect(11,11,17,17,1.5f,1.5f,p);
                for(int n=10;n<=18;n+=4){
                    original.drawLine(n,3,n,7,p);original.drawLine(n,21,n,25,p);
                    original.drawLine(3,n,7,n,p);original.drawLine(21,n,25,n,p);
                }
            }else {
                original.drawLine(10,4,18,4,p);original.drawLine(12,4,12,13,p);
                original.drawLine(16,4,16,13,p);original.drawLine(12,13,6,23,p);
                original.drawLine(16,13,22,23,p);original.drawLine(6,23,22,23,p);
                original.drawLine(9,19,19,19,p);
            }
            original.restoreToCount(save);
        }
    }
    private static final class Sfd2TrustActivityHolder {
        static Activity context;
    }
    @Override protected void onStart() {
        Sfd2TrustActivityHolder.context=this;
        super.onStart();
    }
}

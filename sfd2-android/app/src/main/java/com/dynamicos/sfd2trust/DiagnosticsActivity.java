package com.dynamicos.sfd2trust;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Modern standalone dynamicOS ScanPro user interface.
 * Unlike the separately installed reference app, this is original native code.
 */
public final class DiagnosticsActivity extends Activity {
    private static final int PERMISSIONS=227;
    private static final int BG=Color.rgb(9,14,19);
    private static final int PANEL=Color.rgb(20,29,37);
    private static final int PANEL2=Color.rgb(27,39,47);
    private static final int OUTLINE=Color.rgb(42,57,66);
    private static final int FG=Color.rgb(237,244,247);
    private static final int SECONDARY=Color.rgb(150,171,181);
    private static final int ICE=Color.rgb(151,207,225);
    private static final int GOLD=Color.rgb(226,192,144);
    private static final int OK=Color.rgb(143,210,181);
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService tasks=Executors.newSingleThreadExecutor();
    private final AtomicBoolean polling=new AtomicBoolean(false);
    private ObdLink transports;
    private ObdCore engine;
    private SharedPreferences prefs;
    private final ArrayList<ObdLink.Option> options=new ArrayList<>();
    private ObdLink.Option chosen;
    private int page=0;
    private String status="Bereit";
    private String rawCustom="";
    private boolean recording=true;
    private boolean autoConnect=true;
    private boolean isClosing=false;
    private String wifiHost="192.168.0.10:35000";
    private String customPid="010C";
    private LinearLayout root, content;
    private TextView headlineStatus,rpmValue,speedValue,temperatureValue,connectionValue;
    private EditText wifiField,customField;
    private Sparkline graph;
    private boolean scannerBusy=false;
    private final ArrayList<Float> speedHistory=new ArrayList<>();
    private boolean wifiSelected=false;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        prefs=getSharedPreferences("dynamicOS_scanpro_v10",MODE_PRIVATE);
        wifiHost=prefs.getString("wifi","192.168.0.10:35000");
        customPid=prefs.getString("custom_pid","010C");
        recording=prefs.getBoolean("record",true);
        autoConnect=prefs.getBoolean("auto",true);
        transports=new ObdLink(this);
        engine=new ObdCore();engine.setLogging(recording);
        refreshPaired();
        String lastType=prefs.getString("adapter_type","");
        String lastAddress=prefs.getString("adapter_address","");
        String lastName=prefs.getString("adapter_name","");
        if(!lastType.isEmpty()&&!lastAddress.isEmpty()){
            chosen=new ObdLink.Option(lastName,lastAddress,lastType);
        }
        show();
        if(autoConnect&&chosen!=null){
            if(ObdLink.WIFI.equals(chosen.type)||transports.permitted())
                main.postDelayed(this::connectSelected,500);
        }
    }
    @Override protected void onDestroy(){
        isClosing=true;
        transports.stopScan();
        tasks.execute(()->engine.close());
        tasks.shutdown();super.onDestroy();
    }
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius,int stroke){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);
        return d;
    }
    private GradientDrawable gradient(){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(34,52,60),Color.rgb(23,36,44),Color.rgb(18,28,35)});
        d.setCornerRadius(dp(18));d.setStroke(dp(1),OUTLINE);return d;
    }
    private LinearLayout col(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);return x;}
    private LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);x.setGravity(Gravity.CENTER_VERTICAL);return x;}
    private void pad(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);t.setIncludeFontPadding(false);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        return t;
    }
    private TextView caption(String s){
        TextView t=text(s.toUpperCase(Locale.ROOT),10,SECONDARY,true);t.setLetterSpacing(.12f);return t;
    }
    private void add(LinearLayout parent,View child,int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(margin);parent.addView(child,p);
    }
    private LinearLayout panel(String title,String secondary,int top){
        LinearLayout p=col();pad(p,16,15);p.setBackground(shape(PANEL,16,OUTLINE));
        add(content,p,top);
        if(title!=null)add(p,text(title,16,FG,true),0);
        if(secondary!=null)add(p,text(secondary,12,SECONDARY,false),6);
        return p;
    }
    private TextView button(String title,boolean accent,Runnable run){
        TextView b=text(title,13,accent?BG:FG,true);b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);b.setEllipsize(android.text.TextUtils.TruncateAt.END);
        b.setBackground(shape(accent?ICE:PANEL2,11,accent?0:OUTLINE));
        pad(b,10,11);b.setMinHeight(dp(45));
        b.setOnClickListener(v->{
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);run.run();
        });
        b.setContentDescription(title);return b;
    }
    private void buttons(LinearLayout parent,String a,Runnable actA,String b,Runnable actB){
        LinearLayout r=row();
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);
        r.addView(button(a,true,actA),p);
        LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,dp(46),1);q.leftMargin=dp(8);
        r.addView(button(b,false,actB),q);add(parent,r,12);
    }
    private void show(){
        wifiField=customField=null;
        root=col();root.setBackgroundColor(BG);setContentView(root);
        LinearLayout header=row();pad(header,18,15);
        LinearLayout brand=col();
        TextView logotype=text("dynamicOS",22,FG,true);logotype.setLetterSpacing(-.03f);
        add(brand,logotype,0);
        TextView sub=caption("SCAN PRO  /  DIAGNOSTICS 10.0");sub.setTextColor(ICE);
        add(brand,sub,6);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        headlineStatus=text(engine.connected()?"● ONLINE":"○ OFFLINE",
            11,engine.connected()?OK:GOLD,true);
        pad(headlineStatus,10,9);headlineStatus.setBackground(shape(PANEL,11,OUTLINE));
        header.addView(headlineStatus,new LinearLayout.LayoutParams(-2,-2));
        root.addView(header,new LinearLayout.LayoutParams(-1,-2));
        View line=new View(this);line.setBackgroundColor(OUTLINE);
        root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        ScrollView s=new ScrollView(this);s.setFillViewport(true);
        s.setVerticalScrollBarEnabled(false);
        content=col();pad(content,17,14);
        s.addView(content);
        root.addView(s,new LinearLayout.LayoutParams(-1,0,1));
        switch(page){
            case 0:home();break;
            case 1:connections();break;
            case 2:live();break;
            case 3:faults();break;
            case 4:trip();break;
            case 5:extras();break;
            default:settings();break;
        }
        root.addView(navigation(),new LinearLayout.LayoutParams(-1,-2));
    }
    private View navigation(){
        HorizontalScrollView hsv=new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);hsv.setFillViewport(true);hsv.setBackgroundColor(PANEL);
        LinearLayout nav=row();pad(nav,6,9);
        final String[] titles={"Home","Adapter","Live","Fehler","Fahrt","SFD2","Mehr"};
        for(int i=0;i<titles.length;i++){
            final int index=i;LinearLayout item=col();item.setGravity(Gravity.CENTER);
            pad(item,7,8);
            if(page==i)item.setBackground(shape(PANEL2,11,0));
            TextView emblem=text(new String[]{"⌂","⌁","∿","!","◈","◇","•••"}[i],
                20,page==i?ICE:SECONDARY,true);
            emblem.setGravity(Gravity.CENTER);item.addView(emblem,new LinearLayout.LayoutParams(-1,dp(28)));
            TextView name=text(titles[i],11,page==i?FG:SECONDARY,page==i);
            name.setGravity(Gravity.CENTER);add(item,name,1);
            item.setOnClickListener(v->{page=index;show();});
            nav.addView(item,new LinearLayout.LayoutParams(dp(77),dp(62)));
        }
        hsv.addView(nav);main.post(()->hsv.smoothScrollTo(dp(Math.max(0,page-2)*77),0));
        return hsv;
    }
    private void heading(String eyebrow,String title,String detail){
        TextView over=caption(eyebrow);over.setTextColor(ICE);add(content,over,8);
        TextView h=text(title,26,FG,true);h.setLetterSpacing(-.02f);add(content,h,8);
        TextView subtitle=text(detail,13,SECONDARY,false);subtitle.setLineSpacing(dp(2),1f);
        add(content,subtitle,9);
    }
    private void actionTile(LinearLayout container,String title,String desc,int target){
        LinearLayout tile=row();tile.setBackground(shape(PANEL,13,OUTLINE));pad(tile,14,13);
        LinearLayout c=col();add(c,text(title,15,FG,true),0);
        add(c,text(desc,11,SECONDARY,false),6);
        tile.addView(c,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=text("›",23,ICE,false);tile.addView(arrow,new LinearLayout.LayoutParams(-2,-2));
        add(container,tile,9);tile.setOnClickListener(v->{page=target;show();});
    }
    private void home(){
        heading("FAHRZEUGDIAGNOSE / LIVE","Übersicht","OBD-II-Echtzeitdaten, Diagnose und Fahrten in einer Oberfläche.");
        ObdCore.Report r=engine.snapshot();
        LinearLayout hero=col();hero.setBackground(gradient());pad(hero,18,17);add(content,hero,18);
        add(hero,caption("FAHRZEUGVERBINDUNG"),0);
        connectionValue=text(engine.connected()?r.description:"Kein Adapter verbunden",
            16,FG,true);add(hero,connectionValue,9);
        add(hero,text(status,12,SECONDARY,false),7);
        LinearLayout metrics=row();
        LinearLayout left=col(),right=col();
        add(left,caption("TEMPO"),0);
        speedValue=text(read(r,"speed"),32,FG,true);add(left,speedValue,7);
        add(right,caption("DREHZAHL"),0);
        rpmValue=text(read(r,"rpm"),32,FG,true);add(right,rpmValue,7);
        metrics.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        metrics.addView(right,new LinearLayout.LayoutParams(0,-2,1));
        add(hero,metrics,19);
        if(!engine.connected())add(hero,button("Adapter verbinden",true,()->{page=1;show();}),17);
        else add(hero,button("Live-Daten anzeigen",true,()->{page=2;show();}),17);
        LinearLayout quick=col();add(content,quick,16);
        actionTile(quick,"Verbindung","Bluetooth Classic · BLE UART · WLAN",1);
        actionTile(quick,"Live-Messwerte","Motor, Temperatur, Luftmasse und Spannung",2);
        actionTile(quick,"Fehlerdiagnose","DTC, Freeze-Frame, Abgasbereitschaft, Mode 06",3);
        actionTile(quick,"Fahrten & Protokolle","Fahrstrecke, CSV, Beschleunigungsmessung",4);
        actionTile(quick,"SFD2 Trust Center","Signaturprüfung und virtuelle Test-ECU",5);
    }
    private String read(ObdCore.Report r,String key){
        ObdCore.Reading v=r.data.get(key);return v==null?"—":v.display();
    }
    private void refreshPaired(){
        options.clear();options.addAll(transports.paired());
    }
    private void connections(){
        heading("KOMMUNIKATION / 01","Adapter","OBD-Hardware auswählen und den ELM327-Handshake starten.");
        LinearLayout state=panel("Verbindungsstatus",status,17);
        ObdCore.Report r=engine.snapshot();
        add(state,text("Adapter: "+r.description+"\nProtokoll: "+r.protocol+
            "\nELM: "+r.elmVersion+"\nFIN: "+r.vin,12,SECONDARY,false),12);
        if(engine.connected())
            add(state,button("Verbindung trennen",false,this::disconnect),15);
        if(!transports.permitted()){
            LinearLayout p=panel("Bluetooth-Berechtigung","Wird für gekoppelte Geräte und BLE-Scan benötigt.",11);
            add(p,button("Bluetooth freigeben",true,this::requestBluetooth),11);
        } else if(!transports.enabled()){
            add(panel("Bluetooth ist deaktiviert","Aktiviere Bluetooth in den Android-Schnelleinstellungen.",11),
                text("Für WLAN-Adapter ist Bluetooth nicht erforderlich.",12,SECONDARY,false),11);
        }
        LinearLayout paired=panel("Gekoppelte Geräte","Bluetooth Classic / SPP: direkt auswählbar.",13);
        if(transports.permitted()&&options.isEmpty())refreshPaired();
        if(options.isEmpty())add(paired,text("Noch keine gekoppelten Geräte gefunden.",12,SECONDARY,false),12);
        for(ObdLink.Option o:new ArrayList<>(options)){
            add(paired,button(o.name+"   ·   "+o.address,false,()->choose(o)),8);
        }
        buttons(paired,"Bluetooth aktualisieren",()->{
            refreshPaired();status="Gekoppelte Geräte aktualisiert";show();
        },"BLE-Geräte suchen",this::scanBle);
        if(chosen!=null){
            LinearLayout selected=panel("Ausgewählter Adapter",chosen.display(),12);
            buttons(selected,"Verbinden",this::connectSelected,"Auswahl vergessen",()->{
                chosen=null;prefs.edit().remove("adapter_type").remove("adapter_address").remove("adapter_name").apply();
                status="Auswahl zurückgesetzt";show();
            });
        }
        LinearLayout net=panel("WLAN-Diagnoseadapter","ELM327-TCP-Adapter, Adresse und Port des Geräts.",12);
        wifiField=input(wifiHost,"192.168.0.10:35000");
        add(net,wifiField,12);
        add(net,button("WLAN-Verbindung wählen",true,()->{
            wifiHost=wifiField.getText().toString().trim();
            prefs.edit().putString("wifi",wifiHost).apply();
            choose(new ObdLink.Option("ELM327 über WLAN",wifiHost,ObdLink.WIFI));
        }),12);
        add(content,text("Wichtig: Wähle den Diagnoseadapter, nicht die Bluetooth-Freisprecheinrichtung des Fahrzeugs. BLE-UART ist adapterabhängig.",11,SECONDARY,false),16);
    }
    private EditText input(String initial,String hint){
        EditText e=new EditText(this);e.setText(initial);e.setHint(hint);
        e.setTextSize(13);e.setTextColor(FG);e.setHintTextColor(SECONDARY);
        e.setSingleLine(true);pad(e,12,11);e.setBackground(shape(PANEL2,10,OUTLINE));
        return e;
    }
    private void choose(ObdLink.Option option){
        chosen=option;
        prefs.edit().putString("adapter_type",option.type).putString("adapter_address",option.address)
            .putString("adapter_name",option.name).apply();
        status="Adapter gewählt: "+option.name;show();
    }
    private void requestBluetooth(){
        if(android.os.Build.VERSION.SDK_INT>=31)
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN},PERMISSIONS);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] outcomes){
        super.onRequestPermissionsResult(request,permissions,outcomes);
        if(request==PERMISSIONS){
            refreshPaired();show();
            if(transports.permitted()&&autoConnect&&chosen!=null)connectSelected();
        }
    }
    private void scanBle(){
        if(scannerBusy)return;
        if(!transports.permitted()){requestBluetooth();return;}
        scannerBusy=true;status="BLE-Scanner aktiv …";show();
        transports.scanBle(new ObdLink.SearchCallback(){
            public void found(ObdLink.Option option){
                main.post(()->{
                    boolean exists=false;
                    for(ObdLink.Option o:options)if(o.address.equals(option.address)&&o.type.equals(option.type))exists=true;
                    if(!exists){options.add(option);if(page==1)show();}
                });
            }
            public void done(String msg){main.post(()->{scannerBusy=false;status=msg;if(page==1)show();});}
        });
    }
    private void connectSelected(){
        if(chosen==null){status="Zuerst einen Adapter auswählen";show();return;}
        if(engine.connected()){status="Bereits verbunden";show();return;}
        if(!ObdLink.WIFI.equals(chosen.type)&&!transports.permitted()){
            requestBluetooth();return;
        }
        final ObdLink.Option selected=chosen;
        status="Verbinde: "+selected.display();show();
        tasks.execute(()->{
            try{
                ObdLink.Connection conn=transports.open(selected);
                engine.open(conn);
                main.post(()->{
                    status="OBD-Protokoll initialisiert";
                    page=0;show();schedulePolling();
                });
            }catch(Exception e){
                main.post(()->{
                    status="Verbindung fehlgeschlagen: "+e.getMessage();
                    show();Toast.makeText(this,status,Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private void disconnect(){
        status="Verbindung wird getrennt";show();
        tasks.execute(()->{
            engine.close();main.post(()->{status="Adapter getrennt";show();});
        });
    }
    private void schedulePolling(){
        if(isClosing||!engine.connected()||!polling.compareAndSet(false,true))return;
        tasks.execute(()->{
            try{
                ObdCore.Report r=engine.poll();
                main.post(()->{
                    if(isClosing)return;
                    if(r.data.containsKey("speed")){
                        speedHistory.add((float)r.data.get("speed").value);
                        if(speedHistory.size()>90)speedHistory.remove(0);
                    }
                    updateLive(r);
                });
            }catch(Exception e){
                main.post(()->{status="OBD-Abfrage: "+e.getMessage();updateStatus();});
            }finally{
                polling.set(false);
                main.postDelayed(()->{if(!isClosing&&engine.connected())schedulePolling();},500);
            }
        });
    }
    private void updateStatus(){
        if(headlineStatus!=null)
            headlineStatus.setText(engine.connected()?"● ONLINE":"○ OFFLINE");
    }
    private void updateLive(ObdCore.Report r){
        updateStatus();
        if(speedValue!=null)speedValue.setText(read(r,"speed"));
        if(rpmValue!=null)rpmValue.setText(read(r,"rpm"));
        if(temperatureValue!=null)temperatureValue.setText(read(r,"coolant"));
        if(graph!=null)graph.invalidate();
    }
    private void live(){
        heading("FAHRZEUGDATEN / 02","Live-Messwerte","Standardisierte OBD-Sensoren, direkt aus dem Motorsteuergerät.");
        ObdCore.Report r=engine.snapshot();
        LinearLayout hero=col();hero.setBackground(gradient());pad(hero,17,17);add(content,hero,18);
        add(hero,caption("GESCHWINDIGKEIT  /  KM/H"),0);
        speedValue=text(read(r,"speed"),35,FG,true);add(hero,speedValue,9);
        graph=new Sparkline();LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(100));
        gp.topMargin=dp(14);hero.addView(graph,gp);
        add(hero,text("Sensorverlauf · OBD-Abtastrate abhängig vom Adapter",11,SECONDARY,false),9);
        LinearLayout result=panel("Alle Standard-Sensoren",engine.connected()?"Onlinewerte werden automatisch aktualisiert":"Adapter nicht verbunden",14);
        for(String key:new String[]{"rpm","speed","coolant","throttle","load","maf","voltage","intake","map","fuel","oil","stft","ltft"}){
            ObdCore.Reading v=r.data.get(key);
            line(result,v==null?key:v.name,v==null?"—":v.display());
        }
        add(result,button("Erweiterte Standard-Sensoren abrufen",true,()->job("Sensoren vollständig aktualisiert",()->engine.pollExtended())),13);
        LinearLayout custom=panel("Eigene Standard-PID-Abfrage","Zulässig: OBD-Modus 01, beispielsweise 010C oder 0142.",13);
        customField=input(customPid,"01xx");add(custom,customField,11);
        add(custom,button("PID lesen",true,()->{
            customPid=customField.getText().toString().trim().toUpperCase(Locale.ROOT);
            prefs.edit().putString("custom_pid",customPid).apply();
            job("PID-Abfrage abgeschlossen",()->rawCustom=engine.customStandardPid(customPid));
        }),11);
        if(!rawCustom.isEmpty()){
            TextView raw=text(rawCustom,11,SECONDARY,false);raw.setTextIsSelectable(true);
            add(custom,raw,11);
        }
    }
    private void line(LinearLayout target,String left,String right) {
        LinearLayout r=row();
        TextView a=text(left,13,SECONDARY,false);r.addView(a,new LinearLayout.LayoutParams(0,-2,1));
        TextView b=text(right,14,FG,true);r.addView(b,new LinearLayout.LayoutParams(-2,-2));
        add(target,r,13);
    }
    private interface Checked {void doWork() throws Exception;}
    private void job(String done,Checked work){
        if(!engine.connected()){
            Toast.makeText(this,"Zuerst Adapter verbinden",Toast.LENGTH_SHORT).show();return;
        }
        status="Diagnose läuft …";updateStatus();
        tasks.execute(()->{
            try{
                work.doWork();
                main.post(()->{status=done;show();});
            }catch(Exception ex){
                main.post(()->{
                    status="Diagnosefehler: "+ex.getMessage();show();
                    Toast.makeText(this,status,Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private void faults(){
        heading("DIAGNOSE / 03","Fehlerspeicher","Standard-OBD-Fehlercodes und Emissions-Selbsttests.");
        ObdCore.Report r=engine.snapshot();
        LinearLayout actions=panel("Fahrzeugdiagnose",
            "DTC: gespeichert (03), ausstehend (07), permanent (0A)",17);
        buttons(actions,"Fehlercodes lesen",()->job("DTC-Auslesung abgeschlossen",()->engine.readDtc()),
                "Readiness",()->job("Emissionsstatus gelesen",()->engine.readReadiness()));
        buttons(actions,"Freeze / Mode 06",()->job("Diagnosetests gelesen",()->engine.readSpecial()),
                "Bericht teilen",()->share("dynamicOS Diagnosebericht",engine.snapshot().diagnosticText(),"text/plain"));
        LinearLayout codes=panel("Fehlercodes","Daten des zuletzt ausgeführten Scans.",14);
        block(codes,"Gespeichert",r.dtcStored);
        block(codes,"Ausstehend",r.dtcPending);
        block(codes,"Permanent",r.dtcPermanent);
        LinearLayout tests=panel("OBD-Selbsttests",null,14);
        block(tests,"Emissionsbereitschaft",r.readiness);
        block(tests,"Freeze-Frame (Rohdaten)",r.freeze);
        block(tests,"Mode 06 (Rohdaten)",r.mode06);
        LinearLayout sensitive=panel("Fehlercodes löschen","Diese Aktion setzt auch OBD-Bereitschaftsmonitore zurück.",14);
        add(sensitive,text("Nur auf ausdrücklichen Wunsch. Keine automatische Fehlerlöschung und kein SFD2-Bypass.",12,GOLD,false),11);
        add(sensitive,button("Standard-OBD-Fehler löschen …",false,()->new AlertDialog.Builder(this)
            .setTitle("Fehlerspeicher wirklich löschen?")
            .setMessage("Der generische OBD-Modus 04 löscht Fehler- und ggf. Freeze-Frame-Daten. Er kann Emissionsbereitschaftsmonitore zurücksetzen. Nicht während der Fahrt ausführen.")
            .setNegativeButton("Abbrechen",(d,w)->{})
            .setPositiveButton("Bewusst löschen",(d,w)->job("OBD-Löschvorgang bestätigt",()->engine.clearDtcAfterConfirmation()))
            .show()),12);
    }
    private void block(LinearLayout c,String title,String detail) {
        add(c,caption(title),15);
        TextView v=text(detail,12,FG,false);v.setTextIsSelectable(true);v.setLineSpacing(dp(3),1f);
        add(c,v,7);
    }
    private void trip(){
        heading("FAHRDATEN / 04","Fahrten & Statistik","Lokale Aufzeichnung und Berechnung aus OBD-Geschwindigkeitsdaten.");
        ObdCore.Report r=engine.snapshot();
        LinearLayout hero=col();hero.setBackground(gradient());pad(hero,17,17);add(content,hero,17);
        add(hero,caption("AUFGEZEICHNETE FAHRSTRECKE"),0);
        add(hero,text(String.format(Locale.GERMANY,"%.2f km",r.tripKm),32,FG,true),9);
        add(hero,text("Höchsttempo: "+String.format(Locale.GERMANY,"%.0f km/h",r.peakSpeed),14,SECONDARY,false),13);
        add(hero,text("0–100 km/h (OBD-Schätzung): "+(r.accelTime<0?"—":
            String.format(Locale.GERMANY,"%.1f s",r.accelTime)),13,SECONDARY,false),7);
        LinearLayout recorder=panel("Aufzeichnung",recording?"CSV-Aufzeichnung aktiviert":"CSV-Aufzeichnung pausiert",14);
        buttons(recorder,recording?"Aufzeichnung pausieren":"Aufzeichnung starten",()->{
                    recording=!recording;engine.setLogging(recording);
                    prefs.edit().putBoolean("record",recording).apply();show();
                },"CSV exportieren",()->share("dynamicOS Fahrt.csv",engine.csv(),"text/csv"));
        add(recorder,button("Fahrtstatistik zurücksetzen",false,()->new AlertDialog.Builder(this)
            .setTitle("Fahrt zurücksetzen?")
            .setMessage("Alle lokalen Messwerte dieser Fahrt und die CSV-Aufzeichnung werden gelöscht.")
            .setNegativeButton("Abbrechen",(a,b)->{})
            .setPositiveButton("Zurücksetzen",(a,b)->{engine.resetTrip();show();}).show()),13);
        add(content,text("Messungen sind Näherungen aus zeitlich begrenzten OBD-Abfragen. Nicht als amtliche Geschwindigkeits- oder Beschleunigungsmessung verwenden.",11,SECONDARY,false),16);
    }
    private void extras(){
        heading("SFD2 / 05","Trust Center","Unabhängiger Sicherheits- und Kryptografie-Prüfbereich.");
        LinearLayout hero=col();hero.setBackground(gradient());pad(hero,18,19);add(content,hero,19);
        add(hero,caption("VW SECURE DIAGNOSTICS"),0);
        add(hero,text("Vertrauenskette & Token",21,FG,true),10);
        add(hero,text("PKIX-Kettenprüfung · Signaturnachweise · lokale Test-ECU · SFD2-Fahrzeugkatalog",13,SECONDARY,false),9);
        add(hero,button("SFD2 Trust Center öffnen",true,
            ()->startActivity(new Intent(this,Sfd2TrustActivity.class))),18);
        LinearLayout boundary=panel("Sicherheitsstatus","Die Diagnose-App erhält keine VW-Herstellerschlüssel.",17);
        add(boundary,text("SFD2-Freigabe: NICHT VORHANDEN",14,GOLD,true),11);
        add(boundary,text("OBD-II-Lesefunktionen sind unabhängig von SFD2. Für herstellergeschützte Codierungen wäre eine vom Steuergerät bestätigte Autorisierung erforderlich.",12,SECONDARY,false),10);
    }
    private void settings(){
        heading("EINSTELLUNGEN / 06","Konfiguration","Verbindungsoptionen und unabhängige dynamicOS-Implementierung.");
        LinearLayout general=panel("Autoverbindung","Beim Start zum zuletzt ausgewählten Adapter verbinden, sofern Android die Berechtigung bereits erteilt hat.",17);
        add(general,button(autoConnect?"Automatisch verbinden: AN":"Automatisch verbinden: AUS",true,()->{
            autoConnect=!autoConnect;prefs.edit().putBoolean("auto",autoConnect).apply();show();
        }),12);
        LinearLayout info=panel("Diagnoseabdeckung",null,13);
        block(info,"Transport","Bluetooth Classic SPP, generischer BLE-UART (adapterabhängig), WLAN TCP.");
        block(info,"Standard-OBD","Modi 01, 02, 03, 04 (nach Bestätigung), 06, 07, 09 und 0A. Ausgewählte PIDs dekodiert.");
        block(info,"Nicht enthalten","Proprietäre Car-Scanner-Datenbanken, USB-Seriell, fahrzeugspezifische Codierungsroutinen und automatische SFD2-Entsperrung.");
        add(info,button("OBD-PID-Liste anzeigen",false,()->new AlertDialog.Builder(this)
            .setTitle("Unterstützte Sensorabfragen")
            .setMessage(ObdCore.sensorList())
            .setPositiveButton("Schließen",(a,b)->{}).show()),13);
        add(info,button("Diagnosebericht exportieren",true,()->
            share("dynamicOS Diagnosebericht",engine.snapshot().diagnosticText(),"text/plain")),10);
        add(content,text("dynamicOS Scan Pro 10.0 · Eigenständige Android-Implementierung. Kein Ersatz für herstellerspezifische Sicherheitsfreigaben.",11,SECONDARY,false),17);
    }
    private void share(String subject,String data,String mime){
        try{
            Intent send=new Intent(Intent.ACTION_SEND);
            send.setType(mime);send.putExtra(Intent.EXTRA_SUBJECT,subject);
            send.putExtra(Intent.EXTRA_TEXT,data);
            startActivity(Intent.createChooser(send,"Exportieren"));
        }catch(Exception e){Toast.makeText(this,"Teilen nicht verfügbar",Toast.LENGTH_SHORT).show();}
    }
    private final class Sparkline extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Sparkline(){super(DiagnosticsActivity.this);}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float w=getWidth(),h=getHeight();
            paint.setColor(OUTLINE);paint.setStrokeWidth(dp(1));
            for(int i=1;i<4;i++)canvas.drawLine(0,h*i/4,w,h*i/4,paint);
            if(speedHistory.size()<2)return;
            paint.setColor(ICE);paint.setStrokeWidth(dp(2.5f));
            paint.setStyle(Paint.Style.STROKE);
            Path p=new Path();
            for(int i=0;i<speedHistory.size();i++){
                float x=i*w/Math.max(1,speedHistory.size()-1);
                float y=h-6-(Math.min(200,speedHistory.get(i))/200f)*(h-12);
                if(i==0)p.moveTo(x,y);else p.lineTo(x,y);
            }
            canvas.drawPath(p,paint);paint.setStyle(Paint.Style.FILL);
        }
    }
}

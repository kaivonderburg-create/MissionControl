#!/usr/bin/env python3
"""Merge Fiesta HOME and native Open Headunit receiver into ONE Android application."""
from pathlib import Path
import shutil
import re

root=Path("receiver")
fiesta=Path("dynamicOS-Fiesta/app/src/main")
src=root/"app/src/main"
javadir=src/"java/de/vd3/fiesta"
javadir.mkdir(parents=True,exist_ok=True)
for name in ("LauncherActivity.java","MainActivity.java"):
    data=(fiesta/"java/de/vd3/fiesta"/name).read_text()
    data=data.replace("package de.vd3.fiesta;", "package de.vd3.fiesta;\nimport com.andrerinas.openheadunit.R;")
    if name=="LauncherActivity.java":
        data=data.replace("if (isDefaultHome()) home(); else setupHome();",
            'if (!getSharedPreferences("dynamicos_setup", MODE_PRIVATE).getBoolean("complete", false)) {\n'
            '            startActivity(new Intent(this, SetupActivity.class)); finish(); return;\n'
            '        }\n'
            '        if (isDefaultHome()) home(); else setupHome();')
        data=re.sub(r'// The Android Auto receiver must be installed separately until its source is integrated\..*?Toast\.makeText\(this, "Kein kompatibler Empfänger installiert", Toast\.LENGTH_LONG\)\.show\(\);',
            'startActivity(new Intent(this, com.andrerinas.openheadunit.main.MainActivity.class));',
            data,flags=re.S)
        data=data.replace("Empfänger separat erforderlich.", "Empfänger ist Bestandteil von dynamicOS.")
        data=data.replace('() -> setting(Settings.ACTION_BLUETOOTH_SETTINGS)', 'this::openWirelessSetup')
        data=data.replace('() -> setting(Settings.ACTION_WIFI_SETTINGS)', 'this::openWirelessSetup')
        data=data.replace('    private void openMusic() {',
            '    private void openWirelessSetup() { startActivity(new Intent(this, WirelessSetupActivity.class)); }\n\n    private void openMusic() {')

        # Unified dark-blue launcher with six primary actions, designed for 1 GB RAM.
        data=data.replace("private final int bg = 0xff070f1b, panel = 0xff0e2030, cyan = 0xff00dcef;",
            "private final int bg = 0xff071326, panel = 0xff122943, cyan = 0xff3d9dff;")
        data=data.replace("new LinearLayout.LayoutParams(dp(66), dp(66))",
            "new LinearLayout.LayoutParams(dp(45), dp(45))")
        data=data.replace('label(title, 22, Color.WHITE, true)',
            'label(title, 17, Color.WHITE, true)')
        data=data.replace("new LinearLayout.LayoutParams(-1, dp(42))",
            "new LinearLayout.LayoutParams(-1, dp(32))")
        old_home_start=data.index("    private void home() {")
        old_home_end=data.index("    private void addTile(",old_home_start)
        new_home=r'''    private void home() {
        scaffold("Fiesta Unified");
        LinearLayout content = new LinearLayout(this);
        content.setPadding(0, dp(12), 0, dp(10));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout vehicle = new LinearLayout(this);
        vehicle.setOrientation(LinearLayout.VERTICAL);
        vehicle.setGravity(Gravity.CENTER);
        vehicle.setPadding(dp(6), dp(6), dp(6), dp(6));
        vehicle.setBackground(shape(0xff102641, 0xff235178));
        ImageView car = new ImageView(this);
        car.setImageResource(R.drawable.fiesta_graphite);
        car.setScaleType(ImageView.ScaleType.FIT_CENTER);
        vehicle.addView(car, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView model = label("FORD FIESTA  /  Mk7", 18, Color.WHITE, true);
        model.setGravity(Gravity.CENTER);
        vehicle.addView(model, new LinearLayout.LayoutParams(-1, dp(30)));
        TextView subtitle = label("W-S 107  ·  2015", 13, 0xffa4bfd4, false);
        subtitle.setGravity(Gravity.CENTER);
        vehicle.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(24)));
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(0,-1,.35f);
        vp.rightMargin = dp(10);
        content.addView(vehicle,vp);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setRowCount(2);
        content.addView(grid,new LinearLayout.LayoutParams(0,-1,.65f));
        addTile(grid,"auto","Android Auto",true,this::launchReceiver,0,0);
        addTile(grid,"phone","Telefon",false,this::openPhone,0,1);
        addTile(grid,"music","Medien",false,this::openMusic,0,2);
        addTile(grid,"car","Fahrzeug",false,this::vehicleMenu,1,0);
        addTile(grid,"wifi","Verbindung",false,this::openWirelessSetup,1,1);
        addTile(grid,"settings","Einrichtung",false,()->
            startActivity(new Intent(this,SetupActivity.class)),1,2);
        navBar("Start");
    }

'''
        data=data[:old_home_start]+new_home+data[old_home_end:]
        data=data.replace("private void openMusic() {", """private void openPhone() {
        try { startActivity(new Intent(Intent.ACTION_DIAL)); }
        catch (Exception ex) {
            Toast.makeText(this, "Telefonie benötigt ein kompatibles Bluetooth-HFP-Profil", Toast.LENGTH_LONG).show();
            openWirelessSetup();
        }
    }

    private void openMusic() {""")
        data=data.replace('} else if ("car".equals(type)) {',
            """} else if ("phone".equals(type)) {
                Path q=new Path();q.moveTo(29,21);q.lineTo(40,17);q.lineTo(49,35);
                q.lineTo(41,43);q.quadTo(51,60,62,63);q.lineTo(70,55);
                q.lineTo(85,64);q.lineTo(80,81);q.quadTo(66,90,43,73);
                q.quadTo(14,52,22,32);q.close();canvas.drawPath(q,p);
            } else if ("wifi".equals(type)) {
                canvas.drawArc(14,21,86,85,207,126,false,p);
                canvas.drawArc(28,39,72,83,208,124,false,p);
                canvas.drawCircle(50,78,4,p);
            } else if ("car".equals(type)) {""")

    (javadir/name).write_text(data)
(src/"res/drawable-nodpi").mkdir(parents=True,exist_ok=True)
shutil.copy2(fiesta/"res/drawable-nodpi/fiesta_graphite.png",src/"res/drawable-nodpi/fiesta_graphite.png")

setup=r'''package de.vd3.fiesta;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.Toast;

/** 1024x600 landscape responsive onboard configuration.
 * Fixed dual-column composition; no ScrollView, WebView, or animation loop.
 */
public final class SetupActivity extends Activity {
    private static final int BG=0xff071525, SIDE=0xff102a44, CARD=0xff183650;
    private static final int ACCENT=0xff3b9df5, FG=0xfff7fbff, SUB=0xffb7cada, GREEN=0xff79e1ae;
    private int page=0;
    private LinearLayout root, left, right, tiles, controls;
    private int dp(float n){ return (int)(getResources().getDisplayMetrics().density*n+0.5f); }
    private GradientDrawable shape(int fill,int line){
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(12));
        if(line!=0)d.setStroke(dp(1),line);return d;
    }
    private TextView label(String value,float sp,int color,boolean bold){
        TextView t=new TextView(this); t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);t.setMaxLines(2);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private LinearLayout vertical(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private void add(LinearLayout parent,View view,int marginTop){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(marginTop);
        parent.addView(view,lp);
    }
    private void tile(String symbol,String title,String detail,String status,Runnable action){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14),dp(8),dp(14),dp(8));
        row.setBackground(shape(CARD,0xff294963));
        TextView glyph=label(symbol,24,ACCENT,true);glyph.setGravity(Gravity.CENTER);
        row.addView(glyph,new LinearLayout.LayoutParams(dp(44),dp(48)));
        LinearLayout info=vertical();
        add(info,label(title,17,FG,true),0);
        add(info,label(detail,12,SUB,false),2);
        row.addView(info,new LinearLayout.LayoutParams(0,-2,1f));
        if(status!=null){
            TextView state=label(status,12,GREEN,true);state.setGravity(Gravity.CENTER);
            state.setPadding(dp(8),dp(4),dp(8),dp(4));
            row.addView(state,new LinearLayout.LayoutParams(dp(165),-2));
        }
        if(action!=null){row.setOnClickListener(v->action.run());row.setClickable(true);}
        add(tiles,row,8);
    }
    private void button(String text,boolean primary,Runnable action){
        TextView b=label(text,16,FG,true);b.setGravity(Gravity.CENTER);
        b.setBackground(shape(primary?ACCENT:CARD,primary?0:0xff395875));
        b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1f);
        lp.leftMargin=dp(4);lp.rightMargin=dp(4);controls.addView(b,lp);
    }
    private String bt(){
        try{BluetoothManager m=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter a=m==null?null:m.getAdapter();
            return a==null?"Nicht erkannt":a.isEnabled()?"Aktiv":"Deaktiviert";
        }catch(Exception e){return "Nicht zugänglich";}
    }
    private String wifi(){
        try{WifiManager m=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return m==null?"Nicht erkannt":m.isWifiEnabled()?"Aktiv":"Deaktiviert";
        }catch(Exception e){return "Nicht zugänglich";}
    }
    private void settings(String action){
        try{startActivity(new Intent(action));}
        catch(Exception e){Toast.makeText(this,"Einstellung auf dieser Firmware nicht verfügbar",Toast.LENGTH_LONG).show();}
    }
    private void go(int n){page=n;render();}
    private void finishSetup(){
        getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putBoolean("complete",true).apply();
        startActivity(new Intent(this,LauncherActivity.class));finish();
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if(state!=null)page=state.getInt("page",0);render();
    }
    @Override protected void onSaveInstanceState(Bundle b){b.putInt("page",page);super.onSaveInstanceState(b);}
    @Override protected void onResume(){super.onResume();if(root!=null&&page==2)render();}
    private void render(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BG);root.setPadding(dp(12),dp(12),dp(12),dp(12));
        left=vertical();left.setBackground(shape(SIDE,0xff1e4160));
        left.setPadding(dp(22),dp(16),dp(18),dp(14));
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(0,-1,.34f);
        l.rightMargin=dp(12);root.addView(left,l);

        add(left,label("dynamicOS",23,FG,true),0);
        add(left,label("FORD FIESTA  ·  UNIFIED",12,ACCENT,true),3);
        View line=new View(this);line.setBackgroundColor(0xff2c4f6c);
        add(left,line,18);line.getLayoutParams().height=dp(1);
        String[] sections={"WILLKOMMEN","FAHRZEUG","VERBINDUNG","ANZEIGE"};
        for(int i=0;i<4;i++){
            TextView step=label((i+1<10?"0":"")+(i+1)+"    "+sections[i],
                14,i==page?FG:(i<page?ACCENT:SUB),i==page);
            step.setBackground(i==page?shape(0xff24547b,0):null);
            step.setPadding(dp(11),0,0,0);
            add(left,step,9);step.getLayoutParams().height=dp(44);
        }
        left.addView(new View(this),new LinearLayout.LayoutParams(1,0,1f));
        add(left,label("FIESTA Mk7  /  2015",13,SUB,false),4);
        add(left,label("Eine Oberfläche. Ein System.",12,SUB,false),3);

        right=vertical();right.setPadding(dp(8),dp(7),dp(5),dp(3));
        root.addView(right,new LinearLayout.LayoutParams(0,-1,.66f));
        LinearLayout header=vertical();right.addView(header);
        tiles=vertical();right.addView(tiles,new LinearLayout.LayoutParams(-1,0,1f));
        controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);
        right.addView(controls,new LinearLayout.LayoutParams(-1,-2));
        if(page==0){
            add(header,label("Willkommen in deinem Fiesta",26,FG,true),0);
            add(header,label("Dein Infotainment. Navigation, Medien, Telefon und Fahrzeug – nahtlos vereint.",14,SUB,false),7);
            tile("◆","Fiesta Unified","Optimiert für die Headunit und das Fahrzeugprofil","Bereit",null);
            tile("▣","Eine Bedienoberfläche","Android Auto als Vollbild oder Fenster im Fiesta-Dashboard",null,null);
            button("Einrichtung starten",true,()->go(1));
            button("Später",false,this::finishSetup);
        }else if(page==1){
            add(header,label("Dein Fahrzeug",26,FG,true),0);
            add(header,label("Die Darstellung und Kernfunktionen sind auf deinen Fiesta abgestimmt.",14,SUB,false),7);
            tile("▣","Ford Fiesta Mk7","2015 · Fahrzeugprofil W-S 107","Gespeichert",null);
            tile("◉","Raise CAN-Decoder","Fahrzeugsignale nur bei zugänglicher OEM-Schnittstelle","Automatisch",null);
            tile("⚡","Ressourcenschonend","Native Oberfläche für 1 GB RAM · keine WebView",null,null);
            button("Zurück",false,()->go(0));button("Weiter",true,()->go(2));
        }else if(page==2){
            add(header,label("Verbindungen",26,FG,true),0);
            add(header,label("Schnittstellen prüfen und nur bei Bedarf konfigurieren.",14,SUB,false),5);
            tile("ᛒ","Bluetooth","Smartphone und Freisprechschnittstelle",bt(),()->
                settings(Settings.ACTION_BLUETOOTH_SETTINGS));
            tile("◉","WLAN","Kabellose Android-Auto-Verbindung",wifi(),()->
                settings(Settings.ACTION_WIFI_SETTINGS));
            WifiP2pManager p=(WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
            tile("⇄","Wi-Fi Direct","Native Verbindungsunterstützung",p==null?"Nicht erkannt":"Vorhanden",null);
            button("Zurück",false,()->go(1));button("Weiter",true,()->go(3));
        }else{
            add(header,label("Ansicht auswählen",26,FG,true),0);
            add(header,label("Jederzeit während der Fahrt über die Ansichtssteuerung umschalten.",14,SUB,false),7);
            boolean pip=getSharedPreferences("dynamicos_setup",MODE_PRIVATE).getInt("viewmode",0)==1;
            tile("▣","Fiesta + Android Auto","Android Auto als Fenster im Fiesta-Dashboard",pip?"Ausgewählt":"Wählen",()->
                {getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putInt("viewmode",1).apply();render();});
            tile("⛶","Android Auto Vollbild","Android Auto nutzt die gesamte Displayfläche",!pip?"Ausgewählt":"Wählen",()->
                {getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putInt("viewmode",0).apply();render();});
            button("Zurück",false,()->go(2));button("Fiesta starten",true,this::finishSetup);
        }
        setContentView(root);
    }
    @Override public void onBackPressed(){if(page>0)go(page-1);else super.onBackPressed();}
}'''
(javadir/"SetupActivity.java").write_text(setup)


# Native connectivity diagnostics use the Android platform's existing Bluetooth
# and Wi-Fi drivers; no separate radio-control APK, no fake driver implementation.
wireless=r'''package de.vd3.fiesta;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.Settings;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;

public final class WirelessSetupActivity extends Activity {
    private LinearLayout body;
    private TextView status;
    private void action(String caption, Runnable run) {
        Button b = new Button(this); b.setAllCaps(false); b.setText(caption);
        b.setOnClickListener(v -> run.run()); body.addView(b);
    }
    private void settings(String name) {
        try { startActivity(new Intent(name)); }
        catch (Exception e) { status.setText("Firmware stellt diese Systemeinstellung nicht bereit."); }
    }
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(24,16,24,16); body.setBackgroundColor(Color.rgb(7,20,38));
        TextView heading = new TextView(this); heading.setText("dynamicOS Fiesta\nWireless Android Auto");
        heading.setTextColor(Color.WHITE); heading.setTextSize(25); body.addView(heading);
        status = new TextView(this); status.setTextColor(0xffc6deee); status.setTextSize(16);
        status.setPadding(0,12,0,12); body.addView(status);
        action("Funkmodule prüfen", this::updateStatus);
        action("Smartphone mit Headunit-Bluetooth koppeln", () -> settings(Settings.ACTION_BLUETOOTH_SETTINGS));
        action("WLAN / Hotspot der Headunit konfigurieren", () -> settings(Settings.ACTION_WIFI_SETTINGS));
        action("Android-Auto-Funkmodus und Hersteller-Adapter konfigurieren", () ->
            startActivity(new Intent(this, com.andrerinas.openheadunit.main.SettingsActivity.class)));
        action("Android Auto aktivieren", () ->
            startActivity(new Intent(this, com.andrerinas.openheadunit.main.MainActivity.class)));
        action("Zum Fiesta-Launcher", this::finish);
        TextView note=new TextView(this);
        note.setText("Die integrierte Empfänger-Engine nutzt Android-Wi-Fi-Direct oder den vorhandenen Hotspot sowie den Bluetooth-RFCOMM-Handshake. Bei QS9008-Radios mit separatem OEM-Bluetooth-Modul kann die Kopplung nur funktionieren, wenn der passende Herstellertransport unterstützt wird. Diese Diagnose bestätigt keine HFP- oder AA-Verbindung.");
        note.setTextColor(0xffa4bfce); note.setTextSize(13); note.setPadding(0,12,0,0);
        body.addView(note); scroll.addView(body); setContentView(scroll);
        updateStatus();
    }
    private void updateStatus() {
        BluetoothManager bm=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter=bm==null?null:bm.getAdapter();
        WifiManager wifi=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiP2pManager p2p=(WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        String bt=adapter==null?"Kein Android-Bluetooth-Adapter":(adapter.isEnabled()?"Eingeschaltet":"Ausgeschaltet");
        String wl=wifi==null?"Kein Android-WLAN-Manager":(wifi.isWifiEnabled()?"Eingeschaltet":"Ausgeschaltet");
        String p2pState=p2p==null?"Nicht verfügbar":"Manager vorhanden (Funktion nicht verifiziert)";
        status.setText("Native Headunit-Schnittstellen:\nBluetooth: "+bt+"\nWLAN: "+wl+"\nWi-Fi Direct: "+p2pState+
            "\n\nFunkmodul-Zugriff erfolgt über Android-Systemdienste; Herstellerfunktionen können zusätzliche Berechtigungen erfordern.");
    }
    @Override protected void onResume() { super.onResume(); if(status!=null)updateStatus(); }
}
''';
(javadir/"WirelessSetupActivity.java").write_text(wireless)

manifest=src/"AndroidManifest.xml"
m=manifest.read_text()
# The Fiesta launcher is the single visible Android launcher and HOME candidate.
main_start=m.index('<activity\n            android:name=".main.MainActivity"')
filter_start=m.index('<!-- Primary Launcher Filter -->', main_start)
filter_end=m.index('</intent-filter>',filter_start)+len('</intent-filter>')
m=m[:filter_start]+m[filter_end:]
addon='''\n        <!-- One installed app: native Fiesta HOME, setup, and Android Auto receiver. -->
        <activity android:name="de.vd3.fiesta.LauncherActivity" android:exported="true"
            android:screenOrientation="landscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
        <activity android:name="de.vd3.fiesta.SetupActivity" android:exported="false"
            android:screenOrientation="landscape" />
        <activity android:name="de.vd3.fiesta.WirelessSetupActivity" android:exported="false"
            android:screenOrientation="landscape" />
        <activity android:name="de.vd3.fiesta.MainActivity" android:exported="false"
            android:screenOrientation="landscape" />
'''
m=m.replace('</application>',addon+'\n    </application>')
manifest.write_text(m)

gradle=root/"app/build.gradle.kts"
g=gradle.read_text()
g=g.replace('applicationId = "com.andrerinas.headunitrevived"','applicationId = "de.vd3.fiesta.unified"')
g=g.replace('versionName = "4.0.0-beta2"','versionName = "1.6.0-unified"')
g=g.replace('versionCode = 122','versionCode = 106')
# Fiesta uses API 23+ and original launcher was built against 28; retain the receiver
# target behavior until verified on the physical Android 8.1 head unit.
g=g.replace('create("github") {\n            dimension = "distribution"',
            'create("github") {\n            dimension = "distribution"\n            minSdk = 23')
gradle.write_text(g)

# Mark the receiver app identity throughout settings.
for folder in ("values","values-de"):
    path=src/"res"/folder/"strings.xml"
    if path.exists():
        s=path.read_text()
        s=re.sub(r'(<string name="app_name">).*?(</string>)',r'\1dynamicOS Fiesta\2',s)
        s=re.sub(r'(<string name="title">).*?(</string>)',r'\1dynamicOS Fiesta\2',s)
        path.write_text(s)

# Install a lightweight Ford-inspired control dock over the projection, keeping the
# original decoder and transport in the SAME Android application.
proj=src/"java/com/andrerinas/openheadunit/aap/AapProjectionActivity.kt"
k=proj.read_text()
anchor="        setContentView(R.layout.activity_headunit)"
assert k.count(anchor)==1
k=k.replace(anchor,anchor+"\n        installFiestaDock()")
anchor2="    private enum class OverlayState { STARTING, RECONNECTING, HIDDEN }"
assert k.count(anchor2)==1
dock=r'''
    // One app, one view hierarchy: the Fiesta dashboard stays mounted behind
    // the actual Android Auto renderer. The receiver can be FULL or a PiP-like
    // resizable surface; no screenshot or second decoder is used.
    private var fiestaViewMode = 0 // 0 full, 1 dashboard+PiP
    private var fiestaDashboard: LinearLayout? = null
    private var fiestaSwitcher: TextView? = null
    private fun fiestaDp(n: Int) = (resources.displayMetrics.density * n).toInt()
    private fun fiestaText(s: String, sp: Float = 16f, bold: Boolean = false): TextView =
        TextView(this).apply {
            text=s; textSize=sp; setTextColor(Color.WHITE)
            gravity=Gravity.CENTER_VERTICAL
            setPadding(fiestaDp(12),fiestaDp(6),fiestaDp(10),fiestaDp(6))
            if(bold) typeface=android.graphics.Typeface.DEFAULT_BOLD
        }
    private fun fiestaTile(title: String, detail: String, action: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18,42,68))
            setPadding(fiestaDp(6),fiestaDp(4),fiestaDp(6),fiestaDp(4))
            addView(fiestaText(title,17f,true))
            addView(fiestaText(detail,12f))
            minimumHeight=fiestaDp(64)
            setOnClickListener { action() }
        }
    private fun fiestaMediaKey(code: Int) {
        val audio = getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
        val now = android.os.SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now,now,KeyEvent.ACTION_DOWN,code,0))
        audio.dispatchMediaKeyEvent(KeyEvent(now,now,KeyEvent.ACTION_UP,code,0))
    }
    private fun installFiestaDock() {
        val host=findViewById<FrameLayout>(R.id.container) ?: return
        fiestaViewMode = getSharedPreferences("dynamicos_setup",MODE_PRIVATE).getInt("viewmode",0)
        val dashboard=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(7,19,38))
            setPadding(fiestaDp(12),fiestaDp(12),fiestaDp(12),fiestaDp(12))
        }
        fiestaDashboard=dashboard
        dashboard.addView(fiestaText("dynamicOS   |   FORD FIESTA",21f,true))
        dashboard.addView(fiestaText("FIESTA Mk7  ·  W-S 107",13f))
        val tiles=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        tiles.addView(fiestaTile("Telefon","Kopplung / Anruf") { showFiestaCommunication() },
            LinearLayout.LayoutParams(0,-2,1f).apply { rightMargin=fiestaDp(6) })
        tiles.addView(fiestaTile("Medien","Wiedergabe") { showFiestaMedia() },
            LinearLayout.LayoutParams(0,-2,1f).apply { rightMargin=fiestaDp(6) })
        tiles.addView(fiestaTile("Fahrzeug","Fiesta Mk7") {
            startActivity(Intent(this, de.vd3.fiesta.LauncherActivity::class.java))
        },LinearLayout.LayoutParams(0,-2,1f))
        dashboard.addView(tiles,LinearLayout.LayoutParams(-1,-2).apply { topMargin=fiestaDp(12) })
        val modes=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        modes.addView(fiestaTile("Android Auto","Vollbild") { fiestaViewMode=0; applyFiestaSplitLayout() },
            LinearLayout.LayoutParams(0,-2,1f).apply { rightMargin=fiestaDp(6) })
        modes.addView(fiestaTile("Verbindung","Telefon einrichten") {
            showFiestaCommunication()
        },LinearLayout.LayoutParams(0,-2,1f))
        dashboard.addView(modes,LinearLayout.LayoutParams(-1,-2).apply { topMargin=fiestaDp(8) })
        host.addView(dashboard,0,FrameLayout.LayoutParams(-1,-1))
        val switcher=fiestaText("▣  Ansicht",14f,true).apply {
            setBackgroundColor(Color.rgb(15,58,100))
            elevation=fiestaDp(8).toFloat()
            contentDescription="Android Auto Vollbild oder Bild-in-Bild umschalten"
            setOnClickListener {
                fiestaViewMode=if(fiestaViewMode==0)1 else 0
                applyFiestaSplitLayout()
            }
        }
        fiestaSwitcher=switcher
        host.addView(switcher,FrameLayout.LayoutParams(fiestaDp(104),fiestaDp(48),Gravity.RIGHT or Gravity.TOP))
        applyFiestaSplitLayout()
    }
    private fun applyFiestaSplitLayout() {
        val dashboard=fiestaDashboard ?: return
        val floating=fiestaViewMode==1
        dashboard.visibility=if(floating)View.VISIBLE else View.GONE
        getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putInt("viewmode",fiestaViewMode).apply()
        if(::projectionView.isInitialized) {
            val view=projectionView as View
            val lp=(view.layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(-1,-1)
            val w=resources.displayMetrics.widthPixels
            val h=resources.displayMetrics.heightPixels
            if(floating) {
                // PiP occupies the right 61% of a 1024x600 display, while the
                // Fiesta dashboard remains visible on the left and underneath.
                lp.width=(w*0.61f).toInt()
                lp.height=(h*0.73f).toInt()
                lp.gravity=Gravity.RIGHT or Gravity.BOTTOM
                lp.setMargins(0,fiestaDp(64),fiestaDp(13),fiestaDp(14))
                view.elevation=fiestaDp(5).toFloat()
            } else {
                lp.width=FrameLayout.LayoutParams.MATCH_PARENT
                lp.height=FrameLayout.LayoutParams.MATCH_PARENT
                lp.gravity=Gravity.TOP or Gravity.LEFT
                lp.setMargins(0,0,0,0)
                view.elevation=0f
            }
            view.layoutParams=lp
            view.requestLayout()
        }
        fiestaSwitcher?.bringToFront()
    }
    private fun showFiestaCommunication() {
        val state = try {
            val manager=getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            when { manager?.adapter==null -> "Kein Android-Bluetooth"
                manager.adapter.isEnabled -> "Bluetooth eingeschaltet"
                else -> "Bluetooth ausgeschaltet" }
        } catch (_: Exception) { "Hersteller-Bluetooth nicht über Android erreichbar" }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Fiesta  /  Telefon")
            .setMessage("$state\n\nTelefonie und Freisprechen erfordern ein von der Firmware unterstütztes HFP-Profil.")
            .setPositiveButton("Koppeln") { _, _ ->
                startActivity(Intent(this,de.vd3.fiesta.WirelessSetupActivity::class.java))
            }
            .setNeutralButton("Wählen") { _, _ ->
                try { startActivity(Intent(Intent.ACTION_DIAL)) }
                catch (_: Exception) { Toast.makeText(this,"Telefonie nicht verfügbar",Toast.LENGTH_LONG).show() }
            }
            .setNegativeButton("Schließen",null).show()
    }
    private fun showFiestaMedia() {
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(6,8,6,8) }
        fun add(label:String,code:Int) {
            row.addView(fiestaText(label,21f,true).apply {
                gravity=Gravity.CENTER
                setOnClickListener { fiestaMediaKey(code) }
            },LinearLayout.LayoutParams(0,fiestaDp(56),1f))
        }
        add("❮❮",KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        add("▶Ⅱ",KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        add("❯❯",KeyEvent.KEYCODE_MEDIA_NEXT)
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Fiesta  /  Medien")
            .setView(row).setPositiveButton("Schließen",null).show()
    }
'''
k=k.replace(anchor2,dock+anchor2)
render_anchor="        container.addView(view)"
assert k.count(render_anchor)==1
k=k.replace(render_anchor,render_anchor+"\n        applyFiestaSplitLayout()")
proj.write_text(k)

print("Single-APK merge completed; Fiesta and receiver share one applicationId and one APK.")

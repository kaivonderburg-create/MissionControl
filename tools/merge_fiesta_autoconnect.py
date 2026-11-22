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
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.Settings;

/** Lean four-stage, TV-style touch onboarding, entirely inside the unified APK. */
public final class SetupActivity extends Activity {
  private static final int BG=0xff071326, PANEL=0xff102740, BLUE=0xff177fe9;
  private static final int WHITE=0xfff7fbff, MUTED=0xff9fb8cf;
  private int page=0;
  private LinearLayout root,body,footer;
  private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
  private GradientDrawable shape(int color,int stroke) {
    GradientDrawable d=new GradientDrawable();
    d.setColor(color);d.setCornerRadius(dp(15));
    if(stroke!=0)d.setStroke(dp(1),stroke);
    return d;
  }
  private TextView text(String text,int sp,int color,boolean bold) {
    TextView t=new TextView(this);t.setText(text);t.setTextSize(sp);t.setTextColor(color);
    if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    return t;
  }
  private void add(LinearLayout target,View view,int top) {
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
    lp.topMargin=dp(top);target.addView(view,lp);
  }
  private void button(LinearLayout target,String title,boolean primary,Runnable run){
    TextView b=text(title,17,WHITE,true);b.setGravity(Gravity.CENTER);
    b.setMinHeight(dp(51));b.setPadding(dp(14),dp(9),dp(14),dp(9));
    b.setBackground(shape(primary?BLUE:PANEL,primary?0:0xff315170));
    b.setOnClickListener(v->run.run());add(target,b,8);
  }
  private void card(String icon,String title,String detail,String status,Runnable action){
    LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(18),dp(13),dp(18),dp(13));
    card.setBackground(shape(PANEL,0xff284762));
    add(card,text(icon+"  "+title,18,WHITE,true),0);
    add(card,text(detail,13,MUTED,false),5);
    if(status!=null)add(card,text(status,13,0xff76dcb1,true),7);
    if(action!=null)card.setOnClickListener(v->action.run());
    add(body,card,10);
  }
  private void start(Intent i){
    try{startActivity(i);}catch(Exception e){android.widget.Toast.makeText(this,"Systemfunktion auf dieser Headunit nicht verfügbar",android.widget.Toast.LENGTH_LONG).show();}
  }
  private String bt(){
    try {
      BluetoothManager manager=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
      BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
      return adapter==null?"Nicht verfügbar":adapter.isEnabled()?"Eingeschaltet":"Ausgeschaltet";
    }catch(Exception e){return "Zugriff eingeschränkt";}
  }
  private String wifi(){
    try{
      WifiManager manager=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
      return manager==null?"Nicht verfügbar":manager.isWifiEnabled()?"Eingeschaltet":"Ausgeschaltet";
    }catch(Exception e){return "Zugriff eingeschränkt";}
  }
  private boolean p2p(){
    return getSystemService(Context.WIFI_P2P_SERVICE) instanceof WifiP2pManager;
  }
  @Override public void onCreate(Bundle saved){
    super.onCreate(saved);if(saved!=null)page=saved.getInt("page",0);render();
  }
  @Override protected void onSaveInstanceState(Bundle out){out.putInt("page",page);super.onSaveInstanceState(out);}
  @Override protected void onResume(){super.onResume();if(root!=null&&page>0&&page<3)render();}
  private void gotoPage(int n){page=n;render();}
  private void finishSetup(){
    getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putBoolean("complete",true).apply();
    start(new Intent(this,LauncherActivity.class));finish();
  }
  private void render(){
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(BG);root.setPadding(dp(24),dp(12),dp(24),dp(12));
    TextView brand=text("dynamicOS   /   FIESTA UNIFIED",15,WHITE,true);
    add(root,brand,0);
    LinearLayout progress=new LinearLayout(this);progress.setOrientation(LinearLayout.HORIZONTAL);
    for(int n=0;n<4;n++){
      TextView dot=text(n==page?"●":"●",20,n<=page?0xff2395ff:0xff40546a,true);
      dot.setGravity(Gravity.CENTER);
      progress.addView(dot,new LinearLayout.LayoutParams(0,dp(28),1));
    }
    add(root,progress,3);
    add(root,text("Schritt "+(page+1)+" von 4",13,MUTED,false),0);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
    body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(0,dp(10),0,dp(14));scroll.addView(body);
    root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    footer=new LinearLayout(this);footer.setOrientation(LinearLayout.HORIZONTAL);
    root.addView(footer,new LinearLayout.LayoutParams(-1,-2));
    if(page==0){
      add(body,text("Willkommen bei dynamicOS",27,WHITE,true),7);
      add(body,text("Dein Ford Fiesta. Eine Oberfläche für Navigation, Telefon und Medien.",16,MUTED,false),8);
      card("◇","Einfach in vier Schritten","Verbinden, Funkmodule prüfen und direkt losfahren.",null,null);
      button(footer,"Einrichtung starten",true,()->gotoPage(1));
      button(footer,"Später",false,this::finishSetup);
    }else if(page==1){
      add(body,text("Smartphone verbinden",27,WHITE,true),0);
      add(body,text("Kopple dein Smartphone mit der Bluetooth-Schnittstelle der Headunit. Danach kann Android Auto die WLAN-Verbindung aufbauen.",15,MUTED,false),7);
      card("ᛒ","Bluetooth-Kopplung","Android-Systemeinstellungen öffnen","Status: "+bt(),()->start(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
      card("➤","Android Auto kabellos","Native Receiver-Konfiguration öffnen",null,
        ()->start(new Intent(this,com.andrerinas.openheadunit.main.SettingsActivity.class)));
      button(footer,"Zurück",false,()->gotoPage(0));
      button(footer,"Weiter",true,()->gotoPage(2));
    }else if(page==2){
      add(body,text("Verbindungen prüfen",27,WHITE,true),0);
      add(body,text("Die Anzeige prüft vorhandene Android-Schnittstellen. Sie bestätigt noch keine erfolgreiche Projektion.",14,MUTED,false),7);
      card("ᛒ","Bluetooth","Kopplung über die vorhandene Android-/OEM-Funkschnittstelle",bt(),()->start(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
      card("◉","WLAN / Hotspot","Für den kabellosen Android-Auto-Datenkanal",wifi(),()->start(new Intent(Settings.ACTION_WIFI_SETTINGS)));
      card("⇄","Wi-Fi Direct","Erforderlich bei nativer P2P-Verbindung",p2p()?"Manager vorhanden":"Nicht verfügbar",null);
      card("⚙","Erweiterte Funkkonfiguration","Herstellermodul, Wi-Fi Direct und Hotspot in AutoConnect einstellen",null,
        ()->start(new Intent(this,com.andrerinas.openheadunit.main.SettingsActivity.class)));
      button(footer,"Zurück",false,()->gotoPage(1));
      button(footer,"Weiter",true,()->gotoPage(3));
    }else{
      add(body,text("Einrichtung abgeschlossen",27,WHITE,true),0);
      add(body,text("dynamicOS Fiesta ist vorbereitet. Die tatsächliche Verbindung wird beim Start von Android Auto geprüft.",15,MUTED,false),8);
      card("⌂","Startseite","Alle Funktionen in einer Oberfläche",null,this::finishSetup);
      card("➤","Android Auto","Receiver starten und Smartphone verbinden",null,
        ()->start(new Intent(this,com.andrerinas.openheadunit.main.MainActivity.class)));
      button(footer,"Zurück",false,()->gotoPage(2));
      button(footer,"Jetzt loslegen",true,this::finishSetup);
    }
    setContentView(root);
  }
  @Override public void onBackPressed(){if(page>0)gotoPage(page-1);else super.onBackPressed();}
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
g=g.replace('versionName = "4.0.0-beta2"','versionName = "1.4.0-unified"')
g=g.replace('versionCode = 122','versionCode = 104')
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
    // The projection renderer is hosted in this Activity, not launched via a
    // separate package. The dock and the projected surface coexist in one View tree.
    private var fiestaSplitEnabled = true
    private var fiestaPanel: LinearLayout? = null
    private var fiestaToggle: TextView? = null
    private val fiestaPanelWidth: Int get() = (resources.displayMetrics.density * 228).toInt()
    private fun fiestaLabel(label: String, size: Float = 15f): TextView =
        TextView(this).apply {
            text = label
            textSize = size
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            setPadding(12, 5, 8, 5)
        }
    private fun fiestaAction(label: String, action: () -> Unit): TextView =
        fiestaLabel(label, 17f).apply {
            minHeight = (resources.displayMetrics.density * 44).toInt()
            setBackgroundColor(Color.rgb(17, 49, 78))
            setOnClickListener { action() }
        }
    private fun fiestaMediaKey(code: Int) {
        val audio = getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
        val now = android.os.SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
    }
    private fun installFiestaDock() {
        val host = findViewById<FrameLayout>(R.id.container) ?: return
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(7, 24, 45))
            setPadding(4, 6, 4, 6)
        }
        fiestaPanel = panel
        panel.addView(fiestaLabel("dynamicOS  /  FIESTA", 15f))
        panel.addView(fiestaAction("⌂  Startseite") {
            startActivity(Intent(this, de.vd3.fiesta.LauncherActivity::class.java))
        })
        panel.addView(fiestaAction("☎  Telefon") {
            showFiestaCommunication()
        })
        panel.addView(fiestaAction("♫  Medien") {
            showFiestaMedia()
        })
        panel.addView(fiestaAction("⚙  Verbindung") {
            startActivity(Intent(this, de.vd3.fiesta.WirelessSetupActivity::class.java))
        })
        panel.addView(fiestaAction("⛶  Vollbild / geteilt") {
            fiestaSplitEnabled = !fiestaSplitEnabled
            applyFiestaSplitLayout()
        })
        host.addView(panel, FrameLayout.LayoutParams(fiestaPanelWidth,
            FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT))
        val toggle = fiestaAction("▣") {
            fiestaSplitEnabled = !fiestaSplitEnabled
            applyFiestaSplitLayout()
        }
        fiestaToggle = toggle
        host.addView(toggle, FrameLayout.LayoutParams(
            (resources.displayMetrics.density * 44).toInt(),
            (resources.displayMetrics.density * 44).toInt(),
            Gravity.LEFT or Gravity.TOP))
        toggle.bringToFront()
    }
    private fun applyFiestaSplitLayout() {
        val host = findViewById<FrameLayout>(R.id.container) ?: return
        val panel = fiestaPanel ?: return
        panel.visibility = if (fiestaSplitEnabled) View.VISIBLE else View.GONE
        if (::projectionView.isInitialized) {
            val view = projectionView as View
            // Make actual room for the panel instead of obscuring projection pixels.
            // Projection size and touch geometry follow the renderer surface resize.
            val lp = (view.layoutParams as? FrameLayout.LayoutParams)
                ?: FrameLayout.LayoutParams(-1, -1)
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT
            lp.height = FrameLayout.LayoutParams.MATCH_PARENT
            lp.rightMargin = if (fiestaSplitEnabled) fiestaPanelWidth else 0
            lp.gravity = Gravity.LEFT or Gravity.TOP
            view.layoutParams = lp
            view.requestLayout()
            panel.bringToFront()
            fiestaToggle?.bringToFront()
        }
    }
    private fun showFiestaCommunication() {
        val bluetooth = (getSystemService(android.content.Context.BLUETOOTH_SERVICE)
            as? android.bluetooth.BluetoothManager)?.adapter
        val state = when {
            bluetooth == null -> "Bluetooth nicht verfügbar"
            bluetooth.isEnabled -> "Bluetooth eingeschaltet"
            else -> "Bluetooth ausgeschaltet"
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("dynamicOS  ·  Kommunikation")
            .setMessage("$state\n\nTelefonie über das gekoppelte Smartphone setzt ein erreichbares HFP-Telefonieprofil voraus. Die Freisprechfunktion muss von der Radio-Firmware bereitgestellt werden.")
            .setPositiveButton("Bluetooth koppeln") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            .setNeutralButton("Wählen") { _, _ ->
                try { startActivity(Intent(Intent.ACTION_DIAL)) }
                catch (_: Exception) {
                    Toast.makeText(this, "Keine Telefonie-Schnittstelle verfügbar", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Schließen", null)
            .show()
    }
    private fun showFiestaMedia() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
        }
        fun media(label: String, code: Int) {
            row.addView(fiestaAction(label) { fiestaMediaKey(code) },
                LinearLayout.LayoutParams(0, -2, 1f))
        }
        media("❮❮", KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        media("▶Ⅱ", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        media("❯❯", KeyEvent.KEYCODE_MEDIA_NEXT)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("dynamicOS  ·  Medien")
            .setView(row)
            .setPositiveButton("Schließen", null)
            .show()
    }

'''
k=k.replace(anchor2,dock+anchor2)
render_anchor="        container.addView(view)"
assert k.count(render_anchor)==1
k=k.replace(render_anchor,render_anchor+"\n        applyFiestaSplitLayout()")
proj.write_text(k)

print("Single-APK merge completed; Fiesta and receiver share one applicationId and one APK.")

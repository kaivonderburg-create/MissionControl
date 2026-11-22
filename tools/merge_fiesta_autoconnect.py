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
            '    private void openWirelessSetup() { startActivity(new Intent(this, WirelessSetupActivity.class)); }\\n\\n    private void openMusic() {')
    (javadir/name).write_text(data)
(src/"res/drawable-nodpi").mkdir(parents=True,exist_ok=True)
shutil.copy2(fiesta/"res/drawable-nodpi/fiesta_graphite.png",src/"res/drawable-nodpi/fiesta_graphite.png")

setup='''package de.vd3.fiesta;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
public final class SetupActivity extends Activity {
  private LinearLayout layout;
  private void button(String title, Runnable action) {
    Button b=new Button(this); b.setAllCaps(false); b.setText(title);
    layout.addView(b,new LinearLayout.LayoutParams(-1,-2)); b.setOnClickListener(v->action.run());
  }
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(32,20,32,20);layout.setGravity(Gravity.CENTER_VERTICAL);
    layout.setBackgroundColor(Color.rgb(7,15,27));
    TextView title=new TextView(this);title.setText("dynamicOS  /  FIESTA\\nErsteinrichtung");
    title.setTextSize(27);title.setTextColor(Color.WHITE);layout.addView(title);
    TextView info=new TextView(this);
    info.setText("Einmalige Einrichtung für Bluetooth, WLAN und den integrierten Android-Auto-Empfänger. Die benötigten Funkberechtigungen müssen vom Gerät gewährt werden.");
    info.setTextSize(16); info.setTextColor(0xffaec5d4);info.setPadding(0,18,0,20);
    layout.addView(info);
    button("1  Native Funkverbindung einrichten",()->startActivity(new Intent(this,WirelessSetupActivity.class)));
    button("2  WLAN und Bluetooth prüfen",()->startActivity(new Intent(this,WirelessSetupActivity.class)));
    button("3  Android Auto konfigurieren",()->startActivity(new Intent(this,com.andrerinas.openheadunit.main.MainActivity.class)));
    button("4  Einrichtung abschließen",()->{
      getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putBoolean("complete",true).apply();
      startActivity(new Intent(this,LauncherActivity.class));finish();
    });
    setContentView(layout);
  }
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
g=g.replace('versionName = "4.0.0-beta2"','versionName = "1.2.0-unified"')
g=g.replace('versionCode = 122','versionCode = 102')
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

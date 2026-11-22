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
    button("1  Bluetooth-Kopplung",()->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
    button("2  WLAN / Hotspot",()->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
    button("3  Android Auto konfigurieren",()->startActivity(new Intent(this,com.andrerinas.openheadunit.main.MainActivity.class)));
    button("4  Einrichtung abschließen",()->{
      getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putBoolean("complete",true).apply();
      startActivity(new Intent(this,LauncherActivity.class));finish();
    });
    setContentView(layout);
  }
}'''
(javadir/"SetupActivity.java").write_text(setup)

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
        <activity android:name="de.vd3.fiesta.MainActivity" android:exported="false"
            android:screenOrientation="landscape" />
'''
m=m.replace('</application>',addon+'\n    </application>')
manifest.write_text(m)

gradle=root/"app/build.gradle.kts"
g=gradle.read_text()
g=g.replace('applicationId = "com.andrerinas.headunitrevived"','applicationId = "de.vd3.fiesta.unified"')
g=g.replace('versionName = "4.0.0-beta2"','versionName = "1.0.0-unified"')
g=g.replace('versionCode = 122','versionCode = 100')
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
print("Single-APK merge completed; Fiesta and receiver share one applicationId and one APK.")

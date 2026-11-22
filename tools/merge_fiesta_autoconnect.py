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
(javadir/"MainActivity.java").write_text((fiesta/"java/de/vd3/fiesta/MainActivity.java").read_text().replace("package de.vd3.fiesta;", "package de.vd3.fiesta;\nimport com.andrerinas.openheadunit.R;"))
launcher=r'''package de.vd3.fiesta;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.provider.Settings;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.andrerinas.openheadunit.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** dynamicOS Fiesta's independent SYNC-style home. One activity, no WebView, no external UI bridge. */
public final class LauncherActivity extends Activity {
    private static final int BG=0xff081421, PANEL=0xff10263b, PANEL2=0xff17334e;
    private static final int WHITE=0xfff7faff, MUTED=0xffa5bfd5, BLUE=0xff53a9ff, GREEN=0xff82dab2;
    private static final String[] TABS={"Start","Navigation","Telefon","Medien","Fahrzeug","System"};
    private int page=0;
    private LinearLayout root, content, dock;
    private boolean started=false;
    private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
    private GradientDrawable bg(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private TextView text(String s,int sp,boolean bold,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);t.setSingleLine(false);t.setMaxLines(2);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private void add(LinearLayout p,View v,int top){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);p.addView(v,lp);
    }
    private TextView action(String title,String sub,Runnable call) {
        TextView b=text(title+"     ›",18,true,WHITE);
        b.setContentDescription(title+" "+sub);
        b.setPadding(dp(17),dp(10),dp(14),dp(10));
        b.setBackground(bg(PANEL2,13));
        b.setOnClickListener(v->call.run());
        return b;
    }
    private LinearLayout card(String title,String info,Runnable click){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(dp(18),dp(14),dp(18),dp(14));l.setBackground(bg(PANEL,14));
        add(l,text(title,19,true,WHITE),0);add(l,text(info,13,false,MUTED),7);
        if(click!=null)l.setOnClickListener(v->click.run());
        return l;
    }
    private void openActivity(Class<?> activity){
        try{startActivity(new Intent(this,activity).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));}
        catch(Exception e){Toast.makeText(this,"Funktion nicht verfügbar",Toast.LENGTH_LONG).show();}
    }
    private void openReceiver(){openActivity(com.andrerinas.openheadunit.main.MainActivity.class);}
    private void media(int key){
        AudioManager audio=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if(audio==null)return;
        long t=android.os.SystemClock.uptimeMillis();
        audio.dispatchMediaKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_DOWN,key,0));
        audio.dispatchMediaKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_UP,key,0));
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        if(saved!=null)page=saved.getInt("page",0);
        started=getSharedPreferences("dynamicos_setup",MODE_PRIVATE).getBoolean("complete",false);
        if(!started){openActivity(SetupActivity.class);finish();return;}
        showPage();
    }
    @Override public void onSaveInstanceState(Bundle out){out.putInt("page",page);super.onSaveInstanceState(out);}
    @Override protected void onResume(){super.onResume();if(started)showPage();}
    private void jump(int index){page=index;showPage();}
    private void showPage(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        root.setPadding(dp(17),dp(10),dp(17),dp(8));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("dynamicOS  |  FIESTA",20,true,WHITE);
        bar.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView clock=text(new SimpleDateFormat("HH:mm",Locale.GERMANY).format(new Date()),16,true,MUTED);
        clock.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        bar.addView(clock,new LinearLayout.LayoutParams(dp(100),dp(48)));
        root.addView(bar);
        View line=new View(this);line.setBackgroundColor(0xff22465f);
        root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0,dp(13),0,dp(12));
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1f));
        if(page==0)home();
        else if(page==1)navigation();
        else if(page==2)phone();
        else if(page==3)music();
        else if(page==4)vehicle();
        else system();
        dock=new LinearLayout(this);dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);dock.setPadding(dp(3),dp(3),dp(3),dp(3));
        dock.setBackground(bg(0xff112c45,16));
        for(int i=0;i<TABS.length;i++){
            final int n=i;
            TextView tab=text(TABS[i],14,i==page,i==page?BLUE:WHITE);
            tab.setGravity(Gravity.CENTER);tab.setMinHeight(dp(58));
            if(i==page)tab.setBackground(bg(0xff214560,12));
            tab.setOnClickListener(v->jump(n));
            dock.addView(tab,new LinearLayout.LayoutParams(0,dp(58),1));
        }
        root.addView(dock);
        setContentView(root);
    }
    private void home(){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(row,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout car=card("FORD FIESTA Mk7","2015  •  W-S 107",()->jump(4));
        ImageView image=new ImageView(this);
        image.setImageResource(R.drawable.fiesta_graphite);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        car.addView(image,0,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout.LayoutParams carLp=new LinearLayout.LayoutParams(0,-1,.40f);carLp.rightMargin=dp(12);
        row.addView(car,carLp);
        LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);
        row.addView(right,new LinearLayout.LayoutParams(0,-1,.60f));
        TextView prompt=text("Guten Tag. Wohin geht die Fahrt?",22,true,WHITE);
        add(right,prompt,0);
        TextView subtitle=text("Alles in einem System. Bereit für Navigation, Telefon und Musik.",13,false,MUTED);
        add(right,subtitle,5);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams topLp=new LinearLayout.LayoutParams(-1,0,1f);topLp.topMargin=dp(12);
        right.addView(top,topLp);
        LinearLayout nav=card("Navigation","Android Auto und Karten",this::openReceiver);
        LinearLayout med=card("Musik","Wiedergabe und Medien",()->jump(3));
        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-1,1f);half.rightMargin=dp(8);
        top.addView(nav,half);top.addView(med,new LinearLayout.LayoutParams(0,-1,1f));
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bottomLp=new LinearLayout.LayoutParams(-1,0,1f);bottomLp.topMargin=dp(8);
        right.addView(bottom,bottomLp);
        LinearLayout phone=card("Telefon","Verbinden und anrufen",()->jump(2));
        LinearLayout auto=card("Android Auto","Vollbild oder Fiesta-Ansicht",this::openReceiver);
        LinearLayout.LayoutParams half2=new LinearLayout.LayoutParams(0,-1,1f);half2.rightMargin=dp(8);
        bottom.addView(phone,half2);bottom.addView(auto,new LinearLayout.LayoutParams(0,-1,1f));
    }
    private void heading(String title,String sub){
        add(content,text(title,26,true,WHITE),0);
        add(content,text(sub,14,false,MUTED),6);
    }
    private void navigation(){
        heading("Navigation","Android Auto ist Teil des Fiesta-Infotainments.");
        add(content,action("Karten und Navigation öffnen","Projektion",this::openReceiver),19);
        add(content,action("Fiesta-Dashboard","Zur Startseite",()->jump(0)),9);
    }
    private void phone(){
        heading("Telefon","Ein zentraler Bereich für Verbindung und Anrufe.");
        add(content,action("Smartphone verbinden","Bluetooth",()->openActivity(WirelessSetupActivity.class)),19);
        add(content,action("Wähltastatur öffnen","Verfügbare Telefonie",()->{
            try{startActivity(new Intent(Intent.ACTION_DIAL));}
            catch(Exception e){Toast.makeText(this,"Telefonie von der Headunit nicht bereitgestellt",Toast.LENGTH_LONG).show();}
        }),9);
    }
    private void music(){
        heading("Medien","Wiedergabe steuern, ohne das Fiesta-System zu verlassen.");
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<3;i++){
            final int key=(i==0?KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                i==1?KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:KeyEvent.KEYCODE_MEDIA_NEXT);
            TextView b=text(i==0?"❮❮":i==1?"▶ / Ⅱ":"❯❯",22,true,WHITE);
            b.setGravity(Gravity.CENTER);b.setBackground(bg(PANEL2,14));
            b.setOnClickListener(v->media(key));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(100),1f);
            lp.leftMargin=dp(6);row.addView(b,lp);
        }
        add(content,row,24);
    }
    private void vehicle(){
        heading("Dein Fiesta","Fahrzeugprofil und vorhandene Fahrzeugschnittstellen.");
        add(content,action("Ford Fiesta Mk7 · 2015","W-S 107",()->{}),19);
        add(content,action("Raise CAN-Decoder","Fahrzeugsignale",()->openActivity(de.vd3.fiesta.MainActivity.class)),9);
    }
    private void system(){
        heading("System und Verbindung","Einrichtung und Ansichtsmodus innerhalb von dynamicOS.");
        add(content,action("Ersteinrichtung erneut öffnen","Assistent",()->openActivity(SetupActivity.class)),19);
        add(content,action("Bluetooth und WLAN","Verbindung",()->openActivity(WirelessSetupActivity.class)),9);
        add(content,action("Android Auto-Einstellungen","Integrierter Empfänger",()->openActivity(com.andrerinas.openheadunit.main.SettingsActivity.class)),9);
    }
    @Override public void onBackPressed(){jump(0);}
}'''

# The unified build uses this generated launcher, not the standalone APK's
# launcher. Keep the customer-facing UI and media behavior corrected here.
launcher=launcher.replace('{"Start","Navigation","Telefon","Medien","Fahrzeug","System"}',
                          '{"Start","Audio","Telefon","Navigation","System"}')
launcher=launcher.replace('else if(page==1)navigation();\n        else if(page==2)phone();\n        else if(page==3)music();\n        else if(page==4)vehicle();',
                          'else if(page==1)music();\n        else if(page==2)phone();\n        else if(page==3)navigation();\n        else if(page==4)vehicle();')
# Keep sixth page addressable for vehicle maintenance, but out of the primary tab row.
launcher=launcher.replace('else if(page==4)vehicle();\n        else system();',
                          'else if(page==4)system();\n        else vehicle();')
# Page remapping must occur atomically: music=1, phone=2, navigation=3,
# settings=4 and vehicle diagnostics=5.
assert 'else if(page==4)system();' in launcher
assert 'else vehicle();' in launcher
launcher=launcher.replace('()->jump(4)', '()->jump(5)')
launcher=launcher.replace('()->jump(3)', '()->jump(1)')
launcher=launcher.replace('Navigation","Android Auto und Karten",this::openReceiver',
                          'Navigation","Android Auto und Karten",()->jump(3)')
launcher=launcher.replace('LinearLayout auto=card("Android Auto","Vollbild oder Fiesta-Ansicht",this::openReceiver);',
                          'LinearLayout auto=card("Android Auto","Projektion starten",this::openReceiver);')
launcher=launcher.replace('TextView brand=text("dynamicOS  |  FIESTA",20,true,WHITE);',
                          'TextView brand=text("SYNC  |  dynamicOS",20,true,WHITE);')
launcher=launcher.replace('TextView prompt=text("Guten Tag. Wohin geht die Fahrt?",22,true,WHITE);',
                          'TextView prompt=text("Ford SYNC",25,true,WHITE);')
launcher=launcher.replace('TextView subtitle=text("Alles in einem System. Bereit für Navigation, Telefon und Musik.",13,false,MUTED);',
                          'TextView subtitle=text("Audio  ·  Telefon  ·  Navigation",14,false,MUTED);')
# One primary media path; use Android MediaSession APIs where notification access
# is granted, falling back to standard media keys rather than silently doing nothing.
launcher=launcher.replace('import android.media.AudioManager;',
                          'import android.media.AudioManager;\nimport android.media.MediaMetadata;\nimport android.media.session.MediaController;\nimport android.media.session.MediaSessionManager;\nimport android.media.session.PlaybackState;\nimport android.content.ComponentName;\nimport java.util.List;')
start=launcher.index('    private void media(int key){')
end=launcher.index('    @Override public void onCreate',start)
launcher=launcher[:start]+'''    private MediaController activeMedia(){
        try{
            MediaSessionManager m=(MediaSessionManager)getSystemService(Context.MEDIA_SESSION_SERVICE);
            if(m==null)return null;
            List<MediaController> list=m.getActiveSessions(new ComponentName(this,MediaAccessService.class));
            for(MediaController c:list){
                PlaybackState p=c.getPlaybackState();
                if(p!=null && p.getState()==PlaybackState.STATE_PLAYING)return c;
            }
            return list.isEmpty()?null:list.get(0);
        }catch(SecurityException e){return null;}
    }
    private void media(int key){
        MediaController controller=activeMedia();
        if(controller!=null){
            MediaController.TransportControls t=controller.getTransportControls();
            if(key==KeyEvent.KEYCODE_MEDIA_NEXT)t.skipToNext();
            else if(key==KeyEvent.KEYCODE_MEDIA_PREVIOUS)t.skipToPrevious();
            else{
                PlaybackState state=controller.getPlaybackState();
                if(state!=null && state.getState()==PlaybackState.STATE_PLAYING)t.pause();
                else t.play();
            }
        }else{
            AudioManager audio=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
            if(audio!=null){
                long t=android.os.SystemClock.uptimeMillis();
                audio.dispatchMediaKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_DOWN,key,0));
                audio.dispatchMediaKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_UP,key,0));
            }
        }
        if(page==1)showPage();
    }
''' + launcher[end:]
start=launcher.index('    private void music(){')
end=launcher.index('    private void vehicle(){',start)
launcher=launcher[:start]+'''    private void music(){
        heading("Audio","Wiedergabe aus der aktiven Medienquelle.");
        MediaController controller=activeMedia();
        MediaMetadata meta=controller==null?null:controller.getMetadata();
        String title=meta==null?null:meta.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist=meta==null?null:meta.getString(MediaMetadata.METADATA_KEY_ARTIST);
        TextView name=text(title==null||title.isEmpty()?"Keine aktive Wiedergabe":title,24,true,WHITE);
        name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        add(content,name,18);
        add(content,text(artist==null||artist.isEmpty()?"Musikquelle verbinden":artist,15,false,MUTED),5);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<3;i++){
            final int key=i==0?KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                          i==1?KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:KeyEvent.KEYCODE_MEDIA_NEXT;
            TextView b=text(i==0?"❮":i==1?"▶ / Ⅱ":"❯",23,true,WHITE);
            b.setGravity(Gravity.CENTER);b.setBackground(bg(PANEL2,12));
            b.setOnClickListener(v->media(key));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(75),1f);
            lp.leftMargin=dp(8);row.addView(b,lp);
        }
        add(content,row,16);
        if(controller==null)add(content,action("Medienzugriff erlauben","Optional",()->{
            try{startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));}
            catch(Exception e){Toast.makeText(this,"Einstellung nicht verfügbar",Toast.LENGTH_SHORT).show();}
        }),9);
    }
''' + launcher[end:]
# Actual unified build uses one native receiver; ensure NotificationListenerService is in that APK.
(javadir/"MediaAccessService.java").write_text(
    (fiesta/"java/de/vd3/fiesta/MediaAccessService.java").read_text())

(javadir/"LauncherActivity.java").write_text(launcher)
(src/"res/drawable-nodpi").mkdir(parents=True,exist_ok=True)
shutil.copy2(fiesta/"res/drawable-nodpi/fiesta_graphite.png",src/"res/drawable-nodpi/fiesta_graphite.png")

setup=r'''package de.vd3.fiesta;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.bluetooth.BluetoothManager;
import android.net.wifi.WifiManager;
import com.andrerinas.openheadunit.R;

/** First-run flow sharing the Fiesta system visual and navigation architecture. */
public final class SetupActivity extends Activity {
    private int page=0;
    private static final int BG=0xff081421,BLUE=0xff53a9ff,FG=0xfff7faff,MUTED=0xffa6bfd4,PANEL=0xff142d45;
    private LinearLayout root,body,buttons;
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(13));return d;}
    private TextView label(String s,int sp,boolean bold,int col){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(col);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private void add(LinearLayout parent,View child,int gap){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(gap);parent.addView(child,lp);
    }
    private void button(String caption,boolean accent,Runnable fn){
        TextView t=label(caption,16,true,FG);t.setGravity(Gravity.CENTER);
        t.setBackground(shape(accent?0xff257fd7:PANEL));t.setOnClickListener(v->fn.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1f);lp.leftMargin=dp(5);
        buttons.addView(t,lp);
    }
    private void option(String title,String detail,Runnable select,boolean active){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18),dp(10),dp(18),dp(10));l.setBackground(shape(active?0xff205078:PANEL));
        add(l,label(title,19,true,FG),0);add(l,label(detail,13,false,MUTED),3);
        if(select!=null)l.setOnClickListener(v->select.run());
        add(body,l,10);
    }
    private String wifi(){
        try{WifiManager m=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return m==null?"Hardware nicht erkannt":m.isWifiEnabled()?"WLAN aktiv":"WLAN ausgeschaltet";
        }catch(Exception ex){return "Systemzugriff eingeschränkt";}
    }
    private String bluetooth(){
        try{BluetoothManager m=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            return m==null||m.getAdapter()==null?"OEM-Bluetooth":m.getAdapter().isEnabled()?"Bluetooth aktiv":"Bluetooth ausgeschaltet";
        }catch(Exception ex){return "Systemzugriff eingeschränkt";}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);if(state!=null)page=state.getInt("page",0);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        render();
    }
    @Override public void onSaveInstanceState(Bundle b){b.putInt("page",page);super.onSaveInstanceState(b);}
    @Override public void onBackPressed(){if(page>0){page--;render();}else super.onBackPressed();}
    private void next(int n){page=n;render();}
    private void finishSetup(){
        getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putBoolean("complete",true).apply();
        startActivity(new Intent(this,LauncherActivity.class));finish();
    }
    private void render(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);root.setPadding(dp(19),dp(12),dp(19),dp(11));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(label("dynamicOS  |  FIESTA",19,true,FG),new LinearLayout.LayoutParams(0,dp(49),1f));
        head.addView(label("EINRICHTUNG  ·  "+(page+1)+"/4",13,true,BLUE));
        root.addView(head);
        View line=new View(this);line.setBackgroundColor(0xff245171);root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,0,1f);pp.topMargin=dp(10);root.addView(panel,pp);
        LinearLayout side=new LinearLayout(this);side.setOrientation(LinearLayout.VERTICAL);
        side.setBackground(shape(0xff10283f));side.setPadding(dp(13),dp(13),dp(13),dp(11));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-1,.32f);sp.rightMargin=dp(12);panel.addView(side,sp);
        ImageView car=new ImageView(this);car.setImageResource(R.drawable.fiesta_graphite);
        car.setScaleType(ImageView.ScaleType.FIT_CENTER);
        side.addView(car,new LinearLayout.LayoutParams(-1,0,1f));
        add(side,label("FORD FIESTA",20,true,FG),5);
        add(side,label("Mk7  ·  2015  ·  W-S 107",12,false,MUTED),2);
        LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(dp(8),dp(6),dp(8),dp(5));
        panel.addView(right,new LinearLayout.LayoutParams(0,-1,.68f));
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        right.addView(body,new LinearLayout.LayoutParams(-1,0,1f));
        buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);right.addView(buttons);
        if(page==0){
            add(body,label("Willkommen in deinem Fiesta.",25,true,FG),0);
            add(body,label("Ein System für Navigation, Telefon, Musik und Fahrzeug.",15,false,MUTED),6);
            option("Alles an einem Ort","Die gesamte Bedienung folgt derselben Fiesta-Oberfläche.",null,true);
            option("Android Auto integriert","Zwischen Vollbild und kombinierter Fiesta-Ansicht umschalten.",null,false);
            button("Später",false,this::finishSetup);button("Einrichten",true,()->next(1));
        }else if(page==1){
            add(body,label("Dein Fahrzeug.",25,true,FG),0);
            add(body,label("Fiesta Mk7, 2015, Raise-CAN-Profil.",15,false,MUTED),6);
            option("Fahrzeugprofil","Die Anzeige verwendet das für deinen Fiesta angelegte Profil.",null,true);
            option("Ressourcenschonend","Native Oberfläche ohne WebView oder laufende Designanimationen.",null,false);
            button("Zurück",false,()->next(0));button("Weiter",true,()->next(2));
        }else if(page==2){
            add(body,label("Verbindungen.",25,true,FG),0);
            add(body,label("Das System prüft, welche Funkschnittstellen verfügbar sind.",15,false,MUTED),6);
            option("Bluetooth   ·   "+bluetooth(),"Smartphone und Freisprechen sind abhängig von der OEM-Firmware.",null,false);
            option("WLAN   ·   "+wifi(),"Verbindung für die kabellose Projektion.",null,false);
            button("Zurück",false,()->next(1));button("Weiter",true,()->next(3));
        }else{
            add(body,label("Wähle deine Ansicht.",25,true,FG),0);
            add(body,label("Du kannst jederzeit zwischen beiden Ansichten wechseln.",15,false,MUTED),6);
            final boolean split=getSharedPreferences("dynamicos_setup",MODE_PRIVATE).getInt("viewmode",0)==1;
            option("Fiesta + Android Auto","Gemeinsame Ansicht mit eigenem Projektionsbereich.",()->
                {getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putInt("viewmode",1).apply();render();},split);
            option("Android Auto Vollbild","Projektion nutzt den gesamten Bildschirm.",()->
                {getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putInt("viewmode",0).apply();render();},!split);
            button("Zurück",false,()->next(2));button("Fiesta starten",true,this::finishSetup);
        }
        setContentView(root);
    }
}'''
(javadir/"SetupActivity.java").write_text(setup)


# Native connectivity diagnostics use the Android platform's existing Bluetooth
# and Wi-Fi drivers; no separate radio-control APK, no fake driver implementation.
wireless=r'''package de.vd3.fiesta;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.Settings;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Built-in wireless control screen; no WebView or second launcher. */
public final class WirelessSetupActivity extends Activity {
    private LinearLayout list;
    private TextView state;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;
    }
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private void row(String title,String desc,String status,Runnable action) {
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(dp(15),dp(8),dp(14),dp(8));
        line.setBackground(shape(0xff152e46));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        info.addView(label(title,18,0xfff4faff,true));
        info.addView(label(desc,13,0xffadc5d8,false));
        line.addView(info,new LinearLayout.LayoutParams(0,-2,1f));
        TextView badge=label(status,13,0xff7bdfb3,true);
        badge.setGravity(Gravity.CENTER);
        line.addView(badge,new LinearLayout.LayoutParams(dp(170),dp(48)));
        if(action!=null)line.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1f);
        lp.bottomMargin=dp(9);list.addView(line,lp);
    }
    private void goSettings(String what){
        try{startActivity(new Intent(what));}
        catch(Exception e){Toast.makeText(this,"Systemfunktion nicht verfügbar",Toast.LENGTH_LONG).show();}
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        render();
    }
    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff081421);root.setPadding(dp(20),dp(12),dp(20),dp(12));
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);
        TextView title=label("dynamicOS   |   Verbindung",21,0xfff5faff,true);
        head.addView(title,new LinearLayout.LayoutParams(0,dp(55),1f));
        TextView back=label("‹  Fiesta",16,0xff70bdff,true);
        back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());
        head.addView(back,new LinearLayout.LayoutParams(dp(125),dp(55)));
        root.addView(head);
        root.addView(label("Smartphone und Funkverbindung",24,0xfff5faff,true));
        root.addView(label("Die Verbindungsarten sind im gemeinsamen Fiesta-System verfügbar.",14,0xffb7c9d8,false));
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1f);lp.topMargin=dp(18);
        root.addView(list,lp);
        String bt="Nicht erkannt",wl="Nicht erkannt",p2p="Nicht erkannt";
        try{
            BluetoothManager bm=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter=bm==null?null:bm.getAdapter();
            bt=adapter==null?"OEM-Modul":adapter.isEnabled()?"Aktiv":"Aus";
        }catch(Exception ex){bt="OEM-Modul";}
        try{
            WifiManager wm=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wl=wm==null?"Nicht erkannt":wm.isWifiEnabled()?"Aktiv":"Aus";
        }catch(Exception ex){wl="Nicht zugänglich";}
        try{p2p=getSystemService(Context.WIFI_P2P_SERVICE)!=null?"Verfügbar":"Nicht verfügbar";}
        catch(Exception ignored){p2p="Nicht zugänglich";}
        row("Bluetooth","Smartphone und Anrufprofile",bt,()->goSettings(Settings.ACTION_BLUETOOTH_SETTINGS));
        row("WLAN / Hotspot","Kabelloser Android-Auto-Datenkanal",wl,()->goSettings(Settings.ACTION_WIFI_SETTINGS));
        row("Wi-Fi Direct","Empfänger-Funkverbindung",p2p,null);
        LinearLayout footer=new LinearLayout(this);footer.setOrientation(LinearLayout.HORIZONTAL);
        TextView receiver=label("Android Auto verbinden  ›",18,0xfff5faff,true);
        receiver.setGravity(Gravity.CENTER);receiver.setBackground(shape(0xff267fd5));
        receiver.setOnClickListener(v->startActivity(new Intent(this,
            com.andrerinas.openheadunit.main.MainActivity.class)));
        footer.addView(receiver,new LinearLayout.LayoutParams(0,dp(55),1f));
        TextView configure=label("Erweitert",15,0xfff5faff,true);
        configure.setGravity(Gravity.CENTER);configure.setOnClickListener(v->
            startActivity(new Intent(this,com.andrerinas.openheadunit.main.SettingsActivity.class)));
        footer.addView(configure,new LinearLayout.LayoutParams(dp(140),dp(55)));
        root.addView(footer);setContentView(root);
    }
    @Override protected void onResume(){super.onResume();if(list!=null)render();}
}
'''
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
            android:theme="@android:style/Theme.Material.NoActionBar"
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
            android:theme="@android:style/Theme.Material.NoActionBar"
            android:screenOrientation="landscape" />
        <activity android:name="de.vd3.fiesta.WirelessSetupActivity" android:exported="false"
            android:screenOrientation="landscape" />
        <service android:name="de.vd3.fiesta.MediaAccessService"
            android:label="dynamicOS Mediensteuerung"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="false">
            <intent-filter><action android:name="android.service.notification.NotificationListenerService"/></intent-filter>
        </service>
        <activity android:name="de.vd3.fiesta.MainActivity" android:exported="false"
            android:screenOrientation="landscape" />
'''
m=m.replace('</application>',addon+'\n    </application>')
manifest.write_text(m)

gradle=root/"app/build.gradle.kts"
g=gradle.read_text()
g=g.replace('applicationId = "com.andrerinas.headunitrevived"','applicationId = "de.vd3.fiesta.unified"')
g=g.replace('versionName = "4.0.0-beta2"','versionName = "2.1.0-unified"')
g=g.replace('versionCode = 122','versionCode = 210')
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

# Embed native receiver connection status in the Fiesta visual language.
# Keep the real receiver's pairing controls and protocol engine intact.
receiverMain=src/"java/com/andrerinas/openheadunit/main/MainActivity.kt"
receiverText=receiverMain.read_text()
mainAnchor="        setContentView(R.layout.activity_main)"
assert receiverText.count(mainAnchor)==1
receiverText=receiverText.replace(mainAnchor,mainAnchor+"\n        installFiestaConnectionShell()")
hook="    override fun onCreate(savedInstanceState: Bundle?) {"
assert receiverText.count(hook)==1
receiverUi=r'''
    // Native Fiesta shell around the real receiver HomeFragment.
    // The connection button delegates to the receiver's own click handler,
    // not another Android app or a mocked wireless connection.
    private fun installFiestaConnectionShell() {
        val host=findViewById<FrameLayout>(R.id.root) ?: return
        val density=resources.displayMetrics.density
        fun dp(n:Int)=(density*n).toInt()
        val dashboard=android.widget.LinearLayout(this).apply {
            orientation=android.widget.LinearLayout.VERTICAL
            setPadding(dp(22),dp(14),dp(22),dp(14))
            setBackgroundColor(Color.rgb(7,19,38))
            isClickable=true
        }
        val header=android.widget.LinearLayout(this).apply {
            orientation=android.widget.LinearLayout.HORIZONTAL
            gravity=android.view.Gravity.CENTER_VERTICAL
        }
        val brand=TextView(this).apply {
            text="dynamicOS    FIESTA"
            setTextColor(Color.WHITE)
            textSize=20f
            typeface=android.graphics.Typeface.DEFAULT_BOLD
        }
        header.addView(brand,android.widget.LinearLayout.LayoutParams(0,dp(45),1f))
        val home=TextView(this).apply {
            text="⌂  Startseite"
            textSize=15f
            setTextColor(Color.rgb(117,198,255))
            gravity=android.view.Gravity.CENTER
            setPadding(dp(14),0,dp(14),0)
            setOnClickListener {
                startActivity(Intent(this@MainActivity,de.vd3.fiesta.LauncherActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION))
                overridePendingTransition(0,0)
            }
        }
        header.addView(home,android.widget.LinearLayout.LayoutParams(-2,dp(44)))
        dashboard.addView(header)

        val body=android.widget.LinearLayout(this).apply {
            orientation=android.widget.LinearLayout.HORIZONTAL
            gravity=android.view.Gravity.CENTER_VERTICAL
        }
        val info=android.widget.LinearLayout(this).apply {
            orientation=android.widget.LinearLayout.VERTICAL
            gravity=android.view.Gravity.CENTER_VERTICAL
        }
        fun caption(value:String,size:Float,color:Int,bold:Boolean=false):TextView =
            TextView(this).apply {
                text=value;textSize=size;setTextColor(color)
                if(bold)typeface=android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0,dp(4),0,dp(4))
            }
        info.addView(caption("ANDROID AUTO",12f,Color.rgb(89,184,255),true))
        info.addView(caption("Smartphone verbinden",26f,Color.WHITE,true))
        info.addView(caption("Android Auto kabellos und direkt im Fiesta nutzen.",15f,
            Color.rgb(171,196,218)))
        val connect=TextView(this).apply {
            text="Verbinden   ›"
            setTextColor(Color.WHITE)
            textSize=19f
            typeface=android.graphics.Typeface.DEFAULT_BOLD
            gravity=android.view.Gravity.CENTER
            setBackgroundColor(Color.rgb(39,136,231))
            setOnClickListener {
                val native=findViewById<View>(R.id.home_primary_action)
                if(native!=null&&native.isEnabled)native.performClick()
                else Toast.makeText(this@MainActivity,
                    "Empfänger initialisiert noch oder die Verbindungsart ist nicht verfügbar",
                    Toast.LENGTH_LONG).show()
            }
        }
        val cp=android.widget.LinearLayout.LayoutParams(dp(220),dp(56))
        cp.topMargin=dp(18)
        info.addView(connect,cp)
        val nativeMode=TextView(this).apply {
            text="Weitere Verbindungsarten"
            textSize=14f
            setTextColor(Color.rgb(169,208,243))
            setPadding(0,dp(14),0,dp(8))
            setOnClickListener { dashboard.visibility=View.GONE }
        }
        info.addView(nativeMode)
        body.addView(info,android.widget.LinearLayout.LayoutParams(0,-2,0.62f))
        val car=ImageView(this).apply {
            setImageResource(R.drawable.fiesta_graphite)
            scaleType=ImageView.ScaleType.FIT_CENTER
        }
        body.addView(car,android.widget.LinearLayout.LayoutParams(0,-1,0.38f))
        dashboard.addView(body,android.widget.LinearLayout.LayoutParams(-1,0,1f))

        val bottom=android.widget.LinearLayout(this).apply {
            orientation=android.widget.LinearLayout.HORIZONTAL
            gravity=android.view.Gravity.CENTER_VERTICAL
        }
        val status=TextView(this).apply {
            text="FIESTA UNIFIED     •     Telefon     •     Navigation     •     Medien"
            textSize=13f
            setTextColor(Color.rgb(168,195,215))
            gravity=android.view.Gravity.CENTER_VERTICAL
        }
        bottom.addView(status,android.widget.LinearLayout.LayoutParams(-1,dp(48)))
        dashboard.addView(bottom)
        val anchor=findViewById<View>(R.id.main_content)
        val place=if(anchor!=null)host.indexOfChild(anchor)+1 else host.childCount
        host.addView(dashboard,place,FrameLayout.LayoutParams(-1,-1))
    }

'''
receiverText=receiverText.replace(hook,receiverUi+hook)
receiverMain.write_text(receiverText)

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
    // One video renderer, one Fiesta system shell. Never layer large floating
    // panels on top of Android Auto: in combined mode the frame has dedicated space.
    private var fiestaViewMode = 1
    private var fiestaShell: LinearLayout? = null
    private var fiestaNav: LinearLayout? = null
    private var fiestaSwitcher: TextView? = null
    private fun fd(v: Int) = (resources.displayMetrics.density * v).toInt()
    private fun ft(label: String, size: Float, strong: Boolean=false): TextView =
        TextView(this).apply {
            text=label;textSize=size;setTextColor(Color.WHITE)
            gravity=Gravity.CENTER_VERTICAL
            setPadding(fd(12),fd(7),fd(12),fd(7))
            if(strong)typeface=android.graphics.Typeface.DEFAULT_BOLD
            maxLines=2
        }
    private fun clickText(label: String, run: () -> Unit): TextView =
        ft(label,15f,true).apply {
            gravity=Gravity.CENTER
            setOnClickListener { run() }
        }
    private fun fiestaMediaKey(code: Int) {
        val audio=getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
        val t=android.os.SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(t,t,KeyEvent.ACTION_DOWN,code,0))
        audio.dispatchMediaKeyEvent(KeyEvent(t,t,KeyEvent.ACTION_UP,code,0))
    }
    private fun installFiestaDock() {
        val host=findViewById<FrameLayout>(R.id.container) ?: return
        fiestaViewMode=getSharedPreferences("dynamicos_setup",MODE_PRIVATE).getInt("viewmode",1)
        val shell=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8,20,33))
        }
        fiestaShell=shell
        val left=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10,28,46))
            setPadding(fd(15),fd(12),fd(15),fd(9))
        }
        val statusBar=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
        }
        statusBar.addView(ft("dynamicOS",19f,true),
            LinearLayout.LayoutParams(0,fd(43),1f))
        val time=ft(java.text.SimpleDateFormat("HH:mm",java.util.Locale.GERMANY)
            .format(java.util.Date()),15f,true).apply { gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL }
        statusBar.addView(time)
        left.addView(statusBar)
        left.addView(ft("FIESTA  •  2015",12f,true))
        val carPanel=FrameLayout(this).apply {
            background=android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(26,62,94),Color.rgb(9,28,45))
            ).apply { cornerRadius=fd(14).toFloat() }
        }
        val photo=ImageView(this).apply {
            setImageResource(com.andrerinas.openheadunit.R.drawable.fiesta_graphite)
            scaleType=ImageView.ScaleType.FIT_CENTER
            setPadding(fd(8),fd(8),fd(8),fd(8))
        }
        carPanel.addView(photo,FrameLayout.LayoutParams(-1,-1))
        left.addView(carPanel,LinearLayout.LayoutParams(-1,0,1f).apply {
            topMargin=fd(9);bottomMargin=fd(8)
        })
        val carName=ft("Ford Fiesta Mk7",17f,true)
        left.addView(carName)
        left.addView(ft("W-S 107  ·  Graphitgrau",12f))
        val separator=View(this).apply{setBackgroundColor(Color.rgb(36,76,102))}
        left.addView(separator,LinearLayout.LayoutParams(-1,fd(1)).apply{
            topMargin=fd(9);bottomMargin=fd(9)
        })
        val connection=ft(if(commManager.isConnected)
            "●  Android Auto aktiv" else "○  Smartphone verbinden",13f,true).apply {
            setTextColor(Color.rgb(119,209,174))
            setOnClickListener { showFiestaCommunication() }
        }
        left.addView(connection)
        val actions=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.CENTER_VERTICAL
        }
        actions.addView(clickText("❮") { fiestaMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
            LinearLayout.LayoutParams(0,fd(42),1f))
        actions.addView(clickText("▶Ⅱ") { fiestaMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
            LinearLayout.LayoutParams(0,fd(42),1f))
        actions.addView(clickText("❯") { fiestaMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) },
            LinearLayout.LayoutParams(0,fd(42),1f))
        left.addView(actions,LinearLayout.LayoutParams(-1,fd(42)).apply{topMargin=fd(7)})
        val upper=FrameLayout(this)
        upper.addView(left,FrameLayout.LayoutParams(
            (resources.displayMetrics.widthPixels*.27f).toInt(),-1,Gravity.LEFT))
        shell.addView(upper,LinearLayout.LayoutParams(-1,0,1f))
        val nav=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(12,41,66))
        }
        fiestaNav=nav
        fun navItem(title: String, action: () -> Unit) {
            nav.addView(clickText(title,action),LinearLayout.LayoutParams(0,fd(54),1f))
        }
        navItem("⌂  Fiesta") {
            startActivity(Intent(this,de.vd3.fiesta.LauncherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION))
            overridePendingTransition(0,0)
        }
        navItem("☎  Telefon"){showFiestaCommunication()}
        navItem("♫  Medien"){showFiestaMedia()}
        navItem("◉  Verbindung") {
            startActivity(Intent(this,de.vd3.fiesta.WirelessSetupActivity::class.java))
        }
        navItem("⛶  Vollbild") { fiestaViewMode=0;applyFiestaSplitLayout() }
        shell.addView(nav)
        host.addView(shell,0,FrameLayout.LayoutParams(-1,-1))
        val toggle=clickText("▣  Fiesta-Ansicht") {
            fiestaViewMode=if(fiestaViewMode==1)0 else 1
            applyFiestaSplitLayout()
        }.apply {
            setBackgroundColor(Color.rgb(18,65,101))
            elevation=fd(10).toFloat()
        }
        fiestaSwitcher=toggle
        host.addView(toggle,FrameLayout.LayoutParams(fd(143),fd(48),Gravity.LEFT or Gravity.BOTTOM))
        applyFiestaSplitLayout()
    }
    private fun applyFiestaSplitLayout() {
        val split=fiestaViewMode==1
        fiestaShell?.visibility=if(split)View.VISIBLE else View.GONE
        fiestaSwitcher?.text=if(split)"⛶  Vollbild" else "▣  Fiesta-Ansicht"
        getSharedPreferences("dynamicos_setup",MODE_PRIVATE).edit().putInt("viewmode",fiestaViewMode).apply()
        if(::projectionView.isInitialized) {
            val view=projectionView as View
            val lp=(view.layoutParams as? FrameLayout.LayoutParams)
                ?: FrameLayout.LayoutParams(-1,-1)
            val w=resources.displayMetrics.widthPixels
            val h=resources.displayMetrics.heightPixels
            if(split) {
                lp.width=(w*.73f).toInt()
                lp.height=(h-fd(54)).coerceAtLeast(fd(190))
                lp.gravity=Gravity.RIGHT or Gravity.TOP
                lp.setMargins(0,0,0,fd(54))
            }else{
                lp.width=FrameLayout.LayoutParams.MATCH_PARENT
                lp.height=FrameLayout.LayoutParams.MATCH_PARENT
                lp.gravity=Gravity.TOP or Gravity.LEFT
                lp.setMargins(0,0,0,0)
            }
            view.layoutParams=lp
            view.requestLayout()
        }
        // Critical: upstream Open Headunit uses a transparent full-screen
        // OverlayTouchView to forward Android Auto taps. A full-screen
        // interceptor consumed ALL Fiesta dashboard and bottom dock taps.
        // Constrain this touch layer to precisely the rendered AA surface.
        val overlay=touchOverlayView
        if(overlay!=null) {
            val w=resources.displayMetrics.widthPixels
            val h=resources.displayMetrics.heightPixels
            val touchLp=FrameLayout.LayoutParams(
                if(split)(w*.73f).toInt() else FrameLayout.LayoutParams.MATCH_PARENT,
                if(split)(h-fd(54)).coerceAtLeast(fd(190)) else FrameLayout.LayoutParams.MATCH_PARENT,
                if(split)Gravity.RIGHT or Gravity.TOP else Gravity.TOP or Gravity.LEFT
            )
            if(split)touchLp.setMargins(0,0,0,fd(54))
            overlay.layoutParams=touchLp
            overlay.requestLayout()
        }
        // Keep local buttons interactive above the projection touch surface.
        fiestaSwitcher?.bringToFront()
    }
    private fun showFiestaCommunication() {
        val status=try{
            val m=getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            when{m?.adapter==null->"Hersteller-Bluetooth";m.adapter.isEnabled->"Bluetooth eingeschaltet";else->"Bluetooth ausgeschaltet"}
        }catch(_:Exception){"Bluetooth-Status nicht erreichbar"}
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Fiesta  ·  Telefon")
            .setMessage("$status\n\nFreisprechen benötigt die Bluetooth-HFP-Schnittstelle der Headunit.")
            .setPositiveButton("Verbindung") { _,_ ->
                startActivity(Intent(this,de.vd3.fiesta.WirelessSetupActivity::class.java))
            }.setNegativeButton("Schließen",null).show()
    }
    private fun showFiestaMedia() {
        val controls=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        fun item(title:String,key:Int) {
            controls.addView(clickText(title) { fiestaMediaKey(key) },
                LinearLayout.LayoutParams(0,fd(57),1f))
        }
        item("❮",KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        item("▶Ⅱ",KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        item("❯",KeyEvent.KEYCODE_MEDIA_NEXT)
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Fiesta  ·  Medien")
            .setView(controls).setPositiveButton("Schließen",null).show()
    }
'''
k=k.replace(anchor2,dock+anchor2)
render_anchor="        container.addView(view)"
assert k.count(render_anchor)==1
k=k.replace(render_anchor,render_anchor+"\n        applyFiestaSplitLayout()")
# The touch overlay is created AFTER the video surface, so update its
# dimensions once it is attached; changing only the video did not fix taps.
touchAttach="        container.addView(overlayView)"
assert k.count(touchAttach)==1
k=k.replace(touchAttach,touchAttach+"\n        applyFiestaSplitLayout()")

proj.write_text(k)

print("Single-APK merge completed; Fiesta and receiver share one applicationId and one APK.")

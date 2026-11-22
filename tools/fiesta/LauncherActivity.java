package de.vd3.fiesta;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.andrerinas.openheadunit.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Native 1-GB-RAM SYNC shell. One navigation model; no WebView or continuous redraw. */
public final class LauncherActivity extends Activity {
    private static final int BACK = 0xff0b1420, PANEL = 0xff142334, PANEL_LIFT = 0xff192c3f,
            FORE = 0xfff5f7fa, SECONDARY = 0xffaebfcc, ACCENT = 0xff59a9ec, DIVIDER = 0xff2b4052;
    private static final String[] TABS = {"Start","Audio","Telefon","Navigation","System"};
    private LinearLayout root, body;
    private int page;
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable background(int c, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(dp(radius));
        return g;
    }
    private TextView text(String value, int size, boolean strong, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private void add(LinearLayout target, View view, int marginTop) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.topMargin = dp(marginTop);
        target.addView(view,p);
    }
    private void heading(String title, String detail) {
        add(body,text(title,26,true,FORE),1);
        add(body,text(detail,15,false,SECONDARY),5);
    }
    private TextView control(String label, Runnable action) {
        TextView t = text(label,17,true,FORE);
        t.setGravity(Gravity.CENTER);
        t.setBackground(background(PANEL_LIFT,11));
        t.setOnClickListener(v -> action.run());
        t.setContentDescription(label);
        return t;
    }
    private TextView row(String title, String description, Runnable action) {
        TextView t = control(title,action);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(20),0,dp(16),0);
        t.setContentDescription(title + ": " + description);
        return t;
    }
    private void open(Class<?> activity) {
        try {
            startActivity(new Intent(this,activity).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } catch (Exception e) {
            Toast.makeText(this,"Funktion nicht verfügbar",Toast.LENGTH_SHORT).show();
        }
    }
    private void openAuto() { open(com.andrerinas.openheadunit.main.MainActivity.class); }
    private void openSetting(String name) {
        try { startActivity(new Intent(name)); }
        catch (Exception e) { Toast.makeText(this,"Einstellung nicht verfügbar",Toast.LENGTH_SHORT).show(); }
    }
    private void go(int target) { page=target; draw(); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if(b!=null)page=b.getInt("page",0);
        if(!getSharedPreferences("dynamicos_setup",MODE_PRIVATE).getBoolean("complete",false)){
            open(SetupActivity.class);
            finish();
            return;
        }
        draw();
    }
    @Override protected void onResume() { super.onResume(); if(root!=null) draw(); }
    @Override protected void onSaveInstanceState(Bundle b) { b.putInt("page",page); super.onSaveInstanceState(b); }
    @Override public void onBackPressed() { if(page!=0)go(0); else super.onBackPressed(); }

    private void draw() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACK);
        root.setPadding(dp(22),dp(10),dp(22),dp(9));
        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.addView(text("SYNC",22,true,FORE),new LinearLayout.LayoutParams(-2,dp(47)));
        TextView identity = text("  ·  dynamicOS",15,false,SECONDARY);
        status.addView(identity,new LinearLayout.LayoutParams(0,dp(47),1));
        TextView time = text(new SimpleDateFormat("HH:mm",Locale.GERMANY).format(new Date()),19,true,FORE);
        time.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        status.addView(time,new LinearLayout.LayoutParams(dp(95),dp(47)));
        root.addView(status);
        View line = new View(this); line.setBackgroundColor(DIVIDER);
        root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0,dp(15),0,dp(9));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        if(page==0)home();
        else if(page==1)music();
        else if(page==2)phone();
        else if(page==3)navigation();
        else if(page==4)system();
        else vehicle();
        LinearLayout dock=new LinearLayout(this);
        dock.setPadding(dp(2),dp(2),dp(2),dp(2));
        dock.setBackground(background(PANEL,12));
        for(int i=0;i<TABS.length;i++){
            final int index=i;
            TextView tab=text(TABS[i],15,page==i,page==i?ACCENT:FORE);
            tab.setGravity(Gravity.CENTER);
            if(page==i)tab.setBackground(background(0xff22394e,10));
            tab.setOnClickListener(v->go(index));
            dock.addView(tab,new LinearLayout.LayoutParams(0,dp(55),1));
        }
        root.addView(dock);
        setContentView(root);
    }

    /** SYNC dashboard: glanceable source, primary controls and persistent navigation. */
    private void home(){
        LinearLayout horizontal=new LinearLayout(this);
        horizontal.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(horizontal,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout primary=new LinearLayout(this);
        primary.setOrientation(LinearLayout.VERTICAL);
        primary.setPadding(dp(18),dp(12),dp(18),dp(12));
        primary.setBackground(background(PANEL,14));
        LinearLayout.LayoutParams primaryLp=new LinearLayout.LayoutParams(0,-1,.60f);
        primaryLp.rightMargin=dp(12);
        horizontal.addView(primary,primaryLp);
        TextView heading=text("Audio",15,true,ACCENT);
        primary.addView(heading);
        MediaController session=activeMedia();
        MediaMetadata metadata=session==null?null:session.getMetadata();
        String song=metadata==null?null:metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist=metadata==null?null:metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        TextView track=text(song==null||song.isEmpty()?"Keine aktive Wiedergabe":song,25,true,FORE);
        LinearLayout.LayoutParams trackLp=new LinearLayout.LayoutParams(-1,0,1f);
        trackLp.topMargin=dp(10);
        primary.addView(track,trackLp);
        primary.addView(text(artist==null||artist.isEmpty()?"Smartphone oder Medienquelle auswählen":artist,14,false,SECONDARY));
        LinearLayout commands=new LinearLayout(this);
        LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(-1,dp(66));cl.topMargin=dp(11);
        primary.addView(commands,cl);
        final int[] keys={KeyEvent.KEYCODE_MEDIA_PREVIOUS,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,KeyEvent.KEYCODE_MEDIA_NEXT};
        final String[] labels={"❮","▶  /  Ⅱ","❯"};
        for(int i=0;i<3;i++){
            final int key=keys[i];
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
            lp.leftMargin=dp(3);lp.rightMargin=dp(3);
            commands.addView(control(labels[i],()->mediaCommand(key)),lp);
        }
        LinearLayout secondary=new LinearLayout(this);
        secondary.setOrientation(LinearLayout.VERTICAL);
        horizontal.addView(secondary,new LinearLayout.LayoutParams(0,-1,.40f));
        TextView navigation=control("Navigation",this::openAuto);
        TextView phone=control("Telefon",()->go(2));
        TextView vehicle=control("Fahrzeug",()->go(5));
        secondary.addView(navigation,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(-1,0,1f);pl.topMargin=dp(9);
        secondary.addView(phone,pl);
        LinearLayout.LayoutParams vl=new LinearLayout.LayoutParams(-1,0,1f);vl.topMargin=dp(9);
        secondary.addView(vehicle,vl);
    }

    private MediaController activeMedia(){
        try{
            MediaSessionManager manager=(MediaSessionManager)getSystemService(Context.MEDIA_SESSION_SERVICE);
            if(manager==null)return null;
            List<MediaController> controllers=manager.getActiveSessions(new ComponentName(this,MediaAccessService.class));
            for(MediaController controller:controllers){
                PlaybackState state=controller.getPlaybackState();
                if(state!=null && state.getState()==PlaybackState.STATE_PLAYING)return controller;
            }
            return controllers.isEmpty()?null:controllers.get(0);
        }catch(SecurityException e){return null;}
    }
    private void mediaCommand(int key){
        MediaController c=activeMedia();
        if(c!=null){
            MediaController.TransportControls t=c.getTransportControls();
            if(key==KeyEvent.KEYCODE_MEDIA_NEXT)t.skipToNext();
            else if(key==KeyEvent.KEYCODE_MEDIA_PREVIOUS)t.skipToPrevious();
            else {
                PlaybackState s=c.getPlaybackState();
                if(s!=null && s.getState()==PlaybackState.STATE_PLAYING)t.pause();
                else t.play();
            }
        }else{
            AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);
            if(a!=null){
                long now=SystemClock.uptimeMillis();
                a.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,key,0));
                a.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,key,0));
            }
        }
        go(1);
    }
    private void music(){
        heading("Audio","Musik und verfügbare Audioquellen");
        MediaController controller=activeMedia();
        MediaMetadata metadata=controller==null?null:controller.getMetadata();
        String song=metadata==null?null:metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artist=metadata==null?null:metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        TextView title=text(song==null||song.isEmpty()?"Keine aktive Wiedergabe":song,25,true,FORE);
        add(body,title,20);
        add(body,text(artist==null||artist.isEmpty()?"Quelle auswählen oder Smartphone verbinden":artist,15,false,SECONDARY),5);
        LinearLayout controls=new LinearLayout(this);
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,dp(84));
        layout.topMargin=dp(16); body.addView(controls,layout);
        int[] keys={KeyEvent.KEYCODE_MEDIA_PREVIOUS,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,KeyEvent.KEYCODE_MEDIA_NEXT};
        String[] labels={"❮","▶  /  Ⅱ","❯"};
        for(int i=0;i<3;i++){
            final int key=keys[i];
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
            p.leftMargin=dp(5); p.rightMargin=dp(5);
            controls.addView(control(labels[i],()->mediaCommand(key)),p);
        }
        add(body,row("Android Auto","Smartphone-Audio",this::openAuto),12);
        if(controller==null) add(body,row("Medienzugriff aktivieren","Metadaten anderer Apps",()->
                openSetting("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")),7);
    }
    private void phone(){
        heading("Telefon","Smartphone koppeln und Telefonfunktionen öffnen");
        add(body,row("Bluetooth verbinden","Kopplung",()->open(WirelessSetupActivity.class)),16);
        add(body,row("Telefonhardware prüfen","Mikrofon und Freisprechen",()->open(MediaHardwareActivity.class)),8);
        add(body,row("Telefon über Android Auto","Anrufe und Kontakte",this::openAuto),10);
        add(body,row("System-Telefon","Nur wenn vom Gerät unterstützt",()->{
            try{startActivity(new Intent(Intent.ACTION_DIAL));}
            catch(Exception e){Toast.makeText(this,"Kein Telefonmodul vorhanden",Toast.LENGTH_SHORT).show();}
        }),10);
    }
    private void navigation(){
        heading("Navigation","Smartphone-Navigation über Android Auto");
        add(body,row("Karte öffnen","Integrierte Projektion",this::openAuto),20);
        add(body,row("Verbindung einrichten","Bluetooth und WLAN",()->open(WirelessSetupActivity.class)),10);
    }
    private void system(){
        heading("Einstellungen","Verbindungen, Anzeige und System");
        add(body,row("Verbindungen","Bluetooth und WLAN",()->open(WirelessSetupActivity.class)),17);
        add(body,row("Audio & Telefonhardware","Ausgänge, Mikrofon, Bluetooth",()->open(MediaHardwareActivity.class)),9);
        add(body,row("Android Auto","Empfänger-Einstellungen",()->open(
            com.andrerinas.openheadunit.main.SettingsActivity.class)),9);
        add(body,row("Fahrzeugdiagnose","Raise CAN und Lenkradtasten",()->go(5)),9);
        add(body,row("Android-System","Geräteeinstellungen",()->openSetting(Settings.ACTION_SETTINGS)),9);
    }
    private void vehicle(){
        heading("Fahrzeug","Fiesta Mk7  ·  2015  ·  W-S 107");
        add(body,row("USB / CAN-Diagnose","Verfügbare USB-Geräte",()->
            open(de.vd3.fiesta.MainActivity.class)),20);
        add(body,row("Lenkradtasten","Android-KeyEvent-Zuordnung",()->
            open(de.vd3.fiesta.MainActivity.class)),10);
        add(body,row("Zurück zu Einstellungen","Hauptmenü",()->go(4)),10);
    }
}

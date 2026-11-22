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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.andrerinas.openheadunit.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Native 1-GB-RAM SYNC shell. One navigation model; no WebView or continuous redraw. */
public final class LauncherActivity extends Activity {
    private static final int BACK = 0xff0b1117, PANEL = 0xff151f28, PANEL_LIFT = 0xff1c2934,
            FORE = 0xfff3f5f6, SECONDARY = 0xff9aaebc, ACCENT = 0xff80c4ea, DIVIDER = 0xff30414b;
    private static final String[] TABS = {"Start","Audio","Telefon","Navigation","System"};
    private LinearLayout root, body;
    private TextView mediaTitleView, mediaArtistView, mediaSourceView;
    private ImageButton mediaToggleView;
    // Reactive metadata updates avoid timers and expensive full-screen redraws.
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener mediaListener=
        (prefs,key)->{
            if("title".equals(key)||"artist".equals(key)||"playing".equals(key)){
                runOnUiThread(this::refreshMediaLabels);
            }
        };
    private void refreshMediaLabels(){
        if(isFinishing() || isDestroyed())return;
        MediaController session=activeMedia();
        if(mediaTitleView!=null)mediaTitleView.setText(mediaTitle(session));
        if(mediaArtistView!=null)mediaArtistView.setText(mediaArtist(session));
        if(mediaSourceView!=null){
            boolean connected=FiestaMediaBridge.isProjectionActive(this);
            mediaSourceView.setText(connected?"●  ANDROID AUTO AKTIV":"○  SMARTPHONE VERBINDEN");
            mediaSourceView.setTextColor(connected?0xff7cc9b0:SECONDARY);
        }
        if(mediaToggleView!=null){
            boolean playing=FiestaMediaBridge.isProjectionActive(this)
                ?aaMedia().getBoolean("playing",false)
                :session!=null && session.getPlaybackState()!=null &&
                 session.getPlaybackState().getState()==PlaybackState.STATE_PLAYING;
            mediaToggleView.setImageResource(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play);
            mediaToggleView.setContentDescription(playing?"Pause":"Wiedergabe");
        }
    }
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
        add(body,text(title,22,true,FORE),1);
        add(body,text(detail,13,false,SECONDARY),4);
    }
    private TextView control(String label, Runnable action) {
        TextView t = text(label,16,false,FORE);
        t.setGravity(Gravity.CENTER);
        t.setBackground(background(PANEL_LIFT,7));
        t.setOnClickListener(v -> action.run());
        t.setContentDescription(label);
        return t;
    }
    /** Accessible two-line menu cell. No empty bars or nonfunctional actions. */
    private View row(String title, String description, Runnable action) {
        LinearLayout cell=new LinearLayout(this);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setPadding(dp(17),dp(7),dp(12),dp(7));
        cell.setBackground(background(PANEL,7));
        LinearLayout copy=new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title,16,true,FORE),new LinearLayout.LayoutParams(-1,dp(26)));
        copy.addView(text(description,12,false,SECONDARY),new LinearLayout.LayoutParams(-1,dp(21)));
        cell.addView(copy,new LinearLayout.LayoutParams(0,dp(48),1f));
        TextView chevron=text("›",23,false,ACCENT);
        chevron.setGravity(Gravity.CENTER);
        cell.addView(chevron,new LinearLayout.LayoutParams(dp(30),dp(48)));
        cell.setMinimumHeight(dp(62));
        cell.setOnClickListener(v->action.run());
        cell.setContentDescription(title+". "+description);
        return cell;
    }
    private void open(Class<?> activity) {
        try {
            startActivity(new Intent(this,activity).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } catch (Exception e) {
            Toast.makeText(this,"Funktion nicht verfügbar",Toast.LENGTH_SHORT).show();
        }
    }
    private void openAuto() {
        if(FiestaMediaBridge.isProjectionActive(this))
            open(com.andrerinas.openheadunit.aap.AapProjectionActivity.class);
        else
            open(com.andrerinas.openheadunit.main.MainActivity.class);
    }
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
    @Override protected void onResume() {
        super.onResume();
        if(root!=null)draw();
        aaMedia().registerOnSharedPreferenceChangeListener(mediaListener);
        refreshMediaLabels();
    }
    @Override protected void onPause() {
        aaMedia().unregisterOnSharedPreferenceChangeListener(mediaListener);
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle b) { b.putInt("page",page); super.onSaveInstanceState(b); }
    @Override public void onBackPressed() { if(page!=0)go(0); else super.onBackPressed(); }

    private void draw() {
        mediaTitleView=null;
        mediaArtistView=null;
        mediaSourceView=null;
        mediaToggleView=null;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACK);
        root.setPadding(dp(20),dp(6),dp(20),dp(9));
        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.addView(text("SYNC",19,true,FORE),new LinearLayout.LayoutParams(-2,dp(44)));
        TextView identity = text("  /  dynamicOS",12,false,SECONDARY);
        status.addView(identity,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView time = text(new SimpleDateFormat("HH:mm",Locale.GERMANY).format(new Date()),17,false,FORE);
        time.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        status.addView(time,new LinearLayout.LayoutParams(dp(90),dp(44)));
        root.addView(status);
        View line = new View(this); line.setBackgroundColor(DIVIDER);
        root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0,dp(13),0,dp(10));
        if(page==0){
            root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        }else{
            ScrollView scroll=new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
            root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        }
        if(page==0)home();
        else if(page==1)music();
        else if(page==2)phone();
        else if(page==3)navigation();
        else if(page==4)system();
        else vehicle();
        LinearLayout dock=new LinearLayout(this);
        dock.setPadding(dp(2),dp(3),dp(2),0);
        dock.setBackgroundColor(PANEL);
        for(int i=0;i<TABS.length;i++){
            final int index=i;
            LinearLayout item=new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            View indicator=new View(this);
            indicator.setBackgroundColor(page==i?ACCENT:PANEL);
            item.addView(indicator,new LinearLayout.LayoutParams(dp(36),dp(2)));
            TextView tab=text(TABS[i],14,page==i,page==i?FORE:SECONDARY);
            tab.setGravity(Gravity.CENTER);
            item.addView(tab,new LinearLayout.LayoutParams(-1,dp(49)));
            item.setOnClickListener(v->go(index));
            dock.addView(item,new LinearLayout.LayoutParams(0,dp(54),1));
        }
        root.addView(dock);
        setContentView(root);
    }

    /** SYNC dashboard: glanceable source, primary controls and persistent navigation. */
    // Android Auto music comes from the phone's projection protocol, not from
    // the headunit's notification service. This compact snapshot is written
    // directly by the receiver and survives switching to the SYNC home screen.
    private android.content.SharedPreferences aaMedia(){
        return getSharedPreferences("dynamicos_aa_bridge",MODE_PRIVATE);
    }
    private String mediaTitle(MediaController session){
        // Android Auto playback is decoded by the receiver, not a local MediaSession.
        // Prefer the active projection metadata over unrelated local apps.
        if(FiestaMediaBridge.isProjectionActive(this)){
            String projected=aaMedia().getString("title","");
            if(projected!=null&&!projected.trim().isEmpty())return projected.trim();
            return "Android Auto";
        }
        if(session!=null && session.getMetadata()!=null){
            String t=session.getMetadata().getString(MediaMetadata.METADATA_KEY_TITLE);
            if(t!=null&&!t.trim().isEmpty())return t;
        }
        String aa=aaMedia().getString("title","");
        return aa==null||aa.isEmpty()?"Musik & Medien":aa.trim();
    }
    private String mediaArtist(MediaController session){
        if(FiestaMediaBridge.isProjectionActive(this)){
            String projected=aaMedia().getString("artist","");
            return projected==null||projected.trim().isEmpty()
                ?"Wiedergabe vom Smartphone":projected.trim();
        }
        if(session!=null && session.getMetadata()!=null){
            String a=session.getMetadata().getString(MediaMetadata.METADATA_KEY_ARTIST);
            if(a!=null&&!a.trim().isEmpty())return a;
        }
        String aa=aaMedia().getString("artist","");
        return aa==null||aa.isEmpty()?"Android Auto verbinden oder lokale Medien wählen":aa;
    }
    private View transportButton(int key, boolean playing) {
        ImageButton button=new ImageButton(this);
        int image=key==KeyEvent.KEYCODE_MEDIA_PREVIOUS
            ?android.R.drawable.ic_media_previous
            :key==KeyEvent.KEYCODE_MEDIA_NEXT
                ?android.R.drawable.ic_media_next
                :(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play);
        button.setImageResource(image);
        button.setColorFilter(FORE);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(13),dp(13),dp(13),dp(13));
        button.setBackground(background(PANEL_LIFT,7));
        button.setContentDescription(key==KeyEvent.KEYCODE_MEDIA_PREVIOUS?"Vorheriger Titel":
            key==KeyEvent.KEYCODE_MEDIA_NEXT?"Nächster Titel":playing?"Pause":"Wiedergabe");
        button.setOnClickListener(v->mediaCommand(key));
        if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)mediaToggleView=button;
        return button;
    }
    private void playbackButtons(LinearLayout target,MediaController session){
        if(session==null && !FiestaMediaBridge.isProjectionActive(this)){
            target.addView(control("ANDROID AUTO VERBINDEN",this::openAuto),
                new LinearLayout.LayoutParams(-1,dp(54)));
            return;
        }
        boolean playing=FiestaMediaBridge.isProjectionActive(this)
            ?aaMedia().getBoolean("playing",false)
            :session!=null && session.getPlaybackState()!=null
               && session.getPlaybackState().getState()==PlaybackState.STATE_PLAYING;
        int[] keys={KeyEvent.KEYCODE_MEDIA_PREVIOUS,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,KeyEvent.KEYCODE_MEDIA_NEXT};
        for(int key:keys){
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
            p.leftMargin=dp(3);p.rightMargin=dp(3);
            target.addView(transportButton(key,playing),p);
        }
    }

    private void home(){
        LinearLayout horizontal=new LinearLayout(this);
        horizontal.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(horizontal,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout primary=new LinearLayout(this);
        primary.setOrientation(LinearLayout.VERTICAL);
        primary.setPadding(dp(20),dp(15),dp(20),dp(15));
        primary.setBackground(background(PANEL,8));
        LinearLayout.LayoutParams primaryLp=new LinearLayout.LayoutParams(0,-1,.60f);
        primaryLp.rightMargin=dp(9);
        horizontal.addView(primary,primaryLp);
        TextView heading=text("JETZT WIEDERGEBEN",12,true,ACCENT);
        primary.addView(heading);
        MediaController session=activeMedia();
        TextView track=text(mediaTitle(session),22,true,FORE);
        mediaTitleView=track;
        LinearLayout.LayoutParams trackLp=new LinearLayout.LayoutParams(-1,0,1f);
        trackLp.topMargin=dp(10);
        primary.addView(track,trackLp);
        TextView artistView=text(mediaArtist(session),13,false,SECONDARY);
        mediaArtistView=artistView;
        primary.addView(artistView);
        TextView source=text(FiestaMediaBridge.isProjectionActive(this)
            ?"●  ANDROID AUTO VERBUNDEN":"○  SMARTPHONE VERBINDEN",11,true,
            FiestaMediaBridge.isProjectionActive(this)?0xff7cc9b0:SECONDARY);
        LinearLayout.LayoutParams sourceParams=new LinearLayout.LayoutParams(-1,dp(28));
        sourceParams.topMargin=dp(7);
        primary.addView(source,sourceParams);
        mediaSourceView=source;
        LinearLayout commands=new LinearLayout(this);
        LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(-1,dp(58));
        cl.topMargin=dp(12);
        primary.addView(commands,cl);
        playbackButtons(commands,session);
        LinearLayout secondary=new LinearLayout(this);
        secondary.setOrientation(LinearLayout.VERTICAL);
        horizontal.addView(secondary,new LinearLayout.LayoutParams(0,-1,.40f));
        View navigation=row("Navigation","Karten und Routen",this::openAuto);
        View phone=row("Telefon","Anrufe und Verbindung",()->go(2));
        View vehicle=row("Fahrzeug","Fiesta und CAN-Diagnose",()->go(5));
        secondary.addView(navigation,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(-1,0,1f);pl.topMargin=dp(7);
        secondary.addView(phone,pl);
        LinearLayout.LayoutParams vl=new LinearLayout.LayoutParams(-1,0,1f);vl.topMargin=dp(7);
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
        // The projected phone has precedence over any stale local media session.
        if(FiestaMediaBridge.isProjectionActive(this)){
            if(FiestaMediaBridge.sendMediaKey(this,key))draw();
            return;
        }
        MediaController c=activeMedia();
        if(c==null){
            if(!FiestaMediaBridge.sendMediaKey(this,key))openAuto();
            else draw();
            return;
        }
        MediaController.TransportControls t=c.getTransportControls();
        if(key==KeyEvent.KEYCODE_MEDIA_NEXT)t.skipToNext();
        else if(key==KeyEvent.KEYCODE_MEDIA_PREVIOUS)t.skipToPrevious();
        else {
            PlaybackState state=c.getPlaybackState();
            if(state!=null && state.getState()==PlaybackState.STATE_PLAYING)t.pause();
            else t.play();
        }
        refreshMediaLabels();
    }
    private void music(){
        heading("Audio","Medien von Android Auto und installierten Anwendungen");
        MediaController controller=activeMedia();
        mediaTitleView=text(mediaTitle(controller),24,true,FORE);
        mediaArtistView=text(mediaArtist(controller),14,false,SECONDARY);
        add(body,mediaTitleView,20);
        add(body,mediaArtistView,6);
        if(controller==null && !FiestaMediaBridge.isProjectionActive(this)){
            add(body,row("Android Auto öffnen","Smartphone verbinden",this::openAuto),22);
        }else{
            LinearLayout controls=new LinearLayout(this);
            LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,dp(67));
            layout.topMargin=dp(17);
            body.addView(controls,layout);
            playbackButtons(controls,controller);
            add(body,row("Android Auto öffnen","Smartphone-Wiedergabe",this::openAuto),12);
        }
        add(body,row("Audio- und Mikrofoneinstellungen","Headunit prüfen",()->
            open(MediaHardwareActivity.class)),10);
    }
    private void phone(){
        heading("Telefon","Anrufe über Smartphone und Android Auto");
        add(body,row("Telefon in Android Auto","Anrufe, Kontakte und Verlauf",this::openAuto),19);
        add(body,row("Bluetooth-Verbindung","Smartphone koppeln",()->open(WirelessSetupActivity.class)),10);
        add(body,row("Freisprechhardware","Mikrofon und Audioausgänge prüfen",()->
            open(MediaHardwareActivity.class)),10);
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

package de.vd3.fiesta;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/**
 * Read-only hardware capabilities and safe setup shortcuts.
 * Decoder/telephony availability is not claimed without actual device support.
 */
public final class MediaHardwareActivity extends Activity {
    private static final int BG=0xff0b1420, PANEL=0xff172b3e, TEXT=0xfff1f5f8,
            MUTED=0xffaec1d0, ACCENT=0xff69b5ed;
    private LinearLayout content;
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }
    private void section(String heading,String details){
        TextView title=text(heading,20,TEXT,true);
        LinearLayout.LayoutParams h=new LinearLayout.LayoutParams(-1,-2);
        h.topMargin=dp(15);content.addView(title,h);
        TextView body=text(details,15,MUTED,false);
        body.setPadding(dp(12),dp(9),dp(12),dp(9));
        body.setBackgroundColor(PANEL);
        content.addView(body);
    }
    private void action(String label,Runnable click){
        TextView btn=text(label,16,ACCENT,true);
        btn.setGravity(Gravity.CENTER_VERTICAL);
        btn.setPadding(dp(12),dp(10),dp(12),dp(10));
        btn.setOnClickListener(v->click.run());
        content.addView(btn);
    }
    private void setting(String action){
        try{startActivity(new Intent(action));}
        catch(Exception e){android.widget.Toast.makeText(this,"Einstellung nicht verfügbar",android.widget.Toast.LENGTH_SHORT).show();}
    }
    private String bt(){
        try{
            BluetoothManager manager=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
            if(adapter==null)return "Kein Android-Bluetoothadapter verfügbar. OEM-Bluetooth kann separat arbeiten.";
            if(Build.VERSION.SDK_INT>=31&&checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED)
                return "Android-Bluetooth vorhanden. BLUETOOTH_CONNECT-Berechtigung noch nicht erteilt.";
            return adapter.isEnabled()?"Android-Bluetooth eingeschaltet":"Android-Bluetooth ausgeschaltet";
        }catch(SecurityException e){return "Bluetooth-Berechtigung fehlt.";}
    }
    private String audio(){
        AudioManager manager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if(manager==null)return "Audiomanager nicht verfügbar";
        StringBuilder result=new StringBuilder();
        result.append("Musiklautstärke: ").append(manager.getStreamVolume(AudioManager.STREAM_MUSIC))
            .append("/").append(manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).append("\n");
        AudioDeviceInfo[] outputs=manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        result.append("Ausgabegeräte: ").append(outputs.length);
        for(AudioDeviceInfo d:outputs)result.append("\n• ").append(device(d.getType())).append(" (ID ").append(d.getId()).append(")");
        return result.toString();
    }
    private String mic(){
        boolean feature=getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
        boolean granted=Build.VERSION.SDK_INT<23 || checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
        AudioManager manager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int inputs=manager==null?0:manager.getDevices(AudioManager.GET_DEVICES_INPUTS).length;
        return "Mikrofon-Hardware: "+(feature?"gemeldet":"nicht gemeldet")+"\n"
            +"Eingangsgeräte: "+inputs+"\n"
            +"Mikrofonberechtigung: "+(granted?"erteilt":"fehlt")
            +"\nTelefonie über Android Auto erfordert zusätzlich eine funktionierende Audio-Rückleitung.";
    }
    private String device(int type){
        switch(type){
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:return "Lautsprecher";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:return "Bluetooth A2DP";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:return "Bluetooth SCO/HFP";
            case AudioDeviceInfo.TYPE_USB_DEVICE:return "USB-Audio";
            case AudioDeviceInfo.TYPE_USB_HEADSET:return "USB-Headset";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:return "Kopfhörer";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:return "Kabel-Headset";
            default:return "Android-Audiogerät "+type;
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==71)recreate();
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setTitle("dynamicOS/ Ford Automotive");
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24),dp(14),dp(24),dp(16));scroll.addView(content);
        content.addView(text("Audio- und Telefonhardware",24,TEXT,true));
        content.addView(text("Geräteprüfung · Keine simulierten Decoder oder Verbindungen",13,MUTED,false));
        section("Audioausgabe",audio());
        section("Mikrofon / Freisprechen",mic());
        section("Bluetooth / Telefon",bt());
        section("Android Auto", "Der integrierte Empfänger stellt AAC-/PCM-Audio, Mikrofontransport und Video-Decoding bereit. Welche Pfade tatsächlich funktionieren, hängt von Headunit und Smartphone ab.");
        if(Build.VERSION.SDK_INT>=23 &&
           checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            action("Mikrofonberechtigung erteilen",()->
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},71));
        action("Bluetooth-Einstellungen",()->setting(Settings.ACTION_BLUETOOTH_SETTINGS));
        action("Ton-Einstellungen",()->setting(Settings.ACTION_SOUND_SETTINGS));
        action("Neu prüfen",this::recreate);
        setContentView(scroll);
    }
}

package de.vd3.fiesta;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private final String[] actions = {"Keine Aktion", "Lauter", "Leiser", "Stumm", "Play/Pause", "Nächster Titel", "Vorheriger Titel"};
    private final Map<Integer, String> learned = new LinkedHashMap<>();
    private SharedPreferences prefs;
    private TextView status, usbText, mappedText;
    private Spinner actionSpinner;
    private int lastKey = -1;
    private boolean learning = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("fiesta_buttons", MODE_PRIVATE);
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            try { learned.put(Integer.parseInt(entry.getKey()), (String) entry.getValue()); } catch (Exception ignored) { }
        }
        buildUI();
        showDevices();
        renderMappings();
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextColor(color); view.setTextSize(sp);
        view.setPadding(0, 12, 0, 12);
        return view;
    }

    private Button button(String caption, Runnable action) {
        Button b = new Button(this);
        b.setText(caption); b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void buildUI() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(28, 28, 28, 32);
        body.setBackgroundColor(Color.rgb(13, 18, 29));
        TextView title = text("dynamicOS  /  FIESTA", 26, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        body.addView(title);
        body.addView(text("Ford Fiesta Mk7 · CAN & Lenkradsteuerung", 15, 0xff90a5c3));

        body.addView(text("LENKRADTASTEN", 20, 0xff54d7cd));
        body.addView(text("Lernmodus erkennt Tasten, die Android als KeyEvent empfängt. Drücke nach Start eine Lenkradtaste.", 14, Color.LTGRAY));
        status = text("Bereit", 16, Color.WHITE);
        body.addView(status);
        body.addView(button("Tastenerkennung starten", () -> {
            learning = true; lastKey = -1;
            status.setText("Warte auf Lenkradtaste …");
        }));
        actionSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, actions);
        actionSpinner.setAdapter(adapter);
        body.addView(actionSpinner);
        body.addView(button("Aktion für erkannte Taste speichern", () -> {
            if (lastKey < 0) { status.setText("Bitte zuerst eine Taste erkennen."); return; }
            String action = actions[actionSpinner.getSelectedItemPosition()];
            learned.put(lastKey, action);
            prefs.edit().putString(String.valueOf(lastKey), action).apply();
            status.setText("Gespeichert: " + KeyEvent.keyCodeToString(lastKey) + " → " + action);
            renderMappings();
        }));
        mappedText = text("", 14, Color.LTGRAY);
        body.addView(mappedText);
        body.addView(button("Alle Zuordnungen löschen", () -> {
            learned.clear(); prefs.edit().clear().apply(); renderMappings();
        }));

        body.addView(text("USB / CAN-DIAGNOSE", 20, 0xff54d7cd));
        usbText = text("", 14, Color.LTGRAY);
        body.addView(usbText);
        body.addView(button("USB-Geräte neu erkennen", this::showDevices));
        body.addView(text("Hinweis: Die USB-Geräteliste bestätigt nur die Erkennung. Für proprietäre Raise-CAN-Daten ist ein eigener Protokolltreiber erforderlich.", 13, 0xffffc477));
        body.addView(text("FAHRZEUGPROFIL", 20, 0xff54d7cd));
        body.addView(text("Fiesta Mk7 · Baujahr 2015 · SYNC 1 · Lenkrad-Tastenanlernung · lokale Speicherung", 14, Color.LTGRAY));
        body.addView(text("Sicherheitsmodus: keine CAN-Schreibbefehle und keine Steuergeräte-Codierung.", 13, 0xffa6b7d0));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        setContentView(scroll);
    }

    private void renderMappings() {
        if (mappedText == null) return;
        StringBuilder lines = new StringBuilder("Gespeicherte Belegungen:\n");
        if (learned.isEmpty()) lines.append("Noch keine.");
        for (Map.Entry<Integer, String> entry : learned.entrySet())
            lines.append(KeyEvent.keyCodeToString(entry.getKey())).append("  →  ").append(entry.getValue()).append('\n');
        mappedText.setText(lines.toString());
    }

    private void showDevices() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        StringBuilder s = new StringBuilder("USB-Geräte:\n");
        if (manager == null || manager.getDeviceList().isEmpty()) s.append("Keine erkannt / USB-Host nicht aktiv.");
        else for (UsbDevice d : manager.getDeviceList().values()) {
            s.append(d.getDeviceName()).append(" · VID ").append(String.format("%04X", d.getVendorId()))
                    .append(" · PID ").append(String.format("%04X", d.getProductId())).append('\n');
        }
        usbText.setText(s.toString());
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int code = event.getKeyCode();
            if (learning) {
                learning = false; lastKey = code;
                status.setText("Erkannt: " + KeyEvent.keyCodeToString(code) + " (" + code + ")");
                return true;
            }
            String assigned = learned.get(code);
            if (assigned != null && !assigned.equals("Keine Aktion")) {
                executeAction(assigned);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void executeAction(String action) {
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio == null) return;
        switch (action) {
            case "Lauter": audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); break;
            case "Leiser": audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); break;
            case "Stumm": audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI); break;
            case "Play/Pause": sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE); break;
            case "Nächster Titel": sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_NEXT); break;
            case "Vorheriger Titel": sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PREVIOUS); break;
        }
        status.setText("Ausgeführt: " + action);
    }

    private void sendMediaKey(AudioManager audio, int code) {
        long time = android.os.SystemClock.uptimeMillis();
        audio.dispatchMediaKeyEvent(new KeyEvent(time, time, KeyEvent.ACTION_DOWN, code, 0));
        audio.dispatchMediaKeyEvent(new KeyEvent(time, time, KeyEvent.ACTION_UP, code, 0));
    }
}

package de.vd3.fiesta;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Lightweight HOME replacement for Android 8.1 / 1 GB RAM. No WebView or animation loop. */
public final class LauncherActivity extends Activity {
    private final int bg = 0xff070f1b, panel = 0xff0e2030, cyan = 0xff00dcef;
    private LinearLayout root;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        home();
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{color, 0xff081321});
        d.setCornerRadius(18);
        d.setStroke(1, stroke);
        return d;
    }

    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void scaffold(String subtitle) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 10, 22, 12);
        root.setBackgroundColor(bg);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("dynamicOS  |  " + subtitle, 24, Color.WHITE, true);
        top.addView(brand, new LinearLayout.LayoutParams(0, 53, 1));
        String now = new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date());
        top.addView(label(now + "   ◉", 21, 0xffc8eff5, false));
        root.addView(top);
        View line = new View(this); line.setBackgroundColor(0xff125468);
        root.addView(line, new LinearLayout.LayoutParams(-1, 1));
        setContentView(root);
    }

    private TextView tile(String icon, String title, boolean active, Runnable clicked) {
        TextView view = label(icon + "\n" + title, 25, Color.WHITE, false);
        view.setGravity(Gravity.CENTER);
        view.setBackground(shape(panel, active ? cyan : 0xff315062));
        view.setOnClickListener(v -> clicked.run());
        view.setContentDescription(title);
        return view;
    }

    private void home() {
        scaffold("Fiesta");
        LinearLayout content = new LinearLayout(this);
        content.setPadding(0, 14, 0, 14);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout vehicle = new LinearLayout(this);
        vehicle.setOrientation(LinearLayout.VERTICAL);
        vehicle.setGravity(Gravity.CENTER);
        vehicle.setBackground(shape(0xff0c2735, 0xff1a4056));
        ImageView car = new ImageView(this);
        car.setImageResource(R.drawable.fiesta_graphite);
        car.setScaleType(ImageView.ScaleType.FIT_CENTER);
        vehicle.addView(car, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView model = label("FORD FIESTA", 23, Color.WHITE, true); model.setGravity(Gravity.CENTER);
        vehicle.addView(model);
        TextView plate = label("W-S 107", 18, 0xffaccbd4, false); plate.setGravity(Gravity.CENTER);
        vehicle.addView(plate);
        TextView year = label("Mk7 · 2015 · Dunkelgrau", 13, 0xff7f9dac, false); year.setGravity(Gravity.CENTER);
        vehicle.addView(year);
        LinearLayout.LayoutParams vehicleLp = new LinearLayout.LayoutParams(0, -1, 0.38f);
        vehicleLp.setMargins(0, 0, 12, 0);
        content.addView(vehicle, vehicleLp);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2); grid.setRowCount(2);
        content.addView(grid, new LinearLayout.LayoutParams(0, -1, 0.62f));
        addTile(grid, "⌁", "Android Auto", true, this::autoConnect, 0, 0);
        addTile(grid, "♫", "Musik", false, this::openMusic, 0, 1);
        addTile(grid, "▱", "Fahrzeug", false, this::vehicleMenu, 1, 0);
        addTile(grid, "⚙", "Einstellungen", false, this::settingsMenu, 1, 1);

        TextView footer = label("dynamicOS Fiesta Lite   ·   Android 8.1   ·   W-S 107", 13, 0xff7da5b6, false);
        root.addView(footer);
    }

    private void addTile(GridLayout grid, String icon, String name, boolean selected,
                         Runnable action, int row, int col) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(row, 1f), GridLayout.spec(col, 1f));
        lp.width = 0; lp.height = 0;
        lp.setMargins(5, 5, 5, 5);
        grid.addView(tile(icon, name, selected, action), lp);
    }

    private void menu(String title, String[][] items, Runnable[] actions) {
        scaffold(title);
        ScrollView scroller = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(4, 12, 4, 4);
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            TextView item = label(items[i][0] + "     " + items[i][1] + "       ›", 22, Color.WHITE, false);
            item.setPadding(23, 8, 23, 8);
            item.setBackground(shape(panel, 0xff244858));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 69); p.bottomMargin = 8;
            list.addView(item, p);
            item.setOnClickListener(v -> actions[index].run());
        }
        TextView back = label("‹  Zurück zum Start", 18, cyan, false);
        back.setPadding(16, 12, 12, 12);
        back.setOnClickListener(v -> home());
        list.addView(back);
        scroller.addView(list); root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void vehicleMenu() {
        menu("Fahrzeug", new String[][]{
                {"◉", "Lenkradtasten anlernen"}, {"▤", "CAN-Decoder und USB"}, {"▱", "Fahrzeugprofil"}
        }, new Runnable[]{this::openControls, this::openControls, this::openControls});
    }

    private void settingsMenu() {
        menu("Einstellungen", new String[][]{
                {"◉", "Bluetooth"}, {"◌", "WLAN"}, {"♫", "Ton"},
                {"▣", "Anzeige"}, {"▦", "Alle Apps"}, {"⚙", "Android-System"}
        }, new Runnable[]{
                () -> setting(Settings.ACTION_BLUETOOTH_SETTINGS),
                () -> setting(Settings.ACTION_WIFI_SETTINGS),
                () -> setting(Settings.ACTION_SOUND_SETTINGS),
                () -> setting(Settings.ACTION_DISPLAY_SETTINGS),
                this::allApps,
                () -> setting(Settings.ACTION_SETTINGS)
        });
    }

    private void autoConnect() {
        menu("Android Auto", new String[][]{
                {"◉", "AutoConnect öffnen"}, {"◌", "WLAN / Hotspot"}, {"◉", "Bluetooth koppeln"},
                {"▦", "Kompatible Apps"}
        }, new Runnable[]{this::launchReceiver, () -> setting(Settings.ACTION_WIFI_SETTINGS),
                () -> setting(Settings.ACTION_BLUETOOTH_SETTINGS), this::allApps});
    }

    private void launchReceiver() {
        // The Android Auto receiver must be installed separately until its source is integrated.
        for (String pkg : new String[]{"de.vd3.autoconnect", "com.google.android.projection.gearhead", "com.zjinnova.zlink", "com.zlink5"}) {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) { startActivity(i); return; }
        }
        Toast.makeText(this, "Kein kompatibler Empfänger installiert", Toast.LENGTH_LONG).show();
    }

    private void openMusic() {
        Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
        try { startActivity(intent); }
        catch (ActivityNotFoundException e) { allApps(); }
    }

    private void openControls() { startActivity(new Intent(this, MainActivity.class)); }

    private void setting(String action) {
        try { startActivity(new Intent(action)); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Auf diesem Gerät nicht verfügbar", Toast.LENGTH_SHORT).show(); }
    }

    private void allApps() {
        Intent query = new Intent(Intent.ACTION_MAIN); query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = getPackageManager().queryIntentActivities(query, 0);
        found.sort((a,b) -> String.valueOf(a.loadLabel(getPackageManager())).compareToIgnoreCase(String.valueOf(b.loadLabel(getPackageManager()))));
        scaffold("Apps");
        ScrollView scroller = new ScrollView(this);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        for (ResolveInfo app : found) {
            String name = String.valueOf(app.loadLabel(getPackageManager()));
            TextView row = label("▦    " + name, 19, Color.WHITE, false);
            row.setPadding(18, 12, 12, 12);
            list.addView(row, new LinearLayout.LayoutParams(-1, 62));
            row.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setClassName(app.activityInfo.packageName, app.activityInfo.name);
                try { startActivity(i); } catch (Exception ignored) { Toast.makeText(this, "App kann nicht geöffnet werden", Toast.LENGTH_SHORT).show(); }
            });
        }
        TextView back = label("‹  Zurück", 18, cyan, false);
        back.setPadding(18, 16, 12, 16); back.setOnClickListener(v -> home()); list.addView(back);
        scroller.addView(list); root.addView(scroller);
    }

    @Override public void onBackPressed() { home(); }
}

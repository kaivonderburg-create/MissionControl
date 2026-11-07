package de.vd3.fiesta;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
    private boolean setupVisible = false;
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (isDefaultHome()) home(); else setupHome();
    }

    @Override protected void onResume() {
        super.onResume();
        if (setupVisible && isDefaultHome()) {
            setupVisible = false;
            home();
            Toast.makeText(this, "dynamicOS ist jetzt dein Standard-Launcher", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isDefaultHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        ResolveInfo resolved = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolved != null && resolved.activityInfo != null && getPackageName().equals(resolved.activityInfo.packageName);
    }

    private void setupHome() {
        setupVisible = true;
        scaffold("Einrichtung");
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(35),dp(18),dp(35),dp(18));
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setBackground(shape(0xff0d2433,0xff2b5769));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);
        lp.setMargins(0,dp(15),0,dp(15));
        root.addView(content,lp);
        TextView title=label("dynamicOS als Startbildschirm festlegen",28,Color.WHITE,true);
        content.addView(title);
        TextView info=label("Wähle dynamicOS Fiesta als Standard-Home-App. Danach öffnet sich beim Drücken der Home-Taste und nach dem Systemstart diese Oberfläche. Der bisherige Launcher bleibt als Rückfalloption installiert.",17,0xffbbd6e2,false);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(92));ip.topMargin=dp(15);content.addView(info,ip);
        TextView choose=label("   ✓   Standard-Launcher auswählen",21,0xff071923,true);
        choose.setGravity(Gravity.CENTER);choose.setBackground(shape(cyan,cyan));
        content.addView(choose,new LinearLayout.LayoutParams(-1,dp(67)));
        choose.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
            catch (ActivityNotFoundException e) {
                Toast.makeText(this,"Diese Firmware bietet keine Home-App-Auswahl. Android-Einstellungen werden geöffnet.",Toast.LENGTH_LONG).show();
                setting(Settings.ACTION_SETTINGS);
            }
        });
        TextView retry=label("↻  Auswahl prüfen",17,cyan,true);
        retry.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(53));rp.topMargin=dp(8);
        content.addView(retry,rp);
        retry.setOnClickListener(v -> {
            if(isDefaultHome()) { setupVisible=false; home(); }
            else Toast.makeText(this,"dynamicOS ist noch nicht als Standard ausgewählt.",Toast.LENGTH_LONG).show();
        });
        TextView skip=label("Vorerst ohne Standardauswahl öffnen",15,0xffa7bcc9,false);
        skip.setGravity(Gravity.CENTER);
        content.addView(skip,new LinearLayout.LayoutParams(-1,dp(42)));
        skip.setOnClickListener(v->{setupVisible=false;home();});
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
        root.setPadding(dp(22), dp(6), dp(22), dp(6));
        root.setBackgroundColor(bg);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("dynamic", 24, Color.WHITE, true);
        top.addView(brand);
        top.addView(label("OS", 24, cyan, true));
        TextView separator = label("   │   " + subtitle, 19, 0xffb6cad9, false);
        top.addView(separator, new LinearLayout.LayoutParams(0, dp(57), 1));
        String now = new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date());
        top.addView(label(now + "    ◈", 20, 0xffc8eff5, false));
        root.addView(top);
        View line = new View(this); line.setBackgroundColor(0xff125468);
        root.addView(line, new LinearLayout.LayoutParams(-1, 1));
        setContentView(root);
    }

    private LinearLayout tile(String icon, String title, boolean active, Runnable clicked) {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER);
        IconView graphic = new IconView(icon, active ? cyan : Color.WHITE);
        view.addView(graphic, new LinearLayout.LayoutParams(dp(66), dp(66)));
        TextView caption = label(title, 22, Color.WHITE, true);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(42));
        tp.topMargin = dp(8);
        view.addView(caption, tp);
        view.setBackground(shape(panel, active ? cyan : 0xff315062));
        view.setOnClickListener(v -> clicked.run());
        view.setContentDescription(title);
        return view;
    }

    private final class IconView extends View {
        private final String type;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        IconView(String type, int color) {
            super(LauncherActivity.this);
            this.type = type;
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4);
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save(); canvas.scale(getWidth()/100f, getHeight()/100f);
            if ("auto".equals(type)) {
                Path a = new Path(); a.moveTo(50,13); a.lineTo(83,78); a.lineTo(52,62); a.lineTo(19,78); a.close();
                canvas.drawPath(a,p); canvas.drawLine(50,13,50,55,p);
            } else if ("music".equals(type)) {
                canvas.drawLine(43,25,43,72,p);canvas.drawLine(43,25,79,17,p);
                canvas.drawLine(79,17,79,63,p);canvas.drawOval(21,67,43,83,p);canvas.drawOval(57,58,79,74,p);
            } else if ("car".equals(type)) {
                Path c = new Path();c.moveTo(18,57);c.lineTo(28,34);c.quadTo(31,27,40,27);c.lineTo(67,27);c.quadTo(74,28,77,35);c.lineTo(85,57);c.close();canvas.drawPath(c,p);
                canvas.drawRoundRect(12,51,88,75,9,9,p);canvas.drawLine(24,75,24,82,p);canvas.drawLine(76,75,76,82,p);
                canvas.drawLine(25,57,34,57,p);canvas.drawLine(66,57,75,57,p);
            } else {
                canvas.drawCircle(50,50,17,p);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;float x=(float)Math.cos(a),y=(float)Math.sin(a);
                    canvas.drawLine(50+25*x,50+25*y,50+36*x,50+36*y,p);}
            }
            canvas.restore();
        }
    }

    private void home() {
        scaffold("Fiesta");
        LinearLayout content = new LinearLayout(this);
        content.setPadding(0, dp(13), 0, dp(12));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout vehicle = new LinearLayout(this);
        vehicle.setOrientation(LinearLayout.VERTICAL);
        vehicle.setGravity(Gravity.CENTER);
        vehicle.setBackground(shape(0xff0c2735, 0xff1a4056));
        ImageView car = new ImageView(this);
        car.setImageResource(R.drawable.fiesta_graphite);
        car.setScaleType(ImageView.ScaleType.FIT_CENTER);
        vehicle.addView(car, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView model = label("FORD FIESTA  ·  W-S 107", 20, Color.WHITE, true); model.setGravity(Gravity.CENTER);
        vehicle.addView(model, new LinearLayout.LayoutParams(-1, dp(35)));
        TextView year = label("Mk7 Facelift    |    2015    |    Dunkelgrau", 13, 0xff91b2c1, false); year.setGravity(Gravity.CENTER);
        vehicle.addView(year, new LinearLayout.LayoutParams(-1, dp(28)));
        LinearLayout.LayoutParams vehicleLp = new LinearLayout.LayoutParams(0, -1, 0.38f);
        vehicleLp.setMargins(0, 0, dp(10), 0);
        content.addView(vehicle, vehicleLp);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2); grid.setRowCount(2);
        content.addView(grid, new LinearLayout.LayoutParams(0, -1, 0.62f));
        addTile(grid, "auto", "Android Auto", true, this::autoConnect, 0, 0);
        addTile(grid, "music", "Musik", false, this::openMusic, 0, 1);
        addTile(grid, "car", "Fahrzeug", false, this::vehicleMenu, 1, 0);
        addTile(grid, "settings", "Einstellungen", false, this::settingsMenu, 1, 1);

        navBar("Start");
    }

    private void addTile(GridLayout grid, String icon, String name, boolean selected,
                         Runnable action, int row, int col) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(row, 1f), GridLayout.spec(col, 1f));
        lp.width = 0; lp.height = 0;
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(tile(icon, name, selected, action), lp);
    }

    private void navBar(String selected) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER);
        bar.setBackground(shape(0xff0b1b2c, 0xff20475a));
        String[] names={"Start","Fahrzeug","Apps","Einstellungen"};
        Runnable[] actions={this::home,this::vehicleMenu,this::allApps,this::settingsMenu};
        for(int i=0;i<names.length;i++) {
            final int index=i;
            TextView item=label(names[i],16,names[i].equals(selected)?cyan:0xffd9e9f0,names[i].equals(selected));
            item.setGravity(Gravity.CENTER);
            bar.addView(item,new LinearLayout.LayoutParams(0,dp(51),1));
            item.setOnClickListener(v->actions[index].run());
        }
        root.addView(bar);
    }

    private void menu(String title, String[][] items, Runnable[] actions) {
        scaffold(title);
        LinearLayout main = new LinearLayout(this);
        main.setPadding(0,dp(15),0,dp(14));
        root.addView(main,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setPadding(dp(22),dp(25),dp(15),dp(14));
        sidebar.setBackground(shape(0xff0c2435,0xff254e64));
        TextView section = label(title.toUpperCase(Locale.GERMANY),24,Color.WHITE,true);
        sidebar.addView(section);
        TextView subtitle=label("dynamicOS  /  Fiesta",14,cyan,false);
        sidebar.addView(subtitle);
        View divider = new View(this); divider.setBackgroundColor(0xff24556b);
        LinearLayout.LayoutParams divP=new LinearLayout.LayoutParams(-1,dp(1));divP.topMargin=dp(18);divP.bottomMargin=dp(18);
        sidebar.addView(divider,divP);
        String detail="Direktzugriff auf die wichtigsten Funktionen.\nKlare Wege, kurze Reaktionszeiten.";
        if("Fahrzeug".equals(title)) detail="Ford Fiesta Mk7\nDunkelgrau · W-S 107\n\nSteuerung und Diagnose.";
        if("Android Auto".equals(title)) detail="AutoConnect\nBluetooth und WLAN\n\nEmpfänger separat erforderlich.";
        sidebar.addView(label(detail,16,0xffacc7d5,false));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,-1,.30f);
        slp.rightMargin=dp(12);main.addView(sidebar,slp);

        GridLayout cards = new GridLayout(this);
        cards.setColumnCount(2);
        main.addView(cards,new LinearLayout.LayoutParams(0,-1,.70f));
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            LinearLayout card = new LinearLayout(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(20),dp(12),dp(18),dp(12));
            card.setBackground(shape(panel,0xff315468));
            TextView symbol=label(items[i][0],32,cyan,true);
            card.addView(symbol,new LinearLayout.LayoutParams(dp(55),-1));
            TextView item=label(items[i][1],19,Color.WHITE,true);
            card.addView(item,new LinearLayout.LayoutParams(0,-1,1));
            GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(i/2,1f),GridLayout.spec(i%2,1f));
            p.width=0;p.height=0;p.setMargins(dp(5),dp(5),dp(5),dp(5));
            cards.addView(card,p);
            card.setContentDescription(items[i][1]);
            card.setOnClickListener(v -> actions[index].run());
            item.setOnClickListener(v -> actions[index].run());
        }
        navBar(title);
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
        scroller.addView(list); root.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        navBar("Apps");
    }

    @Override public void onBackPressed() { home(); }
}

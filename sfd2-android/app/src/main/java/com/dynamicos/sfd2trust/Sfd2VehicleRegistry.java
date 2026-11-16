package com.dynamicos.sfd2trust;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Informational, non-executable SFD2 vehicle catalogue.
 * Fixed HTTPS endpoint only. Never sends VIN, token or diagnostic data to the network.
 * Remote catalogue strings are presentation data only, not programming instructions.
 */
public final class Sfd2VehicleRegistry {
    public static final String SOURCE_URL =
       "https://carista.com/de/blogs/news/carista-is-first-to-enable-sfd2-customization";
    private static final String CATALOG_URL =
       "https://raw.githubusercontent.com/kaivonderburg-create/MissionControl/" +
       "dynamicOS-sfd2-premium-v9-2/sfd2-android/catalog/sfd2-vehicles.json";
    public interface Callback { void completed(boolean ok,String message); }
    public static final class Vehicle {
        public final String brand,model,code,criteria,scope;
        Vehicle(JSONObject obj) {
            brand=obj.optString("brand");model=obj.optString("model");
            code=obj.optString("code");criteria=obj.optString("criteria");
            scope=obj.optString("scope");
        }
        public String title(){return brand+"  "+model;}
        public String subtitle(){return code+"   ·   "+criteria;}
    }
    private final Context app;
    private final SharedPreferences prefs;
    private volatile List<Vehicle> current=Collections.emptyList();
    private volatile String date="–";
    private volatile String source="Offline-Datenbestand";
    public Sfd2VehicleRegistry(Context ctx){
        app=ctx.getApplicationContext();
        prefs=app.getSharedPreferences("dynamicOS_sfd2_catalog_v1",Context.MODE_PRIVATE);
        try{
            String raw=prefs.getString("catalog","");
            if(!raw.isEmpty())parse(raw,"Zuletzt online gespeichert");
            else offline();
        }catch(Exception ignored) {
            try {offline();}catch(Exception e){current=Collections.emptyList();}
        }
    }
    private String read(InputStream in,int maxBytes) throws Exception {
        try(InputStream stream=in;ByteArrayOutputStream buffer=new ByteArrayOutputStream()){
            byte[] bytes=new byte[8192];int n;
            while((n=stream.read(bytes))!=-1){
                if(buffer.size()+n>maxBytes)throw new IllegalStateException("Katalog überschreitet Größenbegrenzung");
                buffer.write(bytes,0,n);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }
    private synchronized void offline() throws Exception {
        parse(read(app.getAssets().open("sfd2-vehicles.json"),220000),"In der APK hinterlegt");
    }
    private synchronized void parse(String raw,String origin) throws Exception {
        JSONObject doc=new JSONObject(raw);
        if(doc.optInt("schema")!=1)throw new IllegalArgumentException("Unbekanntes Katalogschema");
        JSONArray arr=doc.getJSONArray("vehicles");
        if(arr.length()==0||arr.length()>200)throw new IllegalArgumentException("Unzulässige Anzahl Einträge");
        ArrayList<Vehicle> entries=new ArrayList<>();
        for(int i=0;i<arr.length();i++){
            Vehicle e=new Vehicle(arr.getJSONObject(i));
            if(e.brand.isEmpty()||e.model.isEmpty()||e.code.length()>80||e.criteria.length()>250||
                e.brand.length()>60||e.model.length()>80)
                throw new IllegalArgumentException("Ungültige Fahrzeugdaten");
            entries.add(e);
        }
        current=Collections.unmodifiableList(entries);
        date=doc.optString("updated","–");
        source=origin;
    }
    public List<Vehicle> all(){return current;}
    public int count(){return current.size();}
    public String revision(){return date;}
    public String source(){return source;}
    public List<Vehicle> find(String query){
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        if(q.isEmpty())return all();
        ArrayList<Vehicle> result=new ArrayList<>();
        for(Vehicle v:current){
            String line=(v.brand+" "+v.model+" "+v.code+" "+v.criteria).toLowerCase(Locale.ROOT);
            if(line.contains(q))result.add(v);
        }
        return result;
    }
    public boolean connected(){
        ConnectivityManager mgr=(ConnectivityManager)app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(mgr==null)return false;
        android.net.Network net=mgr.getActiveNetwork();
        if(net==null)return false;
        NetworkCapabilities c=mgr.getNetworkCapabilities(net);
        return c!=null&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
    public void refresh(Callback callback){
        new Thread(()->{
            boolean success=false;
            String message;
            HttpURLConnection c=null;
            try {
                if(!connected())throw new IllegalStateException("Keine Internetverbindung verfügbar");
                URL url=new URL(CATALOG_URL);
                if(!"https".equalsIgnoreCase(url.getProtocol())||
                   !"raw.githubusercontent.com".equalsIgnoreCase(url.getHost()))
                    throw new SecurityException("Nicht freigegebener Katalogserver");
                c=(HttpURLConnection)url.openConnection();
                c.setConnectTimeout(6000);c.setReadTimeout(9000);
                c.setRequestMethod("GET");
                c.setInstanceFollowRedirects(false);
                c.setRequestProperty("Accept","application/json");
                c.setRequestProperty("User-Agent","dynamicOS-SFD2/9.2");
                int status=c.getResponseCode();
                if(status!=200)throw new IllegalStateException("Katalogserver: HTTP "+status);
                String raw=read(c.getInputStream(),220000);
                parse(raw,"GitHub / HTTPS");
                prefs.edit().putString("catalog",raw).apply();
                success=true;
                message=count()+" Fahrzeuge aktualisiert · Stand "+revision();
            } catch(Exception e) {
                message="Online-Aktualisierung nicht möglich: "+e.getMessage();
            } finally {
                if(c!=null)c.disconnect();
            }
            final boolean done=success;
            final String text=message;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.completed(done,text));
        },"dynamicOS-SFD2-Catalog").start();
    }
}

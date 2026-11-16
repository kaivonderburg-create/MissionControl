package com.dynamicos.sfd2trust;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Standards-based OBD-II/EOBD diagnostics. Independent implementation.
 * Explicitly NOT VW UDS programming, SFD2 unlocking, or manufacturer coding.
 */
public final class ObdCore {
    public static final class Reading {
        public final String key,name,unit;
        public final double value;
        public final long timestamp;
        Reading(String key,String name,String unit,double value){
            this.key=key;this.name=name;this.unit=unit;this.value=value;
            this.timestamp=System.currentTimeMillis();
        }
        public String display(){
            int places=("rpm".equals(key)||"speed".equals(key)||"coolant".equals(key)||
                "load".equals(key)||"throttle".equals(key)||"oil".equals(key))?0:1;
            return String.format(Locale.GERMANY,"%."+places+"f %s",value,unit);
        }
    }
    public static final class Report {
        public String vin="Nicht verfügbar";
        public String protocol="Nicht erkannt";
        public String elmVersion="—";
        public String description="Nicht verbunden";
        public String dtcStored="Noch nicht gelesen";
        public String dtcPending="Noch nicht gelesen";
        public String dtcPermanent="Noch nicht gelesen";
        public String freeze="Noch nicht gelesen";
        public String mode06="Noch nicht gelesen";
        public String readiness="Noch nicht gelesen";
        public String supported="Noch nicht gelesen";
        public String lastResponse="";
        public final LinkedHashMap<String,Reading> data=new LinkedHashMap<>();
        public long lastUpdate;
        public double tripKm=0;
        public double peakSpeed=0;
        public double accelTime=-1;
        public String connectionStatus="Getrennt";
        public Report copy(){
            Report o=new Report();
            o.vin=vin;o.protocol=protocol;o.elmVersion=elmVersion;o.description=description;
            o.dtcStored=dtcStored;o.dtcPending=dtcPending;o.dtcPermanent=dtcPermanent;
            o.freeze=freeze;o.mode06=mode06;o.readiness=readiness;
            o.supported=supported;o.lastResponse=lastResponse;
            o.data.putAll(data);
            o.lastUpdate=lastUpdate;o.tripKm=tripKm;o.peakSpeed=peakSpeed;
            o.accelTime=accelTime;o.connectionStatus=connectionStatus;
            return o;
        }
        public String csvHeader(){return "time_ms,rpm,speed_kmh,coolant_c,throttle_percent,load_percent,maf_gps,voltage_v,trip_km\n";}
        public String csvLine(){
            return System.currentTimeMillis()+","+value("rpm")+","+value("speed")+","+
                value("coolant")+","+value("throttle")+","+value("load")+","+
                value("maf")+","+value("voltage")+","+
                String.format(Locale.ROOT,"%.4f",tripKm)+"\n";
        }
        private String value(String key){
            Reading r=data.get(key);
            return r==null?"":String.format(Locale.ROOT,"%.3f",r.value);
        }
        public String diagnosticText(){
            return "dynamicOS Diagnostics · OBD-II\nAdapter: "+description+
                "\nStatus: "+connectionStatus+"\nFIN: "+vin+
                "\nELM: "+elmVersion+"\nProtokoll: "+protocol+
                "\n\nGespeicherte Fehlercodes:\n"+dtcStored+
                "\n\nAusstehende Fehlercodes:\n"+dtcPending+
                "\n\nPermanente Fehlercodes:\n"+dtcPermanent+
                "\n\nEmissionsbereitschaft:\n"+readiness+
                "\n\nFreeze-Frame:\n"+freeze+
                "\n\nMode-06-Selbsttests:\n"+mode06+
                "\n\nFahrstrecke: "+String.format(Locale.GERMANY,"%.2f km",tripKm);
        }
    }
    private static final class Spec {
        final String pid,key,name,unit;
        Spec(String pid,String key,String name,String unit){
            this.pid=pid;this.key=key;this.name=name;this.unit=unit;
        }
    }
    private static final Spec[] SENSORS = new Spec[]{
        new Spec("0C","rpm","Motordrehzahl","U/min"),
        new Spec("0D","speed","Geschwindigkeit","km/h"),
        new Spec("05","coolant","Kühlmittel","°C"),
        new Spec("11","throttle","Drosselklappe","%"),
        new Spec("04","load","Motorlast","%"),
        new Spec("10","maf","Luftmasse","g/s"),
        new Spec("42","voltage","Steuergerätspannung","V"),
        new Spec("0F","intake","Ansauglufttemperatur","°C"),
        new Spec("0B","map","Saugrohrdruck","kPa"),
        new Spec("2F","fuel","Kraftstoffstand","%"),
        new Spec("5C","oil","Öltemperatur","°C"),
        new Spec("06","stft","Kurzzeitkorrektur","%"),
        new Spec("07","ltft","Langzeitkorrektur","%")
    };
    private ObdLink.Connection link;
    private final Report report=new Report();
    private final StringBuilder rows=new StringBuilder();
    private long lastSample,accelStart;
    private boolean accelerating;
    private boolean record=true;
    public synchronized boolean connected(){return link!=null;}
    public synchronized Report snapshot(){return report.copy();}
    public synchronized void setLogging(boolean value){record=value;}
    public synchronized String csv(){
        return report.csvHeader()+rows.toString();
    }
    public synchronized void resetTrip(){
        report.tripKm=0;report.peakSpeed=0;report.accelTime=-1;
        lastSample=0;accelStart=0;accelerating=false;rows.setLength(0);
    }
    private String send(String c,int timeout) throws Exception {
        if(link==null)throw new IllegalStateException("Kein Adapter verbunden");
        String out=link.command(c,timeout);
        report.lastResponse=out;
        return out;
    }
    public synchronized void open(ObdLink.Connection candidate) throws Exception {
        close();
        link=candidate;
        report.connectionStatus="Initialisiere …";
        report.description=candidate.description();
        try {
            send("ATZ",5000);
            send("ATE0",1700);
            send("ATL0",1700);
            send("ATS0",1700);
            send("ATH0",1700);
            send("ATSP0",3500);
            send("ATAT1",1500);
            send("ATST64",1500);
            report.elmVersion=clean(send("ATI",1500));
            report.protocol=clean(send("ATDP",2000));
            String supported=send("0100",4000);
            report.supported=supported.replace('>',' ').trim();
            String vinResponse=send("0902",6000);
            String found=parseVin(vinResponse);
            if(found!=null)report.vin=found;
            report.connectionStatus="Verbunden";
            lastSample=0;
        }catch(Exception ex){close();throw ex;}
    }
    public synchronized void close(){
        if(link!=null){try{link.close();}catch(Exception ignored){}}
        link=null;report.connectionStatus="Getrennt";
    }
    public synchronized Report poll() throws Exception {
        if(link==null)throw new IllegalStateException("Nicht verbunden");
        // Short first cycle; extensions can be requested on the sensor page.
        for(Spec sp:SENSORS){
            if(!sp.pid.equals("0C")&&!sp.pid.equals("0D")&&!sp.pid.equals("05")&&
                !sp.pid.equals("11")&&!sp.pid.equals("04")&&!sp.pid.equals("10"))continue;
            try {
                String raw=send("01"+sp.pid,2100);
                byte[] data=data(raw,"41"+sp.pid);
                if(data!=null){
                    Double val=decode(sp.pid,data);
                    if(val!=null)report.data.put(sp.key,
                        new Reading(sp.key,sp.name,sp.unit,val));
                }
            }catch(java.net.SocketTimeoutException ex){/* unsupported PID, continue */}
        }
        long now=System.currentTimeMillis();
        Reading speed=report.data.get("speed");
        if(speed!=null){
            report.peakSpeed=Math.max(report.peakSpeed,speed.value);
            if(lastSample>0&&now-lastSample<15000)
                report.tripKm+=speed.value*(now-lastSample)/3600000.0;
            if(!accelerating&&speed.value>=3&&speed.value<=25){
                accelerating=true;accelStart=now;
            }
            if(accelerating&&speed.value>=100){
                report.accelTime=(now-accelStart)/1000.0;
                accelerating=false;
            }
            if(accelerating&&speed.value<1){accelerating=false;accelStart=0;}
        }
        lastSample=now;report.lastUpdate=now;
        if(record){
            rows.append(report.csvLine());
            if(rows.length()>550000)rows.delete(0,Math.min(130000,rows.length()));
        }
        return report.copy();
    }
    public synchronized Report pollExtended() throws Exception {
        if(link==null)throw new IllegalStateException("Nicht verbunden");
        for(Spec sp:SENSORS) {
            try {
                byte[] d=data(send("01"+sp.pid,2200),"41"+sp.pid);
                if(d!=null){
                    Double v=decode(sp.pid,d);
                    if(v!=null)report.data.put(sp.key,new Reading(sp.key,sp.name,sp.unit,v));
                }
            }catch(java.net.SocketTimeoutException ignored){}
        }
        report.lastUpdate=System.currentTimeMillis();
        return report.copy();
    }
    public synchronized String customStandardPid(String request) throws Exception {
        if(request==null||!request.toUpperCase(Locale.ROOT).matches("01[0-9A-F]{2}"))
            throw new IllegalArgumentException("Nur Standard-Leseabfragen 01xx unterstützt");
        return send(request.toUpperCase(Locale.ROOT),4500);
    }
    public synchronized Report readDtc() throws Exception {
        report.dtcStored=readCodes("03","43");
        report.dtcPending=readCodes("07","47");
        report.dtcPermanent=readCodes("0A","4A");
        return report.copy();
    }
    private String readCodes(String mode,String positive) throws Exception {
        String raw=send(mode,5300);
        byte[] values=data(raw,positive);
        if(values==null)return "Nicht unterstützt / keine Daten\n"+clean(raw);
        ArrayList<String> decoded=new ArrayList<>();
        for(int i=0;i+1<values.length;i+=2){
            int a=values[i]&255,b=values[i+1]&255;
            if(a==0&&b==0)continue;
            String prefix="PCBU".substring((a>>6)&3,(a>>6&3)+1);
            String code=String.format(Locale.ROOT,"%s%d%X%X%X",
                prefix,(a>>4)&3,a&15,(b>>4)&15,b&15);
            if(!decoded.contains(code))decoded.add(code);
        }
        if(decoded.isEmpty())return "Keine Fehlercodes gemeldet";
        return String.join("\n",decoded);
    }
    public synchronized Report readReadiness() throws Exception {
        String raw=send("0101",5000);
        byte[] v=data(raw,"4101");
        if(v==null||v.length<4)report.readiness="Keine Daten\n"+clean(raw);
        else {
            int a=v[0]&255,b=v[1]&255,c=v[2]&255,d=v[3]&255;
            report.readiness="MIL: "+(((a&128)!=0)?"AN":"AUS")+
                "\nGemeldete DTC-Anzahl: "+(a&127)+
                "\nMotortyp: "+(((b&8)!=0)?"Selbstzünder":"Fremdzünder")+
                "\nUnterstützte/abgeschlossene Monitore, Rohwerte: "+
                String.format(Locale.ROOT,"%02X %02X %02X",b,c,d);
        }
        return report.copy();
    }
    public synchronized Report readSpecial() throws Exception {
        report.freeze=clean(send("0200",5500));
        report.mode06=clean(send("0600",5500));
        return report.copy();
    }
    public synchronized void clearDtcAfterConfirmation() throws Exception {
        // Only generic OBD mode 04 and only when a separate UI dialog has confirmed the action.
        String response=send("04",6500);
        if(data(response,"44")==null)
            throw new IllegalStateException("Löschen nicht bestätigt: "+clean(response));
        report.dtcStored="Nach dem Löschen noch nicht erneut gelesen";
        report.dtcPending="Nach dem Löschen noch nicht erneut gelesen";
        report.readiness="OBD-Monitore können nach dem Löschen zurückgesetzt sein";
    }
    private static String clean(String s){
        return s==null?"":s.replace(">","").replace("\u0000","").trim();
    }
    private static byte[] data(String response,String signature) {
        if(response==null)return null;
        String[] lines=response.toUpperCase(Locale.ROOT).split("[\\r\\n]+");
        for(String line:lines){
            if(line.contains("NO DATA")||line.contains("UNABLE")||line.contains("ERROR"))continue;
            String hex=line.replaceAll("[^0-9A-F]","");
            String wanted=signature.toUpperCase(Locale.ROOT);
            int pos=hex.indexOf(wanted);
            if(pos<0)continue;
            String bytes=hex.substring(pos+wanted.length());
            if(bytes.length()<2)continue;
            int length=bytes.length()/2;
            byte[] out=new byte[length];
            for(int i=0;i<length;i++){
                try{out[i]=(byte)Integer.parseInt(bytes.substring(i*2,i*2+2),16);}
                catch(Exception e){return null;}
            }
            return out;
        }
        return null;
    }
    private static Double decode(String pid,byte[] bytes) {
        if(bytes.length==0)return null;
        double a=bytes[0]&255,b=bytes.length>1?bytes[1]&255:0;
        switch(pid){
            case "0C": return bytes.length>1?(256*a+b)/4.0:null;
            case "0D": return a;
            case "05": case "0F":case "5C":return a-40;
            case "04":case "11":case "2F":return a*100/255.0;
            case "10":return bytes.length>1?(256*a+b)/100.0:null;
            case "42":return bytes.length>1?(256*a+b)/1000.0:null;
            case "0B":return a;
            case "06":case "07":return (a-128)*100.0/128.0;
            default: return null;
        }
    }
    private static String parseVin(String raw) {
        if(raw==null)return null;
        String[] lines=raw.toUpperCase(Locale.ROOT).split("[\\r\\n]+");
        StringBuilder hex=new StringBuilder();
        for(String line:lines){
            String normalized=line.replaceAll("[^0-9A-F]","");
            int start=normalized.indexOf("4902");
            if(start>=0){
                String after=normalized.substring(start+4);
                if(after.length()>=2)after=after.substring(2);
                hex.append(after);
            }else if(line.matches("^\\s*[0-9A-F]{1,2}:.*")){
                int colon=line.indexOf(':');
                hex.append(line.substring(colon+1).replaceAll("[^0-9A-F]",""));
            }
        }
        byte[] b=new byte[hex.length()/2];
        for(int i=0;i<b.length;i++){
            try{b[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);}
            catch(Exception ex){return null;}
        }
        String decoded=new String(b,StandardCharsets.US_ASCII);
        String candidate=decoded.replaceAll("[^A-HJ-NPR-Z0-9]","");
        if(candidate.length()>=17){
            String vin=candidate.substring(candidate.length()-17);
            if(vin.matches("[A-HJ-NPR-Z0-9]{17}"))return vin;
        }
        return null;
    }
    public static String sensorList() {
        StringBuilder b=new StringBuilder();
        for(Spec s:SENSORS)b.append("01").append(s.pid).append("   ")
            .append(s.name).append("  [").append(s.unit).append("]\n");
        return b.toString();
    }
}

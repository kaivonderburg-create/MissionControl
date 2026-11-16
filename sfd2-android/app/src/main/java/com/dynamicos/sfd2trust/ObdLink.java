package com.dynamicos.sfd2trust;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Independent ELM327 transport: paired Bluetooth SPP, best-effort BLE UART, Wi-Fi TCP.
 *  The BLE UART interface is vendor-dependent; proprietary device protocols are NOT assumed.
 */
@SuppressLint("MissingPermission")
public final class ObdLink {
    public static final String CLASSIC="BT", BLE="BLE", WIFI="WIFI";
    public static class Option {
        public final String name,address,type;
        public Option(String name,String address,String type){
            this.name=name==null?"Unbekannt":name;this.address=address;this.type=type;
        }
        public String display(){return name+"  ·  "+type+"  ·  "+address;}
    }
    public interface Connection {
        String command(String command,int timeoutMs) throws Exception;
        void close();
        String description();
    }
    public interface SearchCallback {
        void found(Option option);
        void done(String message);
    }
    private static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final UUID CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final Context ctx;
    private final BluetoothAdapter adapter;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    public ObdLink(Context context){
        ctx=context.getApplicationContext();
        BluetoothManager manager=(BluetoothManager)ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter=manager==null?null:manager.getAdapter();
    }
    public boolean permitted(){
        return Build.VERSION.SDK_INT<31 || (
            ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED &&
            ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED
        );
    }
    public boolean enabled(){return adapter!=null&&adapter.isEnabled();}
    public List<Option> paired(){
        if(!permitted()||!enabled())return Collections.emptyList();
        ArrayList<Option> result=new ArrayList<>();
        for(BluetoothDevice dev:adapter.getBondedDevices()){
            String n=dev.getName();
            result.add(new Option(n,dev.getAddress(),CLASSIC));
        }
        result.sort(Comparator.comparing(o->o.name.toLowerCase(java.util.Locale.ROOT)));
        return result;
    }
    public synchronized void stopScan(){
        if(scanner!=null&&scanCallback!=null) {
            try{scanner.stopScan(scanCallback);}catch(Exception ignored){}
        }
        scanner=null;scanCallback=null;
    }
    public synchronized void scanBle(SearchCallback listener) {
        if(!permitted()||!enabled()){listener.done("Bluetooth-Berechtigung oder Funkverbindung fehlt");return;}
        stopScan();
        scanner=adapter.getBluetoothLeScanner();
        if(scanner==null){listener.done("BLE-Scanner nicht verfügbar");return;}
        final java.util.HashSet<String> seen=new java.util.HashSet<>();
        scanCallback=new ScanCallback(){
            @Override public void onScanResult(int callbackType,ScanResult result){
                BluetoothDevice d=result.getDevice();
                if(d==null||d.getAddress()==null||!seen.add(d.getAddress()))return;
                String name=result.getScanRecord()==null?null:result.getScanRecord().getDeviceName();
                if(name==null)name=d.getName();
                listener.found(new Option(name,d.getAddress(),BLE));
            }
            @Override public void onScanFailed(int errorCode) {
                stopScan();listener.done("BLE-Scanfehler "+errorCode);
            }
        };
        try{scanner.startScan(scanCallback);}
        catch(Exception e){stopScan();listener.done("BLE-Start fehlgeschlagen: "+e.getMessage());return;}
        ui.postDelayed(()->{stopScan();listener.done("BLE-Suche abgeschlossen");},10000);
    }
    public Connection open(Option option) throws Exception {
        if(WIFI.equals(option.type)){
            String[] host=option.address.split(":",2);
            String address=host[0].trim();
            int port=host.length==2?Integer.parseInt(host[1]):35000;
            if(port<1||port>65535)throw new IllegalArgumentException("Ungültiger Port");
            Socket s=new Socket();
            s.connect(new InetSocketAddress(address,port),5000);
            s.setSoTimeout(4500);s.setTcpNoDelay(true);
            return new StreamConnection(s.getInputStream(),s.getOutputStream(),
                "Wi-Fi "+address+":"+port,s::close);
        }
        if(!permitted()||!enabled())throw new SecurityException("Bluetooth nicht freigegeben");
        BluetoothDevice device=adapter.getRemoteDevice(option.address);
        if(BLE.equals(option.type))return new BleConnection(ctx,device);
        adapter.cancelDiscovery();
        BluetoothSocket s=device.createRfcommSocketToServiceRecord(SPP);
        try{s.connect();}
        catch(Exception e){try{s.close();}catch(Exception ignored){}throw e;}
        return new StreamConnection(s.getInputStream(),s.getOutputStream(),
            "Bluetooth SPP "+option.name,s::close);
    }
    private interface Closer {void close() throws Exception;}
    private static final class StreamConnection implements Connection {
        final InputStream in;final OutputStream out;final String id;final Closer closer;
        volatile boolean closed=false;
        StreamConnection(InputStream in,OutputStream out,String id,Closer closer){
            this.in=in;this.out=out;this.id=id;this.closer=closer;
        }
        @Override public synchronized String command(String cmd,int timeoutMs) throws Exception {
            if(closed)throw new IllegalStateException("Verbindung geschlossen");
            while(in.available()>0)in.read();
            out.write((cmd+"\r").getBytes(StandardCharsets.US_ASCII));out.flush();
            long until=System.currentTimeMillis()+Math.max(500,timeoutMs);
            StringBuilder b=new StringBuilder();
            byte[] chunk=new byte[256];
            while(System.currentTimeMillis()<until&&!closed){
                int count=in.available();
                if(count>0){
                    int n=in.read(chunk,0,Math.min(chunk.length,count));
                    if(n>0){
                        b.append(new String(chunk,0,n,StandardCharsets.US_ASCII));
                        if(b.indexOf(">")>=0)return b.toString();
                        if(b.length()>32768)throw new IllegalStateException("Antwort zu groß");
                    }
                }else Thread.sleep(15);
            }
            if(closed)throw new IllegalStateException("Verbindung getrennt");
            if(b.length()==0)throw new java.net.SocketTimeoutException("Keine Adapterantwort");
            return b.toString();
        }
        @Override public String description(){return id;}
        @Override public void close(){
            closed=true;try{closer.close();}catch(Exception ignored){}
        }
    }
    private static final class BleConnection implements Connection {
        final Context ctx;final BluetoothDevice device;
        final CountDownLatch ready=new CountDownLatch(1);
        final LinkedBlockingQueue<Integer> inbox=new LinkedBlockingQueue<>(32768);
        volatile BluetoothGatt gatt;
        volatile BluetoothGattCharacteristic write,notify;
        volatile Exception failure;
        volatile boolean closed=false;
        final BluetoothGattCallback events=new BluetoothGattCallback(){
            @Override public void onConnectionStateChange(BluetoothGatt g,int status,int state) {
                if(state==BluetoothProfile.STATE_CONNECTED&&status==0){g.discoverServices();}
                else{
                    failure=new IllegalStateException("BLE getrennt / Status "+status);
                    ready.countDown();
                }
            }
            @Override public void onServicesDiscovered(BluetoothGatt g,int status) {
                if(status!=0){failure=new IllegalStateException("BLE GATT Services: "+status);ready.countDown();return;}
                BluetoothGattCharacteristic tx=null,rx=null;
                for(BluetoothGattService service:g.getServices()){
                    BluetoothGattCharacteristic candidateTx=null,candidateRx=null;
                    for(BluetoothGattCharacteristic c:service.getCharacteristics()){
                        int flags=c.getProperties();
                        if((flags&(BluetoothGattCharacteristic.PROPERTY_WRITE|
                                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE))!=0)
                            candidateTx=c;
                        if((flags&(BluetoothGattCharacteristic.PROPERTY_NOTIFY|
                                BluetoothGattCharacteristic.PROPERTY_INDICATE))!=0)
                            candidateRx=c;
                    }
                    if(candidateTx!=null&&candidateRx!=null){
                        tx=candidateTx;rx=candidateRx;
                        String serviceId=service.getUuid().toString();
                        if(serviceId.contains("6e400001")||serviceId.contains("ffe0")||
                           serviceId.contains("fff0"))break;
                    }
                }
                if(tx==null||rx==null){
                    failure=new IllegalStateException("Kein transparenter BLE-UART-Dienst vorhanden");
                    ready.countDown();return;
                }
                write=tx;notify=rx;
                if(!g.setCharacteristicNotification(rx,true)){
                    failure=new IllegalStateException("BLE-Benachrichtigungen nicht aktivierbar");
                    ready.countDown();return;
                }
                BluetoothGattDescriptor cccd=rx.getDescriptor(CCCD);
                if(cccd==null){ready.countDown();return;}
                byte[] enable=(rx.getProperties()&BluetoothGattCharacteristic.PROPERTY_NOTIFY)!=0?
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                if(Build.VERSION.SDK_INT>=33){
                    if(g.writeDescriptor(cccd,enable)!=android.bluetooth.BluetoothStatusCodes.SUCCESS){
                        failure=new IllegalStateException("BLE-Notify-Deskriptor nicht beschreibbar");
                        ready.countDown();
                    }
                }else{
                    cccd.setValue(enable);
                    if(!g.writeDescriptor(cccd)){
                        failure=new IllegalStateException("BLE-Benachrichtigungen fehlgeschlagen");
                        ready.countDown();
                    }
                }
            }
            @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){
                if(status!=0)failure=new IllegalStateException("BLE-Notify-Status "+status);
                ready.countDown();
            }
            private void accept(byte[] bytes){
                if(bytes!=null)for(byte b:bytes)inbox.offer(b&255);
            }
            @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){
                if(Build.VERSION.SDK_INT<33)accept(c.getValue());
            }
            @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){
                accept(value);
            }
        };
        BleConnection(Context ctx,BluetoothDevice device) throws Exception {
            this.ctx=ctx;this.device=device;
            gatt=device.connectGatt(ctx,false,events,BluetoothDevice.TRANSPORT_LE);
            if(gatt==null)throw new IllegalStateException("GATT-Verbindung konnte nicht beginnen");
            if(!ready.await(13,TimeUnit.SECONDS)){
                close();throw new java.net.SocketTimeoutException("BLE-Dienstsuche ohne Ergebnis");
            }
            if(failure!=null){close();throw failure;}
            if(write==null||notify==null){close();throw new IllegalStateException("Kein unterstützter BLE-UART-Kanal");}
        }
        @Override public synchronized String command(String cmd,int timeoutMs) throws Exception {
            if(closed||gatt==null)throw new IllegalStateException("BLE-Verbindung geschlossen");
            inbox.clear();
            byte[] payload=(cmd+"\r").getBytes(StandardCharsets.US_ASCII);
            int result;
            if(Build.VERSION.SDK_INT>=33)
                result=gatt.writeCharacteristic(write,payload,
                    (write.getProperties()&BluetoothGattCharacteristic.PROPERTY_WRITE)!=0?
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT:
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            else{
                write.setWriteType((write.getProperties()&BluetoothGattCharacteristic.PROPERTY_WRITE)!=0?
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT:
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                write.setValue(payload);
                result=gatt.writeCharacteristic(write)?0:-1;
            }
            if(result!=0)throw new IllegalStateException("BLE-Sendeversuch fehlgeschlagen: "+result);
            StringBuilder answer=new StringBuilder();
            long until=System.currentTimeMillis()+Math.max(1000,timeoutMs);
            while(System.currentTimeMillis()<until&&!closed){
                Integer ch=inbox.poll(45,TimeUnit.MILLISECONDS);
                if(ch!=null){
                    answer.append((char)(ch&255));
                    if(ch==62)return answer.toString();
                    if(answer.length()>32768)throw new IllegalStateException("BLE-Antwort zu groß");
                }
            }
            if(answer.length()==0)throw new java.net.SocketTimeoutException("Keine BLE-UART-Antwort");
            return answer.toString();
        }
        @Override public String description(){return "Bluetooth LE UART "+device.getAddress();}
        @Override public void close(){
            closed=true;
            BluetoothGatt g=gatt;gatt=null;
            if(g!=null){try{g.disconnect();g.close();}catch(Exception ignored){}}
        }
    }
}

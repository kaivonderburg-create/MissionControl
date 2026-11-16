package com.dynamicos.sfd2trust;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/** Original, small set of generic SAE OBD-II DTC descriptions.
 * This is NOT Car Scanner's proprietary vehicle-specific database.
 */
public final class DtcCatalog {
    private static final Map<String,String> CODES;
    static {
        Map<String,String> m=new HashMap<>();
        m.put("P0100","Luftmassenmesser – Signalfehler");
        m.put("P0101","Luftmassenmesser – Bereich/Leistung");
        m.put("P0102","Luftmassenmesser – Signal zu niedrig");
        m.put("P0103","Luftmassenmesser – Signal zu hoch");
        m.put("P0110","Ansauglufttemperatursensor – Stromkreis");
        m.put("P0115","Kühlmitteltemperatursensor – Stromkreis");
        m.put("P0120","Drosselklappensensor – Stromkreis");
        m.put("P0128","Kühlmitteltemperatur unter Regelwert");
        m.put("P0130","Lambdasonde Bank 1 Sensor 1 – Stromkreis");
        m.put("P0133","Lambdasonde Bank 1 Sensor 1 – langsame Reaktion");
        m.put("P0135","Lambdasonde Bank 1 Sensor 1 – Heizkreis");
        m.put("P0141","Lambdasonde Bank 1 Sensor 2 – Heizkreis");
        m.put("P0171","Gemisch zu mager – Bank 1");
        m.put("P0172","Gemisch zu fett – Bank 1");
        m.put("P0201","Einspritzventil Zylinder 1 – Stromkreis");
        m.put("P0202","Einspritzventil Zylinder 2 – Stromkreis");
        m.put("P0203","Einspritzventil Zylinder 3 – Stromkreis");
        m.put("P0204","Einspritzventil Zylinder 4 – Stromkreis");
        m.put("P0234","Ladedruckregelung – Überdruck");
        m.put("P0299","Ladedruckregelung – Unterdruck");
        m.put("P0300","Zufällige/mehrfache Verbrennungsaussetzer");
        m.put("P0301","Verbrennungsaussetzer Zylinder 1");
        m.put("P0302","Verbrennungsaussetzer Zylinder 2");
        m.put("P0303","Verbrennungsaussetzer Zylinder 3");
        m.put("P0304","Verbrennungsaussetzer Zylinder 4");
        m.put("P0325","Klopfsensor 1 – Stromkreis");
        m.put("P0335","Kurbelwellensensor – Stromkreis");
        m.put("P0340","Nockenwellensensor – Stromkreis");
        m.put("P0401","Abgasrückführung – Durchfluss unzureichend");
        m.put("P0402","Abgasrückführung – Durchfluss zu hoch");
        m.put("P0420","Katalysatorwirkung unter Grenzwert – Bank 1");
        m.put("P0442","Tankentlüftungssystem – kleine Undichtigkeit");
        m.put("P0455","Tankentlüftungssystem – große Undichtigkeit");
        m.put("P0500","Fahrgeschwindigkeitssignal – Fehler");
        m.put("P0507","Leerlaufdrehzahl höher als erwartet");
        m.put("P0562","Bordnetzspannung zu niedrig");
        m.put("P0563","Bordnetzspannung zu hoch");
        m.put("P0606","Motorsteuergerät – Prozessorfehler");
        m.put("P0700","Getriebesteuerung – Fehleranforderung");
        m.put("P2002","Dieselpartikelfilter – Wirkung unter Grenzwert");
        m.put("P2453","Partikelfilter-Differenzdrucksensor – Signalbereich");
        m.put("P2463","Partikelfilter – Rußbeladung");
        CODES=Collections.unmodifiableMap(m);
    }
    public static String describe(String code){
        if(code==null)return "Unbekannter Code";
        String normalized=code.toUpperCase(Locale.ROOT).trim();
        String value=CODES.get(normalized);
        if(value!=null)return value;
        if(normalized.startsWith("P0"))return "Generischer Antriebsstrang-Fehler (Details fahrzeugspezifisch)";
        if(normalized.startsWith("P"))return "Antriebsstrang-Fehler (herstellerspezifische Beschreibung erforderlich)";
        if(normalized.startsWith("C"))return "Fahrwerks-/Chassis-Fehler (Steuergerät prüfen)";
        if(normalized.startsWith("B"))return "Karosserie-Fehler (Steuergerät prüfen)";
        if(normalized.startsWith("U"))return "Netzwerk-/Kommunikationsfehler (Steuergerät prüfen)";
        return "Keine Beschreibung verfügbar";
    }
    private DtcCatalog(){}
}

package com.dynamicos.sfd2trust;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Passive import-only analysis for user-supplied diagnostic logs.
 * It does not send requests, unlock ECUs or infer SFD2 solely from NRC 0x33.
 */
public final class Sfd2EvidenceAnalyzer {
    private static final Pattern VIN = Pattern.compile(
        "(?<![A-Z0-9])[A-HJ-NPR-Z0-9]{17}(?![A-Z0-9])");
    private static final Pattern ECU=Pattern.compile(
        "(?i)(?:Adresse|Address|Steuergerät|ECU)\\s*[:#= ]\\s*([0-9A-F]{2})\\b");
    private static final Pattern SFD2=Pattern.compile(
        "(?i)(?:\\bSFD\\s*[- ]?2\\b|\\bUNECE\\s*R\\s*15[56]\\b|\\bSFD2\\s*UNLOCK\\b)");
    private static final Pattern SFD=Pattern.compile("(?i)\\bSFD\\b");
    private static final Pattern DENIED=Pattern.compile(
        "(?i)(?:\\bNRC\\s*(?:0x)?33\\b|\\bsecurity\\s*access\\s*denied\\b|\\b7F\\s+[0-9A-F]{2}\\s+33\\b)");
    private static final Pattern AUTH=Pattern.compile(
        "(?i)(?:\\bauthentication\\b|\\bauthentifizierung\\b|\\bautorisierung\\b|\\bchallenge\\b)");
    public static final class Result {
        public int lines, matches, sfd2Mentions, sfdMentions, deniedResponses, authMentions;
        public String preview=""; public String verdict="";
        public String ecuList="";
        public String toReport(){
            return "Quelle: importiertes Diagnoseprotokoll\n"+
                "Zeilen: "+lines+"\n"+
                "SFD2-Indizien: "+sfd2Mentions+"\n"+
                "SFD-Marker: "+sfdMentions+"\n"+
                "Berechtigungsablehnungen: "+deniedResponses+"\n"+
                "Authentifizierungsbegriffe: "+authMentions+"\n"+
                "Erkannte ECU-Adressen: "+(ecuList.isEmpty()?"–":ecuList)+"\n\n"+
                verdict+"\n\nAuszug (FIN maskiert):\n"+preview+"\n\n"+
                "Nur Protokollklassifikation. Kein Entsperrnachweis, kein aktiver ECU-Zugriff.";
        }
    }
    public static Result analyze(byte[] content) {
        if(content.length>1024*1024)throw new IllegalArgumentException("Maximal 1 MB pro Protokoll");
        Result result=new Result();
        String all=new String(content,StandardCharsets.UTF_8);
        String[] lines=all.split("\\r?\\n",-1);
        result.lines=lines.length;
        List<String> ecus=new ArrayList<>();
        StringBuilder preview=new StringBuilder();
        for(String line:lines) {
            String part=line.length()>700?line.substring(0,700):line;
            boolean l2=SFD2.matcher(part).find();
            boolean l1=SFD.matcher(part).find();
            boolean d=DENIED.matcher(part).find();
            boolean a=AUTH.matcher(part).find();
            if(l2)result.sfd2Mentions++;
            if(l1)result.sfdMentions++;
            if(d)result.deniedResponses++;
            if(a)result.authMentions++;
            Matcher e=ECU.matcher(part);
            while(e.find()) {
                String addr=e.group(1).toUpperCase(Locale.ROOT);
                if(!ecus.contains(addr) && ecus.size()<24)ecus.add(addr);
            }
            if(l2||l1||d||a){
                result.matches++;
                if(preview.length()<2100) {
                    String masked=VIN.matcher(part).replaceAll("[FIN-MASKIERT]");
                    preview.append("• ").append(masked).append("\n");
                }
            }
        }
        result.preview=preview.toString();
        result.ecuList=String.join(", ",ecus);
        if(result.sfd2Mentions>0)
            result.verdict="SFD2-Hinweis im Protokoll gefunden. Fahrzeug-/ECU-Schutzstatus muss separat geprüft werden.";
        else if(result.sfdMentions>0)
            result.verdict="SFD-Hinweis gefunden, keine eindeutige SFD2-Kennzeichnung.";
        else if(result.deniedResponses>0)
            result.verdict="Berechtigungsablehnung gefunden, SFD2 als Ursache NICHT bewiesen.";
        else
            result.verdict="Keine eindeutigen SFD2-Indizien in diesem Protokoll.";
        return result;
    }
}

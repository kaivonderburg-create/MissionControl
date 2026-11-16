# dynamicOS Scan Pro 10.0

Eigenständige Android-Diagnose-App mit Graphit-/Eisblau-Design und integriertem SFD2 Trust Center.

## Grundlage und Rechte
Die vom Auftraggeber bereitgestellte Datei `Car Scanner_2.2.42.apk` diente **ausschließlich als Funktionsreferenz**. Sie enthielt .NET-MAUI-Komponenten sowie Bluetooth LE, USB-Serial und Grafikbibliotheken. Es wurden keine proprietären Programmroutinen, Ressourcen, Markenlogos, kostenpflichtigen Datenbanken oder Schutzmechanismen übernommen.

## Tatsächlich integrierte Funktionen
- Native Java/Android-Oberfläche mit dynamicOS-Design.
- Verbindungen zu ELM327-kompatiblen OBD-Adaptern über gepaartes Bluetooth Classic (SPP), best-effort transparentes BLE-UART und TCP/WLAN.
- Runtime-Bluetoothberechtigungen, Adapterwahl, Auto-Reconnect beim App-Start (nicht als autonomer Hintergrunddienst).
- ELM-Konfiguration, Protokollanzeige, Standard-VIN-Abfrage (OBD Modus 09).
- Standard-OBD-Live-Sensoren (Modus 01) mit einfachen Zeitverläufen.
- OBD DTC Modus 03, 07 und 0A, eigene kleine Standard-Codeerklärungen.
- Emissionsbereitschaft 0101, Freeze-Frame Rohabfrage 0200, Mode-06-Rohabfrage 0600.
- Generisches OBD-Fehlerlöschen (Modus 04) **nur nach sichtbarer ausdrücklicher Bestätigung**.
- OBD-Fahrtdistanzschätzung, Höchsttempo, grobe 0-100-Schätzung und CSV-Export.
- Eigene Standard-PID-Abfrage auf `01xx` beschränkt.
- Vorhandene v9.2 SFD2-Vertrauenskette, generisches JWS-Labor, Fahrzeugliste, lokale SFD2-Loganalyse.
- Kein Benutzerkonto, keine Werbung, keine Übermittlung von Fahrzeugsensordaten an Dritte.

## Nicht implementiert / nicht als gegeben darstellen
- Vollständige Car-Scanner-Funktions- und Datenbank-Parität, proprietäre Fahrzeugeinstellungen, Coderoutinen, Lizenzen und Herstellerprofile.
- Umfassende Hersteller-UDS-Diagnose (insbesondere Schutzsteuergeräte 15/17/19/75), VAG Long Coding und Anpassungen, vollständige MIL/DPF-Spezialtests.
- Realer SFD/SFD2-Token-Generator, VW-Herstellerautorisierung, automatische ECU-Freigabe.
- Vollständige USB-Serial-Anbindung und umfassender BLE-Kompatibilitätstest mit Carista EVO; letzterer ist von der Gerätefirmware abhängig.
- Exakte normierte 0-100-Messung und ein geeichtes Verbrauchsmodell.

## Sicherheit
Die App unterstützt keinen generischen UDS-Write-Editor. Sie versendet keine 0x2E/0x31-Programmierkommandos, keine Schlüsselgeneratoren und keine SFD2-Bypasses. Standard-OBD-Fehlerlöschen ist eine ausdrücklich bestätigte Diagnosefunktion, kann aber Bereitschaftsmonitore zurücksetzen.

## Bauen
```bash
gradle -p sfd2-android :app:assembleDebug
```
JDK 17, Gradle 8.11, Android SDK 35. Die CI erzeugt ein Debug-APK-Artefakt.

## Installation
Paket: `com.dynamicos.sfd2trust`; Ziel Android 13+.
Bei unterschiedlicher Debug-Signatur kann zunächst die bisherige dynamicOS-App gesichert und entfernt werden müssen.

## Status
Android APK via GitHub Actions erstellt; Real-OBD-Kommunikation und BLE-Carista-EVO-Hardware **nicht** auf einem Fahrzeug validiert.

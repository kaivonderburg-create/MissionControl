# dynamicOS Fiesta Lite — Android Headunit

Android-Projekt für den Ford Fiesta Mk7 (2015), dunkelgrau, Kennzeichen W-S 107. Optimiert als schlanke Home-App für XY-AUTO F9211B / Android 8.1 / 1024×600 / 1 GB RAM mit eigener Oberfläche und lokaler Lenkradtasten-Anlernung.

## Stand v0.2.0

- Tastenbelegung per `KeyEvent` in der geöffneten App anlernen und dauerhaft speichern.
- Android-Medienlautstärke und Mediensteuerung über zugeordnete Tasten.
- USB-Host-Geräteliste inklusive Vendor-ID/Product-ID als Basis für die Adapterintegration.
- Android-Home-Launcher (HOME/DEFAULT) mit vier großzügigen Kacheln, deutschsprachigen Menüs und dunklem Fahrzeugbild.
- App-Übersicht, Bluetooth/WLAN/Audio/Anzeige-Systemeinstellungen und Fahrzeug-Menüs.
- Android-Auto-Startbereich mit Weiterleitung an einen **bereits installierten** kompatiblen Receiver und Verbindungseinstellungen.
- Kein CAN-Schreibzugriff, keine Steuergeräte-Codierung.

**Wichtige Einschränkungen:** Der Launcher ersetzt den Startbildschirm nur, wenn er unter Android als Standard-HOME-App gewählt wird. Er deinstalliert keine OEM-App oder MCU-Dienste. Der Android-Auto-Receiver/AutoConnect-Quellcode ist **nicht in diesem Projekt enthalten**: Das Menü ist eine Start- und Einstellungsintegration, keine funktionierende drahtlose Android-Auto-Engine. Die App erkennt nur Tasten, die Android bereits als `KeyEvent` an die **geöffnete Activity** weiterreicht. Sie ist noch kein systemweiter Key-Mapping-Dienst. Das proprietäre Raise-CAN-Protokoll ist nicht implementiert. USB-Geräteauflistung ist **kein CAN-Datenempfang**. Fahrzeugfunktionen (Türstatus, Klimaanzeige, Uhrzeit, Fahrzeugkonfiguration) sind bis zur Protokollidentifikation **nicht verfügbar**. Das Projekt ist ein entwicklungsfähiger Stand und keine als fertig getestete Fahrzeuglösung.

Nach Installation HOME drücken und bei der Launcher-Auswahl **dynamicOS Fiesta** als Standard auswählen. Für die Rückkehr zu einem anderen Launcher: Einstellungen → Apps → Standard-Apps → Start-App ändern; Menübenennung variiert nach Headunit-Firmware. Das Gerät nicht durch Entfernen des OEM-Launchers oder MCU-Komponenten modifizieren.

## Build über GitHub Actions

1. Projektdateien in ein GitHub-Repository hochladen.
2. Repository → Actions → **Build dynamicOS Fiesta APK** → **Run workflow**.
3. Nach erfolgreichem Lauf das Artifact `dynamicOS-Fiesta-debug-apk` herunterladen; darin liegt `app-debug.apk`.

Lokal mit Android SDK 35 und JDK 17: `gradle :app:assembleDebug`.

## Für den nächsten Integrationsschritt

Benötigt werden Fotos von CAN-Decoder und Beschriftung, VID/PID aus der App, Android-Version der Headunit sowie ein USB-Trace oder die dokumentierte Ausgabe des Decoders bei Drücken von Lauter/Leiser/Telefon. Danach kann ein passender Parser und ein Hintergrunddienst implementiert werden.

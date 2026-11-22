# dynamicOS/ Ford Automotive — aktueller Entwicklungsstand
Datum: 2026-11-22

## Projekt
Ford Fiesta Mk7 2015, QS9008 Android-Headunit mit 1 GB RAM, Raise-CAN-Decoder.
Einzige eigenständige APK mit nativem Ford-Automotive-Launcher und integriertem Android-Auto-Empfänger. Keine separate Overlay-APK.

## Stand der geprüften Quelle
- Branch-Basis: dynamicOS-fiesta-unified
- Letzter zuvor bekannter erfolgreicher Build: GitHub Actions 45068181229, Commit c4c03b6c162fcd29bd737a2a8e421d7ee2c53ef0 (nicht dieser Feature-Branch).
- Quelle wird via tools/merge_fiesta_autoconnect.py und tools/fiesta/LauncherActivity.java zum gemeinsamen Android-Projekt zusammengeführt.
- Zusätzlicher Sicherungsbranch: backup/fiesta-unified-2026-11-22.

## Heute im Feature-Branch geändert
- Kompakte Android-Auto-Medienkachel zeigt tatsächlichen Projektions-Verbindungsstatus und Medienmetadaten.
- Albumcover nutzt bestehenden 256px-Cache beziehungsweise lokale Medienmetadaten.
- Wiedergabe, Zurück, Weiter sind bei laufender Projektion direkt aus der Kachel bedienbar.
- Der vorhandene separate Android-Auto-Videodecoder wird nicht verdoppelt.
- Medienlistener reagiert nun zusätzlich auf verbunden/getrennt.

## Noch NICHT erledigt / unbedingt verifizieren
- Wirklich bewegte Android-Auto-Miniatur im Dashboard. Aktuell ist das eine kompakte Medien-/Statuskarte, keine echte Videoprojektion. Für ein Livebild muss der vorhandene native Videorenderer sicher in ein geteiltes Layout eingebettet und Touchkoordinaten angepasst werden.
- Aktuellen Feature-Branch kompilieren und APK-Artefakt erzeugen. Der vorherige erfolgreiche Build belegt nur den Vorgänger.
- Android Auto Touch-Events auf dem QS9008 tatsächlich testen; insbesondere keine flächendeckenden transparenten Eingabeschichten.
- Laufzeit-/RAM-/Framerate-Messung auf 1-GB-Gerät und Test von Bluetooth, WLAN, Audio, Navigation, Standby und Lenkradtasten.
- Weiteres SYNC-orientiertes UI-Polishing anhand der letzten Nutzer-Screenshots.

## Arbeitsregeln
Keine Behauptung einer fertigen APK ohne Build und Gerätetest. Sicherungsbranch nicht überschreiben. Projektname exakt dynamicOS/ Ford Automotive.

## Abschlussversuch / Build-Auslösung
- Ein isolierter GitHub-Actions-Workflow ist auf dem Standardbranch `master` angelegt: `.github/workflows/ford-automotive-feature-verification.yml`, Commit `de40fca484d528ea062f46accb02756eade86ebf`.
- Dieser Workflow checkt ausschließlich `feature/fiesta-media-polish-20261122` aus, lädt den gepinnten Receiver, führt beide Codebasen zusammen, kompiliert `assembleGithubDebug`, prüft die APK-Metadaten und archiviert die APK bei Erfolg.
- Es liegt noch kein über die verfügbaren Schnittstellen bestätigtes Ergebnis dieses neuen Workflow-Laufs vor; keine Aussage über APK-Fertigstellung oder Hardwaretests.
- GitHub Actions: https://github.com/kaivonderburg-create/MissionControl/actions/workflows/ford-automotive-feature-verification.yml

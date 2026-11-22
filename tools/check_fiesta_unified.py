#!/usr/bin/env python3
"""Fail CI if unified SYNC build drifts from its real source / wiring."""
from pathlib import Path
import re

root = Path("receiver/app/src/main")
launcher = root / "java/de/vd3/fiesta/LauncherActivity.java"
manifest = root / "AndroidManifest.xml"
projection = root / "java/com/andrerinas/openheadunit/aap/AapProjectionActivity.kt"
service = root / "java/de/vd3/fiesta/MediaAccessService.java"
hardware = root / "java/de/vd3/fiesta/MediaHardwareActivity.java"
assert all(p.is_file() for p in (launcher, manifest, projection, service, hardware)), "Missing merged files"
ui = launcher.read_text()
xml = manifest.read_text()
aa = projection.read_text()
hw = hardware.read_text()
assert '{"Start","Audio","Telefon","Navigation","System"}' in ui, "Main tabs out of sync"
assert 'else if(page==1)music();' in ui and 'else if(page==3)navigation();' in ui, "Audio/navigation routes incorrect"
assert 'else if(page==4)system();' in ui, "System tab opens wrong view"
assert 'else vehicle();' in ui, "Vehicle advanced diagnostics not reachable"
assert 'getActiveSessions(' in ui and 'getTransportControls()' in ui, "Media session API missing"
assert 'MediaAccessService' in xml and 'BIND_NOTIFICATION_LISTENER_SERVICE' in xml, "Media permission component missing"
assert 'LauncherActivity' in xml, "Unified HOME target missing"
assert 'installFiestaDock()' in aa, "Native AA interface not wired"
assert 'installFiestaDock()' in aa and 'Zur Ford-Automotive-Startseite' in aa, "Compact SYNC home affordance missing"
assert 'applyFiestaSplitLayout()' not in aa, "Legacy AA split overlay must not intercept touch"
assert 'showFiestaMedia()' not in aa, "Legacy modal media dialog still installed"
assert 'private View row(String title, String description, Runnable action)' in ui, "Descriptive two-line menu cells missing"
assert 'ScrollView scroll=new ScrollView(this)' in ui, "Settings overflow on low-resolution display"
assert 'private View transportButton(int key, boolean playing)' in ui, "Native media transport controls missing"
assert 'if(FiestaMediaBridge.sendMediaKey(this,key))draw();' in ui, "Playback controls unexpectedly switch SYNC pages"
assert 'AlertDialog' not in ui, "Blocking dialogs reintroduced into SYNC launcher"
assert 'MediaAccessService.java' not in xml, "Manifest service name malformed"
assert 'dynamicos_aa_bridge' in ui and 'mediaTitle(' in ui, "Android Auto metadata not surfaced in SYNC"
assert (root/"java/de/vd3/fiesta/FiestaMediaBridge.kt").is_file(), "AA media transport bridge missing"
assert 'FiestaMediaBridge.sendMediaKey' in ui, "AA media commands are not routed to phone"
assert 'this@AapProjectionActivity' in aa, "Incorrect Kotlin context for native return action"

aap_service = (root / "java/com/andrerinas/openheadunit/aap/AapService.kt").read_text()
assert 'dynamicos_aa_bridge' in aap_service, "Projection media bridge not wired"
assert 'putString("title"' in aap_service and 'putString("artist"' in aap_service, "AA title/artist bridge missing"
assert 'showFiestaMedia()' not in aa, "Obsolete modal media controls reintroduced"
assert 'Gravity.RIGHT or Gravity.TOP' in aa, "AA return key obscures bottom media controls"
assert '(resources.displayMetrics.density*48).toInt()' in aa, "AA return key too large"
assert 'mediaToggleView' in ui and 'refreshMediaLabels()' in ui, "Reactive media UI missing"
assert 'registerOnSharedPreferenceChangeListener(mediaListener)' in ui, "Bridge listener missing"
assert 'installFiestaConnectionShell()' not in (root / "java/com/andrerinas/openheadunit/main/MainActivity.kt").read_text(), "Obsolete receiver overlay reintroduced"

assert 'dynamicOS' in ui and 'Ford Automotive' in ui, "New Ford Automotive branding missing"
assert 'home_auto_compact' in ui and 'setHomeMode(boolean compact)' in ui, "Switchable home tiles missing"
assert 'refreshCover(session)' in ui and 'dynamicos_aa_cover.jpg' in ui, "Album artwork not wired"
assert 'art_revision' in aap_service, "Cover cache revision missing from receiver"
assert 'createScaledBitmap' in aap_service, "Cover decode not scaled for limited memory"
assert 'syncHomeButton' in aa, "AA navigation back to Ford Automotive missing"
print("Unified Ford Automotive design and integration checks passed")

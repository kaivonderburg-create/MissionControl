#!/usr/bin/env bash
set -euo pipefail
: "${IOS_CERTIFICATE_P12_BASE64:?Set GitHub Actions secret IOS_CERTIFICATE_P12_BASE64}"
: "${IOS_CERTIFICATE_PASSWORD:?Set GitHub Actions secret IOS_CERTIFICATE_PASSWORD}"
: "${IOS_PROVISION_PROFILE_BASE64:?Set GitHub Actions secret IOS_PROVISION_PROFILE_BASE64}"
: "${IOS_TEAM_ID:?Set GitHub Actions secret IOS_TEAM_ID}"
: "${IOS_KEYCHAIN_PASSWORD:?Set GitHub Actions secret IOS_KEYCHAIN_PASSWORD}"
cd "$(dirname "$0")/.."
umask 077
mkdir -p .signing export
trap 'security delete-keychain "$RUNNER_TEMP/sentinel-signing.keychain-db" >/dev/null 2>&1 || true; rm -rf .signing' EXIT
printf '%s' "$IOS_CERTIFICATE_P12_BASE64" | base64 --decode > .signing/cert.p12
printf '%s' "$IOS_PROVISION_PROFILE_BASE64" | base64 --decode > .signing/profile.mobileprovision
security create-keychain -p "$IOS_KEYCHAIN_PASSWORD" "$RUNNER_TEMP/sentinel-signing.keychain-db"
security set-keychain-settings -lut 21600 "$RUNNER_TEMP/sentinel-signing.keychain-db"
security unlock-keychain -p "$IOS_KEYCHAIN_PASSWORD" "$RUNNER_TEMP/sentinel-signing.keychain-db"
security import .signing/cert.p12 -k "$RUNNER_TEMP/sentinel-signing.keychain-db" -P "$IOS_CERTIFICATE_PASSWORD" -T /usr/bin/codesign
security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$IOS_KEYCHAIN_PASSWORD" "$RUNNER_TEMP/sentinel-signing.keychain-db"
security list-keychains -d user -s "$RUNNER_TEMP/sentinel-signing.keychain-db" "${HOME}/Library/Keychains/login.keychain-db"
security cms -D -i .signing/profile.mobileprovision > .signing/profile.plist
PROFILE_UUID=$(/usr/libexec/PlistBuddy -c 'Print UUID' .signing/profile.plist)
PROFILE_NAME=$(/usr/libexec/PlistBuddy -c 'Print Name' .signing/profile.plist)
PROFILE_TEAM=$(/usr/libexec/PlistBuddy -c 'Print TeamIdentifier:0' .signing/profile.plist)
if [[ "$PROFILE_TEAM" != "$IOS_TEAM_ID" ]]; then echo "Provisioning profile team mismatch"; exit 2; fi
mkdir -p "${HOME}/Library/MobileDevice/Provisioning Profiles"
cp .signing/profile.mobileprovision "${HOME}/Library/MobileDevice/Provisioning Profiles/${PROFILE_UUID}.mobileprovision"
# A valid Apple Distribution identity and matching ad-hoc provisioning profile are required.
xcodebuild -workspace ios/App/App.xcworkspace -scheme App -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' -archivePath "$PWD/ios-build/Sentinel.xcarchive" DEVELOPMENT_TEAM="$IOS_TEAM_ID" CODE_SIGN_STYLE=Manual PROVISIONING_PROFILE_SPECIFIER="$PROFILE_NAME" archive
cat > .signing/ExportOptions.plist <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>method</key><string>ad-hoc</string>
<key>signingStyle</key><string>manual</string>
<key>teamID</key><string>${IOS_TEAM_ID}</string>
<key>provisioningProfiles</key><dict><key>de.vd3dynamic.sentinel</key><string>${PROFILE_NAME}</string></dict>
</dict></plist>
EOF
xcodebuild -exportArchive -archivePath "$PWD/ios-build/Sentinel.xcarchive" -exportPath "$PWD/export/signed" -exportOptionsPlist .signing/ExportOptions.plist
test -s export/signed/App.ipa
mv export/signed/App.ipa export/dynamicOS-Sentinel-signed.ipa

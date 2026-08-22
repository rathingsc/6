#!/usr/bin/env python3
"""v3.1.3 permanent Android signing/update-chain configuration gate."""
from pathlib import Path
import re, sys

ROOT=Path(__file__).resolve().parents[1]
errors=[]

def err(msg): errors.append(msg)
def text(rel):
    p=ROOT/rel
    if not p.exists():
        err(f'missing {rel}')
        return ''
    return p.read_text(encoding='utf-8', errors='replace')

build=text('app/build.gradle')
cm=text('codemagic.yaml')
gitignore=text('.gitignore')
fingerprint=text('signing-certificate-sha256.txt').strip()

# Public certificate fingerprint is safe to commit and prevents accidentally
# switching to a different key under the same Codemagic reference name.
if not re.fullmatch(r'(?:[0-9A-F]{2}:){31}[0-9A-F]{2}', fingerprint): err('invalid/missing signing-certificate-sha256.txt')
if fingerprint and fingerprint not in cm and 'signing-certificate-sha256.txt' not in cm: err('Codemagic does not verify expected signing certificate fingerprint')

# Identity and version chain.
for needle,msg in [
    ("applicationId 'com.italiano2774.nativeapp'", 'applicationId must remain com.italiano2774.nativeapp'),
    ('def defaultVersionCode = 44', 'v3.1.3 local fallback versionCode must be 44'),
    ("versionName '3.1.3-native'", 'versionName must be 3.1.3-native'),
    ('versionCode resolvedVersionCode', 'Gradle versionCode override support missing'),
]:
    if needle not in build: err(msg)

# Release must use the permanent key provided by Codemagic/local private props.
for needle,msg in [
    ('signingConfigs', 'Gradle signingConfigs missing'),
    ('CM_KEYSTORE_PATH', 'CM_KEYSTORE_PATH signing variable missing'),
    ('CM_KEYSTORE_PASSWORD', 'CM_KEYSTORE_PASSWORD signing variable missing'),
    ('CM_KEY_ALIAS', 'CM_KEY_ALIAS signing variable missing'),
    ('CM_KEY_PASSWORD', 'CM_KEY_PASSWORD signing variable missing'),
    ('signingConfig signingConfigs.release', 'release build does not use signingConfigs.release'),
    ("rootProject.file('keystore.properties')", 'optional local private signing file support missing'),
]:
    if needle not in build: err(msg)

for needle,msg in [
    ('android_signing:', 'Codemagic android_signing block missing'),
    ('- zhongxue_release', 'Codemagic must reference permanent keystore name zhongxue_release'),
    (':app:assembleRelease', 'Codemagic must build signed release APK'),
    ('-PversionCode="$UPDATE_VERSION_CODE"', 'Codemagic automatic versionCode injection missing'),
    ('app/build/outputs/apk/release/app-release.apk', 'signed release APK artifact missing'),
    ('signing-fingerprint.txt', 'signing fingerprint artifact/check missing'),
    ('update-version-code.txt', 'versionCode trace artifact missing'),
]:
    if needle not in cm: err(msg)

if ':app:assembleDebug' in cm or 'app-debug.apk' in cm:
    err('official Codemagic workflow still builds/exports debug APK; v3.1.3 must use signed release APK')

# Repository hygiene: private keys must never be inside the GitHub project.
for needle in ['*.jks','*.keystore','keystore.properties']:
    if needle not in gitignore: err(f'.gitignore missing private signing rule: {needle}')

secret_files=[]
for pattern in ('*.jks','*.keystore','keystore.properties'):
    for p in ROOT.rglob(pattern):
        # example file is intentionally harmless and does not match exact keystore.properties.
        if p.name == 'keystore.properties.example':
            continue
        secret_files.append(str(p.relative_to(ROOT)))
if secret_files:
    err('private signing material must not be committed in project: '+', '.join(sorted(secret_files)))

# Catch accidental literal credential injection into tracked text.
credential_re=re.compile(r'(?i)(storePassword|keyPassword)\s*[=:]\s*([A-Za-z0-9]{12,})')
for rel in ['app/build.gradle','codemagic.yaml','GITHUB_上传说明.txt','覆盖升级与签名说明.txt']:
    s=text(rel)
    if credential_re.search(s): err(f'possible literal signing password committed in {rel}')

if errors:
    for e in errors: print('ERROR:', e)
    sys.exit(1)
print('Signing config OK: fixed package, permanent Codemagic keystore reference, signed release APK, monotonic versionCode and secret-file guards')

#!/usr/bin/env python3
"""v3.1.5 guard against bare-person mistranslations for non-pronoun course words."""
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
words=json.load(open(ASSETS/'words.json',encoding='utf-8'))
ledger=json.load(open(ASSETS/'course_translation_quality_v315.json',encoding='utf-8'))
errors=[]
allowed={'io','tu','lui','lei','noi','voi','mi','ti','ci','vi','lo','la','li','le','gli','si','sé','ne','loro'}
pron=r'(?:我|你|您|他|她|它|我们|你们|您们|他们|她们|它们)'
pat=re.compile(r'^'+pron+r'(?:[；;/、]'+pron+r')*$')
for w in words:
    zh=(w.get('chinese') or '').strip()
    if pat.fullmatch(zh) and (w.get('word') or '').lower() not in allowed:
        errors.append(f"{w.get('id')} {w.get('word')}: non-pronoun has bare-person Chinese meaning {zh!r}")
by={int(w['id']):w for w in words}
m=by.get(887,{})
for k,v in {
    'word':'marco','chinese':'我标记；我做记号','lemma':'marcare',
    'formInfo':'marcare · 直陈式现在时 · io 第一人称单数',
    'partOfSpeech':'verb','ipa':'/ˈmarko/'
}.items():
    if m.get(k)!=v: errors.append(f'marco regression: {k} expected {v!r}, got {m.get(k)!r}')
if ledger.get('version')!='3.1.5' or ledger.get('scannedWords')!=2774:
    errors.append('v3.1.5 person-semantics ledger missing/incomplete')
if ledger.get('remainingFalsePronounOnlyMappings'):
    errors.append('v3.1.5 scan ledger still contains false pronoun-only mappings')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Person-semantics check OK: 2774 words scanned; no non-pronoun entry collapses to a bare Chinese person pronoun; marco fixed')

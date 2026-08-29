#!/usr/bin/env python3
"""v3.2.2 learner-facing word example quality gate.

The vocabulary has 2774 rows, but only a subset intentionally has an example pair.
This check audits/locks all existing pairs and blocks recurrence of the templated errors
seen on real devices (e.g. "Vorrei controllare minuto prima di confermare.").
"""
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'app/src/main/assets'
errors=[]
def err(x): errors.append(x)
def load(name):
    try: return json.load(open(AS/name,encoding='utf-8'))
    except Exception as e: err(f'cannot load {name}: {e}'); return None

words=load('words.json') or []
ledger=load('word_example_quality_v322.json') or {}
if len(words)!=2774: err(f'expected 2774 words, found {len(words)}')
if ledger.get('version')!='3.2.2': err('word example ledger version must be 3.2.2')
if ledger.get('allWordRowsScanned')!=2774: err('ledger must record 2774/2774 row scan')
by={int(w.get('id',0)):w for w in words if isinstance(w,dict)}

pairs=[w for w in words if str(w.get('example','')).strip()]
missing=[w for w in words if not str(w.get('example','')).strip()]
# v3.2.2 locks the original audited set but later versions may safely add more examples.
if len(pairs)<ledger.get('existingExamplePairsAudited',742): err(f'example pair count regressed: {len(pairs)}')
if len(missing)>ledger.get('wordsWithoutExample',2032): err(f'missing-example count regressed: {len(missing)}')
if len(pairs)<742: err(f'expected at least 742 audited example pairs, found {len(pairs)}')
if len(missing)>2032: err(f'expected no more than 2032 rows without examples, found {len(missing)}')

# Every example must have a Chinese pair, and no row may have an orphan Chinese example.
for w in words:
    ex=str(w.get('example','')).strip(); zh=str(w.get('exampleZh','')).strip()
    if bool(ex)!=bool(zh): err(f'{w.get("id")} {w.get("word")}: orphan example/exampleZh')
    if ex and re.search(r'[\u4e00-\u9fff]',ex): err(f'{w.get("id")} {w.get("word")}: Chinese leaked into Italian example')
    if zh and not re.search(r'[\u4e00-\u9fff]',zh): err(f'{w.get("id")} {w.get("word")}: Chinese example has no Chinese characters')

cor=ledger.get('exampleCorrections') or []
if ledger.get('exampleCorrectionCount')!=len(cor): err('exampleCorrectionCount mismatch')
if len(cor)!=84: err(f'expected 84 v3.2.2 example corrections, found {len(cor)}')
seen=set()
for c in cor:
    wid=int(c.get('id',0)); w=by.get(wid)
    if wid in seen: err(f'duplicate correction id {wid}')
    seen.add(wid)
    if not w: err(f'missing correction id {wid}'); continue
    if w.get('word')!=c.get('word'): err(f'{wid}: surface drift')
    if w.get('example')!=c.get('newExample'): err(f'{wid} {w.get("word")}: Italian example drift')
    if w.get('exampleZh')!=c.get('newExampleZh'): err(f'{wid} {w.get("word")}: Chinese example drift')

mcor=ledger.get('meaningCorrections') or []
if ledger.get('meaningCorrectionCount')!=len(mcor): err('meaningCorrectionCount mismatch')
for c in mcor:
    wid=int(c.get('id',0)); w=by.get(wid)
    if not w or w.get('chinese')!=c.get('newChinese'): err(f'{wid}: meaning correction drift')

# Broken generator/template text must never return.
banned_it=[
    r'^Vorrei controllare\s+\S+(?:\s+\S+)?\s+prima di confermare\.$',
    r'Vorrei provare la maglione', r'Vorrei provare i mutande',
]
banned_zh=['确认前我想先核对一下','这件衣物或配饰','这个健康相关问题','常温矿泉水','案件编号']
for w in pairs:
    ex=str(w.get('example','')); zh=str(w.get('exampleZh',''))
    for pat in banned_it:
        if re.search(pat,ex): err(f'{w.get("id")} {w.get("word")}: banned templated Italian example: {ex}')
    for token in banned_zh:
        if token in zh: err(f'{w.get("id")} {w.get("word")}: banned stale Chinese example: {zh}')

# Real-device regression and other high-risk corrections.
critical={
    1738:('minuto','Aspetta un minuto, per favore.','请等一分钟。'),
    948:('sono le','Sono le tre.','现在三点。'),
    522:('rossi','I pomodori sono rossi.','西红柿是红色的。'),
    1448:('li','Li ho già inviati.','我已经把它们发出去了。'),
    155:('maglione','Vorrei provare questo maglione.','我想试穿这件毛衣。'),
    1053:('mutande','Cerco delle mutande di cotone.','我在找棉质内裤。'),
    2035:('originale',"Mi serve l'originale per questa pratica.",'办理这个手续我需要原件。'),
    2208:('pratica','Può scrivermi il numero della pratica?','您能把手续编号写给我吗？'),
    90:("un'",'Vorrei un’acqua naturale.','我想要一瓶无气水。'),
    603:('acqua','Vorrei una bottiglia d’acqua naturale.','我想要一瓶无气水。'),
}
for wid,(surface,ex,zh) in critical.items():
    w=by.get(wid,{})
    if w.get('word')!=surface: err(f'critical id {wid} expected {surface}')
    if w.get('example')!=ex or w.get('exampleZh')!=zh: err(f'critical example drift for {wid} {surface}')

if errors:
    for e in errors[:200]: print('ERROR:',e)
    print('WORD EXAMPLE QUALITY CHECK FAILED:',len(errors),'error(s)')
    sys.exit(1)
print(f'Word example quality OK: 2774 rows scanned; {len(pairs)} current example pairs; {len(missing)} without examples; {len(cor)} v3.2.2 corrected pairs locked')

#!/usr/bin/env python3
"""v3.1.6 phrase/core-sentence bilingual semantic consistency gate."""
from pathlib import Path
import json, re, sys

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
errors=[]; warnings=[]

def err(s): errors.append(s)
def load(name):
    p=ASSETS/name
    if not p.exists():
        err(f'missing asset: {name}')
        return []
    try: return json.loads(p.read_text(encoding='utf-8'))
    except Exception as e:
        err(f'invalid JSON {name}: {e}')
        return []

phrases=load('frequent_phrases.json')
core=load('core_sentences.json')
ledger=load('phrase_semantic_quality_v316.json')

if len(phrases)!=430: err(f'frequent_phrases.json expected 430 rows, got {len(phrases)}')
if len(core)!=430: err(f'core_sentences.json expected 430 rows, got {len(core)}')
if isinstance(ledger,dict):
    if ledger.get('version')!='3.1.6' or ledger.get('scannedUniquePairs')!=430 or ledger.get('mirroredCoreSentences')!=430:
        err('v3.1.6 phrase semantic ledger incomplete/stale')
    if int(ledger.get('fixCount',0))<9: err('v3.1.6 phrase semantic ledger expected at least 9 locked fixes')
else:
    err('v3.1.6 phrase semantic ledger malformed')

# IDs must be unique and the learning phrase/core-sentence mirror must stay synchronized.
for label,data in [('frequent phrases',phrases),('core sentences',core)]:
    ids=[str(x.get('id','')) for x in data]
    if len(ids)!=len(set(ids)): err(f'{label}: duplicate IDs found')
    for i,x in enumerate(data,1):
        if not str(x.get('it','')).strip() or not str(x.get('zh','')).strip():
            err(f'{label}: row {i} has blank Italian/Chinese')

if len(phrases)==len(core):
    for i,(a,b) in enumerate(zip(phrases,core),1):
        for k in ('id','it','zh','note'):
            if str(a.get(k,''))!=str(b.get(k,'')):
                err(f'phrase/core mirror mismatch row {i} id={a.get("id")}: field {k}')

by_id={str(x.get('id')):x for x in phrases}
locked={
    'p0007':('Sono allergico alla frutta a guscio.','我对坚果过敏。'),
    'p0129':('Può scrivermi il numero della pratica?','您能把手续编号写给我吗？'),
    'p0130':('Vorrei chiedere informazioni sulla residenza.','我想咨询居住登记。'),
    'p0140':("A che ora è l'ultima corsa di oggi?",'今天最后一班车是几点？'),
    'p0155':("Posso avere una bottiglia d'acqua naturale?",'可以给我一瓶无气水吗？'),
    'v26_0363':('La linea è disturbata.','电话线路有干扰。'),
    'v26_dlg_0390':('Sono quattro euro e cinquanta.','一共4欧元50分。'),
    'v26_dlg_0394':('Costa due euro e venti.','2欧元20分。'),
    'v26_dlg_0429':('Quando sarebbe disponibile per iniziare?','您什么时候可以开始？'),
}
for pid,(it,zh) in locked.items():
    row=by_id.get(pid)
    if not row: err(f'locked phrase missing: {pid}')
    elif row.get('it')!=it or row.get('zh')!=zh: err(f'v3.1.6 locked semantic correction regressed: {pid}')

# High-confidence semantic guards. These are deliberately conservative to avoid false positives.
for x in phrases:
    pid=str(x.get('id')); it=str(x.get('it','')).lower(); zh=str(x.get('zh',''))
    if 'frutta a guscio' in it and '坚果' not in zh: err(f'{pid}: frutta a guscio must map to 坚果类 meaning')
    if re.search(r'\bnoci\b',it) and '坚果' in zh: err(f'{pid}: noci is specifically 核桃, not the broad 坚果 category')
    if 'arachidi' in it and '花生' not in zh: err(f'{pid}: arachidi must map to 花生')
    if 'acqua naturale' in it and '常温' in zh: err(f'{pid}: acqua naturale means still water, not room-temperature water')
    if 'allergic' in it and '过敏' not in zh: err(f'{pid}: allergico/allergica meaning lost in Chinese')
    if ' domani' in ' '+it and '明天' not in zh: err(f'{pid}: domani present but Chinese lacks 明天')
    if ' ieri' in ' '+it and '昨天' not in zh: err(f'{pid}: ieri present but Chinese lacks 昨天')
    if ' oggi' in ' '+it and '今天' not in zh: err(f'{pid}: oggi present but Chinese lacks 今天')

# Known cross-asset duplicates must not retain the old mismatched wording.
all_json='\n'.join(p.read_text(encoding='utf-8') for p in ASSETS.glob('*.json'))
for stale in [
    '"it": "Sono allergico alle noci.",\n    "zh": "我对坚果过敏。"',
    '"it": "Posso avere dell\\u0027acqua naturale?",\n    "zh": "可以给我一瓶常温无气水吗？"',
    '可以给我一瓶常温无气水吗？',
    '"it": "Qual è l\\u0027ultima corsa di oggi?",\n    "zh": "今天最后一班车是几点？"',
    '"it": "Quando sarebbe disponibile?",\n    "zh": "您什么时候可以开始？"',
]:
    if stale in all_json: err('stale bilingual mismatch still exists in assets: '+stale[:55])

# words.json intentionally keeps the "alle noci" example to teach alle; its Chinese must be precise.
words=load('words.json')
for w in words:
    if w.get('example')=='Sono allergico alle noci.' and w.get('exampleZh')!='我对核桃过敏。':
        err('words.json alle/noci example must translate noci as 核桃')

if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
for w in warnings: print('WARNING:',w)
print(f'Phrase semantic quality OK: {len(phrases)} unique phrase pairs mirrored to {len(core)} core sentences; {len(locked)} v3.1.6 corrections locked')

#!/usr/bin/env python3
"""v3.1.2 guided-course Chinese quality gate.

Locks the course-facing fixes for broken conjugations / future forms and checks
high-risk homographs that previously inherited the wrong automatic morphology.
Stdlib only so Codemagic can run it before Gradle dependency resolution.
"""
from pathlib import Path
import json,re,sys

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
errors=[];warnings=[]
def err(x): errors.append(x)
def warn(x): warnings.append(x)

words=json.load(open(ASSETS/'words.json',encoding='utf-8'))
course=json.load(open(ASSETS/'course_curriculum.json',encoding='utf-8'))
ledger=json.load(open(ASSETS/'course_translation_quality_v312.json',encoding='utf-8'))
by_id={int(w['id']):w for w in words}
course_ids=[int(i) for u in course.get('units',[]) for i in u.get('wordIds',[])]

if ledger.get('version')!='3.1.2': err('course_translation_quality_v312.json version must be 3.1.2')
corrections=ledger.get('corrections') or []
meta=ledger.get('metadataFixes') or []
if ledger.get('correctedCount')!=len(corrections): err('v3.1.2 correctedCount mismatch')
if ledger.get('metadataFixCount')!=len(meta): err('v3.1.2 metadataFixCount mismatch')
if len(corrections)<136: err(f'expected >=136 v3.1.2 course meaning fixes, found {len(corrections)}')
if len(meta)<29: err(f'expected >=29 v3.1.2 morphology/context fixes, found {len(meta)}')
if len(course_ids)!=2774 or len(set(course_ids))!=2774: err('guided course must contain every one of the 2774 word ids exactly once')

for c in corrections:
    wid=int(c.get('id',0)); w=by_id.get(wid); expected=(c.get('newChinese') or '').strip()
    if not w: err(f'correction references missing id {wid}'); continue
    if w.get('word')!=c.get('word'): err(f'correction word drift at id {wid}')
    if (w.get('chinese') or '').strip()!=expected: err(f'{wid} {w.get("word")}: expected {expected!r}, got {w.get("chinese")!r}')

for m in meta:
    wid=int(m.get('id',0));w=by_id.get(wid);expected=m.get('new') or {}
    if not w: err(f'metadata fix references missing id {wid}');continue
    for k,v in expected.items():
        if w.get(k,'')!=v: err(f'{wid} {w.get("word")}: metadata {k} expected {v!r}, got {w.get(k)!r}')

# Exact real-device bug and neighboring high-risk forms.
critical={
  729:('fotografia','照片；摄影'),
  881:('chiama','他/她/您叫；打电话'),
  887:('marco','我标记；我做记号'),
  882:('compra','他/她/您买'),
  958:('conosci','你认识；你了解'),
  963:('conosco','我认识；我了解'),
  1048:('paghi','你付款；你支付'),
  1115:('conosce','他/她/您认识；了解'),
  1226:('dà','他/她/它给；您给'),
  1233:('ceneranno','他们/她们将吃晚饭'),
  1248:('cenerò','我将吃晚饭'),
  1285:('dormiranno','他们/她们将睡觉'),
  1341:('mangiato','吃过；吃了'),
  1426:('capisco','我明白；我理解'),
  1523:('sprecare','浪费'),
  1637:('ceniamo','我们吃晚饭'),
  1846:('dimostrare','证明；展示；表明'),
  2003:('sedere','坐；坐下；就座'),
  2063:('svegliare','叫醒；唤醒'),
  2131:('volo','航班；飞行；我飞'),
  2180:('confondere','混淆；使困惑'),
  2191:('preoccupare','使担心；担忧'),
  2580:('ricercare','研究；寻找；搜寻'),
  2600:('stampa','印刷；报刊/新闻界；打印/印刷品'),
}
for wid,(surface,zh) in critical.items():
    w=by_id.get(wid)
    if not w or w.get('word')!=surface or w.get('chinese')!=zh:
        err(f'critical course translation regression {wid}: expected {surface} -> {zh}')

# v3.1.5 real-device bug: lowercase marco is the io form of marcare here, not the Chinese pronoun '我'.
marco=by_id.get(887,{})
if marco.get('chinese')!='我标记；我做记号': err('marco must keep the full verb meaning 我标记；我做记号')
if marco.get('lemma')!='marcare' or marco.get('partOfSpeech')!='verb': err('marco verb morphology metadata regressed')

# The specific screenshot bug must never regress to pronouns without the verb meaning.
compra=by_id.get(882,{})
if '买' not in (compra.get('chinese') or ''): err('compra must contain 买')
if compra.get('chinese') in {'她；他；你','她;他;你'}: err('compra reverted to the broken pronoun-only translation')

# Blocks obvious half-English machine-translation fragments in learner-facing Chinese.
bad_en=re.compile(r'\b(?:will|gives|dinner|well|enjoy|savour|try|not|believe|they|we|you|he|she|it|would)\b',re.I)
for w in words:
    z=(w.get('chinese') or '').strip()
    if not z: err(f'{w.get("id")} {w.get("word")}: empty Chinese course meaning')
    if bad_en.search(z): err(f'{w.get("id")} {w.get("word")}: machine-fragment Chinese remains: {z!r}')

# Context-disambiguated homographs: these course meanings are nouns/prepositions,
# not the accidental verb analyses produced by the old automatic pass.
context_pos={
  1003:('porta','noun'),1038:('pesca','noun'),1599:('cena','noun'),1647:('pranzo','noun'),
  1716:('dai','other'),1728:('entro','other'),1885:('porto','noun'),2087:('prova','noun'),
  2102:('voto','noun'),2124:('visita','noun'),2131:('volo','noun'),2196:('sogno','noun'),
  2231:('aspetto','noun'),2531:('ballo','noun'),2571:('ricerca','noun'),2600:('stampa','noun'),
  2629:('fumo','noun'),2684:('incontro','noun'),2688:('costo','noun'),2700:('mente','noun'),
}
for wid,(surface,pos) in context_pos.items():
    w=by_id.get(wid,{})
    if w.get('word')!=surface or w.get('partOfSpeech')!=pos:
        err(f'context disambiguation regression {wid}: expected {surface} as {pos}')
    if pos=='noun' and (w.get('formInfo') or '').strip(): err(f'{wid} {surface}: noun should not keep verb formInfo')

# word_families must not show stale Chinese for the same stable id/surface.
fam=json.load(open(ASSETS/'word_families.json',encoding='utf-8'))
def walk(x):
    if isinstance(x,dict):
        if isinstance(x.get('id'),int) and x['id'] in by_id and 'word' in x and 'chinese' in x:
            src=by_id[x['id']]
            if x.get('word')==src.get('word') and x.get('chinese')!=src.get('chinese'):
                err(f'word_families stale for {x["id"]} {x.get("word")}: {x.get("chinese")!r} != {src.get("chinese")!r}')
        for v in x.values(): walk(v)
    elif isinstance(x,list):
        for v in x: walk(v)
walk(fam)

if warnings:
    for x in warnings: print('WARNING:',x)
if errors:
    for x in errors[:160]: print('ERROR:',x)
    print('COURSE TRANSLATION QUALITY CHECK FAILED:',len(errors),'error(s)')
    sys.exit(1)
print(f'Course translation quality OK: {len(corrections)} meaning fixes, {len(meta)} context/morphology fixes; compra and machine-fragment regressions blocked')

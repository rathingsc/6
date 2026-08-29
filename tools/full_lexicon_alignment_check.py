#!/usr/bin/env python3
"""v3.2.0 full 2774-row Italian -> Chinese alignment gate.

This gate locks the second, exhaustive manual pass. It is intentionally stricter than the
older semantic checks: every correction and morphology/context fix from the 2774/2774
review is verified before Android compilation.
"""
from pathlib import Path
import json, re, sys

ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'app/src/main/assets'
errors=[]
def err(x): errors.append(x)

def load(name):
    try: return json.load(open(AS/name,encoding='utf-8'))
    except Exception as e: err(f'cannot load {name}: {e}'); return None

words=load('words.json') or []
ledger=load('lexical_alignment_quality_v320.json') or {}
if len(words)!=2774: err(f'expected 2774 words, found {len(words)}')
if ledger.get('version')!='3.2.0': err('lexical alignment ledger version must be 3.2.0')
if ledger.get('scannedWords')!=2774 or ledger.get('reviewCoverage')!='2774/2774':
    err('v3.2.0 ledger must record a complete 2774/2774 review')
cor=ledger.get('corrections') or []
meta=ledger.get('metadataFixes') or []
examples=ledger.get('exampleFixes') or []
if ledger.get('correctionCount')!=len(cor): err('correctionCount mismatch')
if ledger.get('metadataFixCount')!=len(meta): err('metadataFixCount mismatch')
if ledger.get('exampleFixCount')!=len(examples): err('exampleFixCount mismatch')
if len(cor)<184: err(f'expected at least 184 v3.2.0 Chinese alignment fixes, found {len(cor)}')
if len(meta)<61: err(f'expected at least 61 v3.2.0 metadata fixes, found {len(meta)}')

by={int(w.get('id',0)):w for w in words if isinstance(w,dict)}
if sorted(by)!=list(range(1,2775)): err('word ids must remain exactly 1..2774')

seen=set()
for c in cor:
    wid=int(c.get('id',0)); w=by.get(wid)
    if wid in seen: err(f'duplicate correction id {wid}')
    seen.add(wid)
    if not w: err(f'correction id {wid} missing'); continue
    if w.get('word')!=c.get('word'): err(f'id {wid} surface drift: {w.get("word")!r} != {c.get("word")!r}')
    if w.get('chinese')!=c.get('newChinese'): err(f'id {wid} {w.get("word")}: Chinese drift {w.get("chinese")!r} != {c.get("newChinese")!r}')

for m in meta:
    wid=int(m.get('id',0)); w=by.get(wid)
    if not w: err(f'metadata id {wid} missing'); continue
    if w.get('word')!=m.get('word'): err(f'metadata id {wid} surface drift')
    for k,v in (m.get('new') or {}).items():
        if w.get(k,'')!=v: err(f'id {wid} {w.get("word")}: metadata {k} drift {w.get(k)!r} != {v!r}')

for x in examples:
    wid=int(x.get('id',0)); w=by.get(wid)
    if not w: err(f'example id {wid} missing'); continue
    for k,v in (x.get('new') or {}).items():
        if w.get(k,'')!=v: err(f'id {wid} {w.get("word")}: example {k} drift')

# Specific errors found in the exhaustive pass. These checks are deliberately readable so
# future maintainers know why a build is being blocked.
critical={
  421:('molte',('很多','许多'),('非常',)),
  431:('queste',('这些',),('这个',)),
  531:('raso',('剃光','刮净','齐平'),('短的',)),
  712:('gamma',('范围','系列','伽马'),('扫','打扫',)),
  845:('francesi',('法国',),('法语',)),
  951:('scusi',('对不起','劳驾','请问'),('借口',)),
  1452:('cosse',('烹煮','烤了'),('烤过的',)),
  1639:('pranza',('吃午饭',),('午餐',)),
  1709:('sulla',('在……上','关于'),('周围',)),
  2096:('pagella',('成绩单',),('卡',)),
  2168:('fine',('结束','末尾'),()),
  2272:('diritto',('权利','直'),('右边',)),
  2430:('paziente',('病人','患者'),()),
}
for wid,(surface,good,bad) in critical.items():
    w=by.get(wid,{})
    if w.get('word')!=surface: err(f'critical id {wid} expected {surface}')
    zh=str(w.get('chinese',''))
    if not any(x in zh for x in good): err(f'{surface} lacks required aligned Chinese meaning: {zh}')
    if any(x in zh for x in bad): err(f'{surface} still contains known wrong meaning: {zh}')

# Possessive vostro-family must mean plural "your", not singular "your".
for wid in (676,681,688,690,975,976,1658,1660):
    zh=str(by.get(wid,{}).get('chinese',''))
    if '你们' not in zh: err(f'{wid} {by.get(wid,{}).get("word")}: vostro-family must contain 你们: {zh}')

# Person-bearing present forms should retain person information after this precision pass.
person_required={278:'我们',293:'你们',380:'我',412:'我',501:'我们',515:'我们',584:'他们',606:'你们',697:'我们',735:'我们',790:'你们',800:'我们',813:'他们',821:'他们',824:'你们',826:'你们',828:'你们',830:'他们',833:'他们',842:'他们',847:'你们',848:'你们',851:'我们',858:'他们',878:'我',891:'你们',904:'你们',955:'我',964:'你',1076:'我们',1088:'你们',1097:'你们',1103:'我们',1108:'你们',1276:'他们',1582:'你们',1584:'我们',1587:'他们',1589:'他们',1590:'我们',1593:'你们'}
for wid,token in person_required.items():
    zh=str(by.get(wid,{}).get('chinese',''))
    if token not in zh: err(f'{wid} {by.get(wid,{}).get("word")}: person marker {token!r} missing from {zh!r}')

# Course coverage remains one stable row per word.
course=load('course_curriculum.json') or {}
ids=[int(i) for u in course.get('units',[]) for i in u.get('wordIds',[])]
if len(ids)!=2774 or sorted(ids)!=list(range(1,2775)): err('guided course no longer covers each word id exactly once')

# Embedded family copies must match source vocabulary for learner-facing fields.
families=load('word_families.json') or []
family_count=0
def walk(x):
    global family_count
    if isinstance(x,dict):
        try: wid=int(x.get('id',0))
        except: wid=0
        src=by.get(wid)
        if src and x.get('word')==src.get('word'):
            family_count += 1
            for k in ('chinese','lemma','formInfo'):
                if k in x and x.get(k)!=src.get(k): err(f'word_families stale {wid} {x.get("word")}: {k}')
        for v in x.values(): walk(v)
    elif isinstance(x,list):
        for v in x: walk(v)
walk(families)

# No empty learner-facing side and no old punctuation-style placeholder labels.
for w in words:
    if not str(w.get('word','')).strip(): err(f'id {w.get("id")} empty Italian surface')
    if not str(w.get('chinese','')).strip(): err(f'id {w.get("id")} {w.get("word")} empty Chinese meaning')

if errors:
    for e in errors[:180]: print('ERROR:',e)
    print('FULL LEXICON ALIGNMENT CHECK FAILED:',len(errors),'error(s)')
    sys.exit(1)
print(f'Full lexicon alignment OK: 2774/2774 manually reviewed; {len(cor)} Chinese fixes, {len(meta)} metadata fixes, {len(examples)} example fix; {family_count} family members cross-checked')

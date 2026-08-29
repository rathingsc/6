#!/usr/bin/env python3
"""v3.2.1 canonical 2774-word Italian -> Chinese independent retranslation gate."""
from pathlib import Path
import json, sys
ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'app/src/main/assets'
errors=[]
def err(x): errors.append(x)
def load(p):
    try: return json.load(open(p,encoding='utf-8'))
    except Exception as e: err(f'cannot load {p.name}: {e}'); return []

words=load(AS/'words.json')
canon=load(AS/'full_lexicon_retranslation_v321.json')
if len(words)!=2774: err(f'words.json must contain 2774 rows, got {len(words)}')
if len(canon)!=2774: err(f'canonical retranslation must contain 2774 rows, got {len(canon)}')
by={int(w.get('id',0)):w for w in words if isinstance(w,dict)}
cc={int(w.get('id',0)):w for w in canon if isinstance(w,dict)}
if sorted(by)!=list(range(1,2775)): err('words ids must be exactly 1..2774')
if sorted(cc)!=list(range(1,2775)): err('canonical ids must be exactly 1..2774')
for wid in range(1,2775):
    w=by.get(wid,{}) ; c=cc.get(wid,{})
    for k in ('word','chinese','partOfSpeech','lemma','formInfo'):
        if w.get(k,'')!=c.get(k,''): err(f'{wid} {w.get("word")}: canonical {k} drift')
    if not str(w.get('chinese','')).strip(): err(f'{wid} {w.get("word")}: empty Chinese meaning')

# Known real-device/high-risk mistakes must never return.
critical={
  729:('fotografia',('照片','摄影'),('地图',)),
  882:('compra',('买',),('她；他；你',)),
  887:('marco',('标记','记号'),()),
  1423:('castro',('城堡','要塞','设防'),('医生',)),
  1455:('bracciale',('手镯','臂环'),('乐队',)),
  382:('lagna',('抱怨','牢骚'),('牛肉',)),
  2703:('destino',('命运',),('醒来',)),
  2662:('finanziamento',('融资','资金','资助'),('经济的；金融的',)),
  2761:('governo',('政府',),('跑；看；寻找',)),
  2272:('diritto',('权利','法律','直'),('右边',)),
  2430:('paziente',('病人','患者'),()),
}
for wid,(surface,good,bad) in critical.items():
    w=by.get(wid,{}) ; zh=str(w.get('chinese',''))
    if w.get('word')!=surface: err(f'critical id {wid}: expected {surface}')
    if not any(t in zh for t in good): err(f'{surface}: missing expected Chinese concept: {zh}')
    if any(t and t in zh for t in bad): err(f'{surface}: contains known wrong Chinese meaning: {zh}')

# Function words and conjugations should not collapse to an incorrect bare label.
required={
  12:('lo',('定冠词','直接宾语')), 75:('la',('定冠词','直接宾语')), 255:('le',('定冠词','宾语')),
  258:('gli',('定冠词','间接宾语')), 261:('i',('定冠词',)), 231:('viro',('我','转')),
  443:('ha bisogno di',('他/她/您','需要')), 882:('compra',('他/她/您','买')),
  887:('marco',('我','标记')), 829:('studiamo',('我们','学习')), 1636:('pranzano',('他们/她们','吃午饭')),
  1591:('legge',('读','法律')), 2072:('alcuni',('一些','阳性复数')), 2264:('partita',('比赛',)),
}
for wid,(surface,tokens) in required.items():
    w=by.get(wid,{}) ; zh=str(w.get('chinese',''))
    if w.get('word')!=surface: err(f'required id {wid}: expected {surface}')
    for t in tokens:
        if t not in zh: err(f'{wid} {surface}: expected token {t!r} in {zh!r}')

# Embedded word-family copies must agree with the source vocabulary for core teaching fields.
fam=load(AS/'word_families.json')
family_count=0
def walk(x):
    global family_count
    if isinstance(x,dict):
        try: wid=int(x.get('id',0))
        except Exception: wid=0
        src=by.get(wid)
        if src and x.get('word')==src.get('word'):
            family_count += 1
            for k in ('chinese','lemma','formInfo'):
                if k in x and x.get(k)!=src.get(k): err(f'word_families stale {wid} {x.get("word")}: {k}')
        for v in x.values(): walk(v)
    elif isinstance(x,list):
        for v in x: walk(v)
walk(fam)

if errors:
    for e in errors[:200]: print('ERROR:',e)
    print('FULL RETRANSLATION QUALITY CHECK FAILED:',len(errors),'error(s)')
    sys.exit(1)
print(f'Full retranslation quality OK: 2774/2774 canonical Chinese mappings locked; {family_count} family copies cross-checked')

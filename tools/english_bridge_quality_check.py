#!/usr/bin/env python3
"""Validate the curated Italian-English cognate/false-friend teaching layer."""
from pathlib import Path
import json,re,sys,unicodedata
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
RES=ROOT/'app/src/main/res/layout'
errors=[]

def fail(msg): errors.append(msg)
def norm(s):
    s=''.join(c for c in unicodedata.normalize('NFKD',(s or '').lower()) if not unicodedata.combining(c))
    return re.sub(r'[^a-z]','',s)

try: words=json.load(open(ASSETS/'words.json',encoding='utf-8'))
except Exception as e: print('ERROR: words.json:',e);sys.exit(1)
try: doc=json.load(open(ASSETS/'english_bridges.json',encoding='utf-8'))
except Exception as e: print('ERROR: english_bridges.json:',e);sys.exit(1)
entries=doc.get('entries',[])
if doc.get('version')!='3.1.7': fail('english_bridges.json version must be 3.1.7')
if not isinstance(entries,list): fail('entries must be a list');entries=[]
seen=set();surfaces={norm(w.get('word')) for w in words};lemmas={norm(w.get('lemma')) for w in words if w.get('lemma')}
for i,e in enumerate(entries):
    if not isinstance(e,dict): fail(f'entry {i} is not an object');continue
    it=e.get('italian','');en=e.get('english','');kind=e.get('kind','');note=e.get('note','')
    key=norm(it)
    if not key or not norm(en): fail(f'entry {i} missing italian/english')
    if key in seen: fail(f'duplicate Italian bridge key: {it}')
    seen.add(key)
    if key not in surfaces and key not in lemmas: fail(f'bridge headword not found in words/lemmas: {it}')
    if kind not in ('cognate','false_friend'): fail(f'{it}: invalid kind {kind}')
    if len(note.strip())<8: fail(f'{it}: note is too short')

cognates=[e for e in entries if e.get('kind')=='cognate']
false=[e for e in entries if e.get('kind')=='false_friend']
if len(cognates)<190: fail(f'cognate count {len(cognates)} < 190')
if len(false)<15: fail(f'false-friend count {len(false)} < 15')
if doc.get('cognateCount')!=len(cognates): fail('cognateCount ledger mismatch')
if doc.get('falseFriendCount')!=len(false): fail('falseFriendCount ledger mismatch')

pairs={(norm(e.get('italian')),norm(e.get('english')),e.get('kind')) for e in entries}
required=[
 ('importante','important','cognate'),('informazione','information','cognate'),('organizzazione','organization','cognate'),
 ('università','university','cognate'),('fotografia','photography','cognate'),('problema','problem','cognate'),
 ('camera','camera','false_friend'),('libreria','library','false_friend'),('fattoria','factory','false_friend'),
 ('sale','sale','false_friend'),('simpatico','sympathetic','false_friend'),('tempo','tempo','false_friend')]
for it,en,k in required:
    if (norm(it),norm(en),k) not in pairs: fail(f'required bridge missing: {it} / {en} / {k}')

by_word={norm(w.get('word')):w for w in words}
cam=by_word.get('camera')
if not cam or cam.get('chinese')!='房间；卧室': fail('camera Chinese meaning must be 房间；卧室 after false-friend correction')
fam=by_word.get('familiare')
if not fam or not all(k in fam.get('chinese','') for k in ('熟悉','家庭','家属')): fail('familiare Chinese meaning regression')

study=(JAVA/'StudySessionFragment.java').read_text(encoding='utf-8')
adapter=(JAVA/'WordAdapter.java').read_text(encoding='utf-8')
home=(JAVA/'CourseHomeFragment.java').read_text(encoding='utf-8')
repo=(JAVA/'EnglishBridgeRepository.java').read_text(encoding='utf-8') if (JAVA/'EnglishBridgeRepository.java').exists() else ''
layout=(RES/'fragment_study_session.xml').read_text(encoding='utf-8')
item=(RES/'item_word.xml').read_text(encoding='utf-8')
for needle,where in [
 ('EnglishBridgeRepository',study),('text_session_english_bridge',study),('bridge.displayText()',study),
 ('EnglishBridgeRepository',adapter),('text_english_bridge',adapter),('bridge.displayText()',adapter),
 ('english_bridges.json',repo),('英语近似词自动助记',home),('@+id/text_session_english_bridge',layout),('@+id/text_english_bridge',item)]:
    if needle not in where: fail(f'English bridge UI/repository contract missing: {needle}')

if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print(f'English bridge quality OK: {len(cognates)} cognates, {len(false)} false friends, {len(entries)} curated entries')

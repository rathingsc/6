#!/usr/bin/env python3
"""v3.0 guided-course semantic checks.

This is intentionally independent of Android/Gradle. It validates that the beginner path is
actually teachable in short lessons: every one of the 2774 words occurs once in the path,
stages are ordered, units are not too large for the 12-card lesson budget, and learning-node
chunks never silently drop words.
"""
from pathlib import Path
import collections, json, math, sys
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
errors=[]
def err(s): errors.append(s)

try:
    words=json.load(open(ASSETS/'words.json',encoding='utf-8'))
    course=json.load(open(ASSETS/'course_curriculum.json',encoding='utf-8'))
except Exception as e:
    print('ERROR: course assets cannot be parsed:',e);sys.exit(1)
units=course.get('units',[]) if isinstance(course,dict) else []
if len(words)!=2774: err(f'words={len(words)} expected 2774')
if len(units)!=98: err(f'units={len(units)} expected 98')
expected_counts={'A0':8,'A1':24,'A2':30,'B1':36}
counts=collections.Counter(u.get('stage') for u in units)
if dict(counts)!=expected_counts: err(f'stage counts {dict(counts)} != {expected_counts}')
if [u.get('index') for u in units]!=list(range(98)): err('unit indexes are not 0..97 in file order')

all_ids=[];max_unit=(0,'');max_chunk=(0,'')
last_stage=-1;stage_order={'A0':0,'A1':1,'A2':2,'B1':3}
stage_seen=collections.Counter()
for i,u in enumerate(units):
    uid=u.get('id','?');stage=u.get('stage');stage_seen[stage]+=1
    if stage_order.get(stage,-1)<last_stage: err(f'{uid}: stage order moved backwards')
    last_stage=max(last_stage,stage_order.get(stage,-1))
    if u.get('stageUnit')!=stage_seen[stage]: err(f'{uid}: stageUnit is not contiguous')
    if uid!=f"{stage}-U{stage_seen[stage]:02d}": err(f'{uid}: ID does not match stage/unit position')
    ids=u.get('wordIds') or []
    all_ids.extend(ids)
    if not ids: err(f'{uid}: no wordIds')
    if len(ids)>max_unit[0]: max_unit=(len(ids),uid)
    lc=u.get('lessonCount',0)
    if not isinstance(lc,int) or not 5<=lc<=8: err(f'{uid}: lessonCount={lc}, expected 5..8')
    if not u.get('titleZh','').strip(): err(f'{uid}: empty Chinese title')
    if not u.get('sourceSkills'): err(f'{uid}: sourceSkills missing')
    # The last two nodes are review + challenge. Learning chunks must fit all focus words
    # plus at least one teaching/reinforcement card inside CourseLessonEngine's 12-card cap.
    learn=max(1,lc-2);n=len(ids)
    chunks=[]
    covered=[]
    for node in range(learn):
        a=math.floor(node*n/learn);b=math.floor((node+1)*n/learn)
        if b<=a: b=min(n,a+1)
        chunk=ids[a:b];chunks.append(len(chunk));covered.extend(chunk)
    if covered!=ids: err(f'{uid}: learning-node chunking does not cover words exactly in order')
    if chunks and max(chunks)>11: err(f'{uid}: learning chunk {max(chunks)} exceeds 11-word safe budget: {chunks}')
    if chunks and max(chunks)>max_chunk[0]: max_chunk=(max(chunks),uid)

if len(all_ids)!=2774: err(f'course maps {len(all_ids)} word rows, expected 2774')
if len(set(all_ids))!=2774: err(f'course maps only {len(set(all_ids))} unique word IDs')
if sorted(all_ids)!=list(range(1,2775)): err('course must map exact word IDs 1..2774')
word_ids=[int(w.get('id',0)) for w in words]
if word_ids!=list(range(1,2775)): err('words.json IDs are not contiguous 1..2774')

# Verify course order still respects the source course's levelOrder. Units may merge adjacent
# source levels, but they may never reorder them.
word_level={int(w['id']):int(w.get('levelOrder',0)) for w in words}
seq=[]
for u in units:
    levels=[]
    for wid in u.get('wordIds',[]):
        lo=word_level.get(wid,0)
        if not levels or levels[-1]!=lo: levels.append(lo)
    if levels!=sorted(levels): err(f"{u.get('id')}: source level order is not monotonic")
    seq.extend(levels)
compressed=[]
for x in seq:
    if not compressed or compressed[-1]!=x: compressed.append(x)
if compressed!=list(range(1,115)): err('guided units do not preserve source level order 1..114')

# Beginner-facing examples must not expose self-referential or placeholder text.
for w in words:
    ex=(w.get('example') or '').strip();zh=(w.get('exampleZh') or '').strip()
    if ex.startswith('Oggi ripasso la parola «') or ex.startswith("Ripeto l'espressione «"):
        err(f"{w.get('id')} {w.get('word')}: self-referential example remains")
    if w.get('word') and w.get('word').lower() in zh.lower():
        err(f"{w.get('id')} {w.get('word')}: Chinese support leaks the answer")
luna=next((w for w in words if int(w.get('id',0))==52),None)
if not luna or luna.get('example')!='Guardo la luna.' or luna.get('exampleZh')!='我看着月亮。': err('A0 luna example must be the natural beginner sentence Guardo la luna.')
key=next((w for w in words if int(w.get('id',0))==385),None)
if not key or key.get('chinese')!='下一个；下一位' or key.get('example')!='Qual è il prossimo treno?': err('il prossimo real-device regression is not fixed')
sto=next((w for w in words if int(w.get('id',0))==600),None)
if not sto or sto.get('example')!='Sto cercando lavoro.' or sto.get('exampleZh')!='我正在找工作。': err('sto must use a meaningful context sentence')

if errors:
    for e in errors: print('ERROR:',e)
    print(f'COURSE CHECK FAILED: {len(errors)} error(s)')
    sys.exit(1)
print(f'Course check OK: 98 units, 2774 words, stages 8/24/30/36, largest unit {max_unit[0]} words ({max_unit[1]}), largest learning chunk {max_chunk[0]} words ({max_chunk[1]})')

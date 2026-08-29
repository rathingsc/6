#!/usr/bin/env python3
"""v5.0.1 gate: fixed course units must not be polluted by cross-unit due-review targets."""
from pathlib import Path
import json, sys
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
errors=[]
def err(x): errors.append(x)

words_raw=json.load(open(ASSETS/'words.json',encoding='utf-8'))
words=words_raw.get('words',[]) if isinstance(words_raw,dict) else words_raw
course=json.load(open(ASSETS/'course_curriculum.json',encoding='utf-8')).get('units',[])
engine=(JAVA/'CourseLessonEngine.java').read_text(encoding='utf-8')
word_repo=(JAVA/'WordRepository.java').read_text(encoding='utf-8')

if len(course)!=98: err(f'expected 98 fixed course units, got {len(course)}')
if len(words)!=2774: err(f'expected 2774 words, got {len(words)}')

owners={}
assignments=0
for u in course:
    ids=u.get('wordIds',[])
    assignments += len(ids)
    if len(ids)!=len(set(ids)):
        err(f"{u.get('id')}: duplicate word id inside the same unit")
    for wid in ids:
        owners.setdefault(wid,[]).append(u)

if assignments!=2774: err(f'course assignments must total 2774, got {assignments}')
if len(owners)!=2774: err(f'course must assign each word exactly once; unique assigned ids={len(owners)}')
for wid,us in owners.items():
    if len(us)!=1:
        err(f'word id {wid} assigned to {len(us)} units: '+','.join(str(x.get('id')) for x in us))

# The reported regression word should belong only to its actual topic unit.
pre=next((w for w in words if w.get('word')=='prenotazione'),None)
if not pre:
    err('prenotazione missing from shipped lexicon')
else:
    us=owners.get(pre.get('id'),[])
    if len(us)!=1:
        err(f"prenotazione must have exactly one course owner, got {len(us)}")
    elif us[0].get('id')!='A1-U19' or us[0].get('titleZh')!='披萨店':
        err(f"prenotazione unexpectedly owned by {us[0].get('id')} {us[0].get('titleZh')}")

# CourseLessonEngine may reinforce already-seen words from the SAME unit, but must never
# pull global due-review targets into an unrelated fixed unit.
if 'addSeenReinforcement' not in engine:
    err('same-unit reinforcement disappeared from CourseLessonEngine')
if 'reviewDue(' in engine or 'addDueReview' in engine:
    err('CourseLessonEngine still injects global due reviews into fixed units')
if 'global due reviews must stay in the' not in engine.lower():
    err('v5.0.1 course isolation maintenance guard/comment missing')
# Global due review itself must still exist for dedicated review/smart-plan routes.
if 'List<Word> reviewDue' not in word_repo:
    err('global reviewDue route was removed; reviews must be moved out of course, not deleted')

if errors:
    for x in errors: print('ERROR:',x)
    print('COURSE UNIT ISOLATION CHECK FAILED:',len(errors),'error(s)')
    sys.exit(1)
print('Course unit isolation OK: 98 units / 2774 unique assignments; prenotazione only in A1-U19; global due review stays outside fixed lessons.')

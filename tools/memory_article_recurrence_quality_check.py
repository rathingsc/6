#!/usr/bin/env python3
from pathlib import Path
import json,re,sys,collections
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
errors=[]
def err(x): errors.append(x)
try:
    articles=json.load(open(ASSETS/'memory_articles.json',encoding='utf-8'))
    words=json.load(open(ASSETS/'words.json',encoding='utf-8'))
except Exception as e:
    print('ERROR: cannot load article/word data:',e);sys.exit(1)
byid={int(w.get('id',0)):w for w in words}
flat=[]
for a in articles:
    flat.extend(a.get('sections',[]))
placements=collections.Counter(); reinforcement_count=0
for idx,s in enumerate(flat):
    target=list(map(int,s.get('targetWordIds',[])))
    reviews=list(map(int,s.get('reviewWordIds',[])))
    items=s.get('reinforcementItems',[])
    if idx==0:
        if reviews: err(f"{s.get('id')}: first section should not review unseen words")
    else:
        if len(reviews)!=12: err(f"{s.get('id')}: expected 12 reviewWordIds, got {len(reviews)}")
        if len(set(reviews))!=len(reviews): err(f"{s.get('id')}: duplicate reviewWordIds")
        if set(reviews)&set(target): err(f"{s.get('id')}: reviewWordIds overlap new targetWordIds")
        prior_max=idx*40
        if any(wid<1 or wid>prior_max for wid in reviews): err(f"{s.get('id')}: review word not introduced earlier")
        if len(items)<4: err(f"{s.get('id')}: expected at least 4 natural reinforcementItems")
    for wid in reviews: placements[wid]+=1
    for item in items:
        reinforcement_count+=1
        wid=int(item.get('wordId',0));sent=str(item.get('sentence','')).strip();zh=str(item.get('translation','')).strip()
        if wid not in reviews: err(f"{s.get('id')}: reinforcement word {wid} is not in reviewWordIds")
        if not sent or not zh: err(f"{s.get('id')}: empty reinforcement sentence/translation for {wid}")
        word=str(byid.get(wid,{}).get('word','')).strip()
        if word and re.search(r'(?iu)(?<![\wÀ-ÖØ-öø-ÿ])'+re.escape(word)+r'(?![\wÀ-ÖØ-öø-ÿ])',sent) is None:
            err(f"{s.get('id')}: reinforcement sentence does not contain target word {word!r}")
if len(flat)!=50: err(f'expected 50 sections, got {len(flat)}')
# The highest-frequency 200 words must all come back later at least once.
missing_core=[i for i in range(1,201) if placements[i]<1]
if missing_core: err('core 1..200 words never repeated: '+','.join(map(str,missing_core[:30])))
if sum(placements.values())!=588: err(f'expected 588 old-word review placements, got {sum(placements.values())}')
if reinforcement_count<196: err(f'expected at least 196 natural reinforcement micro-contexts, got {reinforcement_count}')
# Source contracts for tracking real article encounters and exposing clear stats.
study=(JAVA/'MemoryArticleStudyFragment.java').read_text(encoding='utf-8')
detail=(JAVA/'MemoryArticleDetailFragment.java').read_text(encoding='utf-8')
listing=(JAVA/'MemoryArticleListFragment.java').read_text(encoding='utf-8')
home=(JAVA/'CourseHomeFragment.java').read_text(encoding='utf-8')
store=(JAVA/'ProgressStore.java').read_text(encoding='utf-8')
repo=(JAVA/'MemoryArticleRepository.java').read_text(encoding='utf-8')
for marker in ['reviewWordIds','reinforcementItems','MemoryArticleReinforcement']:
    if marker not in repo: err('repository recurrence contract missing '+marker)
for marker in ['recordMemoryArticleExposure','memoryArticleExposureCount','memoryArticleEncounteredCount','memoryArticleRecognizedCount','memoryArticleMasteredCount','memoryArticleExposure','migrateMemoryArticleExposureIfNeeded']:
    if marker not in store: err('progress recurrence/backup contract missing '+marker)
for marker in ['旧词复现','fullAudioText','combinedReviewIds','recordMemoryArticleExposure','真正掌握','待巩固']:
    if marker not in study: err('study recurrence contract missing '+marker)
for src,name in [(detail,'detail'),(listing,'list'),(home,'home')]:
    for marker in ['已遇到','已认识','掌握'] if name!='home' else ['已遇到','已认识','真正掌握','螺旋复现']:
        if marker not in src: err(f'{name} progress stats missing {marker}')
if errors:
    print(f'Memory article recurrence quality FAILED: {len(errors)} error(s)')
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Memory article recurrence quality OK: 49 later sections x 12 old words = 588 review placements; first 200 core words all recur; natural reinforcement contexts:',reinforcement_count)

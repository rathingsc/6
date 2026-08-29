#!/usr/bin/env python3
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
errors=[]
def err(x): errors.append(x)
def load(name):
    p=ASSETS/name
    if not p.exists(): err(f'missing asset {name}'); return {}
    try:return json.load(open(p,encoding='utf-8'))
    except Exception as e:err(f'invalid {name}: {e}');return {}
words=load('words.json');morph=load('morphology_hints.json');chunks=load('memory_chunks.json');phrases=load('frequent_phrases.json')
word_keys={str(w.get('word','')).lower() for w in words}
phrase_by_id={str(p.get('id','')):p for p in phrases}
if morph.get('version')!='3.1.8':err('morphology_hints.json version must be 3.1.8')
if chunks.get('version')!='3.1.8':err('memory_chunks.json version must be 3.1.8')
mentries=morph.get('entries',[]);centries=chunks.get('entries',[])
if len(mentries)<140:err(f'morphology hint coverage too small: {len(mentries)}')
if len(centries)<220:err(f'memory chunk coverage too small: {len(centries)}')
seen=set()
for x in mentries:
    key=str(x.get('italian','')).lower()
    if key in seen:err(f'duplicate morphology key {key}')
    seen.add(key)
    if key not in word_keys:err(f'morphology target not in words.json: {key}')
    if not x.get('title') or not x.get('note'):err(f'incomplete morphology hint: {key}')
for critical in ['impossibile','inutile','possibilità','velocemente','prenotazione','decisione']:
    if critical not in seen:err(f'critical morphology hint missing: {critical}')
seen=set();fixed=0
for x in centries:
    key=str(x.get('italian','')).lower();source=str(x.get('sourceId',''))
    if key in seen:err(f'duplicate memory chunk key {key}')
    seen.add(key)
    if key not in word_keys:err(f'memory chunk target not in words.json: {key}')
    if x.get('kind')=='fixed':fixed+=1
    p=phrase_by_id.get(source)
    if not p:err(f'chunk source missing from frequent_phrases.json: {key} -> {source}')
    elif p.get('it')!=x.get('phrase') or p.get('zh')!=x.get('chinese'):err(f'chunk text diverged from validated source: {key}')
if fixed<8:err(f'fixed collocation count too small: {fixed}')
java=(ROOT/'app/src/main/java/com/italiano2774/nativeapp/StudySessionFragment.java').read_text(encoding='utf-8')
engine=(ROOT/'app/src/main/java/com/italiano2774/nativeapp/SmartReviewModeEngine.java').read_text(encoding='utf-8')
for needle,msg,where in [
 ('SmartReviewModeEngine.choose','Chinese-to-Italian/four-mode recall routing missing',java),
 ('repo.isChinesePromptUnique(w)','reverse recall ambiguity guard missing',engine),
 ('recordSmartWordRating(w.id,effective,response,currentMode)','smart-memory rating must be recorded with its exact mode',java),
 ('中 → 意主动回忆','reverse recall learner label missing',engine),
 ('显示意大利语答案','reverse recall reveal action missing',java),
 ('MemoryAidRepository.MorphologyHint','morphology aid not bound in smart memory',java),
 ('MemoryAidRepository.MemoryChunk','chunk aid not bound in smart memory',java),
 ('sessionAudio.setVisibility(View.GONE)','active recall/cloze must hide audio before reveal',java)]:
    if needle not in where:err(msg)
adapter=(ROOT/'app/src/main/java/com/italiano2774/nativeapp/WordAdapter.java').read_text(encoding='utf-8')
for needle in ['text_memory_morphology','text_memory_chunk','memoryAidRepo']:
    if needle not in adapter:err('vocabulary detail memory-aid hook missing: '+needle)
layout=(ROOT/'app/src/main/res/layout/fragment_study_session.xml').read_text(encoding='utf-8')
for needle in ['text_session_answer_word','text_session_morphology','text_session_chunk']:
    if needle not in layout:err('study-session layout missing '+needle)
if errors:
    for e in errors:print('ERROR:',e)
    sys.exit(1)
print(f'Memory aid quality OK: {len(mentries)} morphology hints, {len(centries)} chunks ({fixed} fixed), four-mode active recall wired')

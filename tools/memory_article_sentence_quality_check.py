#!/usr/bin/env python3
"""v3.3.8 gate for aligned sentence study, dual-speed TTS and memorisation progress backup."""
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
LAYOUT=ROOT/'app/src/main/res/layout'
errors=[]
def err(x): errors.append(x)
def text(p):
    if not p.exists(): err(f'MISSING: {p.relative_to(ROOT)}'); return ''
    return p.read_text(encoding='utf-8')
try:
    articles=json.loads((ASSETS/'memory_articles.json').read_text(encoding='utf-8'))
except Exception as e:
    err(f'invalid memory_articles.json: {e}'); articles=[]
sections=0;pairs=0
for a in articles:
    for s in a.get('sections',[]):
        sections+=1;sp=s.get('sentences') or []
        if not (3<=len(sp)<=8): err(f"{s.get('id')}: expected 3..8 aligned sentences, got {len(sp)}")
        pairs+=len(sp)
        it=[];zh=[]
        for i,p in enumerate(sp):
            ii=(p.get('italian') or '').strip();cc=(p.get('chinese') or '').strip()
            if not ii or not cc: err(f"{s.get('id')} sentence {i+1}: empty Italian/Chinese")
            if ii and ii[-1] not in '.!?': err(f"{s.get('id')} sentence {i+1}: Italian punctuation missing")
            if cc and cc[-1] not in '。！？': err(f"{s.get('id')} sentence {i+1}: Chinese punctuation missing")
            it.append(ii);zh.append(cc)
        if ' '.join(it).strip()!=re.sub(r'\s+',' ',(s.get('text') or '').strip()): err(f"{s.get('id')}: Italian sentence pairs do not reconstruct section text")
        if re.sub(r'\s+','', ''.join(zh))!=re.sub(r'\s+','',(s.get('translation') or '')): err(f"{s.get('id')}: Chinese sentence pairs do not reconstruct translation")
if sections!=50: err(f'expected 50 memory sections, got {sections}')
if pairs<250: err(f'expected at least 250 aligned sentence pairs, got {pairs}')
frag=text(JAVA/'MemoryArticleSentenceStudyFragment.java')
for marker in ['第1轮 · 双语理解','第2轮 · 听读','第3轮 · 挖空回忆','第4轮 · 中文反推','audio.speak(current().italian,rate)','0.70f','markMemoryArticleSentenceStudyDone']:
    if marker not in frag: err('sentence study marker missing: '+marker)
repo=text(JAVA/'MemoryArticleRepository.java')
for marker in ['optJSONArray("sentences")','MemoryArticleSentence']:
    if marker not in repo: err('repository sentence marker missing: '+marker)
section=text(JAVA/'MemoryArticleSection.java')
if 'List<MemoryArticleSentence> sentences' not in section: err('MemoryArticleSection sentence list missing')
main=text(JAVA/'MainActivity.java')
if 'openMemoryArticleSentenceStudy' not in main: err('MainActivity sentence navigation missing')
detail=text(JAVA/'MemoryArticleDetailFragment.java')
for marker in ['逐句背文章 · 正常/慢速','openMemoryArticleSentenceStudy','memoryArticleSentenceStudyDone']:
    if marker not in detail: err('article detail sentence mode marker missing: '+marker)
store=text(JAVA/'ProgressStore.java')
for marker in ['memory_article_sentence_done_','memoryArticleSentenceDone','memoryArticleSentenceStudyDone','markMemoryArticleSentenceStudyDone']:
    if marker not in store: err('progress backup sentence marker missing: '+marker)
m=re.search(r'o\.put\("version",(\d+)\)',store)
if not m or int(m.group(1))<23: err('progress backup version must remain >=23 for sentence-study history')
study=text(JAVA/'MemoryArticleStudyFragment.java')
for marker in ['button_memory_audio_normal','button_memory_audio_slow','audio.speak(fullAudioText(),1.0f)','audio.speak(fullAudioText(),0.70f)']:
    if marker not in study: err('full-section dual-speed marker missing: '+marker)
for f in ['fragment_memory_article_sentence_study.xml','fragment_memory_article_study.xml']:
    if not (LAYOUT/f).exists(): err('missing layout '+f)
if errors:
    print('Memory article sentence study FAILED')
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print(f'Memory article sentence study OK: {sections} sections, {pairs} aligned sentence pairs, 4-round memorisation, normal/slow TTS, backup enabled')

#!/usr/bin/env python3
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
RES=ROOT/'app/src/main/res/layout'
errors=[]
def err(x): errors.append(x)
def req(p):
    if not p.exists(): err(f'MISSING: {p.relative_to(ROOT)}')
    return p
p=req(ASSETS/'memory_articles.json')
words_p=req(ASSETS/'words.json')
for f in ['MemoryArticle.java','MemoryArticleSection.java','MemoryArticleReinforcement.java','MemoryArticleRepository.java','MemoryArticleListFragment.java','MemoryArticleDetailFragment.java','MemoryArticleStudyFragment.java']:
    req(JAVA/f)
for f in ['fragment_memory_article_list.xml','fragment_memory_article_detail.xml','fragment_memory_article_study.xml']:
    req(RES/f)
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
try: articles=json.load(open(p,encoding='utf-8'))
except Exception as e: err(f'invalid memory_articles.json: {e}');articles=[]
try: words=json.load(open(words_p,encoding='utf-8'))
except Exception as e: err(f'invalid words.json: {e}');words=[]
word_ids={int(w.get('id',0)) for w in words}
if len(articles)!=10: err(f'expected 10 memory articles, got {len(articles)}')
all_ids=[];section_ids=[];all_section_word_ids=[]
for ai,a in enumerate(articles,1):
    aid=a.get('id','')
    if not re.fullmatch(r'memory_\d{2}',aid): err(f'article {ai} bad id {aid!r}')
    for k in ('title','titleZh','subtitle','emoji'):
        if not str(a.get(k,'')).strip(): err(f'{aid}: missing {k}')
    ids=a.get('targetWordIds',[])
    if len(ids)!=200: err(f'{aid}: targetWordIds={len(ids)}, expected 200')
    if len(set(ids))!=len(ids): err(f'{aid}: duplicate target word IDs')
    if any(int(x) not in word_ids for x in ids): err(f'{aid}: contains nonexistent word IDs')
    sections=a.get('sections',[])
    if len(sections)!=5: err(f'{aid}: sections={len(sections)}, expected 5')
    local=[]
    for si,s in enumerate(sections,1):
        sid=s.get('id','');section_ids.append(sid)
        if sid!=f'{aid}_s{si}': err(f'{aid}: unexpected section id {sid!r} at position {si}')
        for k in ('title','titleZh','text','translation'):
            if not str(s.get(k,'')).strip(): err(f'{sid}: missing {k}')
        sids=s.get('targetWordIds',[])
        if len(sids)!=40: err(f'{sid}: targetWordIds={len(sids)}, expected 40')
        if len(set(sids))!=len(sids): err(f'{sid}: duplicate target word IDs')
        local.extend(sids);all_section_word_ids.extend(sids)
        text=str(s.get('text','')).strip();zh=str(s.get('translation','')).strip()
        # Keep sections short enough to memorise, but substantial enough to be a real mini-passage.
        it_tokens=re.findall(r"[A-Za-zÀ-ÖØ-öø-ÿ']+",text)
        if len(it_tokens)<45: err(f'{sid}: Italian passage too short ({len(it_tokens)} tokens)')
        if len(zh)<35: err(f'{sid}: Chinese translation too short')
        cloze=s.get('clozeWords',[])
        if len(cloze)!=4: err(f'{sid}: clozeWords={len(cloze)}, expected 4')
        low=text.casefold()
        for c in cloze:
            if not str(c).strip(): err(f'{sid}: empty cloze word');continue
            if re.search(r'(?iu)(?<![\wÀ-ÖØ-öø-ÿ])'+re.escape(str(c))+r'(?![\wÀ-ÖØ-öø-ÿ])',text) is None:
                err(f'{sid}: cloze word not present in text: {c}')
    if set(local)!=set(ids) or len(local)!=200: err(f'{aid}: article IDs do not exactly equal its five section ID sets')
    all_ids.extend(ids)
if len(section_ids)!=50 or len(set(section_ids))!=50: err(f'section IDs must be 50 unique, got {len(section_ids)} / {len(set(section_ids))}')
if len(all_ids)!=2000: err(f'global article target total={len(all_ids)}, expected 2000')
if len(set(all_ids))!=2000: err(f'global target IDs not unique: unique={len(set(all_ids))}')
if set(all_ids)!=set(range(1,2001)): err('global target IDs must be exactly 1..2000')
if all_section_word_ids!=all_ids: err('section target sequence must match article target sequence')
# Source/UI contracts: route is reachable, five-step study exists, progress is backed up,
# and article completion never calls a mastery setter directly.
home=(JAVA/'CourseHomeFragment.java').read_text(encoding='utf-8')
main=(JAVA/'MainActivity.java').read_text(encoding='utf-8')
study=(JAVA/'MemoryArticleStudyFragment.java').read_text(encoding='utf-8')
session=(JAVA/'StudySessionFragment.java').read_text(encoding='utf-8')
store=(JAVA/'ProgressStore.java').read_text(encoding='utf-8')
layout=(RES/'fragment_course_home.xml').read_text(encoding='utf-8')
for marker in ['text_memory_article_summary','button_memory_articles','2000词 · 十篇通关']:
    if marker not in layout: err(f'home ten-article entry missing {marker}')
for marker in ['openMemoryArticles','openMemoryArticle','openMemoryArticleStudy','openMemoryArticleReview']:
    if marker not in main: err(f'MainActivity route method missing {marker}')
for marker in ['双语精读','只看意大利语','只听','挖空回忆','中文反推','recordMemoryArticleExposure','旧词复现']:
    if marker not in study: err(f'five-step article study contract missing {marker}')
if 'setMastery' in study or 'recordSmartWordRating' in study: err('reading an article must not directly mark its target words mastered')
for marker in ['newArticleReviewInstance','articleWordIds','十篇通关定向复习','modeName(){return articleReview?"memory_article"']:
    if marker not in session: err(f'targeted article smart-review contract missing {marker}')
for marker in ['memoryArticleDone','memory_article_done_','memoryArticleCompletedTotal','memoryArticleExposure','recordMemoryArticleExposure','"memory_article"']:
    if marker not in store: err(f'backup/progress contract missing {marker}')
if 'renderMemoryArticles' not in home or 'openMemoryArticles' not in home: err('home article progress renderer/entry missing')
if errors:
    print(f'Memory article quality FAILED: {len(errors)} error(s)')
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Memory article quality OK: 10 articles, 50 sections, 2000 unique target words (IDs 1..2000), five-step study + spiral old-word recurrence + targeted smart review + backup progress.')

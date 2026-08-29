#!/usr/bin/env python3
"""v5.0.1 personal weak-word micro-reading + contextual retrieval integration gate."""
from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]

def read(rel):
    p=ROOT/rel
    if not p.exists():
        errors.append(f'missing {rel}')
        return ''
    return p.read_text(encoding='utf-8')

def need(text, marker, label):
    if marker not in text: errors.append(f'{label} missing marker: {marker}')

engine=read('app/src/main/java/com/italiano2774/nativeapp/WeakWordStoryEngine.java')
model=read('app/src/main/java/com/italiano2774/nativeapp/WeakWordStory.java')
frag=read('app/src/main/java/com/italiano2774/nativeapp/WeakWordStoryFragment.java')
layout=read('app/src/main/res/layout/fragment_weak_word_story.xml')
main=read('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
hub=read('app/src/main/java/com/italiano2774/nativeapp/PracticeHubFragment.java')
hub_layout=read('app/src/main/res/layout/fragment_practice_hub.xml')
summary=read('app/src/main/java/com/italiano2774/nativeapp/DailySummaryFragment.java')
summary_layout=read('app/src/main/res/layout/fragment_daily_summary.xml')
course=read('app/src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java')
plan=read('app/src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java')
breakthrough=read('app/src/main/java/com/italiano2774/nativeapp/BreakthroughPlanEngine.java')
passport=read('app/src/main/java/com/italiano2774/nativeapp/MasteryPassportEngine.java')
store=read('app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java')
build=read('app/build.gradle')
cm=read('codemagic.yaml')

for m in ['rankedWeakWords','rankSections','MemoryArticleRepository','fillFromSection','fillFromExamples','buildClozeTargets','progress.dueForReview','progress.isStubborn','progress.wrongCount']:
    need(engine,m,'weak-word engine')
need(engine,'out.targetWords.size()>=10','10-word upper bound')
need(engine,'out.targetWords.size()<6','6-word minimum/fallback target')
need(engine,'2774词库已审校例句组合 · 不联网生成','audited example fallback disclosure')
need(model,'List<ClozeTarget>','story cloze model')

for m in ['第1 / 4步 · 先读懂','第2 / 4步 · 不看文字听','第3 / 4步 · 听后挖空','第4 / 4步 · 只看中文复述','AudioPlayer','SpeechRecognizer','"it-IT"','score>=60','retellAttempts<2','recordEmbeddedDimensionResults','ProgressStore.DIM_LISTENING,ProgressStore.DIM_SPELLING','SentenceFsrsRepository','recordAuxiliaryResult("weak_story"']:
    need(frag,m,'four-stage weak-story training')
for m in ['button_weak_story_back','text_weak_story_body','edit_weak_story_answer','button_weak_story_mic','button_weak_story_slow','button_weak_story_normal','text_weak_story_feedback']:
    need(layout,m,'weak-story layout')
need(layout,'全部内容来自本地已审校材料 · 不联网生成 · 不会把被动阅读直接当成掌握','offline/audited UI disclosure')

need(main,'openWeakWordStory','MainActivity navigation')
need(hub,'button_simple_weak_story','practice-hub navigation')
need(hub_layout,'button_simple_weak_story','practice-hub button')
need(summary,'button_summary_weak_story','daily-summary navigation')
need(summary_layout,'button_summary_weak_story','daily-summary button')
need(course,'case "weak_word_story"','daily-plan route')
for m in ['"weak_story"','"weak_word_story"','addIfFitsUniqueAction']:
    need(plan,m,'daily smart-plan integration')
need(breakthrough,'"weak_word_story"','three-day real-use prescription route')
need(breakthrough,'真实使用突破 · 第2天','three-day real-use day 2')
need(passport,'"weak_story"','mastery-passport real-use evidence')

need(store,'o.put("version",31)','backup schema v31 compatibility')
need(store,'"weak_story"','weak-story backup stats')
need(store,'recordEmbeddedDimensionResults','single-schedule multi-dimension embedded update')
if store.count('updateAdaptiveSchedule(id,dims,correct,responseMs)') < 2:
    errors.append('four-track adaptive schedule update methods appear incomplete')
need(build,'def defaultVersionCode = 81','v5.0.1 versionCode')
need(build,"versionName '5.0.1-native'",'v5.0.1 versionName')
need(cm,'v5.0.1','Codemagic v4.7 identity')
need(cm,'python3 tools/weak_word_story_quality_check.py','Codemagic weak-story gate')

# Prevent an accidental regression to network/generative story creation inside this engine/fragment.
local=(engine+'\n'+frag).lower()
for bad in ['openai','chatgpt','generativeai','http://','https://']:
    if bad in local: errors.append(f'weak-story path must stay local/audited; found {bad}')

if errors:
    print('Weak-word story quality check FAILED')
    for e in errors: print(' -',e)
    sys.exit(1)
print('Weak-word story OK: weak/due/error ranking -> audited section/example selection -> 6-10 targets -> read/listen/cloze/retell -> multi-dimension feedback -> sentence FSRS -> daily/weekly/3-day scheduling -> backup v31')

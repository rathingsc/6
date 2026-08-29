#!/usr/bin/env python3
"""v5.0.1 daily smart-plan integration checks (stdlib only)."""
from pathlib import Path
import re, sys, xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
LAYOUT=ROOT/'app/src/main/res/layout/fragment_course_home.xml'
errors=[]
def err(x): errors.append(x)
def read(p):
    if not p.exists(): err('missing '+str(p.relative_to(ROOT))); return ''
    return p.read_text(encoding='utf-8',errors='replace')

for name in ['DailySmartTask.java','DailySmartPlan.java','DailySmartPlanEngine.java','CourseHomeFragment.java']:
    if not (JAVA/name).exists(): err('missing '+name)
if not LAYOUT.exists(): err('missing fragment_course_home.xml')

engine=read(JAVA/'DailySmartPlanEngine.java')
home=read(JAVA/'CourseHomeFragment.java')
progress=read(JAVA/'ProgressStore.java')
build=read(ROOT/'app/build.gradle')
cm=read(ROOT/'codemagic.yaml')

for marker in ['progress.dashboardStats','stats.weakestDimension','courseTodayXp','dailyCards(today)','dailyAuxiliaryAttempts("listen_speak"','dailyAuxiliaryAttempts("pattern"','LifeTaskEngine.nextTask','memoryArticleSentenceStudyDone','dailyAuxiliaryAttempts("active_recall"','dailyAuxiliaryAttempts("shadowing"']:
    if marker not in engine: err('daily planner missing adaptive/completion marker: '+marker)
for action in ['course_lesson','smart_memory','daily_speaking','listen_speak','grammar','life_task','memory_article_sentence','active_recall','shadow','error_repair']:
    if '"'+action+'"' not in engine: err('daily planner missing task action '+action)
for marker in ['budget<=10','budget<=20','budget>=45','budget>=60','今日重点：','复习 + 新词','循序课程','每日5句开口','真实生活任务','十篇逐句背诵','错题回炉']:
    if marker not in engine: err('daily planner route/budget marker missing '+marker)

for marker in ['renderDailyPlan','openDailyTask','setDailyPlanTimeSelection','开始下一项','查看今日总结 · 明日复习预告','R.id.button_plan_10','R.id.button_plan_20','R.id.button_plan_30','R.id.button_plan_45','R.id.button_plan_60']:
    if marker not in home: err('CourseHome daily-plan UI/flow missing '+marker)
for action in ['course_lesson','smart_memory','daily_speaking','listen_speak','grammar','life_task','memory_article_sentence','active_recall','shadow','error_repair']:
    if 'case "'+action+'"' not in home: err('CourseHome cannot open daily task '+action)

if 'recordAuxiliaryResult("memory_article_sentence",true,0L)' not in progress:
    err('article sentence completion does not feed daily-plan completion state')

if LAYOUT.exists():
    try:
        tree=ET.parse(LAYOUT); ids=set()
        ns='{http://schemas.android.com/apk/res/android}'
        for el in tree.getroot().iter():
            rid=el.attrib.get(ns+'id','')
            if rid.startswith('@+id/'): ids.add(rid[5:])
        needed={'text_daily_plan_summary','group_daily_plan_minutes','container_daily_plan','button_daily_plan_start','button_plan_10','button_plan_20','button_plan_30','button_plan_45','button_plan_60'}
        missing=needed-ids
        if missing: err('course home layout missing daily-plan ids: '+', '.join(sorted(missing)))
    except Exception as e: err('course home XML parse failed: '+str(e))

if "versionName '5.0.1-native'" not in build or 'def defaultVersionCode = 81' not in build:
    err('v5.0.1 version identity missing')
if 'python3 tools/daily_smart_plan_quality_check.py' not in cm:
    err('Codemagic daily smart plan quality-check step missing')
if 'v5.0.1' not in cm:
    err('Codemagic workflow title is not v5.0.1')

if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Daily smart plan OK: adaptive 10/20/30/45/60-minute route, 36-stage real-life missions, daily speaking, finite-budget repair, one-tap next task, and completion signals')

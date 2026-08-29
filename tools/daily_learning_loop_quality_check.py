#!/usr/bin/env python3
"""v5.0.1 daily recovery/close-loop integration checks (stdlib only)."""
from pathlib import Path
import sys, xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
errors=[]
def err(x): errors.append(x)
def read(p):
    if not p.exists(): err('missing '+str(p.relative_to(ROOT))); return ''
    return p.read_text(encoding='utf-8',errors='replace')

engine=read(JAVA/'DailySmartPlanEngine.java')
plan=read(JAVA/'DailySmartPlan.java')
home=read(JAVA/'CourseHomeFragment.java')
summary=read(JAVA/'DailySummaryFragment.java')
layout=ROOT/'app/src/main/res/layout/fragment_daily_summary.xml'
build=read(ROOT/'app/build.gradle')
cm=read(ROOT/'codemagic.yaml')

for marker in ['stats.wrong>=6','stats.stubborn>=3','stats.reviewPressure>=1','pressureProtection','dailyAuxiliaryAttempts("error_repair"','"error_repair"','错题回炉','复习保护模式','学习 + 补漏模式']:
    if marker not in engine: err('daily recovery planner missing marker: '+marker)
for marker in ['wrongWords','stubbornWords','recoveryMode']:
    if marker not in plan: err('DailySmartPlan missing recovery field '+marker)
for marker in ['case "error_repair"','openWrongWordRepair','查看今日总结 · 明日复习预告','openDailySummary','今天已自动优先处理反复出错和容易遗忘的内容。']:
    if marker not in home: err('CourseHome close-loop marker missing '+marker)
for marker in ['dashboardStats','learnedDimensionPct','dueCount(all,today.plusDays(1))','tomorrowAdvice','button_summary_repair','openWrongWordRepair','四维掌握（已学词）','明日建议']:
    if marker not in summary: err('Daily summary close-loop marker missing '+marker)

if layout.exists():
    try:
        tree=ET.parse(layout);ns='{http://schemas.android.com/apk/res/android}';ids=set()
        for el in tree.getroot().iter():
            rid=el.attrib.get(ns+'id','')
            if rid.startswith('@+id/'): ids.add(rid[5:])
        needed={'text_daily_summary_stats','text_daily_summary_advice','text_daily_summary_words','button_summary_words','button_summary_review','button_summary_repair','button_summary_back'}
        if needed-ids: err('daily summary layout missing ids: '+', '.join(sorted(needed-ids)))
    except Exception as e: err('daily summary XML parse failed: '+str(e))
else: err('missing fragment_daily_summary.xml')

if "versionName '5.0.1-native'" not in build or 'def defaultVersionCode = 81' not in build:
    err('v5.0.1 version identity missing')
if 'python3 tools/daily_learning_loop_quality_check.py' not in cm:
    err('Codemagic daily learning-loop check step missing')
if 'v5.0.1' not in cm:
    err('Codemagic workflow title is not v5.0.1')

if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Daily learning loop OK: wrong/stubborn-word recovery, pressure protection, finite-budget repair slot, four-dimension summary, tomorrow due preview, and post-plan summary CTA')

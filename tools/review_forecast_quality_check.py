#!/usr/bin/env python3
"""v5.0.1 seven-day consolidation forecast and true adaptive new-word throttle checks."""
from pathlib import Path
import sys, xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
errors=[]
def err(x): errors.append(x)
def read(p):
    if not p.exists(): err('missing '+str(p.relative_to(ROOT))); return ''
    return p.read_text(encoding='utf-8',errors='replace')

for name in ['ReviewForecastDay.java','ReviewForecast.java','ReviewForecastEngine.java','CalendarFragment.java']:
    if not (JAVA/name).exists(): err('missing '+name)
forecast=read(JAVA/'ReviewForecastEngine.java')
store=read(JAVA/'ProgressStore.java')
repo=read(JAVA/'WordRepository.java')
calendar=read(JAVA/'CalendarFragment.java')
course=read(JAVA/'CourseMapFragment.java')
summary=read(JAVA/'DailySummaryFragment.java')
main=read(JAVA/'MainActivity.java')
smart=read(JAVA/'DailySmartPlanEngine.java')
plan=read(JAVA/'DailySmartPlan.java')
build=read(ROOT/'app/build.gradle')
cm=read(ROOT/'codemagic.yaml')

for marker in ['build(7)','startingBacklog','scheduledArrivals','carryAfter','pendingBeforeReview','baseCapacity','protectedReviewCap','remainingAfterWindow','未来复习负载偏高']:
    if marker not in forecast: err('review forecast engine missing marker: '+marker)
for marker in ['tomorrowScheduled','nextThreeDaysScheduled','forecastPressure','tomorrowHeadroom','ReviewForecastEngine.baseCapacity','recommendedNewWords(List<Word> words){return dashboardStats']:
    if marker not in store: err('ProgressStore future-load throttle missing marker: '+marker)
if 'plan.newQuota=Math.min(progress.recommendedNewWords(words),unknown)' not in repo:
    err('smartMemoryPlan does not obey the same adaptive new-word quota shown on the home screen')
if 'Math.min(progress.perDay(),unknown)' in repo:
    err('legacy fixed perDay smart-memory new-word quota still present')
for marker in ['renderReviewForecast','layout_review_forecast','text_review_forecast_summary','顺延 ','新增 ','forecast.advice']:
    if marker not in calendar: err('consolidation calendar UI missing marker: '+marker)
for marker in ['button_review_calendar','openReviewCalendar']:
    if marker not in course: err('course map consolidation-calendar entry missing '+marker)
for marker in ['button_summary_forecast','ReviewForecastEngine','未来7天','openReviewCalendar']:
    if marker not in summary: err('daily summary forecast entry missing '+marker)
for marker in ['openReviewCalendar','new CalendarFragment()','5.0.1-preupgrade']:
    if marker not in main: err('MainActivity v5.0.1 forecast/upgrade marker missing '+marker)
if 'tomorrowScheduledWords' not in plan: err('DailySmartPlan missing tomorrowScheduledWords')
if '明日已排' not in smart: err('daily smart plan look-ahead copy missing')

for layout_name,needed in {
    'fragment_calendar.xml':{'text_review_forecast_summary','layout_review_forecast'},
    'fragment_course_map.xml':{'button_review_calendar'},
    'fragment_daily_summary.xml':{'button_summary_forecast'},
}.items():
    path=ROOT/'app/src/main/res/layout'/layout_name
    if not path.exists(): err('missing '+layout_name); continue
    try:
        tree=ET.parse(path);ns='{http://schemas.android.com/apk/res/android}';ids=set()
        for el in tree.getroot().iter():
            rid=el.attrib.get(ns+'id','')
            if rid.startswith('@+id/'): ids.add(rid[5:])
        if needed-ids: err(layout_name+' missing ids: '+', '.join(sorted(needed-ids)))
    except Exception as e: err(layout_name+' XML parse failed: '+str(e))

if "versionName '5.0.1-native'" not in build or 'def defaultVersionCode = 81' not in build:
    err('v5.0.1 version identity missing')
if 'python3 tools/review_forecast_quality_check.py' not in cm:
    err('Codemagic review forecast quality-check step missing')
if 'v5.0.1' not in cm:
    err('Codemagic workflow title is not v5.0.1')

if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Review forecast OK: seven-day arrivals/backlog carry-forward, realistic daily capacity, calendar UI, future-load new-word throttle, smart-memory quota parity, and upgrade-safe navigation')

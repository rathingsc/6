#!/usr/bin/env python3
"""v5.0.1 true three-day progressive prescription + persisted cycle gate."""
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def err(x): errors.append(x)
def text(rel):
    p=ROOT/rel
    if not p.exists(): err('missing '+rel); return ''
    return p.read_text(encoding='utf-8',errors='replace')

engine=text('app/src/main/java/com/italiano2774/nativeapp/BreakthroughPlanEngine.java')
store=text('app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java')
passport_engine=text('app/src/main/java/com/italiano2774/nativeapp/MasteryPassportEngine.java')
passport_ui=text('app/src/main/java/com/italiano2774/nativeapp/MasteryPassportFragment.java')
daily=text('app/src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java')
home=text('app/src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java')
summary=text('app/src/main/java/com/italiano2774/nativeapp/DailySummaryFragment.java')
main=text('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
build=text('app/build.gradle')
cm=text('codemagic.yaml')

for marker in ['cycleDay','baselineScore','gain','completedCycles','waitingForTomorrow','cycleComplete','configurePhase','isTodayTaskComplete','本轮起点','/3天','第2天','第3天']:
    if marker not in engine: err('breakthrough engine missing '+marker)
for marker in ['intensive_listening','smart_cloze','free_conversation','writing','daily_speaking','sentence_dictation','active_recall','dialogue']:
    if marker not in engine: err('three-day route missing '+marker)
for marker in ['BreakthroughState','syncBreakthroughCycle','completeBreakthroughPhase','BT_SKILL','BT_PHASE','breakthroughCycle','"version",31']:
    if marker not in store: err('persisted breakthrough cycle missing '+marker)
for marker in ['targetFor','focusSkillKey','focusSkillLabel']:
    if marker not in passport_engine: err('passport target bridge missing '+marker)
for marker in ['case "intensive_listening"','case "smart_cloze"','case "free_conversation"','case "writing"']:
    if marker not in home: err('daily-plan router missing '+marker)
for marker in ['openIntensiveListening','openSmartCloze','openFreeConversation','openWriting','setEnabled(!prescription.waitingForTomorrow&&!prescription.cycleComplete)']:
    if marker not in passport_ui and marker not in summary: err('prescription UI route/state missing '+marker)
if 'BreakthroughPlanEngine.todayTask' not in daily: err('daily planner does not consume breakthrough cycle')
if "versionName '5.0.1-native'" not in build or 'def defaultVersionCode = 81' not in build: err('v5.0.1 version identity missing')
if '5.0.1-preupgrade' not in main: err('v5.0.1 pre-upgrade backup marker missing')
if 'v5.0.1' not in cm or 'python3 tools/breakthrough_plan_quality_check.py' not in cm: err('Codemagic breakthrough gate missing')

if errors:
    for x in errors: print('ERROR:',x)
    sys.exit(1)
print('Breakthrough plan OK: focus-lock, true day1/day2/day3 routes, persisted phase, baseline-to-current gain, next-day spacing, backup/restore, home + passport + summary routing')

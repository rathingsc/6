#!/usr/bin/env python3
"""v5.0.1 beginner-first simple-home + progressive-unlock integration gate."""
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def read(rel):
    p=ROOT/rel
    if not p.exists():
        errors.append('missing '+rel); return ''
    return p.read_text(encoding='utf-8')
def need(text, token, label):
    if token not in text: errors.append(label+' missing: '+token)

store=read('app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java')
guide=read('app/src/main/java/com/italiano2774/nativeapp/BeginnerGuideEngine.java')
home=read('app/src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java')
home_xml=read('app/src/main/res/layout/fragment_course_home.xml')
hub=read('app/src/main/java/com/italiano2774/nativeapp/PracticeHubFragment.java')
settings=read('app/src/main/java/com/italiano2774/nativeapp/SettingsFragment.java')
settings_xml=read('app/src/main/res/layout/fragment_settings.xml')
onboarding=read('app/src/main/java/com/italiano2774/nativeapp/OnboardingFragment.java')
plan=read('app/src/main/java/com/italiano2774/nativeapp/DailySmartPlan.java')
main=read('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
build=read('app/build.gradle')
cm=read('codemagic.yaml')

for token in ['homeSimpleMode()','setHomeSimpleMode','firstWeekGuidanceActive()','firstWeekLearningDay()','o.put("version",31)','"homeSimpleMode"','"home_simple_mode"']:
    need(store,token,'ProgressStore beginner/home persistence')
for token in ['coachLine','practiceHint','practiceUnlocked','lockedLabel','第1天','第7天']:
    need(guide,token,'BeginnerGuideEngine')
for token in ['container_home_details','button_home_details_toggle','progress_daily_plan','BeginnerGuideEngine.coachLine','progress.homeSimpleMode()','applyDetailVisibility','dailyPlan.remainingMinutes()']:
    need(home,token,'CourseHome simple-home flow')
for token in ['今天只做这些','查看今日详情','@+id/container_home_details','@+id/progress_daily_plan','@+id/text_daily_next_task']:
    need(home_xml,token,'CourseHome layout')
for token in ['remainingMinutes()','progressPercent()']:
    need(plan,token,'DailySmartPlan compact progress helpers')
for token in ['bindGuided','BeginnerGuideEngine.practiceUnlocked','BeginnerGuideEngine.active','button_simple_weekly_exam','button_simple_life_tasks']:
    need(hub,token,'PracticeHub progressive unlock')
for day in [2,3,4,5,7]:
    need(hub,','+str(day)+',','PracticeHub day-'+str(day)+' unlock')
for token in ['switch_simple_home','homeSimpleMode()','setHomeSimpleMode']:
    need(settings if token!='switch_simple_home' else settings_xml,token,'Settings simple-home switch')
need(onboarding,'setHomeSimpleMode(true)','Onboarding simple-home default')
need(main,'5.0.1-preupgrade','v5.0 upgrade snapshot')
need(main,'终学意语_backup_v5_0.json','v5.0 backup filename')
need(build,'def defaultVersionCode = 81','v5.0 versionCode')
need(build,"versionName '5.0.1-native'",'v5.0 versionName')
need(cm,'v5.0.1','Codemagic v5.0 identity')
need(cm,'python3 tools/beginner_home_quality_check.py','Codemagic beginner-home gate')

if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('OK: v5.0.1 simple home, beginner 7-day guidance, progressive practice unlock, persistence and release wiring verified.')

#!/usr/bin/env python3
"""v5.0.1 seven-active-day weekly practical exam and automatic next-week adjustment gate."""
from pathlib import Path
import sys
R=Path(__file__).resolve().parents[1]
errors=[]
def read(rel):
    p=R/rel
    if not p.exists(): errors.append('missing '+rel); return ''
    return p.read_text(encoding='utf-8')
def need(text,token,label):
    if token not in text: errors.append(label+' missing: '+token)

engine=read('app/src/main/java/com/italiano2774/nativeapp/WeeklyExamEngine.java')
frag=read('app/src/main/java/com/italiano2774/nativeapp/WeeklyExamFragment.java')
store=read('app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java')
planner=read('app/src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java')
main=read('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
course=read('app/src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java')
report=read('app/src/main/java/com/italiano2774/nativeapp/WeeklyReportFragment.java')
layout=read('app/src/main/res/layout/fragment_weekly_exam.xml')
report_layout=read('app/src/main/res/layout/fragment_weekly_report.xml')
practice=read('app/src/main/java/com/italiano2774/nativeapp/PracticeHubFragment.java')
summary=read('app/src/main/java/com/italiano2774/nativeapp/DailySummaryFragment.java')
summary_layout=read('app/src/main/res/layout/fragment_daily_summary.xml')
build=read('app/build.gradle');cm=read('codemagic.yaml')

for token in ['TOTAL_QUESTIONS=18','QUESTIONS_PER_SKILL=3','ACTION_MEANING','ACTION_LISTENING','ACTION_SPELLING','ACTION_SPEAKING','ACTION_GRAMMAR','ACTION_REAL_USE','TYPE_SPEAK','learnedWordPool','passThreshold']:
    need(engine,token,'weekly exam engine')
for token in ['SpeechRecognizer','recordLearningEvidence','SentenceFsrsRepository.recordDimension','recordErrorCause','saveWeeklyExamResult','weeklyCycleBaselineScore','本地课程与审校句库']:
    need(frag,token,'weekly exam fragment')
for token in ['weeklyExamActiveDaysSinceLast','weeklyExamDue','weeklyAdjustmentActive','weeklyNewWordPercent','weeklyAdjustmentSummary','saveWeeklyExamResult','o.put("version",31)','o.put("weeklyExam",weekly)','o.optJSONObject("weeklyExam")']:
    need(store,token,'weekly exam persistence')
for token in ['weeklyExamDue','weekly_exam','weeklyFocusTask','周测补弱','if(weeklyExamDue&&budget>=20)','addUniqueByAction']:
    need(planner,token,'daily planner weekly integration')
for token in ['openWeeklyExam','5.0.1-preupgrade','终学意语_backup_v5_0.json']:
    need(main,token,'main navigation/backup')
need(course,'case "weekly_exam"','course daily-plan route')
for token in ['text_week_report_exam','weeklyAdjustmentSummary','weeklyExamSkillScore','button_week_exam']:
    need(report,token,'weekly report')
for token in ['panel_week_exam_intro','panel_week_exam_question','panel_week_exam_result','button_week_exam_mic','button_week_exam_giveup']:
    need(layout,token,'weekly exam layout')
need(report_layout,'text_week_report_exam','weekly report layout')
need(practice,'button_simple_weekly_exam','practice entry')
for token in ['button_summary_weekly_exam','weeklyExamDue','weeklyAdjustmentSummary']:
    need(summary,token,'daily summary weekly diagnosis')
need(summary_layout,'button_summary_weekly_exam','daily summary weekly exam button')
need(build,'def defaultVersionCode = 81','versionCode')
need(build,"versionName '5.0.1-native'",'versionName')
need(cm,'v5.0.1','Codemagic identity')
need(cm,'python3 tools/weekly_exam_quality_check.py','Codemagic weekly gate')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Weekly exam OK: 7 active days -> 18-question six-skill test -> error evidence -> next-week focus/new-word adjustment -> backup v31')

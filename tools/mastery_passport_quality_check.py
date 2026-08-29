#!/usr/bin/env python3
"""v5.0.1 six-skill mastery passport + internal stage checkpoint gate."""
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def err(x): errors.append(x)
def text(rel):
    p=ROOT/rel
    if not p.exists(): err('missing '+rel); return ''
    return p.read_text(encoding='utf-8')

engine=text('app/src/main/java/com/italiano2774/nativeapp/MasteryPassportEngine.java')
frag=text('app/src/main/java/com/italiano2774/nativeapp/MasteryPassportFragment.java')
layout=text('app/src/main/res/layout/fragment_mastery_passport.xml')
main=text('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
profile=text('app/src/main/java/com/italiano2774/nativeapp/ProfileFragment.java')
profile_xml=text('app/src/main/res/layout/fragment_profile.xml')
summary=text('app/src/main/java/com/italiano2774/nativeapp/DailySummaryFragment.java')
summary_xml=text('app/src/main/res/layout/fragment_daily_summary.xml')
build=text('app/build.gradle')
cm=text('codemagic.yaml')

for marker in ['class Snapshot','class Skill','class Checkpoint','ACTION_MEANING','ACTION_LISTENING','ACTION_SPELLING','ACTION_SPEAKING','ACTION_GRAMMAR','ACTION_REAL_USE','dashboardStats','blended(','calibrateMeaning','wordEvidenceCap','grammarScore(','realUseScore(','checkpoint("A1"','checkpoint("A2"','checkpoint("B1"','not an official CEFR']:
    if marker not in engine: err('mastery engine missing '+marker)
for marker in ['text_passport_stage','bindSkill','bindCheckpoint','openAction','button_passport_action','button_passport_exam','App内部学习诊断']:
    if marker not in frag and marker not in layout: err('passport fragment/layout missing '+marker)
for marker in ['progress_passport_meaning','progress_passport_listening','progress_passport_spelling','progress_passport_speaking','progress_passport_grammar','progress_passport_real','text_passport_a1','text_passport_a2','text_passport_b1']:
    if marker not in layout: err('passport layout missing '+marker)
if 'openMasteryPassport' not in main or 'new MasteryPassportFragment()' not in main: err('MainActivity passport navigation missing')
if 'button_profile_passport' not in profile or 'openMasteryPassport' not in profile or 'button_profile_passport' not in profile_xml: err('Profile passport entry missing')
if 'button_summary_passport' not in summary or 'openMasteryPassport' not in summary or 'button_summary_passport' not in summary_xml: err('Daily summary passport entry missing')
exam=text('app/src/main/java/com/italiano2774/nativeapp/LevelExamFragment.java'); exam_xml=text('app/src/main/res/layout/fragment_level_exam.xml')
if 'button_exam_passport' not in exam or 'openMasteryPassport' not in exam or 'button_exam_passport' not in exam_xml: err('Level exam passport close-loop missing')
if "versionName '5.0.1-native'" not in build or 'def defaultVersionCode = 81' not in build: err('v5.0.1 version identity missing')
if '5.0.1-preupgrade' not in main: err('v5.0.1 pre-upgrade backup marker missing')
if 'v5.0.1' not in cm or 'python3 tools/mastery_passport_quality_check.py' not in cm: err('Codemagic mastery passport gate missing')
if '不是官方CEFR考试或证书结论' not in layout: err('user-facing CEFR disclaimer missing')

if errors:
    for x in errors: print('ERROR:',x)
    sys.exit(1)
print('Mastery passport OK: six calibrated skills, A1/A2/B1 internal checkpoints, weakest-skill action, CEFR disclaimer, profile + daily-summary navigation')

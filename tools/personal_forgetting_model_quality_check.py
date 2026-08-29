#!/usr/bin/env python3
"""v5.0.1 four-track personal forgetting model integration gate."""
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def read(rel): return (ROOT/rel).read_text(encoding='utf-8')
def need(src,token,label):
    if token not in src: errors.append('missing '+label+': '+token)
for rel in [
    'app/src/main/java/com/italiano2774/nativeapp/PersonalForgettingModel.java',
    'app/src/main/java/com/italiano2774/nativeapp/ForgettingProfileFragment.java',
    'app/src/main/res/layout/fragment_forgetting_profile.xml']:
    if not (ROOT/rel).exists(): errors.append('missing v4.9 file '+rel)
store=read('app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java')
model=read('app/src/main/java/com/italiano2774/nativeapp/PersonalForgettingModel.java')
mode=read('app/src/main/java/com/italiano2774/nativeapp/SmartReviewModeEngine.java')
session=read('app/src/main/java/com/italiano2774/nativeapp/StudySessionFragment.java')
word=read('app/src/main/java/com/italiano2774/nativeapp/WordAdapter.java')
profile=read('app/src/main/java/com/italiano2774/nativeapp/ProfileFragment.java')
main=read('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
build=read('app/build.gradle');cm=read('codemagic.yaml')
for token in ['dimScheduleKey','ensureDimensionSchedules','dimensionIntervalDays','dimensionDueEpochDay','dimensionMemoryStability','dimensionMemoryDifficulty','dimensionRetrievability','dimensionDueForReview','priorityReviewDimension','forgettingFactor','forgettingObservationCount','forgettingSpeedLabel','updateDimensionSchedule','syncAggregateSchedule']:
    need(store,token,'four-track scheduling')
for token in ['updateFactor','scaledInterval','speedLabel','0.70','1.30']:
    need(model,token,'personal calibration')
for token in ['priorityReviewDimension(w.id, java.time.LocalDate.now())','modeForDimension']:
    need(mode,token,'due-dimension routing')
for token in ['channelReview()','SmartReviewModeEngine.choose','dimensionNextDueDate','dimensionIntervalDays']:
    need(session,token,'due/rescue session routing')
for token in ['四维复习：识义','dimensionNextDueDate','dimensionIntervalDays']:
    need(word,token,'word-detail four-track visibility')
for token in ['button_profile_forgetting','openForgettingProfile']:
    need(profile+main,token,'profile navigation')
for token in ['o.put("version",31)','dimensionSchedules','forgettingProfile','o.optJSONArray("dimensionSchedules")','o.optJSONObject("forgettingProfile")']:
    need(store,token,'backup v31')
need(main,'5.0.1-preupgrade','upgrade snapshot')
need(main,'终学意语_backup_v5_0.json','export filename')
need(build,'def defaultVersionCode = 81','v4.9 versionCode')
need(build,"versionName '5.0.1-native'",'v4.9 versionName')
need(cm,'v5.0.1','Codemagic identity')
need(cm,'python3 tools/personal_forgetting_model_quality_check.py','Codemagic v4.9 gate')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Personal forgetting model OK: four independent word-memory schedules + due-channel routing + learner calibration + profile UI + backup v31')

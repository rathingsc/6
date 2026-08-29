#!/usr/bin/env python3
"""v5.0.1 Shadowing three-pass training-room integration gate."""
from pathlib import Path
import sys
R=Path(__file__).resolve().parents[1]
errors=[]
def read(rel):
    p=R/rel
    if not p.exists(): errors.append('missing '+rel); return ''
    return p.read_text(encoding='utf-8',errors='replace')
def need(text,token,label):
    if token not in text: errors.append(label+' missing: '+token)
frag=read('app/src/main/java/com/italiano2774/nativeapp/ShadowingFragment.java')
layout=read('app/src/main/res/layout/fragment_shadowing.xml')
planner=read('app/src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java')
breakthrough=read('app/src/main/java/com/italiano2774/nativeapp/BreakthroughPlanEngine.java')
path=read('app/src/main/java/com/italiano2774/nativeapp/LearningPathEngine.java')
mastery=read('app/src/main/java/com/italiano2774/nativeapp/MasteryPassportEngine.java')
main=read('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
manifest=read('app/src/main/AndroidManifest.xml')
build=read('app/build.gradle');cm=read('codemagic.yaml')
for token in ['SESSION_SIZE=5','PASS_LINE={60,72,82}','MediaRecorder','MediaPlayer','SpeechRecognizer','第1遍 · 看双语跟读','第2遍 · 只看中文主动说','第3遍 · 裸说迁移','ErrorCauseAnalyzer.analyzeSentence','SentenceFsrsRepository.DIM_SPEAKING','recordErrorCause(ErrorCause.PRONUNCIATION','recordEmbeddedDimensionResult','shadowing_pass','selfAssessFallback','再练5句']:
    need(frag,token,'Shadowing fragment')
for token in ['button_shadow_record','button_shadow_record_play','button_shadow_speech','button_speed_slow','button_speed_mid','button_speed_normal','text_shadow_record_hint','Shadowing 三遍训练室']:
    need(layout,token,'Shadowing layout')
for token in ['Shadowing 三遍训练室','双语跟读 → 中文提取 → 裸说迁移','action="shadow"','dailyAuxiliaryAttempts("shadowing"']:
    need(planner,token,'daily planner integration')
for token in ['口语突破 · 第2天','"shadow",""','Shadowing三遍递进','if("shadow".equals(plan.action))']:
    need(breakthrough,token,'three-day speaking route')
need(path,'Shadowing 三遍训练','learning path')
need(mastery,'"shadowing"','mastery passport speaking evidence')
need(main,'openShadowing','main navigation')
need(manifest,'android.permission.RECORD_AUDIO','microphone permission')
need(manifest,'android.speech.RecognitionService','speech-recognition query')
need(build,'def defaultVersionCode = 81','versionCode')
need(build,"versionName '5.0.1-native'",'versionName')
need(main,'5.0.1-preupgrade','pre-upgrade backup')
need(cm,'v5.0.1','Codemagic version')
need(cm,'python3 tools/shadowing_training_quality_check.py','Codemagic Shadowing gate')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Shadowing v4.7 compatibility OK: 5-sentence session -> bilingual follow -> Chinese-only retrieval -> blind recall -> recording playback -> word diff -> sentence FSRS/error evidence.')

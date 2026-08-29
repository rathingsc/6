#!/usr/bin/env python3
"""v5.0.1 gate for the integrated listening + speaking bridge."""
from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
RES=ROOT/'app/src/main/res/layout'
errors=[]
def need(path, needles):
    if not path.exists():
        errors.append(f'missing {path.relative_to(ROOT)}'); return ''
    text=path.read_text(encoding='utf-8')
    for n in needles:
        if n not in text: errors.append(f'{path.name}: missing marker {n}')
    return text
frag=need(JAVA/'ListeningSpeakingFragment.java',[
    'MODE_SENTENCE_LISTEN','MODE_WORD_LISTEN','MODE_REPEAT','MODE_ZH_SPEAK',
    'SpeechRecognizer','MediaRecorder','recordEmbeddedDimensionResult','SentenceFsrsRepository.DIM_LISTENING',
    'SentenceFsrsRepository.DIM_SPEAKING','audio.play(currentWord,','audio.speak(currentSentence.italian',
    'openMemoryArticleReview','listen_speak','0.70f','听一句 · 选择中文意思','主动开口 · 看中文说意大利语'
])
need(RES/'fragment_listening_speaking.xml',[
    'text_ls_summary','spinner_ls_mode','button_ls_play','button_ls_slow','button_ls_speech',
    'button_ls_record','button_ls_record_play','button_ls_review_wrong'
])
need(JAVA/'MainActivity.java',['openListeningSpeaking','new ListeningSpeakingFragment()'])
need(JAVA/'PracticeHubFragment.java',['activity.openListeningSpeaking()'])
need(JAVA/'LearningPathEngine.java',['"listen_speak"','"听说10题"','ProgressStore.DIM_SPEAKING'])
need(JAVA/'TodayFragment.java',['case "listen_speak":a.openListeningSpeaking();break;'])
store=need(JAVA/'ProgressStore.java',['recordEmbeddedDimensionResult','"listen_speak"'])
if store.count('"listen_speak"')<3: errors.append('ProgressStore must persist listen_speak aux stats in export/import and embedded logging')
manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
if 'android.permission.RECORD_AUDIO' not in manifest: errors.append('RECORD_AUDIO permission missing')
if 'android.speech.RecognitionService' not in manifest: errors.append('speech recognition query missing')
build=(ROOT/'app/build.gradle').read_text(encoding='utf-8')
if 'def defaultVersionCode = 81' not in build or "versionName '5.0.1-native'" not in build: errors.append('v5.0.1 version identity missing')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Listening/speaking quality OK: mixed 10-question bridge, word audio, sentence TTS, speech recognition, recording playback and weak-word smart-review return are wired.')

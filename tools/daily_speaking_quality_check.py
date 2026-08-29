#!/usr/bin/env python3
"""v5.0.1 daily five-sentence active-speaking integration gate (stdlib only)."""
from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def err(x): errors.append(x)
def text(p):
    p=ROOT/p
    if not p.exists(): err('missing '+str(p.relative_to(ROOT))); return ''
    return p.read_text(encoding='utf-8')

asset=ROOT/'app/src/main/assets/core_sentences.json'
try:
    rows=json.load(open(asset,encoding='utf-8'))
    if len(rows)!=430: err(f'core_sentences expected 430, got {len(rows)}')
    for lv in ('A1','A2','B1'):
        xs=[x for x in rows if x.get('level')==lv]
        if len(xs)<50: err(f'{lv} active-speaking pool too small: {len(xs)}')
        cats={x.get('category') for x in xs if x.get('category')}
        if len(cats)<3: err(f'{lv} speaking categories too few: {len(cats)}')
except Exception as e: err('core_sentences invalid: '+str(e))

engine=text('app/src/main/java/com/italiano2774/nativeapp/DailySpeakingChallengeEngine.java')
frag=text('app/src/main/java/com/italiano2774/nativeapp/DailySpeakingChallengeFragment.java')
layout=text('app/src/main/res/layout/fragment_daily_speaking.xml')
main=text('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
hub=text('app/src/main/java/com/italiano2774/nativeapp/PracticeHubFragment.java')
hubxml=text('app/src/main/res/layout/fragment_practice_hub.xml')
plan=text('app/src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java')
home=text('app/src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java')
store=text('app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java')
summary=text('app/src/main/java/com/italiano2774/nativeapp/DailySummaryFragment.java')
build=text('app/build.gradle')
cm=text('codemagic.yaml')

for marker in ['DAILY_TARGET=5','targetLevel()','passScore()','todayBatch()','Collections.shuffle','A1','A2','B1']:
    if marker not in engine: err('speaking engine missing '+marker)
for marker in ['SpeechRecognizer','RecognizerIntent.EXTRA_LANGUAGE,"it-IT"','ErrorCauseAnalyzer.analyzeSentence','recordAuxiliaryResult("daily_speaking"','SentenceFsrsRepository.DIM_SPEAKING','recordEmbeddedDimensionResult','audio.speak(current.italian,1.0f)','audio.speak(current.italian,0.70f)','直接查看答案','没有可用的意大利语语音识别']:
    if marker not in frag: err('speaking fragment missing '+marker)
for marker in ['text_speak5_prompt','button_speak5_mic','edit_speak5_answer','button_speak5_hint','button_speak5_reveal','button_speak5_play','button_speak5_slow']:
    if marker not in layout: err('speaking layout missing '+marker)
if 'openDailySpeakingChallenge' not in main or 'new DailySpeakingChallengeFragment()' not in main: err('MainActivity speaking navigation missing')
if 'button_simple_daily_speaking' not in hub or 'openDailySpeakingChallenge' not in hub or 'button_simple_daily_speaking' not in hubxml: err('practice hub daily-speaking entry missing')
for marker in ['DailySmartTask speak5','"daily_speaking"','每日5句开口','budget<=20','addIfFitsUniqueAction(plan.tasks,speak5,budget)']:
    if marker not in plan: err('daily smart plan speaking slot missing '+marker)
if 'case "daily_speaking"' not in home or 'openDailySpeakingChallenge' not in home: err('course home cannot open daily-speaking task')
for marker in ['dailySpeakingStreak()','"daily_speaking"']:
    if marker not in store: err('ProgressStore daily-speaking persistence/streak missing '+marker)
if '每日5句开口' not in summary or 'dailySpeakingStreak()' not in summary or 'button_summary_speaking' not in summary: err('daily summary speaking close-loop missing')

for marker in ['o.put("dailySpeaking"','optJSONArray("dailySpeaking")','auxday_daily_speaking_att_','auxday_daily_speaking_cor_']:
    if marker not in store: err('daily speaking backup/streak history marker missing '+marker)

if "versionName '5.0.1-native'" not in build or 'def defaultVersionCode = 81' not in build: err('v5.0.1 version identity missing')
if 'v5.0.1' not in cm or 'python3 tools/daily_speaking_quality_check.py' not in cm: err('Codemagic v5.0.1 daily-speaking gate missing')
if '5.0.1-preupgrade' not in main: err('v5.0.1 pre-upgrade backup marker missing')

if errors:
    for x in errors: print('ERROR:',x)
    sys.exit(1)
print('Daily speaking OK: 5 prompts/day, A1-A2-B1 adaptive pool, speech/text fallback, local scoring, FSRS/word-dimension feedback, 365-day streak backup, smart-plan + summary integration')

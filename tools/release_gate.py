#!/usr/bin/env python3
"""Strict pre-build release gate for ZhongXueYiYu.

Uses only Python stdlib so it can run on Codemagic before Gradle resolves Android deps.
It targets the recurring failure classes seen in this project: missing root files/assets,
invalid XML attributes, broken R references, bad Manifest components, helper arity mistakes,
layout/id mismatches, corrupt word/audio data, migration SQL typos, and CI script syntax.
"""
from pathlib import Path
import collections, json, re, sqlite3, subprocess, sys, tempfile, xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/'app'
JAVA=APP/'src/main/java/com/italiano2774/nativeapp'
RES=APP/'src/main/res'
ASSETS=APP/'src/main/assets'
A='{http://schemas.android.com/apk/res/android}'
errors=[]; warnings=[]; checks=collections.Counter()

def error(msg): errors.append(msg)
def warn(msg): warnings.append(msg)
def require(rel):
    p=ROOT/rel
    if not p.exists(): error(f'MISSING: {rel}')
    return p

def read(p): return p.read_text(encoding='utf-8')

def strip_java(src):
    out=[];i=0;state='code';quote=''
    while i<len(src):
        c=src[i];n=src[i+1] if i+1<len(src) else ''
        if state=='line':
            out.append('\n' if c=='\n' else ' ')
            if c=='\n': state='code'
        elif state=='block':
            out.append('\n' if c=='\n' else ' ')
            if c=='*' and n=='/': out.append(' ');i+=1;state='code'
        elif state=='string':
            out.append(' ')
            if c=='\\':
                if i+1<len(src): out.append(' ');i+=1
            elif c==quote: state='code'
        else:
            if c=='/' and n=='/': out.extend((' ',' '));i+=1;state='line'
            elif c=='/' and n=='*': out.extend((' ',' '));i+=1;state='block'
            elif c in ('"',"'"): out.append(' ');quote=c;state='string'
            else: out.append(c)
        i+=1
    return ''.join(out)

def balanced_java(src):
    clean=strip_java(src);stack=[];pairs={')':'(',']':'[','}':'{'}
    for c in clean:
        if c in '([{': stack.append(c)
        elif c in ')]}':
            if not stack or stack.pop()!=pairs[c]: return False
    return not stack

def split_args(src):
    args=[];cur=[];depth=0;state='code';quote='';i=0
    while i<len(src):
        c=src[i]
        if state=='string':
            cur.append(c)
            if c=='\\' and i+1<len(src): i+=1;cur.append(src[i])
            elif c==quote: state='code'
        else:
            if c in ('"',"'"): state='string';quote=c;cur.append(c)
            elif c in '([{<': depth+=1;cur.append(c)
            elif c in ')]}>': depth=max(0,depth-1);cur.append(c)
            elif c==',' and depth==0: args.append(''.join(cur).strip());cur=[]
            else: cur.append(c)
        i+=1
    if ''.join(cur).strip(): args.append(''.join(cur).strip())
    return args

def find_calls(clean,name):
    # Return unqualified calls name(...) with argument source. Skip declarations and member calls.
    pat=re.compile(r'(?<![.\w])'+re.escape(name)+r'\s*\(')
    for m in pat.finditer(clean):
        before=clean[max(0,m.start()-100):m.start()]
        if re.search(r'\b(?:class|interface|enum|new)\s*$',before): continue
        pos=m.end();depth=1;i=pos
        while i<len(clean) and depth:
            if clean[i]=='(': depth+=1
            elif clean[i]==')': depth-=1
            i+=1
        if depth==0: yield clean[pos:i-1]

# 1. Root shape and build config
for rel in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','codemagic.yaml','app/src/main/AndroidManifest.xml','覆盖升级与签名说明.txt','tools/signing_config_check.py','signing-certificate-sha256.txt']:
    require(rel)
checks['root files']=9
if not (ROOT/'.gitignore').exists(): warn('.gitignore missing from checkout; continuing because secret signing files are checked directly')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
app_gradle=read(APP/'build.gradle');root_gradle=read(ROOT/'build.gradle');cm=read(ROOT/'codemagic.yaml')
for needle,msg in [
    ('def defaultVersionCode = 81','v5.0.1 local fallback versionCode must be 79'),
    ('versionCode resolvedVersionCode','dynamic versionCode support missing'),
    ("versionName '5.0.1-native'",'versionName must be 4.9.0-native'),
    ("applicationId 'com.italiano2774.nativeapp'",'applicationId update identity changed'),
    ('signingConfig signingConfigs.release','release signingConfig missing'),
    ('CM_KEYSTORE_PATH','Codemagic release signing env wiring missing'),
    ('compileSdk 35','compileSdk must be 35'),('targetSdk 35','targetSdk must be 35'),('minSdk 26','minSdk must be 26'),
    ('JavaVersion.VERSION_17','Java source/target must be 17')]:
    if needle not in app_gradle: error(msg)
if "com.android.application' version '8.9.2'" not in root_gradle: error('AGP 8.9.2 declaration missing')
if '--gradle-version 8.11.1' not in cm: error('Codemagic must generate Gradle 8.11.1 wrapper')
if 'java: 17' not in cm: error('Codemagic JDK 17 declaration missing')
if 'python3 tools/java_source_syntax_check.py' not in cm: error('Codemagic Java source literal check step missing')
if 'python3 tools/course_check.py' not in cm: error('Codemagic guided course semantic check step missing')
if 'python3 tools/exercise_quality_check.py' not in cm: error('Codemagic exercise quality check step missing')
if 'python3 tools/translation_quality_check.py' not in cm: error('Codemagic translation quality check step missing')
if 'python3 tools/lexical_semantic_quality_check.py' not in cm: error('Codemagic v3.1.9 lexical semantic regression check step missing')
if 'python3 tools/full_lexicon_alignment_check.py' not in cm: error('Codemagic v3.2.1 full lexicon alignment check step missing')
if 'python3 tools/full_retranslation_quality_check.py' not in cm: error('Codemagic v3.2.1 full retranslation quality check step missing')
if 'python3 tools/word_example_quality_check.py' not in cm: error('Codemagic v3.2.2 word example quality check step missing')
if 'python3 tools/word_example_expansion_check.py' not in cm: error('Codemagic v3.3.3 high-frequency example expansion check step missing')
if 'python3 tools/course_translation_quality_check.py' not in cm: error('Codemagic course translation quality check step missing')
if 'python3 tools/person_semantics_check.py' not in cm: error('Codemagic person/conjugation semantic check step missing')
if 'python3 tools/phrase_semantic_quality_check.py' not in cm: error('Codemagic phrase/core-sentence semantic check step missing')
if 'python3 tools/english_bridge_quality_check.py' not in cm: error('Codemagic English bridge quality check step missing')
if 'python3 tools/memory_aid_quality_check.py' not in cm: error('Codemagic active-recall/memory-aid quality check step missing')
if 'python3 tools/smart_mastery_quality_check.py' not in cm: error('Codemagic v3.3 four-mode smart mastery check step missing')
if 'python3 tools/teach_before_test_quality_check.py' not in cm: error('Codemagic v3.3.3 teach-before-test check step missing')
if 'python3 tools/course_path_mix_quality_check.py' not in cm: error('Codemagic v3.3.4 mixed course-path check step missing')
if 'python3 tools/course_unit_isolation_quality_check.py' not in cm: error('Codemagic v5.0.1 fixed-course unit isolation check step missing')
if 'python3 tools/memory_articles_quality_check.py' not in cm: error('Codemagic v3.3.8 ten-article / 2000-word route check step missing')
if 'python3 tools/memory_article_recurrence_quality_check.py' not in cm: error('Codemagic v3.3.8 spiral recurrence check step missing')
if 'python3 tools/memory_article_sentence_quality_check.py' not in cm: error('Codemagic v3.3.8 sentence-by-sentence article check step missing')
if 'python3 tools/listening_speaking_quality_check.py' not in cm: error('Codemagic v5.0.1 listening/speaking bridge check step missing')
if 'python3 tools/scenario_conversation_quality_check.py' not in cm: error('Codemagic v5.0.1 three-level scenario conversation check step missing')
if 'python3 tools/micro_grammar_quality_check.py' not in cm: error('Codemagic v5.0.1 micro grammar check step missing')
if 'python3 tools/daily_smart_plan_quality_check.py' not in cm: error('Codemagic v5.0.1 daily smart plan check step missing')
if 'python3 tools/daily_speaking_quality_check.py' not in cm: error('Codemagic v5.0.1 daily five-sentence speaking check step missing')
if 'python3 tools/mastery_passport_quality_check.py' not in cm: error('Codemagic v5.0.1 mastery passport check step missing')
if 'python3 tools/breakthrough_plan_quality_check.py' not in cm: error('Codemagic v5.0.1 breakthrough prescription check step missing')
if 'python3 tools/error_evidence_quality_check.py' not in cm: error('Codemagic v5.0.1 error evidence repair check step missing')
if 'python3 tools/weekly_exam_quality_check.py' not in cm: error('Codemagic v5.0.1 weekly practical exam check step missing')
if 'python3 tools/shadowing_training_quality_check.py' not in cm: error('Codemagic v5.0.1 Shadowing training-room check step missing')
if 'python3 tools/weak_word_story_quality_check.py' not in cm: error('Codemagic v5.0.1 weak-word micro-reading check step missing')
if 'python3 tools/life_task_map_quality_check.py' not in cm: error('Codemagic v5.0.1 real-life task-map check step missing')
if 'python3 tools/personal_forgetting_model_quality_check.py' not in cm: error('Codemagic v5.0.1 personal forgetting model check step missing')
if 'python3 tools/beginner_home_quality_check.py' not in cm: error('Codemagic v5.0.1 beginner-first home check step missing')
if 'python3 tools/daily_learning_loop_quality_check.py' not in cm: error('Codemagic v5.0.1 daily learning-loop check step missing')
if 'python3 tools/review_forecast_quality_check.py' not in cm: error('Codemagic v5.0.1 seven-day consolidation forecast check step missing')
if 'python3 tools/release_gate.py' not in cm: error('Codemagic strict release gate step missing')
if 'v5.0.1' not in cm: error('Codemagic workflow title must identify v5.0.1')
if 'android_signing:' not in cm or '- zhongxue_release' not in cm: error('Codemagic permanent signing identity zhongxue_release missing')
if ':app:assembleRelease' not in cm or 'app/build/outputs/apk/release/app-release.apk' not in cm: error('Codemagic must build/export signed release APK')
if ':app:assembleDebug' in cm or 'app-debug.apk' in cm: error('Codemagic official workflow must not publish debug APK after v3.1.5')
if 'python3 tools/signing_config_check.py' not in cm: error('Codemagic permanent signing config gate missing')
if '-PversionCode="$UPDATE_VERSION_CODE"' not in cm: error('Codemagic monotonic versionCode injection missing')
course_engine=read(APP/'src/main/java/com/italiano2774/nativeapp/CourseLessonEngine.java')
if 'reviewDue(' in course_engine or 'addDueReview' in course_engine: error('v5.0.1 fixed course still injects global due-review words')
if 'addSeenReinforcement' not in course_engine: error('v5.0.1 same-unit reinforcement missing')
course_fragment=read(APP/'src/main/java/com/italiano2774/nativeapp/CourseLessonFragment.java')
if 'meaning(w,unit,rnd,true)' not in course_engine or 'teachBeforeTest' not in course_engine: error('v3.3.3 guided learning must flag meaning cards for teach-before-test')
if '先看一遍翻译' not in course_fragment or '第一次见新词不需要猜' not in course_fragment or '我看过了 · 开始选择' not in course_fragment: error('v3.3.3 teach-before-test UI/flow missing')
for marker in ['词汇起步','听力训练','句子理解','主动表达','单元挑战','pathSummary','articleQuestion','verbQuestion']:
    if marker not in course_engine: error('v3.3.4 mixed guided path missing '+marker)
course_home=read(APP/'src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java')
if 'pathSummary' not in course_home or '听、说、读、写会由系统自动穿插' not in course_home: error('v5.0.1 compact mixed-skill course-home explanation missing')
if '螺旋复现' not in course_home or '真正掌握' not in course_home or 'openMemoryArticles' not in course_home: error('v3.3.8 ten-article home progress/entry missing')
if 'renderDailyPlan' not in course_home or 'openDailyTask' not in course_home or '开始下一项' not in course_home: error('v5.0.1 daily smart-plan home flow missing')
if 'case "error_repair"' not in course_home or '查看今日总结 · 明日复习预告' not in course_home: error('v5.0.1 recovery/summary close-loop home flow missing')
if 'BeginnerGuideEngine.coachLine' not in course_home or 'button_home_details_toggle' not in course_home or 'progress.homeSimpleMode()' not in course_home: error('v5.0.1 beginner-first compact home flow missing')
beginner_guide=read(JAVA/'BeginnerGuideEngine.java')
practice_hub=read(JAVA/'PracticeHubFragment.java')
if 'practiceUnlocked' not in beginner_guide or '第7天' not in beginner_guide: error('v5.0.1 seven-day beginner guide missing')
if 'bindGuided' not in practice_hub or 'BeginnerGuideEngine.active' not in practice_hub: error('v5.0.1 progressive practice unlock missing')
review_forecast=read(JAVA/'ReviewForecastEngine.java')
calendar_java=read(JAVA/'CalendarFragment.java')
word_repo=read(JAVA/'WordRepository.java')
forecast_store=read(JAVA/'ProgressStore.java')
if 'carryAfter' not in review_forecast or 'baseCapacity' not in review_forecast or 'remainingAfterWindow' not in review_forecast: error('v5.0.1 seven-day review forecast engine missing')
if 'renderReviewForecast' not in calendar_java or 'layout_review_forecast' not in calendar_java: error('v5.0.1 consolidation calendar rendering missing')
if 'plan.newQuota=Math.min(progress.recommendedNewWords(words),unknown)' not in word_repo: error('v5.0.1 smart-memory session ignores adaptive new-word quota')
if 'tomorrowScheduled' not in forecast_store or 'tomorrowHeadroom' not in forecast_store or 'forecastPressure' not in forecast_store: error('v5.0.1 future-load new-word throttle missing')

mastery_engine=read(APP/'src/main/java/com/italiano2774/nativeapp/MasteryPassportEngine.java')
mastery_fragment=read(APP/'src/main/java/com/italiano2774/nativeapp/MasteryPassportFragment.java')
mastery_layout=read(APP/'src/main/res/layout/fragment_mastery_passport.xml')
for _m in ['ACTION_MEANING','ACTION_LISTENING','ACTION_SPELLING','ACTION_SPEAKING','ACTION_GRAMMAR','ACTION_REAL_USE','checkpoint("A1"','checkpoint("A2"','checkpoint("B1"']:
    if _m not in mastery_engine: error('v5.0.1 mastery passport engine missing '+_m)
for _m in ['bindSkill','bindCheckpoint','openAction']:
    if _m not in mastery_fragment: error('v5.0.1 mastery passport fragment missing '+_m)
if '不是官方CEFR考试或证书结论' not in mastery_layout: error('v5.0.1 mastery passport CEFR disclaimer missing')
breakthrough_engine=read(APP/'src/main/java/com/italiano2774/nativeapp/BreakthroughPlanEngine.java')
for _m in ['threeDayText','todayTask','sentence_dictation','daily_speaking','ACTION_REAL_USE']:
    if _m not in breakthrough_engine: error('v5.0.1 breakthrough prescription engine missing '+_m)
if 'BreakthroughPlanEngine.todayTask' not in read(APP/'src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java'): error('v5.0.1 daily planner does not consume passport prescription')
error_evidence=read(APP/'src/main/java/com/italiano2774/nativeapp/ErrorEvidenceRepairFragment.java')
for _m in ['unresolvedPracticeErrors(40)','markMatchingErrorRepaired','markEvidenceRepairComplete','SentenceFsrsRepository.recordDimension']:
    if _m not in error_evidence: error('v5.0.1 error evidence repair loop missing '+_m)
if 'case "error_evidence_repair"' not in course_home: error('v5.0.1 daily planner route for personal error notebook missing')
weekly_engine=read(APP/'src/main/java/com/italiano2774/nativeapp/WeeklyExamEngine.java')
weekly_fragment=read(APP/'src/main/java/com/italiano2774/nativeapp/WeeklyExamFragment.java')
weekly_plan=read(APP/'src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java')
for _m in ['TOTAL_QUESTIONS=18','QUESTIONS_PER_SKILL=3','ACTION_MEANING','ACTION_REAL_USE']:
    if _m not in weekly_engine: error('v5.0.1 weekly exam engine missing '+_m)
for _m in ['SpeechRecognizer','saveWeeklyExamResult','weekly_exam']:
    if _m not in weekly_fragment: error('v5.0.1 weekly exam result/feedback missing '+_m)
for _m in ['weeklyExamDue','weeklyFocusTask','weekly_exam','addIfFitsUniqueAction']:
    if _m not in weekly_plan: error('v5.0.1 weekly diagnosis planner integration missing '+_m)
if 'case "weekly_exam"' not in course_home: error('v5.0.1 daily planner weekly-exam route missing')
weak_story_engine=read(APP/'src/main/java/com/italiano2774/nativeapp/WeakWordStoryEngine.java')
weak_story_fragment=read(APP/'src/main/java/com/italiano2774/nativeapp/WeakWordStoryFragment.java')
weak_story_layout=read(APP/'src/main/res/layout/fragment_weak_word_story.xml')
for _m in ['rankedWeakWords','rankSections','fillFromSection','fillFromExamples','buildClozeTargets']:
    if _m not in weak_story_engine: error('v5.0.1 weak-word story engine missing '+_m)
for _m in ['第1 / 4步','第2 / 4步','第3 / 4步','第4 / 4步','recordEmbeddedDimensionResults','recordAuxiliaryResult("weak_story"','SentenceFsrsRepository']:
    if _m not in weak_story_fragment: error('v5.0.1 weak-word four-stage loop missing '+_m)
if 'button_weak_story_mic' not in weak_story_layout or '不联网生成' not in weak_story_layout: error('v5.0.1 weak-word story layout/disclosure missing')
life_repo=read(APP/'src/main/java/com/italiano2774/nativeapp/LifeTaskRepository.java')
life_engine=read(APP/'src/main/java/com/italiano2774/nativeapp/LifeTaskEngine.java')
life_detail=read(APP/'src/main/java/com/italiano2774/nativeapp/LifeTaskDetailFragment.java')
dialogue_training=read(APP/'src/main/java/com/italiano2774/nativeapp/DialogueTrainingFragment.java')
for _m in ['new LifeTask(','clothing_store','post_office','phone_appointment','bureaucracy']:
    if _m not in life_repo: error('v5.0.1 real-life task repository missing '+_m)
for _m in ['passLine(int level)','completedStages(ProgressStore p)','nextTask(ProgressStore p)','unlocked(ProgressStore p,LifeTask task,int level)']:
    if _m not in life_engine: error('v5.0.1 36-stage task progression missing '+_m)
for _m in ['LifeTaskEngine.unlocked','openLifeTaskStage','先通过上一关']:
    if _m not in life_detail: error('v5.0.1 task detail sequential unlock missing '+_m)
for _m in ['newInstance(String id,int level,String lifeTaskId)','recordAuxiliaryResult("life_task"','LifeTaskEngine.passLine(currentLevel)']:
    if _m not in dialogue_training: error('v5.0.1 task/dialogue result bridge missing '+_m)
if 'case "life_task"' not in course_home: error('v5.0.1 daily plan life-task route missing')
if 'LifeTaskEngine.nextTask(progress)' not in weekly_plan or 'dailyAuxiliaryCorrect("life_task"' not in weekly_plan: error('v5.0.1 daily/weekly task-map scheduling missing')
if '"life_task"' not in mastery_engine: error('v5.0.1 life-task evidence missing from mastery passport')
shadowing=read(APP/'src/main/java/com/italiano2774/nativeapp/ShadowingFragment.java')
shadow_layout=read(APP/'src/main/res/layout/fragment_shadowing.xml')
for _m in ['PASS_LINE={60,72,82}','MediaRecorder','MediaPlayer','SpeechRecognizer','第2遍 · 只看中文主动说','第3遍 · 裸说迁移','SentenceFsrsRepository.DIM_SPEAKING','recordErrorCause(ErrorCause.PRONUNCIATION','shadowing_pass']:
    if _m not in shadowing: error('v5.0.1 Shadowing training room missing '+_m)
for _m in ['button_shadow_record_play','button_shadow_speech','button_speed_mid','Shadowing 三遍训练室']:
    if _m not in shadow_layout: error('v5.0.1 Shadowing layout missing '+_m)
if '口语突破 · 第2天' not in breakthrough_engine or '"shadow",""' not in breakthrough_engine: error('v5.0.1 three-day speaking prescription does not route day 2 to Shadowing')


daily_summary=read(APP/'src/main/java/com/italiano2774/nativeapp/DailySummaryFragment.java')
for _m in ['learnedDimensionPct','dueCount(all,today.plusDays(1))','button_summary_repair','tomorrowAdvice']:
    if _m not in daily_summary: error('v5.0.1 daily summary close-loop missing '+_m)
main_nav=read(APP/'src/main/java/com/italiano2774/nativeapp/MainActivity.java')
for _m in ['openMemoryArticles','openMemoryArticle','openMemoryArticleStudy','openMemoryArticleSentenceStudy','openMemoryArticleReview']:
    if _m not in main_nav: error('v3.3.8 MainActivity memory-article navigation missing '+_m)
if 'openListeningSpeaking' not in main_nav: error('v5.0.1 MainActivity listening/speaking navigation missing')
for _m in ['openMicroGrammar','openMicroGrammarLesson']:
    if _m not in main_nav: error('v5.0.1 MainActivity micro-grammar navigation missing '+_m)
micro_grammar=read(APP/'src/main/java/com/italiano2774/nativeapp/MicroGrammarFragment.java')
for _m in ['今天先学：','20秒看懂 · 做3题','到期复习','基本稳定','openMicroGrammarLesson']:
    if _m not in micro_grammar: error('v5.0.1 micro-grammar route missing '+_m)
listen_speak=read(APP/'src/main/java/com/italiano2774/nativeapp/ListeningSpeakingFragment.java')
for _m in ['SpeechRecognizer','MediaRecorder','MODE_SENTENCE_LISTEN','MODE_WORD_LISTEN','MODE_REPEAT','MODE_ZH_SPEAK','listen_speak','openMemoryArticleReview']:
    if _m not in listen_speak: error('v5.0.1 listening/speaking bridge missing '+_m)
dialogue_training=read(APP/'src/main/java/com/italiano2774/nativeapp/DialogueTrainingFragment.java')
for _m in ['LEVEL_BEGINNER','LEVEL_INTERMEDIATE','LEVEL_ADVANCED','FreeConversationEngine','dialogueScenarioRecommendedLevel','markDialogueScenarioCompletion(current.id,score,currentLevel)']:
    if _m not in dialogue_training: error('v5.0.1 three-level scenario flow missing '+_m)
article_study=read(APP/'src/main/java/com/italiano2774/nativeapp/MemoryArticleStudyFragment.java')
for _m in ['双语精读','只看意大利语','只听','挖空回忆','中文反推','recordMemoryArticleExposure','旧词复现','combinedReviewIds']:
    if _m not in article_study: error('v3.3.8 five-step article study missing '+_m)
article_sentence=read(APP/'src/main/java/com/italiano2774/nativeapp/MemoryArticleSentenceStudyFragment.java')
for _m in ['第1轮 · 双语理解','第2轮 · 听读','第3轮 · 挖空回忆','第4轮 · 中文反推','0.70f','markMemoryArticleSentenceStudyDone']:
    if _m not in article_sentence: error('v3.3.8 sentence-by-sentence memorisation missing '+_m)
article_session=read(APP/'src/main/java/com/italiano2774/nativeapp/StudySessionFragment.java')
if 'newArticleReviewInstance' not in article_session or '十篇通关定向复习' not in article_session: error('v3.3.8 targeted article smart review missing')
checks['build config']=1
require('app/src/main/assets/translation_quality_v311.json')
require('app/src/main/assets/course_translation_quality_v312.json')
require('app/src/main/assets/course_translation_quality_v315.json')
require('app/src/main/assets/phrase_semantic_quality_v316.json')
require('app/src/main/assets/lexical_semantic_quality_v319.json')
require('app/src/main/assets/lexical_alignment_quality_v320.json')
require('app/src/main/assets/word_example_quality_v322.json')
require('tools/word_example_quality_check.py')
require('app/src/main/assets/word_example_expansion_v332.json')
require('app/src/main/assets/word_example_expansion_v333.json')
require('tools/word_examples_v333.tsv')
require('tools/apply_word_examples_v333.py')
require('tools/word_example_expansion_check.py')
require('tools/word_examples_v332.tsv')
require('tools/teach_before_test_quality_check.py')
require('tools/course_path_mix_quality_check.py')
require('tools/course_unit_isolation_quality_check.py')
require('app/src/main/assets/memory_articles.json')
require('tools/memory_articles_quality_check.py')
require('tools/memory_article_recurrence_quality_check.py')
require('tools/memory_article_sentence_quality_check.py')
for _f in ['MemoryArticle.java','MemoryArticleSection.java','MemoryArticleSentence.java','MemoryArticleReinforcement.java','MemoryArticleRepository.java','MemoryArticleListFragment.java','MemoryArticleDetailFragment.java','MemoryArticleStudyFragment.java','MemoryArticleSentenceStudyFragment.java']:
    require('app/src/main/java/com/italiano2774/nativeapp/'+_f)
for _f in ['fragment_memory_article_list.xml','fragment_memory_article_detail.xml','fragment_memory_article_study.xml','fragment_memory_article_sentence_study.xml']:
    require('app/src/main/res/layout/'+_f)
require('app/src/main/assets/english_bridges.json')
require('tools/lexical_semantic_quality_check.py')
require('tools/full_lexicon_alignment_check.py')
require('tools/person_semantics_check.py')
require('tools/phrase_semantic_quality_check.py')
require('tools/english_bridge_quality_check.py')
require('tools/memory_aid_quality_check.py')
require('app/src/main/assets/morphology_hints.json')
require('app/src/main/assets/memory_chunks.json')
require('app/src/main/java/com/italiano2774/nativeapp/MemoryAidRepository.java')
require('app/src/main/java/com/italiano2774/nativeapp/SmartReviewModeEngine.java')
require('app/src/main/java/com/italiano2774/nativeapp/IssueReportStore.java')
require('tools/smart_mastery_quality_check.py')
require('tools/translation_quality_check.py')
require('tools/daily_smart_plan_quality_check.py')
require('tools/daily_speaking_quality_check.py')
require('tools/mastery_passport_quality_check.py')
require('tools/breakthrough_plan_quality_check.py')
require('tools/error_evidence_quality_check.py')
require('tools/weekly_exam_quality_check.py')
require('tools/weak_word_story_quality_check.py')
require('tools/life_task_map_quality_check.py')
require('tools/personal_forgetting_model_quality_check.py')
require('app/src/main/java/com/italiano2774/nativeapp/PersonalForgettingModel.java')
require('app/src/main/java/com/italiano2774/nativeapp/ForgettingProfileFragment.java')
require('app/src/main/res/layout/fragment_forgetting_profile.xml')
require('app/src/main/java/com/italiano2774/nativeapp/LifeTask.java')
require('app/src/main/java/com/italiano2774/nativeapp/LifeTaskRepository.java')
require('app/src/main/java/com/italiano2774/nativeapp/LifeTaskEngine.java')
require('app/src/main/java/com/italiano2774/nativeapp/LifeTaskMapFragment.java')
require('app/src/main/java/com/italiano2774/nativeapp/LifeTaskAdapter.java')
require('app/src/main/java/com/italiano2774/nativeapp/LifeTaskDetailFragment.java')
require('app/src/main/res/layout/fragment_life_task_map.xml')
require('app/src/main/res/layout/item_life_task.xml')
require('app/src/main/res/layout/fragment_life_task_detail.xml')
require('app/src/main/java/com/italiano2774/nativeapp/WeeklyExamEngine.java')
require('app/src/main/java/com/italiano2774/nativeapp/WeeklyExamFragment.java')
require('app/src/main/res/layout/fragment_weekly_exam.xml')
require('app/src/main/java/com/italiano2774/nativeapp/WeakWordStory.java')
require('app/src/main/java/com/italiano2774/nativeapp/WeakWordStoryEngine.java')
require('app/src/main/java/com/italiano2774/nativeapp/WeakWordStoryFragment.java')
require('app/src/main/res/layout/fragment_weak_word_story.xml')
require('app/src/main/java/com/italiano2774/nativeapp/ErrorEvidenceRepairFragment.java')
require('app/src/main/res/layout/fragment_error_evidence_repair.xml')
require('app/src/main/java/com/italiano2774/nativeapp/BreakthroughPlanEngine.java')
require('app/src/main/java/com/italiano2774/nativeapp/MasteryPassportEngine.java')
require('app/src/main/java/com/italiano2774/nativeapp/MasteryPassportFragment.java')
require('app/src/main/res/layout/fragment_mastery_passport.xml')
require('app/src/main/java/com/italiano2774/nativeapp/DailySpeakingChallengeEngine.java')
require('app/src/main/java/com/italiano2774/nativeapp/DailySpeakingChallengeFragment.java')
require('app/src/main/res/layout/fragment_daily_speaking.xml')
require('tools/daily_learning_loop_quality_check.py')
require('tools/course_translation_quality_check.py')

# 2. XML parse, resource inventory, duplicate IDs, risky attrs, touch targets
inventory=collections.defaultdict(set); xml_files=list(RES.rglob('*.xml')); seen_resource_paths=set()
file_types={'layout','drawable','menu','color','mipmap','anim','animator','xml','raw','font','navigation'}
for d in RES.iterdir():
    if not d.is_dir(): continue
    typ=d.name.split('-')[0]
    if typ in file_types:
        for p in d.iterdir():
            if p.is_file() and not p.name.startswith('.'):
                if not re.fullmatch(r'[a-z0-9_]+',p.stem): error(f'Invalid Android resource filename: {p.relative_to(ROOT)}')
                inventory[typ].add(p.stem);seen_resource_paths.add((typ,p.stem))
for p in xml_files:
    try: tree=ET.parse(p);root=tree.getroot()
    except Exception as e: error(f'XML parse failed {p.relative_to(ROOT)}: {e}');continue
    s=read(p)
    for bad,repl in [('android:hintTextColor=','android:textColorHint'),('android:tint=','app:tint'),('app:checkable=','setCheckable(true) in Java')]:
        if bad in s: error(f'{p.relative_to(ROOT)} contains forbidden {bad} (use {repl})')
    ids=[]
    for el in root.iter():
        rid=el.attrib.get(A+'id','')
        m=re.fullmatch(r'@\+id/([A-Za-z0-9_]+)',rid)
        if m: ids.append(m.group(1));inventory['id'].add(m.group(1))
        if el.tag.endswith('ImageButton'):
            if not el.attrib.get(A+'contentDescription','').strip(): error(f'{p.relative_to(ROOT)} ImageButton missing contentDescription')
            for attr in ('layout_width','layout_height'):
                v=el.attrib.get(A+attr,'');mm=re.fullmatch(r'(\d+)dp',v)
                if mm and int(mm.group(1))<48: error(f'{p.relative_to(ROOT)} ImageButton {attr} {v} <48dp')
    if p.parent.name.startswith('layout'):
        dup=[x for x,c in collections.Counter(ids).items() if c>1]
        if dup: error(f'{p.relative_to(ROOT)} duplicate IDs: {dup}')
    if p.parent.name.startswith('values'):
        for el in root:
            typ=el.tag;name=el.attrib.get('name')
            if typ=='item': typ=el.attrib.get('type')
            if name and typ: inventory[typ].add(name)
checks['xml files']=len(xml_files)

# XML local resource references must exist.
ref_pattern=re.compile(r'@(?!android:)([a-zA-Z_][\w]*)/([A-Za-z0-9_.]+)')
# Only validate resource kinds whose complete namespace is owned by this project.
# Material/AppCompat styles such as @style/Widget.MaterialComponents.* are supplied by AAR dependencies.
local_ref_types={'id','layout','drawable','color','menu','string','mipmap','anim','animator','xml','raw','font','navigation'}
for p in xml_files:
    s=read(p)
    for typ,name in ref_pattern.findall(s):
        if typ=='+id' or typ not in local_ref_types: continue
        if typ=='id':
            if name not in inventory['id']: error(f'{p.relative_to(ROOT)} references missing @id/{name}')
        elif name not in inventory.get(typ,set()):
            error(f'{p.relative_to(ROOT)} references missing @{typ}/{name}')
checks['resource refs']=sum(1 for p in xml_files for _ in ref_pattern.findall(read(p)))

# 3. Manifest component classes and referenced resources
manifest=ET.parse(APP/'src/main/AndroidManifest.xml').getroot()
for el in manifest.iter():
    for value in el.attrib.values():
        m=re.fullmatch(r'@(\w+)/([A-Za-z0-9_]+)',value)
        if m and m.group(1) in inventory and m.group(2) not in inventory[m.group(1)]: error(f'Manifest missing @{m.group(1)}/{m.group(2)}')
for tag in ('activity','service','receiver','provider'):
    for el in manifest.findall('.//'+tag):
        name=el.attrib.get(A+'name','')
        if name.startswith('.'):
            jp=JAVA/(name[1:]+'.java')
            if not jp.exists(): error(f'Manifest {tag} class missing: {name}')
checks['manifest']=1

# 4. Java packages/classes, delimiter parser, all R refs
java_files=list(JAVA.glob('*.java')); java_text={p:read(p) for p in java_files}
for p,s in java_text.items():
    pm=re.search(r'^package\s+([\w.]+)\s*;',s,re.M)
    if not pm or pm.group(1)!='com.italiano2774.nativeapp': error(f'Bad package in {p.name}')
    pub=re.search(r'public\s+(?:final\s+|abstract\s+)?(?:class|interface|enum)\s+(\w+)',s)
    if pub and pub.group(1)!=p.stem: error(f'Public type/file mismatch {p.name}: {pub.group(1)}')
    if not balanced_java(s): error(f'Unbalanced Java delimiters: {p.name}')
all_java='\n'.join(java_text.values())
for typ,name in re.findall(r'(?<!android\.)\bR\.([A-Za-z_]\w*)\.([A-Za-z_]\w*)',all_java):
    if name not in inventory.get(typ,set()): error(f'Java references missing R.{typ}.{name}')
if 'setTextAllCaps(' in all_java: error('Invalid MaterialButton setTextAllCaps call remains; use setAllCaps')
checks['java files']=len(java_files)

# Same-class private-helper arity check. It masks comments/strings for locating
# calls, but counts arguments from the original source so commas inside string literals
# do not create false positives. Varargs helpers are skipped.
def arg_count(src):
    if not src.strip(): return 0
    depth=0;commas=0;state='code';quote='';i=0;has_token=False
    while i<len(src):
        c=src[i];n=src[i+1] if i+1<len(src) else ''
        if state=='line':
            if c=='\n': state='code'
        elif state=='block':
            if c=='*' and n=='/': state='code';i+=1
        elif state=='string':
            has_token=True
            if c=='\\': i+=1
            elif c==quote: state='code'
        else:
            if c=='/' and n=='/': state='line';i+=1
            elif c=='/' and n=='*': state='block';i+=1
            elif c in ('"',"'"): state='string';quote=c;has_token=True
            elif c in '([{': depth+=1;has_token=True
            elif c in ')]}': depth=max(0,depth-1);has_token=True
            elif c==',' and depth==0: commas+=1
            elif not c.isspace(): has_token=True
        i+=1
    return commas+1 if has_token or commas else 0

helper_decl=re.compile(r'\bprivate\s+(?:static\s+)?(?:final\s+)?[\w<>\[\],.? ]+\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w., ]+)?\s*\{')
for p,original in java_text.items():
    clean=strip_java(original);decl=collections.defaultdict(set);decl_open=set();varargs=set()
    for m in helper_decl.finditer(clean):
        name=m.group(1);raw_params=original[m.start(2):m.end(2)]
        if '...' in raw_params: varargs.add(name);continue
        decl[name].add(arg_count(raw_params));decl_open.add(clean.find('(',m.start(1),m.end(2)+1))
    for name,arities in decl.items():
        if name in varargs: continue
        pat=re.compile(r'(?<![.\w])'+re.escape(name)+r'\s*\(')
        for cmatch in pat.finditer(clean):
            openpos=clean.find('(',cmatch.start(),cmatch.end()+1)
            if openpos in decl_open: continue
            # Exclude control keywords / constructors and member-qualified calls are already excluded by regex.
            depth=1;i=openpos+1
            while i<len(clean) and depth:
                if clean[i]=='(': depth+=1
                elif clean[i]==')': depth-=1
                i+=1
            if depth: error(f'{p.name}: unterminated call {name}(...)');continue
            argc=arg_count(original[openpos+1:i-1])
            if argc not in arities: error(f'{p.name}: private helper {name}(...) has {argc} args, declared arities={sorted(arities)}')
checks['method arity files']=len(java_files)

# Fragment single-layout R.id consistency.
for p,s in java_text.items():
    lays=sorted(set(re.findall(r'inflate\(R\.layout\.([A-Za-z0-9_]+)',s)))
    if len(lays)!=1: continue
    lp=RES/'layout'/(lays[0]+'.xml')
    if not lp.exists(): continue
    root=ET.parse(lp).getroot();local=set()
    for el in root.iter():
        rid=el.attrib.get(A+'id','');m=re.fullmatch(r'@\+id/([A-Za-z0-9_]+)',rid)
        if m: local.add(m.group(1))
    refs=set(re.findall(r'R\.id\.([A-Za-z0-9_]+)',s))
    # menu navigation ids can legitimately appear in Fragments; only flag ids that are neither layout-local nor globally menu ids.
    menu_ids=set()
    for mp in (RES/'menu').glob('*.xml'):
        for el in ET.parse(mp).getroot().iter():
            rid=el.attrib.get(A+'id','');m=re.fullmatch(r'@\+id/([A-Za-z0-9_]+)',rid)
            if m: menu_ids.add(m.group(1))
    missing=sorted(refs-local-menu_ids)
    if missing: error(f'{p.name} inflates {lays[0]} but references IDs not in layout: {missing}')
checks['fragment layout/id']=1

# 5. JSON + words/audio integrity
required_assets=['words.json','word_quality_v22.json','word_quality_v25.json','word_quality_v26.json','word_families.json','frequent_phrases.json','preposition_exercises.json','writing_prompts.json','sentence_patterns.json','core_sentences.json','listening_courses.json','course_curriculum.json','translation_quality_v311.json','course_translation_quality_v312.json','course_translation_quality_v315.json','lexical_semantic_quality_v319.json','lexical_alignment_quality_v320.json','english_bridges.json','morphology_hints.json','memory_chunks.json','memory_articles.json']
parsed={}
for name in required_assets:
    p=ASSETS/name
    if not p.exists(): error(f'MISSING asset: {name}');continue
    try: parsed[name]=json.load(open(p,encoding='utf-8'))
    except Exception as e: error(f'Invalid JSON {name}: {e}')
if 'words.json' in parsed:
    words=parsed['words.json']
    if len(words)!=2774: error(f'words.json rows={len(words)}, expected 2774')
    ids=[w.get('id') for w in words]
    if ids!=list(range(1,2775)): error('word IDs must be unique contiguous 1..2774 in file order')
    names=[w.get('localAudio') for w in words]
    if any(not isinstance(x,str) or not x for x in names): error('Every word must have non-empty localAudio')
    if len(names)!=len(set(names)): error('Duplicate localAudio names in words.json')
    audio=ASSETS/'audio';mp3=sorted(audio.glob('*.mp3')) if audio.exists() else []
    if len(mp3)!=2774: error(f'Audio count={len(mp3)}, expected 2774')
    actual={p.name for p in mp3};expected=set(names)
    miss=sorted(expected-actual);extra=sorted(actual-expected)
    if miss: error(f'Missing audio files: {miss[:8]}')
    if extra: error(f'Unexpected audio files: {extra[:8]}')
    for p in mp3:
        if p.stat().st_size<=0: error(f'Zero-byte MP3: {p.name}');continue
        head=p.read_bytes()[:4096]
        looks=head.startswith(b'ID3') or any(head[i]==0xff and i+1<len(head) and (head[i+1]&0xe0)==0xe0 for i in range(max(0,len(head)-1)))
        if not looks: error(f'MP3 signature not recognized: {p.name}')
    checks['mp3']=len(mp3)
for p in ROOT.rglob('*'):
    if p.is_file() and p.stat().st_size==0: error(f'Zero-byte project file: {p.relative_to(ROOT)}')
checks['json assets']=len(parsed)

# Data minimums.
minimums={'word_families.json':200,'frequent_phrases.json':400,'preposition_exercises.json':45,'writing_prompts.json':15,'sentence_patterns.json':27,'core_sentences.json':400,'listening_courses.json':18}
for name,n in minimums.items():
    if name in parsed and len(parsed[name])<n: error(f'{name} rows {len(parsed[name])} < {n}')
if 'listening_courses.json' in parsed:
    sent=sum(len(x.get('sentences',[])) for x in parsed['listening_courses.json'])
    if sent<90: error(f'listening sentences {sent} < 90')

# v3.0 guided-course integrity: 98 contiguous units, four stages, every word exactly once.
if 'course_curriculum.json' in parsed:
    course_doc=parsed['course_curriculum.json']
    course=course_doc.get('units',[]) if isinstance(course_doc,dict) else course_doc
    if len(course)!=98: error(f'course_curriculum.json units {len(course)} != 98')
    stage_counts=collections.Counter(x.get('stage') for x in course)
    expected_stage_counts={'A0':8,'A1':24,'A2':30,'B1':36}
    if dict(stage_counts)!=expected_stage_counts: error(f'course stage counts unexpected: {dict(stage_counts)}')
    indexes=[x.get('index') for x in course]
    if indexes!=list(range(98)): error('course unit indexes must be contiguous 0..97 in file order')
    mapped=[]
    for u in course:
        lc=u.get('lessonCount')
        if not isinstance(lc,int) or not 5<=lc<=8: error(f"course {u.get('id')} lessonCount must be 5..8")
        ids=u.get('wordIds') or []
        if not ids: error(f"course {u.get('id')} has no wordIds")
        mapped.extend(ids)
    if len(mapped)!=2774 or len(set(mapped))!=2774 or set(mapped)!=set(range(1,2775)):
        error(f'course word mapping must contain each word ID 1..2774 exactly once (rows={len(mapped)}, unique={len(set(mapped))})')
    ids=[u.get('id') for u in course]
    if len(ids)!=len(set(ids)): error('course unit IDs must be unique')
    checks['guided course units']=len(course)

# v3.0.4 semantic safety + listening support/Italian-choice + system navigation safe-area contract.
engine_source=read(JAVA/'CourseLessonEngine.java');main_source=read(JAVA/'MainActivity.java');practice_source=read(JAVA/'PracticeFragment.java')
if 'ExampleQuality.isUsable' not in engine_source or 'clozeOptions' not in engine_source: error('v3.0.4 course semantic guard missing')
if not (JAVA/'ExampleQuality.java').exists(): error('v3.0.4 ExampleQuality.java missing')
if 'makeMeaningOptions' not in practice_source or 'feedbackAnswer()' not in practice_source or 'Set<String> labels' not in practice_source: error('v3.0.4 duplicate-choice/answer-field guard missing')
if '听音频，选择你听到的意大利语' not in engine_source or 'q.support=zh(w)' not in engine_source or 'q.answer=w.word' not in engine_source or 'q.options.addAll(italianOptions(w,u,rnd))' not in engine_source: error('v3.0.4 course listening contract missing')
if 'question.setText("🔊")' not in practice_source or 'hint.setText(safeChinese(current))' not in practice_source or 'bindAnswer(answers.get(i),options.get(i),options.get(i).word)' not in practice_source: error('v3.0.4 practice listening contract missing')
level_exam_source=read(JAVA/'LevelExamFragment.java')
if 'q.category="听音选词"' not in level_exam_source or 'q.answer=w.word' not in level_exam_source or 'opts.add(w.word)' not in level_exam_source: error('v3.0.4 level exam listening contract missing')
if 'applyFocusBottomInset' not in main_source or 'lastSystemBottomInset' not in main_source: error('v3.0.1 system navigation safe-area guard missing')
try:
    _words=json.load(open(ASSETS/'words.json',encoding='utf-8'))
    if any((w.get('example') or '').startswith('Oggi ripasso la parola «') or (w.get('example') or '').startswith("Ripeto l'espressione «") for w in _words): error('self-referential placeholder examples remain in words.json')
    _luna=next((w for w in _words if int(w.get('id',0))==52),None)
    if not _luna or _luna.get('example')!='Guardo la luna.': error('luna example was not corrected')
    _next=next((w for w in _words if int(w.get('id',0))==385),None)
    if not _next or _next.get('chinese')!='下一个；下一位' or _next.get('example')!='Qual è il prossimo treno?': error('il prossimo real-device regression not fixed')
    _sto=next((w for w in _words if int(w.get('id',0))==600),None)
    if not _sto or _sto.get('example')!='Sto cercando lavoro.': error('sto real-device regression not fixed')
    _photo=next((w for w in _words if int(w.get('id',0))==729),None)
    if not _photo or _photo.get('word')!='fotografia' or _photo.get('chinese')!='照片；摄影' or '地图' in _photo.get('chinese',''): error('v3.1.1 fotografia real-device translation regression not fixed')
    _tq=json.load(open(ASSETS/'translation_quality_v311.json',encoding='utf-8'))
    if _tq.get('version')!='3.1.1' or int(_tq.get('correctedCount',0))<131: error('v3.1.1 translation correction ledger is stale/incomplete')
    _compra=next((w for w in _words if int(w.get('id',0))==882),None)
    if not _compra or _compra.get('word')!='compra' or _compra.get('chinese')!='他/她/您买': error('v3.1.2 compra real-device translation regression not fixed')
    _ctq=json.load(open(ASSETS/'course_translation_quality_v312.json',encoding='utf-8'))
    if _ctq.get('version')!='3.1.2' or int(_ctq.get('correctedCount',0))<136 or int(_ctq.get('metadataFixCount',0))<29: error('v3.1.2 course translation ledger is stale/incomplete')
except Exception as e: error(f'v3.0.4 words semantic check failed: {e}')

# v3.0 user-facing/navigation source contract.
v3_required=[
    'CourseUnit.java','CourseCurriculumRepository.java','CourseQuestion.java','CourseLessonEngine.java',
    'CourseHomeFragment.java','CourseMapFragment.java','CourseLessonFragment.java','ProfileFragment.java'
]
for name in v3_required:
    if not (JAVA/name).exists(): error(f'MISSING v3 Java: {name}')
for name in ['fragment_course_home.xml','fragment_course_map.xml','fragment_course_lesson.xml','fragment_profile.xml']:
    if not (RES/'layout'/name).exists(): error(f'MISSING v3 layout: {name}')
menu_path=RES/'menu'/'menu_bottom.xml'
if menu_path.exists():
    menu_src=read(menu_path)
    try:
        menu_root=ET.parse(menu_path).getroot()
        menu_items=[el for el in menu_root if el.tag.endswith('item')]
        if len(menu_items)!=4: error(f'v3 bottom menu must have exactly 4 tabs, found {len(menu_items)}')
    except Exception as e: error(f'Cannot validate v3 bottom menu: {e}')
    if 'nav_vocabulary' in menu_src: error('v3 bottom menu must not expose nav_vocabulary')
main_src=java_text.get(JAVA/'MainActivity.java','')
for marker in ('CourseHomeFragment','CourseMapFragment','PracticeHubFragment','ProfileFragment','openCourseLesson','openCourseUnit'):
    if marker not in main_src: error(f'v3 MainActivity missing marker: {marker}')
engine_src=java_text.get(JAVA/'CourseLessonEngine.java','')
for marker in ('CourseQuestion.INTRO','CourseQuestion.MEANING','CourseQuestion.LISTEN','CourseQuestion.SPELL_HINT','CourseQuestion.ACTIVE'):
    if marker not in engine_src: error(f'v3 lesson engine missing progressive type: {marker}')
checks['v3 course contract']=1

# 6. Room migration SQL syntax smoke test and destructive-fallback guard.
dbfile=JAVA/'LearningDatabase.java';dbsrc=read(dbfile)
if 'version = 6' not in dbsrc: error('Room database version must be 6')
if 'fallbackToDestructiveMigration' in dbsrc: error('Destructive Room migration fallback is forbidden')
steps=[(int(a),int(b)) for a,b in re.findall(r'new Migration\((\d+),(\d+)\)',dbsrc)]
if steps!=[(1,2),(2,3),(3,4),(4,5),(5,6)]: error(f'Room migration chain unexpected: {steps}')
sqls=re.findall(r'db\.execSQL\("((?:\\.|[^"\\])*)"\);',dbsrc)
try:
    con=sqlite3.connect(':memory:')
    con.executescript('''
    CREATE TABLE study_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, createdAt INTEGER NOT NULL, itemType TEXT NOT NULL, itemId TEXT NOT NULL, dimension INTEGER NOT NULL, correct INTEGER NOT NULL, responseMs INTEGER NOT NULL, detail TEXT NOT NULL);
    CREATE INDEX index_study_events_createdAt ON study_events(createdAt);
    CREATE INDEX index_study_events_itemType_itemId ON study_events(itemType,itemId);
    ''')
    for raw in sqls:
        sql=bytes(raw,'utf-8').decode('unicode_escape') if '\\' in raw else raw
        con.execute(sql)
    con.commit()
    expected_tables={'study_events','custom_study_items','word_progress','daily_stats','error_records','sentence_progress','grammar_progress','skill_progress'}
    tables={r[0] for r in con.execute("SELECT name FROM sqlite_master WHERE type='table'")}
    missing=expected_tables-tables
    if missing: error(f'Room migration simulation missing tables: {sorted(missing)}')
    fk=list(con.execute('PRAGMA foreign_key_check'))
    if fk: error(f'Room migration foreign_key_check failed: {fk[:3]}')
    con.close();checks['room migration sql']=len(sqls)
except Exception as e: error(f'Room migration SQL simulation failed: {e}')

# 7. CI shell block syntax, and required hard build gate order.
# Extract YAML literal script blocks without a YAML dependency.
lines=cm.splitlines();blocks=[];i=0
while i<len(lines):
    if re.match(r'^\s+script:\s*\|\s*$',lines[i]):
        indent=len(lines[i])-len(lines[i].lstrip());i+=1;buf=[]
        while i<len(lines):
            raw=lines[i];cur=len(raw)-len(raw.lstrip()) if raw.strip() else indent+2
            if raw.strip() and cur<=indent: break
            cut=min(len(raw),indent+2);buf.append(raw[cut:] if len(raw)>=cut else '')
            i+=1
        blocks.append('\n'.join(buf)+'\n');continue
    i+=1
for idx,b in enumerate(blocks,1):
    with tempfile.NamedTemporaryFile('w',suffix='.sh',delete=False,encoding='utf-8') as f: f.write(b);name=f.name
    r=subprocess.run(['bash','-n',name],capture_output=True,text=True)
    Path(name).unlink(missing_ok=True)
    if r.returncode: error(f'codemagic script block {idx} shell syntax: {r.stderr.strip()}')
checks['codemagic shell blocks']=len(blocks)
order=[cm.find('python3 tools/preflight.py'),cm.find('python3 tools/regression_check.py'),cm.find('python3 tools/translation_quality_check.py'),cm.find('python3 tools/lexical_semantic_quality_check.py'),cm.find('python3 tools/full_lexicon_alignment_check.py'),cm.find('python3 tools/course_check.py'),cm.find('python3 tools/memory_articles_quality_check.py'),cm.find('python3 tools/course_translation_quality_check.py'),cm.find('python3 tools/person_semantics_check.py'),cm.find('python3 tools/exercise_quality_check.py'),cm.find('python3 tools/micro_grammar_quality_check.py'),cm.find('python3 tools/daily_speaking_quality_check.py'),cm.find('python3 tools/mastery_passport_quality_check.py'),cm.find('python3 tools/breakthrough_plan_quality_check.py'),cm.find('python3 tools/weak_word_story_quality_check.py'),cm.find('python3 tools/life_task_map_quality_check.py'),cm.find('python3 tools/personal_forgetting_model_quality_check.py'),cm.find('python3 tools/signing_config_check.py'),cm.find('python3 tools/release_gate.py'),cm.find('Verify permanent signing identity'),cm.find(':app:assembleRelease'),cm.find(':app:testDebugUnitTest'),cm.find(':app:lintRelease')]
if any(x<0 for x in order) or order!=sorted(order): error(f'Codemagic gate/build/test/lint order invalid: {order}')

# 8. Local-only/no accidental WebView or generative-AI SDK markers.
lower=all_java.lower()
for token in ('openai','chatgpt','generativeai','anthropic'):
    if token in lower: error(f'Unexpected generative-AI marker in Java: {token}')
if re.search(r'\bWebView\b',all_java): warn('WebView marker found; current app is intended to be native-only')
checks['local design']=1

print('Strict release gate summary:')
for k,v in checks.items(): print(f'  OK {k}: {v}')
for w in warnings: print('WARNING:',w)
# v3.1 smart-memory contract.
smart_session=read(JAVA/'StudySessionFragment.java');smart_repo=read(JAVA/'WordRepository.java');smart_store=read(JAVA/'ProgressStore.java');smart_home=read(JAVA/'CourseHomeFragment.java');smart_home_layout=read(RES/'layout/fragment_course_home.xml')
if not (JAVA/'SmartMemoryScheduler.java').exists(): error('v3.1 SmartMemoryScheduler.java missing')
if 'newSmartMemoryInstance' not in smart_session or 'recordSmartWordRating' not in smart_session or 'smartMemory' not in smart_session: error('v3.1 three-choice smart session missing')
if 'smartMemoryPlan' not in smart_repo: error('v3.1 smart memory queue missing')
if 'updateDimensionSchedule' not in smart_store: error('v3.1 smart-memory dimension scheduler missing')
if 'dimensionDueEpochDay' not in smart_store or 'priorityReviewDimension' not in smart_store or 'forgettingFactor' not in smart_store: error('v5.0.1 four-track forgetting scheduler missing')
if 'channelReview()' not in smart_session or 'dimensionNextDueDate' not in smart_session: error('v5.0.1 due-dimension review routing missing')
if 'button_smart_memory' not in smart_home_layout or 'openSmartMemory' not in smart_home: error('v3.1 smart-memory home entry missing')

if errors:
    print(f'RELEASE GATE FAILED: {len(errors)} error(s)')
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('RELEASE GATE OK')

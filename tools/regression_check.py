#!/usr/bin/env python3
from pathlib import Path
import re, sys, json, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
errors=[];warnings=[]

def err(msg): errors.append(msg)

def text(path): return path.read_text(encoding='utf-8')

# Core project shape
required=[
    'app/src/main/AndroidManifest.xml','app/build.gradle','settings.gradle','codemagic.yaml',
    'app/src/main/java/com/italiano2774/nativeapp/MainActivity.java',
    'app/src/main/java/com/italiano2774/nativeapp/TodayFragment.java',
    'app/src/main/java/com/italiano2774/nativeapp/StudySessionFragment.java',
    'app/src/main/java/com/italiano2774/nativeapp/OnboardingFragment.java',
    'app/src/main/java/com/italiano2774/nativeapp/SessionQualityEngine.java',
    'app/src/main/res/layout/fragment_onboarding.xml',
    'app/src/main/res/layout/fragment_today.xml',
    'app/src/main/res/layout/fragment_study_session.xml',
    'app/src/main/java/com/italiano2774/nativeapp/CourseUnit.java',
    'app/src/main/java/com/italiano2774/nativeapp/CourseCurriculumRepository.java',
    'app/src/main/java/com/italiano2774/nativeapp/CourseQuestion.java',
    'app/src/main/java/com/italiano2774/nativeapp/CourseLessonEngine.java',
    'app/src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java',
    'app/src/main/java/com/italiano2774/nativeapp/CourseMapFragment.java',
    'app/src/main/java/com/italiano2774/nativeapp/CourseLessonFragment.java',
    'app/src/main/java/com/italiano2774/nativeapp/ProfileFragment.java',
    'app/src/main/java/com/italiano2774/nativeapp/ExampleQuality.java',
    'app/src/main/res/layout/fragment_course_home.xml',
    'app/src/main/res/layout/fragment_course_map.xml',
    'app/src/main/res/layout/fragment_course_lesson.xml',
    'app/src/main/res/layout/fragment_profile.xml',
    'app/src/main/assets/course_curriculum.json',
    'tools/course_check.py',
    'tools/exercise_quality_check.py',
    'tools/translation_quality_check.py',
    'tools/daily_speaking_quality_check.py',
    'app/src/main/java/com/italiano2774/nativeapp/DailySpeakingChallengeEngine.java',
    'app/src/main/java/com/italiano2774/nativeapp/DailySpeakingChallengeFragment.java',
    'app/src/main/java/com/italiano2774/nativeapp/WeeklyExamEngine.java',
    'app/src/main/java/com/italiano2774/nativeapp/WeeklyExamFragment.java',
    'app/src/main/res/layout/fragment_weekly_exam.xml',
    'app/src/main/res/layout/fragment_daily_speaking.xml',
    'app/src/main/java/com/italiano2774/nativeapp/WeakWordStory.java',
    'app/src/main/java/com/italiano2774/nativeapp/WeakWordStoryEngine.java',
    'app/src/main/java/com/italiano2774/nativeapp/WeakWordStoryFragment.java',
    'app/src/main/res/layout/fragment_weak_word_story.xml',
    'tools/weak_word_story_quality_check.py',
    'app/src/main/java/com/italiano2774/nativeapp/LifeTask.java',
    'app/src/main/java/com/italiano2774/nativeapp/LifeTaskRepository.java',
    'app/src/main/java/com/italiano2774/nativeapp/LifeTaskEngine.java',
    'app/src/main/java/com/italiano2774/nativeapp/LifeTaskMapFragment.java',
    'app/src/main/java/com/italiano2774/nativeapp/LifeTaskAdapter.java',
    'app/src/main/java/com/italiano2774/nativeapp/LifeTaskDetailFragment.java',
    'app/src/main/res/layout/fragment_life_task_map.xml',
    'app/src/main/res/layout/item_life_task.xml',
    'app/src/main/res/layout/fragment_life_task_detail.xml',
    'tools/life_task_map_quality_check.py',
    'tools/personal_forgetting_model_quality_check.py',
    'app/src/main/java/com/italiano2774/nativeapp/PersonalForgettingModel.java',
    'app/src/main/java/com/italiano2774/nativeapp/ForgettingProfileFragment.java',
    'app/src/main/res/layout/fragment_forgetting_profile.xml',
    'app/src/main/assets/translation_quality_v311.json',
]
for rel in required:
    if not (ROOT/rel).exists(): err('missing '+rel)

# Parse XML and collect IDs/layouts
ids=set();layouts={p.stem for p in (ROOT/'app/src/main/res/layout').glob('*.xml')}
for p in (ROOT/'app/src/main/res').rglob('*.xml'):
    try: ET.parse(p)
    except Exception as e: err(f'bad XML {p.relative_to(ROOT)}: {e}')
    s=text(p);ids.update(re.findall(r'@\+id/([A-Za-z0-9_]+)',s))
    for bad in ('android:hintTextColor=','android:tint=','app:checkable='):
        if bad in s: err(f'legacy/invalid attribute {bad} in {p.relative_to(ROOT)}')

# Java delimiter/syntax smoke check (ignores strings and comments).
def java_delimiters_ok(src):
    stack=[];i=0;state='code';quote='';pairs={'}':'{',')':'(',']':'['}
    while i<len(src):
        c=src[i];n=src[i+1] if i+1<len(src) else ''
        if state=='line':
            if c=='\n': state='code'
        elif state=='block':
            if c=='*' and n=='/': state='code';i+=1
        elif state=='string':
            if c=='\\': i+=1
            elif c==quote: state='code'
        else:
            if c=='/' and n=='/': state='line';i+=1
            elif c=='/' and n=='*': state='block';i+=1
            elif c in ('\"',"'"): state='string';quote=c
            elif c in '{([': stack.append(c)
            elif c in '}])':
                if not stack or stack[-1]!=pairs[c]: return False
                stack.pop()
        i+=1
    return state not in ('block','string') and not stack

# Java layout/id smoke check
java_files=list((ROOT/'app/src/main/java').rglob('*.java'))
for jp in java_files:
    if not java_delimiters_ok(text(jp)): err('unbalanced Java delimiters '+str(jp.relative_to(ROOT)))
java='\n'.join(text(p) for p in java_files)
for layout in re.findall(r'(?<!android\.)R\.layout\.([A-Za-z0-9_]+)',java):
    if layout not in layouts: err('missing referenced layout '+layout)
# R.id includes menu ids too; collect all resource XML ids globally
for rid in re.findall(r'(?<!android\.)R\.id\.([A-Za-z0-9_]+)',java):
    if rid not in ids: err('missing referenced id '+rid)

# v2.9 key behavior smoke checks
checks={
 'first-run onboarding':'new OnboardingFragment()' in java,
 'session resume':'saveStudySession(' in java and 'loadStudySession(' in java,
 'fatigue detection':'SessionQualityEngine.isFatigued' in java,
 'audio preload':'audio.preload(' in java and 'public void preload(Word w)' in java,
 'font sizing':'font_scale_mode' in java and 'attachBaseContext' in java,
 'cross-session exposure dedup':'recentWordIds()' in java and 'markWordExposure(' in java,
 'state repair':'repairCorruptState(' in java,
 'upgrade snapshot':'5.0.1-preupgrade' in java and 'ensureVersionBackupThen' in java,
 'Room destructive fallback disabled':'fallbackToDestructiveMigration' not in java,
}
for name,ok in checks.items():
    if not ok: err('v2.9 behavior missing: '+name)

# v3.0 guided-course behavior
v3_checks={
 'guided home':'CourseHomeFragment.newInstance()' in java and 'openCourseLesson(' in java,
 'four-tab navigation':'nav_vocabulary' not in text(ROOT/'app/src/main/res/menu/menu_bottom.xml') and text(ROOT/'app/src/main/res/menu/menu_bottom.xml').count('<item ')==4,
 'A0-A1-A2-B1 curriculum':'course_curriculum.json' in java and 'CourseCurriculumRepository' in java,
 'short progressive lessons':'CourseQuestion.INTRO' in java and 'CourseQuestion.LISTEN' in java and 'CourseQuestion.ACTIVE' in java,
 'course resume':'saveCourseResume(' in java and 'hasCourseResume(' in java,
 'placement advances path':'advanceFromPlacement' in java and 'advanceCourseUnlockedUnit' in java,
 'advanced tools folded':'container_advanced_practice' in text(ROOT/'app/src/main/res/layout/fragment_practice_hub.xml'),
 'beginner profile':'new ProfileFragment()' in java,
}
for name,ok in v3_checks.items():
    if not ok: err('v3.0 behavior missing: '+name)

# Keep the app local/non-generative-AI by construction.
lower=java.lower()
for token in ('openai','chatgpt','gemini api','generativeai','anthropic'):
    if token in lower: err('unexpected AI/cloud SDK marker: '+token)

# Dataset/audio smoke checks
assets=ROOT/'app/src/main/assets'
try:
    words=json.load(open(assets/'words.json',encoding='utf-8'))
    if len(words)!=2774: err(f'expected 2774 words, found {len(words)}')
    ids_list=[w.get('id') for w in words]
    if len(ids_list)!=len(set(ids_list)): err('duplicate word IDs in words.json')
    audio=assets/'audio'
    mp3=list(audio.glob('*.mp3'))
    if len(mp3)!=2774: err(f'expected 2774 mp3, found {len(mp3)}')
except Exception as e: err('dataset check failed: '+str(e))

try:
    course=json.load(open(assets/'course_curriculum.json',encoding='utf-8'))
    units=course.get('units',[])
    if len(units)!=98: err(f'expected 98 course units, found {len(units)}')
    counts={k:sum(1 for u in units if u.get('stage')==k) for k in ('A0','A1','A2','B1')}
    if counts!={'A0':8,'A1':24,'A2':30,'B1':36}: err('course stage counts wrong: '+str(counts))
    all_ids=[wid for u in units for wid in u.get('wordIds',[])]
    if len(all_ids)!=2774 or sorted(all_ids)!=list(range(1,2775)): err('course curriculum must cover each word ID exactly once')
    if any(not (5<=int(u.get('lessonCount',0))<=8) for u in units): err('course lessonCount must be 5..8')
except Exception as e: err('course curriculum check failed: '+str(e))

# Minimum touch target for image buttons (common lint regression).
ANDROID='{http://schemas.android.com/apk/res/android}'
for p in (ROOT/'app/src/main/res/layout').glob('*.xml'):
    try: root=ET.parse(p).getroot()
    except Exception: continue
    for el in root.iter():
        if el.tag.endswith('ImageButton'):
            for attr in ('layout_width','layout_height'):
                v=el.attrib.get(ANDROID+attr,'')
                m=re.fullmatch(r'(\d+)dp',v)
                if m and int(m.group(1))<48: err(f'{p.name}: ImageButton {attr} {v} < 48dp')

build=text(ROOT/'app/build.gradle') if (ROOT/'app/build.gradle').exists() else ''
if "versionName '5.0.1-native'" not in build: err('versionName is not 4.9.0-native')
if 'def defaultVersionCode = 81' not in build or 'versionCode resolvedVersionCode' not in build: err('v5.0.1 versionCode update-chain config missing')
if "applicationId 'com.italiano2774.nativeapp'" not in build: err('applicationId changed; direct Android update chain would break')
if 'signingConfig signingConfigs.release' not in build or 'CM_KEYSTORE_PATH' not in build: err('permanent release signing config missing')
cm=text(ROOT/'codemagic.yaml') if (ROOT/'codemagic.yaml').exists() else ''
if 'python3 tools/java_source_syntax_check.py' not in cm: err('v3.3.8 Java source literal check step missing')
if 'v5.0.1' not in cm or 'android_signing:' not in cm or '- zhongxue_release' not in cm or ':app:assembleRelease' not in cm or 'app-release.apk' not in cm: err('v5.0.1 Codemagic signed-release update chain missing')
passport=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/MasteryPassportEngine.java')
passport_ui=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/MasteryPassportFragment.java')
main_for_passport=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
if 'ACTION_REAL_USE' not in passport or 'checkpoint("A1"' not in passport or 'checkpoint("B1"' not in passport: err('v5.0.1 mastery passport engine missing')
if 'openMasteryPassport' not in main_for_passport or 'bindCheckpoint' not in passport_ui: err('v5.0.1 mastery passport navigation/UI missing')
breakthrough=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/BreakthroughPlanEngine.java')
if 'threeDayText' not in breakthrough or 'todayTask' not in breakthrough or 'focusSkillKey' not in passport: err('v5.0.1 three-day breakthrough engine missing')
if ':app:assembleDebug' in cm or 'app-debug.apk' in cm: err('official Codemagic workflow must not publish debug APK after v3.1.5')

engine=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/CourseLessonEngine.java')
main=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
if 'ExampleQuality.isUsable' not in engine or 'clozeOptions' not in engine: err('v3.0.4 semantic question guard missing')
if 'reviewDue(' in engine or 'addDueReview' in engine: err('v5.0.1 fixed course still injects cross-unit due-review words')
if 'addSeenReinforcement' not in engine: err('v5.0.1 same-unit course reinforcement missing')
if 'applyFocusBottomInset' not in main or 'lastSystemBottomInset' not in main: err('v3.0.1 bottom system navigation inset guard missing')

practice=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/PracticeFragment.java')
if 'makeMeaningOptions' not in practice or 'feedbackAnswer()' not in practice: err('v3.0.4 meaning-choice uniqueness/feedback guard missing')
if '听音频，选择你听到的意大利语' not in engine or 'q.support=zh(w)' not in engine or 'q.answer=w.word' not in engine or 'q.options.addAll(italianOptions(w,u,rnd))' not in engine: err('v3.0.4 course listening must show Chinese support and keep Italian choices')
if 'question.setText("🔊")' not in practice or 'hint.setText(safeChinese(current))' not in practice or 'bindAnswer(answers.get(i),options.get(i),options.get(i).word)' not in practice: err('v3.0.4 practice listening must show Chinese hint with Italian choices')
level_exam=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/LevelExamFragment.java')
if 'q.category="听音选词"' not in level_exam or 'q.answer=w.word' not in level_exam or 'opts.add(w.word)' not in level_exam: err('v3.0.4 level exam listening must show Chinese support with Italian choices')
smart_session=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/StudySessionFragment.java')
smart_repo=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/WordRepository.java')
smart_store=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java')
smart_home=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java')
smart_layout=text(ROOT/'app/src/main/res/layout/fragment_course_home.xml')
if 'newSmartMemoryInstance' not in smart_session or 'smartMemory' not in smart_session or 'recordSmartWordRating' not in smart_session: err('v3.1 smart-memory three-choice session missing')
if 'smartMemoryPlan' not in smart_repo or 'updateDimensionSchedule' not in smart_store: err('v3.1 smart-memory dimension scheduling missing')
if 'dimensionDueEpochDay' not in smart_store or 'priorityReviewDimension' not in smart_store or 'forgettingFactor' not in smart_store: err('v5.0.1 four-track forgetting scheduler missing')
if 'channelReview()' not in smart_session or 'dimensionNextDueDate' not in smart_session: err('v5.0.1 due-dimension review routing missing')
beginner_guide=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/BeginnerGuideEngine.java')
beginner_home=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java')
beginner_hub=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/PracticeHubFragment.java')
if 'coachLine' not in beginner_guide or 'practiceUnlocked' not in beginner_guide: err('v5.0.1 beginner guide engine missing')
if 'button_home_details_toggle' not in beginner_home or 'progress.homeSimpleMode()' not in beginner_home: err('v5.0.1 compact home/detail toggle missing')
if 'bindGuided' not in beginner_hub or 'BeginnerGuideEngine.active' not in beginner_hub: err('v5.0.1 progressive practice unlock missing')
if 'button_smart_memory' not in smart_layout or 'openSmartMemory' not in smart_home: err('v3.1 smart-memory home entry missing')
if not (ROOT/'app/src/main/java/com/italiano2774/nativeapp/SmartMemoryScheduler.java').exists(): err('v3.1 SmartMemoryScheduler.java missing')
course_translation_asset=ROOT/'app/src/main/assets/course_translation_quality_v312.json'
if not course_translation_asset.exists(): err('v3.1.2 course translation ledger missing')
else:
    try:
        _ct=json.load(open(course_translation_asset,encoding='utf-8'))
        if _ct.get('version')!='3.1.2' or int(_ct.get('correctedCount',0))<136: err('v3.1.2 course translation ledger incomplete')
        _compra=next((w for w in words if int(w.get('id',0))==882),None)
        if not _compra or _compra.get('chinese')!='他/她/您买': err('v3.1.2 compra translation regression')
    except Exception as e: err('v3.1.2 course translation check failed: '+str(e))

person_asset=ROOT/'app/src/main/assets/course_translation_quality_v315.json'
if not person_asset.exists(): err('v3.1.5 person-semantics ledger missing')
else:
    _marco=next((w for w in words if int(w.get('id',0))==887),None)
    if not _marco or _marco.get('chinese')!='我标记；我做记号' or _marco.get('lemma')!='marcare': err('v3.1.5 marco translation/morphology regression')
if 'python3 tools/person_semantics_check.py' not in cm: err('v3.1.5 Codemagic person-semantics check missing')

phrase_asset=ROOT/'app/src/main/assets/phrase_semantic_quality_v316.json'
if not phrase_asset.exists(): err('v3.1.6 phrase semantic ledger missing')
else:
    try:
        _ps=json.load(open(phrase_asset,encoding='utf-8'))
        if _ps.get('version')!='3.1.6' or _ps.get('scannedUniquePairs')!=430 or int(_ps.get('fixCount',0))<9: err('v3.1.6 phrase semantic ledger incomplete')
    except Exception as e: err('v3.1.6 phrase semantic ledger failed: '+str(e))
if 'python3 tools/phrase_semantic_quality_check.py' not in cm: err('v3.1.6 Codemagic phrase semantic check missing')
memory_morph=ROOT/'app/src/main/assets/morphology_hints.json'
memory_chunks=ROOT/'app/src/main/assets/memory_chunks.json'
if not memory_morph.exists() or not memory_chunks.exists(): err('v3.1.8 memory aid assets missing')
_smart_engine_path=ROOT/'app/src/main/java/com/italiano2774/nativeapp/SmartReviewModeEngine.java'
smart_engine=_smart_engine_path.read_text(encoding='utf-8') if _smart_engine_path.exists() else ''
if 'MODE_ZH_IT' not in smart_session or '中 → 意主动回忆' not in smart_engine: err('v3.3 Chinese-to-Italian active recall missing')
if 'MemoryAidRepository.MorphologyHint' not in smart_session or 'MemoryAidRepository.MemoryChunk' not in smart_session: err('v3.1.8 smart-memory aids not wired')
if 'python3 tools/memory_aid_quality_check.py' not in cm: err('v3.1.8 Codemagic memory-aid quality check missing')
if 'python3 tools/lexical_semantic_quality_check.py' not in cm: err('v3.1.9 lexical semantic regression check missing')
if 'python3 tools/full_lexicon_alignment_check.py' not in cm: err('v3.2.1 full 2774-word alignment quality check missing')
if 'python3 tools/full_retranslation_quality_check.py' not in cm: err('v3.2.1 full independent retranslation quality check missing')
if 'python3 tools/word_example_quality_check.py' not in cm: err('v3.2.2 word example quality check missing')
if 'python3 tools/word_example_expansion_check.py' not in cm: err('v3.3.3 high-frequency example expansion check missing')
if not (ROOT/'app/src/main/assets/word_example_expansion_v333.json').exists(): err('v3.3.3 example expansion ledger missing')
if not (ROOT/'app/src/main/assets/word_example_expansion_v332.json').exists(): err('v3.3.2 historical example expansion ledger missing')
if not (ROOT/'tools/word_examples_v333.tsv').exists(): err('v3.3.3 reviewed example TSV missing')
if 'python3 tools/smart_mastery_quality_check.py' not in cm: err('v3.3.3 smart mastery quality check missing')
if 'python3 tools/teach_before_test_quality_check.py' not in cm: err('v3.3.3 teach-before-test quality check missing')
if 'python3 tools/course_path_mix_quality_check.py' not in cm: err('v3.3.4 mixed-skill guided path quality check missing')
if 'python3 tools/course_unit_isolation_quality_check.py' not in cm: err('v5.0.1 fixed-course unit isolation quality check missing')
if 'python3 tools/memory_articles_quality_check.py' not in cm: err('v3.3.8 ten-article / 2000-word quality check missing')
if 'python3 tools/memory_article_recurrence_quality_check.py' not in cm: err('v3.3.8 spiral recurrence quality check missing')
if 'python3 tools/memory_article_sentence_quality_check.py' not in cm: err('v3.3.8 sentence-by-sentence article quality check missing')
if 'python3 tools/listening_speaking_quality_check.py' not in cm: err('v5.0.1 listening/speaking bridge quality check missing')
if 'python3 tools/scenario_conversation_quality_check.py' not in cm: err('v5.0.1 scenario difficulty quality check missing')
if 'python3 tools/micro_grammar_quality_check.py' not in cm: err('v5.0.1 micro grammar quality check missing')
if 'python3 tools/daily_smart_plan_quality_check.py' not in cm: err('v5.0.1 daily smart plan quality check missing')
if 'python3 tools/daily_speaking_quality_check.py' not in cm: err('v5.0.1 daily five-sentence speaking quality check missing')
if 'python3 tools/mastery_passport_quality_check.py' not in cm: err('v5.0.1 mastery passport quality check missing')
if 'python3 tools/breakthrough_plan_quality_check.py' not in cm: err('v5.0.1 breakthrough prescription quality check missing')
if 'python3 tools/daily_learning_loop_quality_check.py' not in cm: err('v5.0.1 daily learning-loop quality check missing')
if 'python3 tools/review_forecast_quality_check.py' not in cm: err('v5.0.1 seven-day consolidation forecast quality check missing')
if 'meaning(w,unit,rnd,true)' not in engine or 'teachBeforeTest' not in engine: err('v3.3.3 learning vocabulary is not marked teach-before-test')
course_fragment=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/CourseLessonFragment.java')
if '先看一遍翻译' not in course_fragment or '我看过了 · 开始选择' not in course_fragment: err('v3.3.3 translation-first meaning-choice UI missing')
for _mix in ['词汇起步','听力训练','句子理解','主动表达','单元挑战','pathSummary']:
    if _mix not in engine: err('v3.3.4 mixed course path missing '+_mix)
if 'pathSummary' not in smart_home or '听、说、读、写会由系统自动穿插' not in smart_home: err('v5.0.1 compact course-home mixed-skill explanation missing')
memory_asset=ROOT/'app/src/main/assets/memory_articles.json'
if not memory_asset.exists(): err('v3.3.8 memory_articles.json missing')
else:
    try:
        _ma=json.load(open(memory_asset,encoding='utf-8'))
        _ids=[wid for a in _ma for wid in a.get('targetWordIds',[])]
        if len(_ma)!=10 or sum(len(a.get('sections',[])) for a in _ma)!=50 or len(_ids)!=2000 or set(_ids)!=set(range(1,2001)): err('v3.3.8 ten-article target coverage must be 10 articles / 50 sections / IDs 1..2000')
    except Exception as e: err('v3.3.8 memory article asset failed: '+str(e))
for _m in ['MemoryArticleSentence.java','MemoryArticleReinforcement.java','MemoryArticleRepository.java','MemoryArticleListFragment.java','MemoryArticleDetailFragment.java','MemoryArticleStudyFragment.java','MemoryArticleSentenceStudyFragment.java']:
    if not (ROOT/'app/src/main/java/com/italiano2774/nativeapp'/_m).exists(): err('v3.3.8 missing '+_m)
if 'openMemoryArticles' not in main or 'openMemoryArticleSentenceStudy' not in main or 'newArticleReviewInstance' not in smart_session or 'memoryArticleDone' not in smart_store or 'memoryArticleExposure' not in smart_store or 'memoryArticleSentenceDone' not in smart_store or 'recordMemoryArticleExposure' not in smart_store: err('v3.3.8 ten-article navigation/review/sentence-backup contract missing')

bridge_asset=ROOT/'app/src/main/assets/english_bridges.json'
if not bridge_asset.exists(): err('v3.1.7 English bridge asset missing')
else:
    try:
        _eb=json.load(open(bridge_asset,encoding='utf-8'))
        if _eb.get('version')!='3.1.7' or int(_eb.get('cognateCount',0))<190 or int(_eb.get('falseFriendCount',0))<15: err('v3.1.7 English bridge ledger incomplete')
    except Exception as e: err('v3.1.7 English bridge ledger failed: '+str(e))
if 'python3 tools/english_bridge_quality_check.py' not in cm: err('v3.1.7 Codemagic English bridge quality check missing')



# v4.4 persisted error-evidence repair loop
err_frag=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/ErrorEvidenceRepairFragment.java')
err_dao=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/LearningStateDao.java')
err_db=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/LearningDatabase.java')
if 'markMatchingErrorRepaired' not in err_frag or 'unresolvedPracticeErrors' not in err_dao: err('v5.0.1 personal error-evidence repair loop missing')
if 'version = 6' not in err_db or 'MIGRATION_5_6' not in err_db: err('v5.0.1 Room v6 repair-state migration missing')
if 'python3 tools/error_evidence_quality_check.py' not in cm: err('v5.0.1 error-evidence Codemagic gate missing')
if 'python3 tools/weekly_exam_quality_check.py' not in cm: err('v5.0.1 weekly practical-exam Codemagic gate missing')
if 'python3 tools/weak_word_story_quality_check.py' not in cm: err('v5.0.1 weak-word micro-reading Codemagic gate missing')
if 'python3 tools/life_task_map_quality_check.py' not in cm: err('v5.0.1 real-life task-map Codemagic gate missing')
if 'python3 tools/personal_forgetting_model_quality_check.py' not in cm: err('v5.0.1 personal forgetting model Codemagic gate missing')
if 'python3 tools/beginner_home_quality_check.py' not in cm: err('v5.0.1 beginner-first home Codemagic gate missing')
weak_story_engine=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/WeakWordStoryEngine.java')
weak_story_fragment=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/WeakWordStoryFragment.java')
if 'rankedWeakWords' not in weak_story_engine or 'fillFromExamples' not in weak_story_engine or 'buildClozeTargets' not in weak_story_engine: err('v5.0.1 weak-word micro-reading engine missing')
if 'recordAuxiliaryResult("weak_story"' not in weak_story_fragment or 'recordEmbeddedDimensionResults' not in weak_story_fragment or 'score>=60' not in weak_story_fragment: err('v5.0.1 weak-word read/listen/cloze/retell feedback loop missing')
life_repo=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/LifeTaskRepository.java')
life_engine=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/LifeTaskEngine.java')
life_detail=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/LifeTaskDetailFragment.java')
life_dialogue=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/DialogueTrainingFragment.java')
life_plan=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java')
if life_repo.count('new LifeTask(')!=12: err('v5.0.1 task map must define 12 real-life missions')
for _m in ['passLine(int level)','completedStages(ProgressStore p)','nextTask(ProgressStore p)','unlocked(ProgressStore p,LifeTask task,int level)']:
    if _m not in life_engine: err('v5.0.1 task progression engine missing '+_m)
for _m in ['LifeTaskEngine.unlocked','openLifeTaskStage','先通过上一关']:
    if _m not in life_detail: err('v5.0.1 sequential task unlock missing '+_m)
if 'recordAuxiliaryResult("life_task"' not in life_dialogue or 'newInstance(String id,int level,String lifeTaskId)' not in life_dialogue: err('v5.0.1 life-task dialogue result bridge missing')
if 'LifeTaskEngine.nextTask(progress)' not in life_plan or '"life_task"' not in life_plan: err('v5.0.1 daily/weekly life-task scheduling missing')
weekly_engine=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/WeeklyExamEngine.java') if (ROOT/'app/src/main/java/com/italiano2774/nativeapp/WeeklyExamEngine.java').exists() else ''
weekly_fragment=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/WeeklyExamFragment.java') if (ROOT/'app/src/main/java/com/italiano2774/nativeapp/WeeklyExamFragment.java').exists() else ''
weekly_store=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java')
weekly_plan=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java')
for _m in ['TOTAL_QUESTIONS=18','QUESTIONS_PER_SKILL=3','ACTION_REAL_USE']:
    if _m not in weekly_engine: err('v5.0.1 weekly exam engine missing '+_m)
for _m in ['SpeechRecognizer','saveWeeklyExamResult','recordAuxiliaryResult("weekly_exam"']:
    if _m not in weekly_fragment: err('v5.0.1 weekly exam feedback loop missing '+_m)
for _m in ['weeklyExamDue()','weeklyAdjustmentSummary()','weeklyNewWordPercent()']:
    if _m not in weekly_store: err('v5.0.1 weekly diagnosis persistence missing '+_m)
for _m in ['weeklyFocusTask','weekly_exam','addIfFitsUniqueAction']:
    if _m not in weekly_plan: err('v5.0.1 weekly diagnosis daily scheduling missing '+_m)


if errors:
    for x in errors: print('ERROR:',x)
    sys.exit(1)
for x in warnings: print('WARNING:',x)
print('Regression OK: startup, resources, v3.0.4 meaningful exercises, Chinese listening support + Italian choices, unique visible choices, system-nav safe area, dataset/audio and touch targets')

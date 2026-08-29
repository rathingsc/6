#!/usr/bin/env python3
from pathlib import Path
import json,re,sys,xml.etree.ElementTree as ET,zipfile
ROOT=Path(__file__).resolve().parents[1]
errors=[];warnings=[]
def req(p):
    if not p.exists(): errors.append(f'MISSING: {p.relative_to(ROOT)}')
manifest=ROOT/'app/src/main/AndroidManifest.xml';req(manifest);req(ROOT/'app/build.gradle');req(ROOT/'settings.gradle');req(ROOT/'codemagic.yaml')
# XML parse + resource ids
ids=set();layouts=set();drawables=set();colors=set();menus=set();strings=set()
res=ROOT/'app/src/main/res'
for p in res.rglob('*.xml'):
    try: ET.parse(p)
    except Exception as e: errors.append(f'XML invalid {p.relative_to(ROOT)}: {e}')
    text=p.read_text(encoding='utf-8')
    if 'app:checkable=' in text:
        errors.append(f'Unsupported app:checkable attribute in {p.relative_to(ROOT)}; set Chip checkable state in Java instead')
    if 'android:hintTextColor=' in text:
        errors.append(f'Invalid android:hintTextColor in {p.relative_to(ROOT)}; use android:textColorHint')
    if 'android:tint=' in text:
        errors.append(f'AppCompat lint incompatibility in {p.relative_to(ROOT)}: replace android:tint with app:tint')
    ids.update(re.findall(r'@\+id/([A-Za-z0-9_]+)',text))
    if '<ImageButton' in text:
        for tag in re.findall(r'<ImageButton\b[^>]*>',text,re.S):
            for attr in ('layout_width','layout_height'):
                m=re.search(r'android:'+attr+r'="(\d+)dp"',tag)
                if m and int(m.group(1))<48: errors.append(f'ImageButton touch target <48dp in {p.relative_to(ROOT)}: {attr}={m.group(1)}dp')
for p in (res/'layout').glob('*.xml'): layouts.add(p.stem)
for p in (res/'drawable').glob('*'): drawables.add(p.stem)
for p in (res/'color').glob('*.xml'): colors.add(p.stem)
for p in (res/'menu').glob('*.xml'): menus.add(p.stem)
# values names
for p in (res/'values').glob('*.xml'):
    t=p.read_text(encoding='utf-8');colors.update(re.findall(r'<color\s+name="([^"]+)"',t));strings.update(re.findall(r'<string\s+name="([^"]+)"',t))
# JSON assets
assets=ROOT/'app/src/main/assets'
required_assets=['words.json','word_quality_v22.json','word_quality_v25.json','word_quality_v26.json','word_families.json','frequent_phrases.json','preposition_exercises.json','writing_prompts.json','sentence_patterns.json','core_sentences.json','listening_courses.json','course_curriculum.json','translation_quality_v311.json','english_bridges.json','morphology_hints.json','memory_chunks.json','memory_articles.json']
for name in required_assets:
    req(assets/name)
for ap in assets.glob('*.json'):
    try: json.load(open(ap,encoding='utf-8'))
    except Exception as e: errors.append(f'JSON invalid {ap.name}: {e}')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
words=json.load(open(assets/'words.json',encoding='utf-8'))
if len(words)!=2774: errors.append(f'words.json expected 2774 rows, got {len(words)}')
audio=assets/'audio';req(audio)
mp3=list(audio.glob('*.mp3')) if audio.exists() else []
if len(mp3)!=2774: errors.append(f'audio expected 2774 mp3, got {len(mp3)}')
missing=[x.get('localAudio') for x in words if x.get('localAudio') and not (audio/x['localAudio']).exists()]
if missing: errors.append(f'{len(missing)} localAudio files missing, first={missing[:5]}')
# Java resource reference checks
java='\n'.join(p.read_text(encoding='utf-8') for p in (ROOT/'app/src/main/java').rglob('*.java'))

# Catch simple helper-method arity mistakes that otherwise only appear at javac time.
def _split_java_args(src):
    args=[];cur='';in_str=False;esc=False;depth=0
    for ch in src:
        if in_str:
            cur+=ch
            if esc: esc=False
            elif ch=='\\': esc=True
            elif ch=='"': in_str=False
        else:
            if ch=='"': in_str=True;cur+=ch
            elif ch in '([{': depth+=1;cur+=ch
            elif ch in ')]}': depth-=1;cur+=ch
            elif ch==',' and depth==0: args.append(cur.strip());cur=''
            else: cur+=ch
    if cur.strip(): args.append(cur.strip())
    return args
for jp in (ROOT/'app/src/main/java').rglob('*.java'):
    jt=jp.read_text(encoding='utf-8')
    sigs=re.findall(r'\b(?:private|public|protected)\s+(?:static\s+)?(?:void|[\w<>\[\], ?]+)\s+add\s*\(([^)]*)\)',jt)
    if not sigs: continue
    arities={0 if not s.strip() else len(_split_java_args(s)) for s in sigs}
    for lineno,line in enumerate(jt.splitlines(),1):
        s=line.strip()
        if s.startswith('add(') and s.endswith(');'):
            argc=len(_split_java_args(s[4:-2]))
            if argc not in arities:
                errors.append(f'Java helper arity mismatch {jp.relative_to(ROOT)}:{lineno}: add(...) has {argc} args, expected {sorted(arities)}')
if 'setTextAllCaps(' in java: errors.append('Java contains invalid setTextAllCaps(...) call; use setAllCaps(...)')
for typ,known in [('id',ids),('layout',layouts),('drawable',drawables),('color',colors),('menu',menus),('string',strings)]:
    refs=set(re.findall(r'(?<!android\.)R\.'+typ+r'\.([A-Za-z0-9_]+)',java))
    miss=sorted(refs-known)
    if miss: errors.append(f'Missing R.{typ} resources: {miss[:20]}')
# required v2.7 files
for f in ['SentenceDictationFragment.java','ErrorCauseAnalyzer.java','WordProgressEntity.java','ErrorRecordEntity.java','DailyStatEntity.java','LearningStateDao.java','WordFamilyFragment.java','WordFamilyRepository.java','VerbConjugator.java','VerbCenterFragment.java','GrammarMapFragment.java','SmartClozeEngine.java','SmartClozeFragment.java','ReadingComprehensibilityEngine.java','IntensiveListeningFragment.java','StubbornWordsFragment.java','PhraseRepository.java','PhraseFragment.java','PrepositionRepository.java','PrepositionFragment.java','PastTenseFragment.java','PronounFragment.java','WordFamilyTrainingFragment.java','WeeklyReportFragment.java','LocalBackupManager.java','WritingFragment.java','WritingEvaluator.java','SentenceReviewFragment.java','SentenceFsrsRepository.java','SentenceProgressEntity.java','GrammarProgressEntity.java','GrammarFsrs.java','StudyTimeTracker.java','ListeningCourseFragment.java','ListeningCourseRepository.java','PronunciationMapFragment.java','ActiveRecallFragment.java','CoreSentenceFragment.java','CoreSentenceRepository.java','FocusModeFragment.java','SkillProgressEntity.java']:
    req(ROOT/'app/src/main/java/com/italiano2774/nativeapp'/f)
# required v2.8 stability/performance files
for f in ['DailySummaryFragment.java','WrongWordRepairFragment.java','DatabaseHealthFragment.java','DatabaseHealthManager.java','LocalErrorLog.java','DailySpeakingChallengeEngine.java','DailySpeakingChallengeFragment.java','MasteryPassportEngine.java','MasteryPassportFragment.java','WeeklyExamEngine.java','WeeklyExamFragment.java','WeakWordStory.java','WeakWordStoryEngine.java','WeakWordStoryFragment.java']:
    req(ROOT/'app/src/main/java/com/italiano2774/nativeapp'/f)
for f in ['fragment_daily_summary.xml','fragment_wrong_word_repair.xml','fragment_database_health.xml','fragment_daily_speaking.xml','fragment_mastery_passport.xml','fragment_weekly_exam.xml','fragment_weak_word_story.xml']:
    req(ROOT/'app/src/main/res/layout'/f)
# required v2.9 quality/fluency files
for f in ['OnboardingFragment.java','SessionQualityEngine.java']:
    req(ROOT/'app/src/main/java/com/italiano2774/nativeapp'/f)
for f in ['fragment_onboarding.xml']:
    req(ROOT/'app/src/main/res/layout'/f)
# required v3.0 beginner-first guided course
for f in ['CourseUnit.java','CourseCurriculumRepository.java','CourseQuestion.java','CourseLessonEngine.java','CourseHomeFragment.java','CourseMapFragment.java','CourseLessonFragment.java','ProfileFragment.java','ExampleQuality.java']:
    req(ROOT/'app/src/main/java/com/italiano2774/nativeapp'/f)
for f in ['fragment_course_home.xml','fragment_course_map.xml','fragment_course_lesson.xml','fragment_profile.xml']:
    req(ROOT/'app/src/main/res/layout'/f)
req(ROOT/'app/src/main/assets/course_curriculum.json')
req(ROOT/'tools/regression_check.py')
req(ROOT/'tools/course_check.py')
req(ROOT/'tools/exercise_quality_check.py')
req(ROOT/'tools/translation_quality_check.py')
req(ROOT/'tools/course_translation_quality_check.py')
req(ROOT/'tools/word_example_quality_check.py')
req(ROOT/'app/src/main/assets/word_example_quality_v322.json')
req(ROOT/'tools/word_example_expansion_check.py')
req(ROOT/'app/src/main/assets/word_example_expansion_v332.json')
req(ROOT/'app/src/main/assets/word_example_expansion_v333.json')
req(ROOT/'tools/word_examples_v333.tsv')
req(ROOT/'tools/apply_word_examples_v333.py')
req(ROOT/'app/src/main/assets/course_translation_quality_v312.json')
req(ROOT/'app/src/main/assets/course_translation_quality_v315.json')
req(ROOT/'tools/person_semantics_check.py')
req(ROOT/'tools/phrase_semantic_quality_check.py')
req(ROOT/'tools/english_bridge_quality_check.py')
req(ROOT/'tools/memory_aid_quality_check.py')
req(ROOT/'tools/teach_before_test_quality_check.py')
req(ROOT/'tools/course_path_mix_quality_check.py')
req(ROOT/'tools/course_unit_isolation_quality_check.py')
req(ROOT/'tools/memory_articles_quality_check.py')
req(ROOT/'app/src/main/assets/memory_articles.json')
for f in ['MemoryArticle.java','MemoryArticleSection.java','MemoryArticleSentence.java','MemoryArticleReinforcement.java','MemoryArticleRepository.java','MemoryArticleListFragment.java','MemoryArticleDetailFragment.java','MemoryArticleStudyFragment.java','MemoryArticleSentenceStudyFragment.java']:
    req(ROOT/'app/src/main/java/com/italiano2774/nativeapp'/f)
for f in ['fragment_memory_article_list.xml','fragment_memory_article_detail.xml','fragment_memory_article_study.xml','fragment_memory_article_sentence_study.xml']:
    req(ROOT/'app/src/main/res/layout'/f)
req(ROOT/'app/src/main/java/com/italiano2774/nativeapp/MemoryAidRepository.java')
req(ROOT/'app/src/main/assets/morphology_hints.json')
req(ROOT/'app/src/main/assets/memory_chunks.json')
req(ROOT/'app/src/main/assets/english_bridges.json')
req(ROOT/'app/src/main/assets/phrase_semantic_quality_v316.json')
req(ROOT/'tools/java_source_syntax_check.py')
req(ROOT/'tools/release_gate.py')
req(ROOT/'tools/micro_grammar_quality_check.py')
req(ROOT/'tools/daily_smart_plan_quality_check.py')
req(ROOT/'tools/daily_speaking_quality_check.py')
req(ROOT/'tools/mastery_passport_quality_check.py')
req(ROOT/'tools/breakthrough_plan_quality_check.py')
req(ROOT/'tools/error_evidence_quality_check.py')
req(ROOT/'tools/weekly_exam_quality_check.py')
req(ROOT/'tools/shadowing_training_quality_check.py')
req(ROOT/'tools/weak_word_story_quality_check.py')
req(ROOT/'tools/life_task_map_quality_check.py')
req(ROOT/'tools/personal_forgetting_model_quality_check.py')
req(ROOT/'tools/beginner_home_quality_check.py')
req(ROOT/'app/src/main/java/com/italiano2774/nativeapp/PersonalForgettingModel.java')
req(ROOT/'app/src/main/java/com/italiano2774/nativeapp/ForgettingProfileFragment.java')
req(ROOT/'app/src/main/res/layout/fragment_forgetting_profile.xml')
req(ROOT/'tools/signing_config_check.py')
req(ROOT/'tools/memory_article_recurrence_quality_check.py')
req(ROOT/'tools/memory_article_sentence_quality_check.py')
req(ROOT/'覆盖升级与签名说明.txt')
req(ROOT/'signing-certificate-sha256.txt')
if not (ROOT/'.gitignore').exists():
    warnings.append('.gitignore is absent from the GitHub checkout; build will continue because signing secrets are validated separately')
# v3.0 repository synchronization checks. Run before Gradle so stale mixed-source uploads fail early.
cm_text=(ROOT/'codemagic.yaml').read_text(encoding='utf-8')
if 'v5.0.1' not in cm_text or 'python3 tools/java_source_syntax_check.py' not in cm_text or 'python3 tools/translation_quality_check.py' not in cm_text or 'python3 tools/lexical_semantic_quality_check.py' not in cm_text or 'python3 tools/full_lexicon_alignment_check.py' not in cm_text or 'python3 tools/full_retranslation_quality_check.py' not in cm_text or 'python3 tools/word_example_quality_check.py' not in cm_text or 'python3 tools/word_example_expansion_check.py' not in cm_text or 'python3 tools/course_translation_quality_check.py' not in cm_text or 'python3 tools/phrase_semantic_quality_check.py' not in cm_text or 'python3 tools/english_bridge_quality_check.py' not in cm_text or 'python3 tools/memory_aid_quality_check.py' not in cm_text or 'python3 tools/smart_mastery_quality_check.py' not in cm_text or 'python3 tools/teach_before_test_quality_check.py' not in cm_text or 'python3 tools/course_path_mix_quality_check.py' not in cm_text or 'python3 tools/course_unit_isolation_quality_check.py' not in cm_text or 'python3 tools/memory_articles_quality_check.py' not in cm_text or 'python3 tools/memory_article_recurrence_quality_check.py' not in cm_text or 'python3 tools/memory_article_sentence_quality_check.py' not in cm_text or 'python3 tools/listening_speaking_quality_check.py' not in cm_text or 'python3 tools/scenario_conversation_quality_check.py' not in cm_text or 'python3 tools/person_semantics_check.py' not in cm_text or 'python3 tools/exercise_quality_check.py' not in cm_text or 'python3 tools/micro_grammar_quality_check.py' not in cm_text or 'python3 tools/daily_smart_plan_quality_check.py' not in cm_text or 'python3 tools/daily_speaking_quality_check.py' not in cm_text or 'python3 tools/mastery_passport_quality_check.py' not in cm_text or 'python3 tools/breakthrough_plan_quality_check.py' not in cm_text or 'python3 tools/error_evidence_quality_check.py' not in cm_text or 'python3 tools/weekly_exam_quality_check.py' not in cm_text or 'python3 tools/shadowing_training_quality_check.py' not in cm_text or 'python3 tools/weak_word_story_quality_check.py' not in cm_text or 'python3 tools/life_task_map_quality_check.py' not in cm_text or 'python3 tools/personal_forgetting_model_quality_check.py' not in cm_text or 'python3 tools/beginner_home_quality_check.py' not in cm_text or 'python3 tools/daily_learning_loop_quality_check.py' not in cm_text or 'python3 tools/review_forecast_quality_check.py' not in cm_text or 'python3 tools/signing_config_check.py' not in cm_text or 'python3 tools/release_gate.py' not in cm_text or 'android_signing:' not in cm_text or '- zhongxue_release' not in cm_text or ':app:assembleRelease' not in cm_text:
    errors.append('codemagic.yaml is stale/incompletely uploaded: v5.0.1 weak-word story + weekly diagnosis + daily close-loop + permanent-signing release workflow is required')
grammar_diag=(ROOT/'app/src/main/java/com/italiano2774/nativeapp/GrammarDiagnosisFragment.java')
req(grammar_diag)
if grammar_diag.exists():
    gd=grammar_diag.read_text(encoding='utf-8')
    if 'setTextAllCaps(' in gd:
        errors.append('GrammarDiagnosisFragment.java is stale: setTextAllCaps(false) must be setAllCaps(false)')
    if 'setAllCaps(false)' not in gd:
        errors.append('GrammarDiagnosisFragment.java synchronization marker missing: expected setAllCaps(false)')
build_text=(ROOT/'app/build.gradle').read_text(encoding='utf-8')
if "versionName '5.0.1-native'" not in build_text: errors.append('v5.0.1 versionName missing or app/build.gradle was not fully overwritten')
if 'def defaultVersionCode = 81' not in build_text or 'versionCode resolvedVersionCode' not in build_text: errors.append('v5.0.1 dynamic/fallback versionCode configuration missing')
if "applicationId 'com.italiano2774.nativeapp'" not in build_text: errors.append('permanent update package id changed')
if 'signingConfig signingConfigs.release' not in build_text or 'CM_KEYSTORE_PATH' not in build_text: errors.append('v5.0.1 permanent release signing config missing')
if 'fallbackToDestructiveMigration' in (ROOT/'app/src/main/java/com/italiano2774/nativeapp/LearningDatabase.java').read_text(encoding='utf-8'): errors.append('v2.8 must not use destructive Room fallback')
if 'DiffUtil' not in (ROOT/'app/src/main/java/com/italiano2774/nativeapp/WordAdapter.java').read_text(encoding='utf-8'): warnings.append('WordAdapter DiffUtil optimization missing')

# v3.0.4 beginner-course and exercise-quality regressions
lesson_engine=(ROOT/'app/src/main/java/com/italiano2774/nativeapp/CourseLessonEngine.java').read_text(encoding='utf-8')
main_activity=(ROOT/'app/src/main/java/com/italiano2774/nativeapp/MainActivity.java').read_text(encoding='utf-8')
if 'ExampleQuality.isUsable' not in lesson_engine or 'clozeOptions' not in lesson_engine: errors.append('v3.0.4 semantic-safe course question generation missing')
practice_src=(ROOT/'app/src/main/java/com/italiano2774/nativeapp/PracticeFragment.java').read_text(encoding='utf-8')
if 'makeMeaningOptions' not in practice_src or 'feedbackAnswer()' not in practice_src: errors.append('v3.0.4 duplicate-choice/answer-field guard missing')
if any((w.get('example') or '').startswith('Oggi ripasso la parola «') or (w.get('example') or '').startswith("Ripeto l'espressione «") for w in words): errors.append('self-referential placeholder examples remain in words.json')
if 'applyFocusBottomInset' not in main_activity or 'lastSystemBottomInset' not in main_activity: errors.append('v3.0.1 navigation-bar safe-area handling missing')
if any(w.get('exampleZh')=='天气预报提到了这种天气或自然现象。' for w in words): errors.append('stale generic weather examples remain in words.json')
luna=next((w for w in words if int(w.get('id',0))==52),None)
if not luna or luna.get('example')!='Guardo la luna.' or luna.get('exampleZh')!='我看着月亮。': errors.append('luna beginner example regression: expected Guardo la luna.')

quality=json.load(open(assets/'word_quality_v22.json',encoding='utf-8'))
if quality.get('rowsWithCuratedExamples',0)<100: warnings.append('word quality refinement is unexpectedly small')
quality25=json.load(open(assets/'word_quality_v25.json',encoding='utf-8'))
if quality25.get('thirdRoundCorrections',0)<50: errors.append('v2.5 third-round word corrections expected >=50')
families=json.load(open(assets/'word_families.json',encoding='utf-8'))
if len(families)<200: errors.append(f'word_families.json expected >=200 groups, got {len(families)}')

phrases=json.load(open(assets/'frequent_phrases.json',encoding='utf-8'))
if len(phrases)<400: errors.append(f'frequent_phrases.json expected >=400 rows, got {len(phrases)}')
preps=json.load(open(assets/'preposition_exercises.json',encoding='utf-8'))
if len(preps)<45: errors.append(f'preposition_exercises.json expected >=45 rows, got {len(preps)}')

quality26=json.load(open(assets/'word_quality_v26.json',encoding='utf-8'))
if quality26.get('curatedExamples',0)<60: errors.append('v2.6 curated example refinements expected >=60')
writing=json.load(open(assets/'writing_prompts.json',encoding='utf-8'))
if len(writing)<15: errors.append(f'writing_prompts.json expected >=15 prompts, got {len(writing)}')
patterns=json.load(open(assets/'sentence_patterns.json',encoding='utf-8'))
if len(patterns)<27: errors.append(f'sentence_patterns.json expected >=27 patterns, got {len(patterns)}')
core=json.load(open(assets/'core_sentences.json',encoding='utf-8'))
if len(core)<400: errors.append(f'core_sentences.json expected >=400 rows, got {len(core)}')
listen=json.load(open(assets/'listening_courses.json',encoding='utf-8'))
if len(listen)<18: errors.append(f'listening_courses.json expected >=18 lessons, got {len(listen)}')
listen_sent=sum(len(x.get('sentences',[])) for x in listen)
if listen_sent<90: errors.append(f'listening course sentences expected >=90, got {listen_sent}')
course=json.load(open(assets/'course_curriculum.json',encoding='utf-8'))
units=course.get('units',[])
if len(units)!=98: errors.append(f'course_curriculum.json expected 98 units, got {len(units)}')
stage_counts={k:sum(1 for u in units if u.get('stage')==k) for k in ('A0','A1','A2','B1')}
if stage_counts!={'A0':8,'A1':24,'A2':30,'B1':36}: errors.append(f'course stage counts invalid: {stage_counts}')
course_ids=[wid for u in units for wid in u.get('wordIds',[])]
if len(course_ids)!=2774 or sorted(course_ids)!=list(range(1,2775)): errors.append('course curriculum must contain every word ID exactly once')
if any(not (5<=int(u.get('lessonCount',0))<=8) for u in units): errors.append('course curriculum lessonCount must be 5..8')
menu=(ROOT/'app/src/main/res/menu/menu_bottom.xml').read_text(encoding='utf-8')
if menu.count('<item ')!=4 or 'nav_vocabulary' in menu: errors.append('v3.0 bottom navigation must have exactly 学习/课程/练习/我的 four tabs')
print(f'Preflight: {len(words)} words, {len(mp3)} audio, {len(list(res.rglob("*.xml")))} XML files, {len(units)} guided units, {len(families)} word families, {len(patterns)} grammar patterns, {len(phrases)} phrases, {len(core)} core sentences, {len(listen)} listening lessons/{listen_sent} sentences, {len(preps)} preposition drills, {len(writing)} writing prompts')
for w in warnings: print('WARNING:',w)
# v3.1 smart-memory contract
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
RES=ROOT/'app/src/main/res'
smart_session=(JAVA/'StudySessionFragment.java').read_text(encoding='utf-8')
smart_repo=(JAVA/'WordRepository.java').read_text(encoding='utf-8')
smart_store=(JAVA/'ProgressStore.java').read_text(encoding='utf-8')
course_home=(JAVA/'CourseHomeFragment.java').read_text(encoding='utf-8')
if not (JAVA/'SmartMemoryScheduler.java').exists(): errors.append('v3.1 SmartMemoryScheduler.java missing')
if 'newSmartMemoryInstance' not in smart_session or 'recordSmartWordRating' not in smart_session: errors.append('v3.1 smart study session wiring missing')
if 'smartMemoryPlan' not in smart_repo: errors.append('v3.1 smart memory plan missing')
if 'recordSmartWordRating' not in smart_store or 'updateDimensionSchedule' not in smart_store or 'priorityReviewDimension' not in smart_store: errors.append('v4.9 smart four-track rating persistence missing')
if 'button_smart_memory' not in (RES/'layout/fragment_course_home.xml').read_text(encoding='utf-8') or 'openSmartMemory' not in course_home: errors.append('v3.1 smart memory home entry missing')
if not (JAVA/'SmartReviewModeEngine.java').exists() or 'MODE_CLOZE' not in (JAVA/'SmartReviewModeEngine.java').read_text(encoding='utf-8'): errors.append('v3.3 four-mode SmartReviewModeEngine missing')
if 'setupChoices' not in smart_session or 'button_report_word_issue' not in smart_session: errors.append('v3.3 smart four-mode/issue-report wiring missing')
if 'smartOverallPct' not in smart_store or 'smartMastered' not in smart_store: errors.append('v3.3 four-dimensional mastery persistence missing')
if not (JAVA/'IssueReportStore.java').exists(): errors.append('v3.3 local issue-report queue missing')

if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Preflight OK')

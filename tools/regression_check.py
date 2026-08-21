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
 'upgrade snapshot':'3.1.0-preupgrade' in java and 'ensureVersionBackupThen' in java,
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
if "versionName '3.1.0-native'" not in build: err('versionName is not 3.1.0-native')
if 'versionCode 41' not in build: err('versionCode is not 41')

engine=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/CourseLessonEngine.java')
main=text(ROOT/'app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
if 'ExampleQuality.isUsable' not in engine or 'clozeOptions' not in engine: err('v3.0.4 semantic question guard missing')
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
if 'smartMemoryPlan' not in smart_repo or 'SmartMemoryScheduler.nextInterval' not in smart_store: err('v3.1 smart-memory scheduling missing')
if 'button_smart_memory' not in smart_layout or 'openSmartMemory' not in smart_home: err('v3.1 smart-memory home entry missing')
if not (ROOT/'app/src/main/java/com/italiano2774/nativeapp/SmartMemoryScheduler.java').exists(): err('v3.1 SmartMemoryScheduler.java missing')

if errors:
    for x in errors: print('ERROR:',x)
    sys.exit(1)
for x in warnings: print('WARNING:',x)
print('Regression OK: startup, resources, v3.0.4 meaningful exercises, Chinese listening support + Italian choices, unique visible choices, system-nav safe area, dataset/audio and touch targets')

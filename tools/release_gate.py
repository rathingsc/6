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
for rel in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','codemagic.yaml','app/src/main/AndroidManifest.xml','.gitignore','覆盖升级与签名说明.txt','tools/signing_config_check.py','signing-certificate-sha256.txt']:
    require(rel)
checks['root files']=10
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
app_gradle=read(APP/'build.gradle');root_gradle=read(ROOT/'build.gradle');cm=read(ROOT/'codemagic.yaml')
for needle,msg in [
    ('def defaultVersionCode = 44','v3.1.3 local fallback versionCode must be 44'),
    ('versionCode resolvedVersionCode','dynamic versionCode support missing'),
    ("versionName '3.1.3-native'",'versionName must be 3.1.3-native'),
    ("applicationId 'com.italiano2774.nativeapp'",'applicationId update identity changed'),
    ('signingConfig signingConfigs.release','release signingConfig missing'),
    ('CM_KEYSTORE_PATH','Codemagic release signing env wiring missing'),
    ('compileSdk 35','compileSdk must be 35'),('targetSdk 35','targetSdk must be 35'),('minSdk 26','minSdk must be 26'),
    ('JavaVersion.VERSION_17','Java source/target must be 17')]:
    if needle not in app_gradle: error(msg)
if "com.android.application' version '8.9.2'" not in root_gradle: error('AGP 8.9.2 declaration missing')
if '--gradle-version 8.11.1' not in cm: error('Codemagic must generate Gradle 8.11.1 wrapper')
if 'java: 17' not in cm: error('Codemagic JDK 17 declaration missing')
if 'python3 tools/course_check.py' not in cm: error('Codemagic guided course semantic check step missing')
if 'python3 tools/exercise_quality_check.py' not in cm: error('Codemagic exercise quality check step missing')
if 'python3 tools/translation_quality_check.py' not in cm: error('Codemagic translation quality check step missing')
if 'python3 tools/course_translation_quality_check.py' not in cm: error('Codemagic course translation quality check step missing')
if 'python3 tools/release_gate.py' not in cm: error('Codemagic strict release gate step missing')
if 'v3.1.3' not in cm: error('Codemagic workflow title must identify v3.1.3')
if 'android_signing:' not in cm or '- zhongxue_release' not in cm: error('Codemagic permanent signing identity zhongxue_release missing')
if ':app:assembleRelease' not in cm or 'app/build/outputs/apk/release/app-release.apk' not in cm: error('Codemagic must build/export signed release APK')
if ':app:assembleDebug' in cm or 'app-debug.apk' in cm: error('Codemagic official workflow must not publish debug APK after v3.1.3')
if 'python3 tools/signing_config_check.py' not in cm: error('Codemagic permanent signing config gate missing')
if '-PversionCode="$UPDATE_VERSION_CODE"' not in cm: error('Codemagic monotonic versionCode injection missing')
checks['build config']=1
require('app/src/main/assets/translation_quality_v311.json')
require('app/src/main/assets/course_translation_quality_v312.json')
require('tools/translation_quality_check.py')
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
required_assets=['words.json','word_quality_v22.json','word_quality_v25.json','word_quality_v26.json','word_families.json','frequent_phrases.json','preposition_exercises.json','writing_prompts.json','sentence_patterns.json','core_sentences.json','listening_courses.json','course_curriculum.json','translation_quality_v311.json','course_translation_quality_v312.json']
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
if 'version = 5' not in dbsrc: error('Room database version must be 5')
if 'fallbackToDestructiveMigration' in dbsrc: error('Destructive Room migration fallback is forbidden')
steps=[(int(a),int(b)) for a,b in re.findall(r'new Migration\((\d+),(\d+)\)',dbsrc)]
if steps!=[(1,2),(2,3),(3,4),(4,5)]: error(f'Room migration chain unexpected: {steps}')
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
order=[cm.find('python3 tools/preflight.py'),cm.find('python3 tools/regression_check.py'),cm.find('python3 tools/translation_quality_check.py'),cm.find('python3 tools/course_check.py'),cm.find('python3 tools/course_translation_quality_check.py'),cm.find('python3 tools/exercise_quality_check.py'),cm.find('python3 tools/signing_config_check.py'),cm.find('python3 tools/release_gate.py'),cm.find('Verify permanent signing identity'),cm.find(':app:assembleRelease'),cm.find(':app:testDebugUnitTest'),cm.find(':app:lintRelease')]
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
if 'SmartMemoryScheduler.nextInterval' not in smart_store: error('v3.1 smart-memory interval ladder missing')
if 'button_smart_memory' not in smart_home_layout or 'openSmartMemory' not in smart_home: error('v3.1 smart-memory home entry missing')

if errors:
    print(f'RELEASE GATE FAILED: {len(errors)} error(s)')
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('RELEASE GATE OK')

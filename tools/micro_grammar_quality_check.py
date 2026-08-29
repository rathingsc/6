#!/usr/bin/env python3
"""v5.0.1 gate for the beginner-first micro-grammar route."""
from pathlib import Path
import json, re, sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def err(x): errors.append(x)
def text(rel): return (ROOT/rel).read_text(encoding='utf-8')
patterns=json.loads(text('app/src/main/assets/sentence_patterns.json'))
ids=[p.get('id','') for p in patterns]
if len(patterns)!=27: err(f'expected 27 sentence patterns, got {len(patterns)}')
if len(set(ids))!=len(ids): err('duplicate sentence pattern ids')
for p in patterns:
    if not p.get('title') or not p.get('formula') or not p.get('explanation'): err('incomplete pattern '+p.get('id','?'))
    ex=p.get('exercises') or []
    if len(ex)!=3: err(f"{p.get('id')} must have exactly 3 micro exercises, got {len(ex)}")
    for i,e in enumerate(ex):
        for k in ('prompt','answer','it','zh'):
            if not str(e.get(k,'')).strip(): err(f"{p.get('id')} exercise {i+1} missing {k}")
java=text('app/src/main/java/com/italiano2774/nativeapp/MicroGrammarFragment.java')
all_group=[]
for name in ('A1','A2','B1'):
    m=re.search(r'private static final String\[\] '+name+r'=\{([^}]*)\}',java)
    if not m: err('missing '+name+' micro grammar group'); continue
    got=re.findall(r'"([^"]+)"',m.group(1));all_group.extend(got)
if set(all_group)!=set(ids):
    err('A1/A2/B1 micro grammar groups must cover exactly all 27 sentence patterns')
for marker in ['今天先学：','20秒看懂 · 做3题','到期复习','基本稳定','openMicroGrammarLesson','错题会自动进入语法间隔复习']:
    if marker not in java: err('MicroGrammarFragment missing '+marker)
main=text('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java')
for marker in ['openMicroGrammar()','openMicroGrammarLesson(String patternId)','newMicroInstance(patternId)']:
    if marker not in main: err('MainActivity missing '+marker)
hub=text('app/src/main/java/com/italiano2774/nativeapp/PracticeHubFragment.java')
hubxml=text('app/src/main/res/layout/fragment_practice_hub.xml')
if 'button_simple_micro_grammar' not in hub or 'openMicroGrammar()' not in hub: err('practice hub micro grammar navigation missing')
if 'button_simple_micro_grammar' not in hubxml or '微语法实战' not in hubxml: err('practice hub micro grammar button missing')
sp=text('app/src/main/java/com/italiano2774/nativeapp/SentencePatternFragment.java')
for marker in ['newMicroInstance','microMode','微语法 · 3题实战','只学这一个点','完成 · 返回微语法','openMicroGrammar()']:
    if marker not in sp: err('micro lesson flow missing '+marker)
today=text('app/src/main/java/com/italiano2774/nativeapp/TodayFragment.java')
if 'case "grammar":a.openMicroGrammarLesson(n.payload);break;' not in today: err('daily path grammar node must open micro grammar lesson')
build=text('app/build.gradle')
if 'def defaultVersionCode = 81' not in build or "versionName '5.0.1-native'" not in build: err('v5.0.1 version identity missing')
if errors:
    print('Micro grammar quality check FAILED')
    for x in errors: print('ERROR:',x)
    sys.exit(1)
print('Micro grammar quality check OK: 27 points, 81 exercises, A1/A2/B1 route, daily-path integration and beginner UI verified')

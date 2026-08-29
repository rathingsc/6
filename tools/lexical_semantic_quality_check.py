#!/usr/bin/env python3
"""v3.1.9 full-word-list semantic correction gate."""
from pathlib import Path
import json, sys

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
WORDS=ASSETS/'words.json'
LEDGER=ASSETS/'lexical_semantic_quality_v319.json'
errors=[]

def err(msg): errors.append(msg)

def load(path):
    try:
        return json.load(open(path,encoding='utf-8'))
    except Exception as e:
        err(f'cannot load {path.relative_to(ROOT)}: {e}')
        return None

words=load(WORDS) or []
ledger=load(LEDGER) or {}
if len(words)!=2774: err(f'words.json rows={len(words)}, expected 2774')
if ledger.get('version')!='3.1.9': err('lexical semantic ledger version must be 3.1.9')
if ledger.get('scannedWords')!=2774: err('lexical semantic ledger must record 2774 scanned words')
corrections=ledger.get('corrections') or []
if ledger.get('correctionCount')!=len(corrections): err('ledger correctionCount does not match corrections list')
if len(corrections)<123: err(f'expected at least 123 locked lexical corrections, found {len(corrections)}')

by_id={int(w.get('id')):w for w in words if isinstance(w,dict) and str(w.get('id','')).isdigit()}
for item in corrections:
    wid=int(item.get('id',0))
    w=by_id.get(wid)
    if not w:
        err(f'ledger word id {wid} missing from words.json'); continue
    if w.get('word')!=item.get('word'): err(f'id {wid} word mismatch: {w.get("word")!r} != {item.get("word")!r}')
    if w.get('chinese')!=item.get('chinese'): err(f'id {wid} Chinese gloss regressed: {w.get("chinese")!r}')
    if w.get('english')!=item.get('english'): err(f'id {wid} English gloss regressed: {w.get("english")!r}')

# Critical learner-facing regressions that motivated the v3.1.9 audit.
critical={
    1423: ('castro', ('城堡','要塞'), ('医生',)),
    1455: ('bracciale', ('手镯','臂环'), ('乐队',)),
    382: ('lagna', ('抱怨','牢骚'), ('牛肉',)),
    2703: ('destino', ('命运',), ('醒来',)),
    2761: ('governo', ('政府',), ('跑；看；寻找',)),
    2662: ('finanziamento', ('融资','资金','资助'), ('经济的；金融的',)),
    1215: ('misterioso', ('神秘',), ('深的',)),
    2518: ('stile', ('风格','样式'), ('寻找；看',)),
}
for wid,(word,good,bad) in critical.items():
    w=by_id.get(wid,{})
    zh=str(w.get('chinese',''))
    if w.get('word')!=word: err(f'critical id {wid} expected {word}, got {w.get("word")}')
    if not any(x in zh for x in good): err(f'{word} lacks expected Chinese meaning: {zh}')
    if any(x in zh for x in bad): err(f'{word} contains known wrong Chinese meaning: {zh}')

known_bad={
    'castro':['医生'],
    'bracciale':['乐队'],
    'lagna':['牛肉'],
    'lagno':['牛肉'],
    'misterioso':['深的'],
    'finanziamento':['经济的；金融的'],
    'destino':['醒来'],
    'anima':['来；向上；声音'],
    'attacco':['去；店铺；合适'],
    'governo':['跑；看；寻找'],
    'complesso':['她的；她'],
    'stile':['寻找；看'],
    'immaginazione':['花哨的'],
    'nota':['向下；做；制造'],
    'scrivendo':['向下；其他的'],
    'mangiando':['去；向上'],
}
by_word={w.get('word'):w for w in words if isinstance(w,dict)}
for word,bads in known_bad.items():
    w=by_word.get(word)
    if not w: err(f'known audited word missing: {word}'); continue
    zh=str(w.get('chinese',''))
    for bad in bads:
        if bad in zh: err(f'known nonsense gloss returned: {word} -> {zh}')

# Copies embedded in word_families must not show stale meanings.
families=load(ASSETS/'word_families.json') or []
locked={(int(x['id']),x['word']):x['chinese'] for x in corrections}
family_records=0

def walk(node):
    global family_records
    if isinstance(node,dict):
        try: key=(int(node.get('id')),node.get('word'))
        except Exception: key=None
        if key in locked and 'chinese' in node:
            family_records += 1
            if node.get('chinese')!=locked[key]: err(f'word_families stale gloss for {key[1]} (id {key[0]}): {node.get("chinese")!r}')
        for v in node.values(): walk(v)
    elif isinstance(node,list):
        for v in node: walk(v)
walk(families)

if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print(f'Lexical semantic quality OK: {len(words)} words scanned, {len(corrections)} high-confidence corrections locked, {family_records} embedded family copies synchronized')

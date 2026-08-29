#!/usr/bin/env python3
"""v3.3.3 reviewed high-frequency example expansion gate."""
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'app/src/main/assets'
errors=[]
def err(x): errors.append(x)
def load(name):
    try: return json.load(open(AS/name,encoding='utf-8'))
    except Exception as e: err(f'cannot load {name}: {e}'); return None
words=load('words.json') or []
old=load('word_example_expansion_v332.json') or {}
ledger=load('word_example_expansion_v333.json') or {}
if len(words)!=2774: err(f'expected 2774 words, found {len(words)}')
by={int(w.get('id',0)):w for w in words if isinstance(w,dict)}
# Historical v3.3.2 additions must remain intact even though overall totals grow later.
old_adds=old.get('additions') or []
if old.get('version')!='3.3.2' or len(old_adds)!=563: err('v3.3.2 historical example ledger invalid')
for a in old_adds:
    wid=int(a.get('id',0)); w=by.get(wid,{})
    if w.get('word')!=a.get('word') or w.get('example')!=a.get('example') or w.get('exampleZh')!=a.get('exampleZh'):
        err(f'v3.3.2 historical example drift for {wid}:{a.get("word")}')
# Current expansion contract.
if ledger.get('version')!='3.3.3': err('example expansion ledger version must be 3.3.3')
adds=ledger.get('additions') or []
if ledger.get('newExamplePairs')!=413 or len(adds)!=413: err(f'expected 413 v3.3.3 additions, found {len(adds)}')
with_ex=[w for w in words if str(w.get('example','')).strip()]
without=[w for w in words if not str(w.get('example','')).strip()]
if len(with_ex)!=1718: err(f'expected 1718 total examples, found {len(with_ex)}')
if len(without)!=1056: err(f'expected 1056 words without examples, found {len(without)}')
if ledger.get('totalExamplePairs')!=1718 or ledger.get('wordsWithoutExample')!=1056: err('v3.3.3 ledger total counts drift')
first=[w for w in words if int(w.get('id',0))<=1500]
if len(first)!=1500: err(f'expected ids 1-1500 to contain 1500 words, found {len(first)}')
missing_first=[w for w in first if not str(w.get('example','')).strip() or not str(w.get('exampleZh','')).strip()]
if missing_first: err('first 1500 example coverage incomplete: '+', '.join(f"{w.get('id')}:{w.get('word')}" for w in missing_first[:20]))
if ledger.get('first1500Coverage')!=1500 or ledger.get('first1500Target')!=1500: err('v3.3.3 first-1500 ledger coverage drift')
seen=set(); seen_it=set()
for a in adds:
    wid=int(a.get('id',0)); w=by.get(wid)
    if wid in seen: err(f'duplicate v3.3.3 addition id {wid}')
    seen.add(wid)
    if not 1001 <= wid <= 1500: err(f'v3.3.3 addition id outside 1001-1500: {wid}')
    if not w: err(f'missing addition id {wid}'); continue
    if w.get('word')!=a.get('word'): err(f'{wid}: word surface drift')
    if w.get('example')!=a.get('example') or w.get('exampleZh')!=a.get('exampleZh'): err(f'{wid} {w.get("word")}: example drift')
    it=str(w.get('example','')).strip(); zh=str(w.get('exampleZh','')).strip(); target=str(w.get('word','')).strip()
    if target.lower() not in it.lower(): err(f'{wid} {target}: Italian example does not contain target: {it}')
    if not re.search(r'[.!?]$',it): err(f'{wid} {target}: Italian example lacks sentence punctuation: {it}')
    if not re.search(r'[。！？？]$',zh): err(f'{wid} {target}: Chinese example lacks sentence punctuation: {zh}')
    if re.search(r'[\u4e00-\u9fff]',it): err(f'{wid} {target}: Chinese leaked into Italian example')
    if not re.search(r'[\u4e00-\u9fff]',zh): err(f'{wid} {target}: Chinese translation missing Chinese characters')
    if len(it)>150 or len(zh)>100: err(f'{wid} {target}: example too long for beginner card')
    norm=re.sub(r'\s+',' ',it.lower())
    if norm in seen_it: err(f'duplicate Italian example in v3.3.3 additions: {it}')
    seen_it.add(norm)
# Lock representative examples from the new batch and previously reported device issues.
critical={
    203:('mangiare','Vorrei mangiare qualcosa.','我想吃点东西。'),
    729:('fotografia','Ho scattato una fotografia del Duomo.','我拍了一张大教堂的照片。'),
    882:('compra','Lei compra il pane ogni mattina.','她每天早上买面包。'),
    887:('marco','Sul quaderno marco le parole nuove con una stella.','我在笔记本上用星号标记新单词。'),
    1001:('panca','Ci sediamo sulla panca davanti alla casa.','我们坐在房子前的长凳上。'),
    1022:('partire','Dobbiamo partire alle sei.','我们必须六点出发。'),
    1085:('annaffiare','Devo annaffiare le piante ogni sera.','我每天晚上都要给植物浇水。'),
    1161:('conoscere','Vorrei conoscere meglio i miei nuovi colleghi.','我想更好地认识我的新同事。'),
    1183:('toccare','Non toccare la pentola: è calda.','别碰锅，锅很烫。'),
    1250:('cantò','Alla festa cantò una canzone italiana.','在聚会上他唱了一首意大利歌曲。'),
    1308:('trama','La trama del film è molto interessante.','这部电影的情节很有趣。'),
    1400:('ascoltare','Mi piace ascoltare la musica mentre cucino.','我喜欢做饭时听音乐。'),
    1423:('castro','Il castro romano sorgeva su una collina.','这座罗马要塞建在一座山丘上。'),
    1459:('scopare','Devo scopare il pavimento della cucina.','我得扫厨房的地板。'),
    1500:('giochi','I bambini hanno molti giochi nella loro stanza.','孩子们的房间里有很多玩具。'),
}
for wid,(surface,it,zh) in critical.items():
    w=by.get(wid,{})
    if w.get('word')!=surface or w.get('example')!=it or w.get('exampleZh')!=zh:
        err(f'critical example drift for {wid} {surface}')
if errors:
    for e in errors[:300]: print('ERROR:',e)
    print('WORD EXAMPLE EXPANSION CHECK FAILED:',len(errors),'error(s)')
    sys.exit(1)
print(f'Word example expansion OK: v3.3.2 historical +563 preserved; v3.3.3 +{len(adds)} reviewed pairs; total {len(with_ex)}/2774; first 1500 coverage 1500/1500; remaining {len(without)}')

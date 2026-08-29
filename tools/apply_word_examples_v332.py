#!/usr/bin/env python3
from pathlib import Path
import csv, json
ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'app/src/main/assets'
TSV=ROOT/'tools/word_examples_v332.tsv'
words_path=AS/'words.json'
words=json.load(open(words_path,encoding='utf-8'))
by={int(w['id']):w for w in words}
added=[]
with open(TSV,encoding='utf-8') as f:
    for row in csv.reader(f,delimiter='\t'):
        if not row: continue
        if len(row)!=3: raise SystemExit(f'bad TSV row: {row!r}')
        wid=int(row[0]); it=row[1].strip(); zh=row[2].strip()
        if wid not in by: raise SystemExit(f'unknown word id {wid}')
        w=by[wid]
        if w.get('example','').strip():
            # Idempotent re-run only when content already matches.
            if w.get('example')!=it or w.get('exampleZh')!=zh:
                raise SystemExit(f'{wid} {w.get("word")}: existing example differs; refusing overwrite')
            continue
        target=str(w.get('word','')).strip().lower()
        if target not in it.lower(): raise SystemExit(f'{wid} {w.get("word")}: example does not contain target: {it}')
        w['example']=it; w['exampleZh']=zh
        added.append({'id':wid,'word':w.get('word',''),'example':it,'exampleZh':zh})
with open(words_path,'w',encoding='utf-8') as f:
    json.dump(words,f,ensure_ascii=False,indent=2)
    f.write('\n')
with_examples=sum(bool(str(w.get('example','')).strip()) for w in words)
first_1000=sum(bool(str(w.get('example','')).strip()) for w in words if int(w.get('id',0))<=1000)
ledger={
    'version':'3.3.2',
    'description':'High-frequency example expansion. Every word id 1-1000 now has a reviewed Italian-Chinese example pair; existing v3.2.2 examples are preserved.',
    'allWordRows':len(words),
    'previousExamplePairs':742,
    'newExamplePairs':len(added),
    'totalExamplePairs':with_examples,
    'wordsWithoutExample':len(words)-with_examples,
    'first1000Coverage':first_1000,
    'first1000Target':1000,
    'additions':added,
}
with open(AS/'word_example_expansion_v332.json','w',encoding='utf-8') as f:
    json.dump(ledger,f,ensure_ascii=False,indent=2)
    f.write('\n')
print(f'Applied {len(added)} new examples; total={with_examples}; missing={len(words)-with_examples}; first1000={first_1000}/1000')

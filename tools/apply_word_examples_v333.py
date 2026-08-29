#!/usr/bin/env python3
from pathlib import Path
import csv, json
ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'app/src/main/assets'
TSV=ROOT/'tools/word_examples_v333.tsv'
words_path=AS/'words.json'
words=json.load(open(words_path,encoding='utf-8'))
by={int(w['id']):w for w in words}
rows=[]; newly_applied=0
with open(TSV,encoding='utf-8') as f:
    for row in csv.reader(f,delimiter='\t'):
        if not row: continue
        if len(row)!=3: raise SystemExit(f'bad TSV row: {row!r}')
        wid=int(row[0]); it=row[1].strip(); zh=row[2].strip()
        if wid not in by: raise SystemExit(f'unknown word id {wid}')
        if not 1001 <= wid <= 1500: raise SystemExit(f'v3.3.3 id outside 1001-1500: {wid}')
        w=by[wid]
        if w.get('example','').strip():
            if w.get('example')!=it or w.get('exampleZh')!=zh:
                raise SystemExit(f'{wid} {w.get("word")}: existing example differs; refusing overwrite')
        else:
            target=str(w.get('word','')).strip().lower()
            if target not in it.lower(): raise SystemExit(f'{wid} {w.get("word")}: example does not contain target: {it}')
            w['example']=it; w['exampleZh']=zh; newly_applied+=1
        rows.append({'id':wid,'word':w.get('word',''),'example':it,'exampleZh':zh})
with open(words_path,'w',encoding='utf-8') as f:
    json.dump(words,f,ensure_ascii=False,indent=2); f.write('\n')
with_examples=sum(bool(str(w.get('example','')).strip()) for w in words)
first_1500=sum(bool(str(w.get('example','')).strip()) for w in words if int(w.get('id',0))<=1500)
ledger={
    'version':'3.3.3',
    'description':'Second reviewed high-frequency example expansion. Word IDs 1-1500 now all have Italian-Chinese example pairs; v3.3.2 examples are preserved.',
    'allWordRows':len(words),
    'previousExamplePairs':1305,
    'newExamplePairs':len(rows),
    'newlyAppliedThisRun':newly_applied,
    'totalExamplePairs':with_examples,
    'wordsWithoutExample':len(words)-with_examples,
    'first1500Coverage':first_1500,
    'first1500Target':1500,
    'additions':rows,
}
with open(AS/'word_example_expansion_v333.json','w',encoding='utf-8') as f:
    json.dump(ledger,f,ensure_ascii=False,indent=2); f.write('\n')
print(f'v3.3.3 canonical additions={len(rows)}; newly applied={newly_applied}; total={with_examples}; missing={len(words)-with_examples}; first1500={first_1500}/1500')

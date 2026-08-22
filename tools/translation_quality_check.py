#!/usr/bin/env python3
"""v3.1.1 learner-facing Italian -> Chinese meaning quality gate.

The source vocabulary contains broad English glosses, so automatic one-gloss matching can
silently pick the wrong sense (for example fotografia -> 地图). This gate locks the
high-confidence corrections found by a full duplicate-label/semantic audit and protects
critical everyday words from regressing.
"""
from pathlib import Path
import collections, json, sys

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
errors=[]; warnings=[]

def err(msg): errors.append(msg)
def warn(msg): warnings.append(msg)

words=json.load(open(ASSETS/'words.json',encoding='utf-8'))
meta=json.load(open(ASSETS/'translation_quality_v311.json',encoding='utf-8'))
by_id={int(w['id']):w for w in words}
by_word=collections.defaultdict(list)
for w in words: by_word[(w.get('word') or '').strip().lower()].append(w)

if len(words)!=2774: err(f'expected 2774 words, found {len(words)}')
if meta.get('version')!='3.1.1': err('translation_quality_v311.json version must be 3.1.1')
corrections=meta.get('corrections') or []
if meta.get('correctedCount')!=len(corrections): err('translation correction count metadata mismatch')
if len(corrections)<131: err(f'expected at least 131 locked high-confidence corrections, found {len(corrections)}')

seen=set()
for c in corrections:
    wid=int(c.get('id',0)); expected=(c.get('newChinese') or '').strip(); word=(c.get('word') or '').strip()
    if not wid or wid in seen: err(f'duplicate/invalid translation correction id {wid}'); continue
    seen.add(wid)
    w=by_id.get(wid)
    if not w: err(f'correction references missing word id {wid}'); continue
    if word and w.get('word')!=word: err(f'correction id {wid} word drift: {word!r} != {w.get("word")!r}')
    if (w.get('chinese') or '').strip()!=expected: err(f'{wid} {w.get("word")}: expected Chinese {expected!r}, found {w.get("chinese")!r}')
    if not expected: err(f'{wid} {w.get("word")}: empty corrected Chinese meaning')

# Real-device and high-risk semantic regressions. These are intentionally explicit.
critical={
    729:('fotografia','照片；摄影'),
    1085:('annaffiare','浇水'),
    1237:('in bocca al lupo','祝你好运'),
    1580:('buonanotte','晚安'),
    1591:('legge','他/她读；您读'),
    1592:('leggi','你读；阅读'),
    2010:('esprimere','表达'),
    2195:('sognare','做梦；梦想'),
    2260:('premio','奖品；奖金'),
    2264:('partita','比赛；场次'),
    2288:('eccellente','优秀的；极好的'),
    2552:('povertà','贫困；贫穷'),
    2759:('partito','政党；党派'),
}
for wid,(surface,zh) in critical.items():
    w=by_id.get(wid)
    if not w or w.get('word')!=surface or w.get('chinese')!=zh:
        err(f'critical translation regression {wid}: expected {surface} -> {zh}')

# The exact bug reported on a real device: fotografia must never collapse into map.
photo=by_id.get(729)
if photo:
    zh=photo.get('chinese','')
    if '照片' not in zh and '摄影' not in zh: err('fotografia must contain 照片 or 摄影')
    if '地图' in zh: err('fotografia must not contain 地图')
for surface,required in [('mappa','地图'),('cartina','地图'),('foto','照片')]:
    rows=by_word.get(surface,[])
    if not rows: err(f'missing reference word {surface}')
    elif not any(required in (r.get('chinese') or '') for r in rows): err(f'{surface} must retain Chinese concept {required}')

# Basic hygiene. Meaning questions should never receive an empty label.
for w in words:
    if not (w.get('word') or '').strip(): err(f'word id {w.get("id")} has empty Italian surface')
    if not (w.get('chinese') or '').strip(): err(f'{w.get("id")} {w.get("word")}: empty Chinese meaning')

# Non-blocking signal for future manual refinement: heavily reused very short labels are
# worth reviewing, but many are legitimate conjugations/pronouns so they are not build failures.
groups=collections.defaultdict(list)
for w in words: groups[(w.get('chinese') or '').strip()].append(w)
review=[]
for label,rows in groups.items():
    if len(rows)>=4 and len(label)<=5:
        review.append((len(rows),label,[r.get('word','') for r in rows[:8]]))
for n,label,surfaces in sorted(review,reverse=True)[:12]:
    warn(f'reused short label {label!r} x{n}: '+', '.join(surfaces))

if warnings:
    for x in warnings: print('WARNING:',x)
if errors:
    for x in errors[:120]: print('ERROR:',x)
    print('TRANSLATION QUALITY CHECK FAILED:',len(errors),'error(s)')
    sys.exit(1)
print(f'Translation quality OK: {len(words)} words, {len(corrections)} high-confidence corrections locked; fotografia/map regression blocked')

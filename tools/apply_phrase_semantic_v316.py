#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'

FIXES = {
    'p0007': {
        'it': 'Sono allergico alla frutta a guscio.',
        'zh': '我对坚果过敏。',
        'note': '男性说法；女性可说 allergica；frutta a guscio=坚果类',
        'reason': 'noci specifically means walnuts; use frutta a guscio for the broad Chinese meaning 坚果.'
    },
    'p0129': {
        'it': 'Può scrivermi il numero della pratica?',
        'zh': '您能把手续编号写给我吗？',
        'note': '',
        'reason': 'pratica in public-office context means an administrative file/application, not necessarily a legal 案件.'
    },
    'p0130': {
        'it': 'Vorrei chiedere informazioni sulla residenza.',
        'zh': '我想咨询居住登记。',
        'note': '',
        'reason': 'residenza here is registered residence; 居住登记 is the consistent beginner-facing term.'
    },
    'p0140': {
        'it': "A che ora è l'ultima corsa di oggi?",
        'zh': '今天最后一班车是几点？',
        'note': '',
        'reason': 'Chinese explicitly asks for the time; A che ora makes the Italian question equally explicit.'
    },
    'p0155': {
        'it': "Posso avere una bottiglia d'acqua naturale?",
        'zh': '可以给我一瓶无气水吗？',
        'note': '',
        'reason': 'acqua naturale means still/non-carbonated water, not room-temperature water; bottiglia matches 一瓶.'
    },
    'v26_0363': {
        'it': 'La linea è disturbata.',
        'zh': '电话线路有干扰。',
        'note': '电话',
        'reason': 'disturbata means the line has interference/noise; the Chinese now states that directly.'
    },
    'v26_dlg_0390': {
        'it': 'Sono quattro euro e cinquanta.',
        'zh': '一共4欧元50分。',
        'note': 'Bar 和餐厅',
        'reason': 'Use the spoken money format instead of decimal 4.5, matching quattro euro e cinquanta.'
    },
    'v26_dlg_0394': {
        'it': 'Costa due euro e venti.',
        'zh': '2欧元20分。',
        'note': '超市购物',
        'reason': 'Use the spoken money format instead of decimal 2.2, matching due euro e venti.'
    },
    'v26_dlg_0429': {
        'it': 'Quando sarebbe disponibile per iniziare?',
        'zh': '您什么时候可以开始？',
        'note': '工作和面试',
        'reason': 'Chinese asks when the person can start; per iniziare makes that explicit in Italian.'
    },
}


def load(name):
    return json.loads((ASSETS/name).read_text(encoding='utf-8'))

def save(name, data):
    (ASSETS/name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Keep the two 430-entry learning assets exactly synchronized.
for name in ['frequent_phrases.json', 'core_sentences.json']:
    data = load(name)
    by_id = {x['id']: x for x in data}
    for pid, fix in FIXES.items():
        if pid in by_id:
            by_id[pid]['it'] = fix['it']
            by_id[pid]['zh'] = fix['zh']
            if 'note' in by_id[pid]:
                by_id[pid]['note'] = fix['note']
    save(name, data)

# Scenario duplicate of p0007.
scenarios = load('scenarios.json')
for sc in scenarios:
    for p in sc.get('phrases', []):
        if p.get('it') == 'Sono allergico alle noci.' and p.get('zh') == '我对坚果过敏。':
            p.update({
                'it': FIXES['p0007']['it'],
                'zh': FIXES['p0007']['zh'],
                'note': FIXES['p0007']['note']
            })
save('scenarios.json', scenarios)

# The words.json example belongs to the word "alle" and must keep alle in the example;
# therefore keep noci, but translate noci accurately as walnuts.
words = load('words.json')
for w in words:
    if w.get('example') == 'Sono allergico alle noci.' and w.get('exampleZh') == '我对坚果过敏。':
        w['exampleZh'] = '我对核桃过敏。'
    if w.get('example') == "Qual è l'ultima corsa di oggi?" and w.get('exampleZh') == '今天最后一班车是几点？':
        w['example'] = FIXES['p0140']['it']
    if w.get('example') == "Posso avere dell'acqua naturale?" and w.get('exampleZh') == '可以给我一瓶常温无气水吗？':
        w['example'] = FIXES['p0155']['it']
        w['exampleZh'] = FIXES['p0155']['zh']
save('words.json', words)

# Emergency phrase duplicates.
emergency = load('emergency_phrases.json')
for cat in emergency:
    for p in cat.get('phrases', []):
        if p.get('it') == "Qual è l'ultima corsa di oggi?":
            p['it'] = FIXES['p0140']['it']; p['zh'] = FIXES['p0140']['zh']
        if p.get('it') == "Posso avere dell'acqua naturale?":
            p['it'] = FIXES['p0155']['it']; p['zh'] = FIXES['p0155']['zh']
save('emergency_phrases.json', emergency)

# Dialogue duplicates.
dialogues = load('dialogues.json')
for d in dialogues:
    for t in d.get('turns', []):
        if t.get('reply') == 'Sono quattro euro e cinquanta.':
            t['replyZh'] = FIXES['v26_dlg_0390']['zh']
        if t.get('npc') == 'Sono quattro euro e cinquanta.':
            t['npcZh'] = FIXES['v26_dlg_0390']['zh']
        if t.get('reply') == 'Costa due euro e venti.':
            t['replyZh'] = FIXES['v26_dlg_0394']['zh']
        if t.get('reply') == 'Quando sarebbe disponibile?':
            t['reply'] = FIXES['v26_dlg_0429']['it']; t['replyZh'] = FIXES['v26_dlg_0429']['zh']
        if t.get('npc') == 'Quando sarebbe disponibile?':
            t['npc'] = FIXES['v26_dlg_0429']['it']; t['npcZh'] = FIXES['v26_dlg_0429']['zh']
save('dialogues.json', dialogues)

# Listening duplicates.
listening = load('listening_courses.json')
for course in listening:
    for s in course.get('sentences', []):
        pid = s.get('id')
        if pid in FIXES:
            s['it'] = FIXES[pid]['it']; s['zh'] = FIXES[pid]['zh']
            if 'note' in s and FIXES[pid].get('note'):
                s['note'] = FIXES[pid]['note']
save('listening_courses.json', listening)

ledger = {
    'version': '3.1.6',
    'scannedUniquePairs': 430,
    'mirroredCoreSentences': 430,
    'fixCount': len(FIXES),
    'fixes': [dict(id=k, **v) for k,v in FIXES.items()],
    'notes': [
        'frequent_phrases.json and core_sentences.json are intentionally mirrored and must remain synchronized.',
        'The noci example in words.json remains with alle for grammar exposure, but its Chinese is corrected to 核桃.',
        'Duplicate scenario/emergency/dialogue/listening assets are synchronized for affected phrases.'
    ]
}
(ASSETS/'phrase_semantic_quality_v316.json').write_text(json.dumps(ledger, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(f'Applied {len(FIXES)} semantic fixes and wrote phrase_semantic_quality_v316.json')

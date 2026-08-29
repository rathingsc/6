#!/usr/bin/env python3
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
WORDS=ASSETS/'words.json'
FAMILIES=ASSETS/'word_families.json'

# High-confidence learner-facing fixes found by scanning every course word for
# machine-translation fragments, broken conjugations, and context-disambiguation errors.
FIXES={
443:'需要',
481:'他/她/您吃',
483:'我吃',
485:'我做饭',
487:'我洗；我清洗',
490:'你做饭',
491:'你洗；你清洗',
492:'你哭',
494:'你吃',
510:'我们吃',
669:'你展示；你出示',
724:'我们需要',
763:'他喜欢（复数事物）',
764:'他喜欢（单数事物）',
799:'我们喜欢（单数事物）',
801:'我们喜欢（复数事物）',
829:'我们学习',
881:'他/她/您叫；打电话',
882:'他/她/您买',
895:'你放；你穿上',
908:'我更喜欢',
913:'那个；那一个（元音前）',
928:'那个；那一个',
940:'他/她/您转；转弯',
948:'现在是……点',
958:'你认识；你了解',
963:'我认识；我了解',
980:'展示；展览',
988:'你喝',
1015:'你们喝',
1031:'他/她/您可以；可能会',
1036:'味道；口味',
1042:'我可以；我可能会',
1044:'我们喝',
1048:'你付款；你支付',
1057:'你更喜欢',
1081:'他们/她们更喜欢',
1112:'她迷路了',
1115:'他/她/您认识；了解',
1127:'我会来；我愿意来',
1129:'他们/她们很好；身体不错',
1153:'我读；我阅读',
1183:'触摸；碰；涉及',
1194:'同事；同事们',
1212:'你出去；你离开',
1226:'他/她/它给；您给',
1227:'你；给你；你自己',
1233:'他们/她们将吃晚饭',
1234:'他们/她们出去；离开',
1236:'他们/她们将看；将观看',
1238:'他/她/您将写',
1239:'他/她/您将做饭',
1240:'他/她/它出去；离开',
1242:'我们会看；我们会观看',
1243:'你们将看；将观看',
1244:'你们将吃晚饭',
1246:'我们将吃晚饭',
1248:'我将吃晚饭',
1249:'我将认识；我将了解',
1251:'我出去；我离开',
1252:'你将认识；你将了解',
1255:'你们出去；你们离开',
1256:'你将吃晚饭',
1257:'已经出去；已经离开',
1268:'我会做',
1273:'我们醒来；叫醒我们',
1275:'你醒来；叫醒你',
1278:'我醒来；叫醒我',
1283:'他们/她们将出发',
1285:'他们/她们将睡觉',
1288:'我们将睡觉',
1289:'我们将出发',
1290:'你们将睡觉',
1291:'你们将出发',
1292:'他/她将出发',
1293:'他/她/您将睡觉',
1298:'你将出发',
1299:'你将睡觉',
1302:'我将睡觉',
1304:'我将出发',
1307:'够了；足够',
1318:'我清洁；我打扫',
1320:'他/她/它掉下；跌倒',
1341:'吃过；吃了',
1403:'我们将认识；我们将了解',
1404:'我们将去',
1410:'表演；朗诵；背诵',
1426:'我明白；我理解',
1431:'我们将学习',
1432:'我将学习',
1461:'我想不是；我认为不是',
1475:'你们将去',
1476:'他们/她们将去',
1482:'他/她/您将去',
1488:'你将去',
1495:'我将去',
1523:'浪费',
1552:'他/她/您待着；处于',
1557:'正在来；正在到来',
1569:'正在看；正在观看',
1570:'你待着；你处于',
1574:'正在去；正在走',
1579:'晚上好',
1583:'他/她/您写',
1585:'我写',
1586:'你写',
1594:'他们/她们喝',
1637:'我们吃晚饭',
1662:'自己的；本人的',
1778:'建筑师',
1846:'证明；展示；表明',
1853:'指出；指示；表示',
1855:'连接；联合；合并',
1991:'燃烧；烧',
2003:'坐；坐下',
2004:'判断；评判',
2020:'更高的；上级的；高级的',
2063:'叫醒；唤醒',
2131:'航班；飞行',
2180:'混淆；使困惑',
2191:'使担心；担忧',
2254:'跑道；赛道；滑雪道',
2321:'到处；处处',
2344:'实际上；几乎',
2367:'美；美丽',
2406:'离开你们；让你们',
2408:'把它放；把它穿上',
2411:'做它；做这件事',
2510:'想要；愿意',
2531:'舞蹈；舞会',
2580:'研究；寻找；搜寻',
2600:'印刷；新闻界；媒体',
}

# These source forms are homographs that the old automatic morphology pass classified
# as verbs even though their guided-course context is clearly a noun/preposition.
# Cleaning this metadata prevents cloze distractors from treating "porta=门" as a verb etc.
META={
124:  dict(partOfSpeech='noun', lemma='suono', formInfo='', gender='m', number='singular', article='il', plural='suoni'),
183:  dict(partOfSpeech='noun', lemma='lavoro', formInfo='', gender='m', number='singular', article='il', plural='lavori'),
802:  dict(partOfSpeech='noun', lemma='parte', formInfo='', gender='f', number='singular', article='la', plural='parti'),
1003: dict(partOfSpeech='noun', lemma='porta', formInfo='', gender='f', number='singular', article='la', plural='porte'),
1036: dict(partOfSpeech='noun', lemma='gusto', formInfo='', gender='m', number='singular', article='il', plural='gusti'),
1038: dict(partOfSpeech='noun', lemma='pesca', formInfo='', gender='f', number='singular', article='la', plural='pesche'),
1599: dict(partOfSpeech='noun', lemma='cena', formInfo='', gender='f', number='singular', article='la', plural='cene'),
1647: dict(partOfSpeech='noun', lemma='pranzo', formInfo='', gender='m', number='singular', article='il', plural='pranzi'),
1716: dict(partOfSpeech='other', lemma='', formInfo='da + i 的缩合形式（阳性复数）', gender='', number='', article='', plural=''),
1728: dict(partOfSpeech='other', lemma='', formInfo='介词/副词：在……以内；在……之前', gender='', number='', article='', plural=''),
1885: dict(partOfSpeech='noun', lemma='porto', formInfo='', gender='m', number='singular', article='il', plural='porti'),
2087: dict(partOfSpeech='noun', lemma='prova', formInfo='', gender='f', number='singular', article='la', plural='prove'),
2102: dict(partOfSpeech='noun', lemma='voto', formInfo='', gender='m', number='singular', article='il', plural='voti'),
2124: dict(partOfSpeech='noun', lemma='visita', formInfo='', gender='f', number='singular', article='la', plural='visite'),
2131: dict(partOfSpeech='noun', lemma='volo', formInfo='', gender='m', number='singular', article='il', plural='voli'),
2167: dict(partOfSpeech='noun', lemma='inizio', formInfo='', gender='m', number='singular', article="l'", plural='inizi'),
2196: dict(partOfSpeech='noun', lemma='sogno', formInfo='', gender='m', number='singular', article='il', plural='sogni'),
2231: dict(partOfSpeech='noun', lemma='aspetto', formInfo='', gender='m', number='singular', article="l'", plural='aspetti'),
2386: dict(partOfSpeech='noun', lemma='cambio', formInfo='', gender='m', number='singular', article='il', plural='cambi'),
2390: dict(partOfSpeech='noun', lemma='arrivo', formInfo='', gender='m', number='singular', article="l'", plural='arrivi'),
2393: dict(partOfSpeech='noun', lemma='ritorno', formInfo='', gender='m', number='singular', article='il', plural='ritorni'),
2531: dict(partOfSpeech='noun', lemma='ballo', formInfo='', gender='m', number='singular', article='il', plural='balli'),
2571: dict(partOfSpeech='noun', lemma='ricerca', formInfo='', gender='f', number='singular', article='la', plural='ricerche'),
2600: dict(partOfSpeech='noun', lemma='stampa', formInfo='', gender='f', number='singular', article='la', plural='stampe'),
2617: dict(partOfSpeech='noun', lemma='invito', formInfo='', gender='m', number='singular', article="l'", plural='inviti'),
2629: dict(partOfSpeech='noun', lemma='fumo', formInfo='', gender='m', number='singular', article='il', plural='fumi'),
2684: dict(partOfSpeech='noun', lemma='incontro', formInfo='', gender='m', number='singular', article="l'", plural='incontri'),
2688: dict(partOfSpeech='noun', lemma='costo', formInfo='', gender='m', number='singular', article='il', plural='costi'),
2700: dict(partOfSpeech='noun', lemma='mente', formInfo='', gender='f', number='singular', article='la', plural='menti'),
}

# Context-specific meaning fixes for disambiguated homographs / contracted forms.
FIXES.update({
1003:'门',
1038:'桃子',
1599:'晚餐',
1647:'午餐',
1716:'从……；由……（da+i）',
1728:'在……以内；在……之前',
1885:'港口',
2087:'测试；考试；试验',
2102:'分数；成绩',
2124:'访问；参观；拜访',
2154:'穿过；通过；横跨',
2196:'梦；梦想',
2231:'方面；外观',
2386:'更换；兑换；变化',
2390:'到达；抵达',
2393:'返回；返程',
2534:'图画；我画',
2571:'搜索；研究',
2617:'邀请；请柬',
2629:'烟；烟雾',
2684:'见面；会议',
2688:'成本；价格',
2700:'头脑；思维',
})

words=json.load(open(WORDS,encoding='utf-8'))
by_id={int(w['id']):w for w in words}
missing=sorted((set(FIXES)|set(META))-set(by_id))
if missing:
    raise SystemExit(f'missing word ids: {missing}')

corrections=[]
for wid,new in sorted(FIXES.items()):
    w=by_id[wid]
    old=w.get('chinese','')
    if old!=new:
        corrections.append({
            'id':wid,'word':w.get('word',''),'oldChinese':old,'newChinese':new,
            'english':w.get('english',''),'level':w.get('level','')
        })
        w['chinese']=new

meta_changes=[]
for wid,updates in sorted(META.items()):
    w=by_id[wid]
    old={k:w.get(k,'') for k in updates}
    if any(old[k]!=v for k,v in updates.items()):
        meta_changes.append({'id':wid,'word':w.get('word',''),'old':old,'new':updates})
        w.update(updates)

WORDS.write_text(json.dumps(words,ensure_ascii=False,indent=2)+"\n",encoding='utf-8')

# Keep the word-family cards consistent with words.json by stable word id.
if FAMILIES.exists():
    fam=json.load(open(FAMILIES,encoding='utf-8'))
    sync_count=0
    def walk(obj):
        nonlocal_holder=[0]
        def rec(x):
            if isinstance(x,dict):
                if isinstance(x.get('id'),int) and x['id'] in by_id and 'word' in x and 'chinese' in x:
                    src=by_id[x['id']]
                    if x.get('word')==src.get('word') and x.get('chinese')!=src.get('chinese'):
                        x['chinese']=src.get('chinese');nonlocal_holder[0]+=1
                    for k in ('lemma','formInfo'):
                        if k in x and x.get(k)!=src.get(k,''):
                            x[k]=src.get(k,'');nonlocal_holder[0]+=1
                for v in x.values(): rec(v)
            elif isinstance(x,list):
                for v in x: rec(v)
        rec(obj);return nonlocal_holder[0]
    sync_count=walk(fam)
    FAMILIES.write_text(json.dumps(fam,ensure_ascii=False,indent=2)+"\n",encoding='utf-8')
else:
    sync_count=0

ledger={
    'version':'3.1.2',
    'purpose':'Guided-course Chinese cleanup: conjugations, future/conditional forms, machine fragments, and context-disambiguated homographs.',
    'correctedCount':len(corrections),
    'metadataFixCount':len(meta_changes),
    'familySyncEdits':sync_count,
    'corrections':corrections,
    'metadataFixes':meta_changes,
}
(ASSETS/'course_translation_quality_v312.json').write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+"\n",encoding='utf-8')

lines=['终学意语 v3.1.2 课程词义修正清单','',f'中文词义修正：{len(corrections)} 项',f'词性/形态消歧：{len(meta_changes)} 项','']
for c in corrections:
    lines.append(f"{c['id']:04d}  {c['word']}：{c['oldChinese']}  →  {c['newChinese']}")
lines.append('')
lines.append('词性/形态消歧：')
for m in meta_changes:
    lines.append(f"{m['id']:04d}  {m['word']}：{m['old'].get('partOfSpeech','')} / {m['old'].get('formInfo','')}  →  {m['new'].get('partOfSpeech','')} / {m['new'].get('formInfo','')}")
(ROOT/'课程词义修正清单_v3.1.2.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('course translations changed',len(corrections))
print('metadata changed',len(meta_changes))
print('word family synced edits',sync_count)
print('compra:',by_id[882]['chinese'])
print('fotografia:',by_id[729]['chinese'])

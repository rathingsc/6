#!/usr/bin/env python3
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
WORDS=ASSETS/'words.json'

# High-confidence learner-facing corrections found by a full 2774-row semantic review.
# Only obvious mismatches / misleading glosses are changed; ambiguous but defensible entries are left alone.
PATCH={
  52: dict(chinese='月亮', english='moon', partOfSpeech='noun', gender='f', number='singular', article='la', plural='lune'),
  124: dict(chinese='声音；我演奏/发出声音', english='sound; I play; I make a sound'),
  164: dict(chinese='收音机；广播', english='radio'),
  282: dict(chinese='对不起；请原谅（对多人/各位）', english='excuse me; sorry (plural/formal)'),
  363: dict(chinese='下一个（阴性）；下一次', english='the next one; next time'),
  382: dict(chinese='抱怨；牢骚；烦人的抱怨', english='complaint; whining', partOfSpeech='noun', gender='f', number='singular', article='la', plural='lagne'),
  400: dict(chinese='钱包', english='wallet'),
  409: dict(chinese='节日；聚会', english='party; celebration; festival'),
  445: dict(chinese='他/她/您想要；想……', english='he/she/you(formal) would like; would want', lemma='volere', formInfo='volere · 条件式现在时 · 第三人称单数/正式您', partOfSpeech='verb'),
  470: dict(chinese='到……；在……；给……（a+il）', english='to the; at the'),
  473: dict(chinese='他/她/您说；说话', english='he/she/you(formal) speaks', lemma='parlare', formInfo='parlare · 直陈式现在时 · 第三人称单数/正式您', partOfSpeech='verb'),
  476: dict(chinese='他/她/您使用；使用', english='he/she/you(formal) uses', lemma='usare', formInfo='usare · 直陈式现在时 · 第三人称单数/正式您', partOfSpeech='verb'),
  609: dict(chinese='在……里面；在……中（in+il）', english='in the'),
  651: dict(chinese='……的；从……；关于……（di+la）', english='of the; from the; about the'),
  675: dict(chinese='故事；历史', english='story; history', partOfSpeech='noun'),
  699: dict(chinese='他/她/您经过；通过；递给；度过', english='he/she/you(formal) passes; goes by; spends', lemma='passare', formInfo='passare · 直陈式现在时 · 第三人称单数/正式您', partOfSpeech='verb'),
  713: dict(chinese='我们想要；我们想……', english='we would like; we would want', lemma='volere', formInfo='volere · 条件式现在时 · 第一人称复数', partOfSpeech='verb'),
  721: dict(chinese='他们/她们想要；他们/她们想……', english='they would like; they would want', lemma='volere', formInfo='volere · 条件式现在时 · 第三人称复数', partOfSpeech='verb'),
  727: dict(chinese='我有过；我曾有', english='I had', lemma='avere', formInfo='avere · 远过去时 · 第一人称单数', partOfSpeech='verb'),
  734: dict(chinese='他的；她的；您的（阴性复数）', english='his; her; your(formal) (feminine plural)'),
  743: dict(chinese='他/她/您寻找；搜索；找', english='he/she/you(formal) looks for; searches', lemma='cercare', formInfo='cercare · 直陈式现在时 · 第三人称单数/正式您', partOfSpeech='verb'),
  747: dict(chinese='他的；她的；您的（阳性复数）', english='his; her; your(formal) (masculine plural)'),
  760: dict(chinese='我讨人喜欢；别人喜欢我', english='I am liked; I please', lemma='piacere', formInfo='piacere · 直陈式现在时 · 第一人称单数', partOfSpeech='verb'),
  778: dict(chinese='金发的；浅色的', english='blond; fair-haired; light-coloured', partOfSpeech='adjective'),
  839: dict(chinese='德国人（复数）；德国的（阳性复数）', english='Germans; German (masculine plural)'),
  850: dict(chinese='旅行；旅程；我旅行', english='trip; journey; I travel'),
  874: dict(chinese='被邀请的；受邀的', english='invited', lemma='invitare', formInfo='invitare · 过去分词', partOfSpeech='adjective'),
  888: dict(chinese='干净的（阴性复数）；你们清洁/打扫', english='clean (feminine plural); you clean', partOfSpeech='other'),
  894: dict(chinese='在……里面；在……中（in+lo）', english='in the'),
  910: dict(chinese='品牌（复数）；马尔凯大区', english='brands; Marche (region)', partOfSpeech='noun', gender='f', number='plural', article='le', plural='marche'),
  953: dict(chinese='卸下的；空载的；我卸载/下载', english='unloaded; empty; I unload/download'),
  961: dict(chinese='从……；由……（da+la）', english='from the; by the'),
  1000: dict(chinese='舒适；自在', english='ease; comfort', partOfSpeech='noun', gender='m', number='singular', article="l'", plural='agi'),
  1014: dict(chinese='……的；从……；关于……（di+il）', english='of the; from the; about the'),
  1027: dict(chinese='……的；从……；关于……（di+l’）', english='of the; from the; about the'),
  1035: dict(chinese='到……；在……；给……（a+la）', english='to the; at the'),
  1038: dict(chinese='桃子；捕鱼/钓鱼', english='peach; fishing'),
  1039: dict(chinese='……的；从……；关于……（di+lo）', english='of the; from the; about the'),
  1046: dict(chinese='你们想要；你们想……', english='you(plural) would like; would want', lemma='volere', formInfo='volere · 条件式现在时 · 第二人称复数', partOfSpeech='verb'),
  1058: dict(chinese='……的；从……；关于……（di+le）', english='of the; from the; about the'),
  1060: dict(chinese='更好的；最好的', english='better; best', partOfSpeech='adjective'),
  1063: dict(chinese='……的；一些（di+gli）', english='of the; some'),
  1069: dict(chinese='……的；一些（di+i）', english='of the; some'),
  1072: dict(chinese='如此；这样；这么', english='so; thus; like this'),
  1096: dict(chinese='微醺的；醉醺醺的；我闪耀', english='tipsy; slightly drunk; I shine'),
  1100: dict(chinese='这里；这边', english='here; this way'),
  1131: dict(chinese='那里；那边', english='there; over there'),
  1133: dict(chinese='我们；那里；到那里；对此', english='us; there; about it'),
  1136: dict(chinese='抱怨；牢骚（较少用/古语）', english='complaint; lament (rare/old)', partOfSpeech='noun', gender='m', number='singular', article='il', plural='lagni'),
  1162: dict(chinese='更好；最好；较好地', english='better; best', partOfSpeech='adverb'),
  1169: dict(chinese='问题；议题（复数）', english='questions; issues', partOfSpeech='noun', gender='f', number='plural', article='le', plural='questioni'),
  1177: dict(chinese='那些（特殊阳性复数）', english='those (masculine plural)'),
  1193: dict(chinese='在……里面；在……中（in+l’）', english='in the'),
  1197: dict(chinese='从……；由……（da+le）', english='from the; by the'),
  1215: dict(chinese='神秘的', english='mysterious', partOfSpeech='adjective'),
  1216: dict(chinese='你们；给你们；在那里/去那里', english='you(plural); to you(plural); there'),
  1262: dict(chinese='做准备；准备好', english='to get ready; to prepare oneself', lemma='prepararsi', formInfo='反身动词原形', partOfSpeech='verb'),
  1267: dict(chinese='演出；表演；景象', english='show; performance; spectacle', partOfSpeech='noun'),
  1284: dict(chinese='一点；有点', english='a little; a bit'),
  1370: dict(chinese='吃过午饭；吃了午饭', english='had lunch', lemma='pranzare', formInfo='pranzare · 过去分词', partOfSpeech='verb'),
  1381: dict(chinese='拿了；取了；乘了；抓住了', english='taken; got; caught', lemma='prendere', formInfo='prendere · 过去分词', partOfSpeech='verb'),
  1394: dict(chinese='我找到了', english='I found', lemma='trovare', formInfo='trovare · 远过去时 · 第一人称单数', partOfSpeech='verb'),
  1421: dict(chinese='可乐；滴流/流下', english='cola; it drips/flows'),
  1423: dict(chinese='城堡；要塞；设防聚落（历史用语）', english='castle; fortress; fortified settlement (historical)', partOfSpeech='noun', gender='m', number='singular', article='il', plural='castri'),
  1455: dict(chinese='手镯；臂环', english='bracelet; armband', partOfSpeech='noun', gender='m', number='singular', article='il', plural='bracciali'),
  1513: dict(chinese='长矛；他/她/您投掷', english='spear; he/she/you(formal) throws'),
  1563: dict(chinese='正在写；正在书写', english='writing', lemma='scrivere', formInfo='scrivere · 现在分词/副动词', partOfSpeech='verb'),
  1565: dict(chinese='正在准备；准备中', english='preparing; getting ready', lemma='preparare', formInfo='preparare · 现在分词/副动词', partOfSpeech='verb'),
  1568: dict(chinese='一整天；整天', english='all day'),
  1573: dict(chinese='正在学习；学习中', english='studying', lemma='studiare', formInfo='studiare · 现在分词/副动词', partOfSpeech='verb'),
  1707: dict(chinese='和……一起；用……；带着……（con+il）', english='with the; by the'),
  1712: dict(chinese='……的；从……（省音形式）', english='of; from'),
  1714: dict(chinese='从……；由……（da+lo）', english='from the; by the'),
  1715: dict(chinese='从……；由……（da+l’）', english='from the; by the'),
  1719: dict(chinese='在……里面；在……中（in+i）', english='in the'),
  1720: dict(chinese='到；向；在（介词变体）', english='to; at'),
  1727: dict(chinese='朝；向；大约；诗句', english='toward(s); about; verse'),
  1729: dict(chinese='直到；只要；在……期间', english='until; as long as; while'),
  1822: dict(chinese='好的（省音形式）', english='good'),
  1827: dict(chinese='持续；延续', english='to last; to continue', lemma='durare', formInfo='动词原形（不定式）', partOfSpeech='verb'),
  1859: dict(chinese='大约；关于；周围', english='about; approximately; around'),
  1865: dict(chinese='相当；宁可；而不是', english='rather; quite; instead'),
  1878: dict(chinese='超过；越过；此外', english='beyond; over; besides'),
  1880: dict(chinese='如何；怎样（省略形式）', english='how'),
  1933: dict(chinese='其中；对此；一些；从那里', english='of it/them; about it; some; from there'),
  1934: dict(chinese='自己；相互；（反身/无人称用法）', english='oneself; each other; impersonal/reflexive marker'),
  2018: dict(chinese='深的；深刻的；低沉的', english='deep; profound; low-pitched', partOfSpeech='adjective'),
  2025: dict(chinese='严重的；沉重的；低沉的', english='serious; grave; heavy; low-pitched', partOfSpeech='adjective'),
  2034: dict(chinese='众多的；人数多的', english='numerous; many', partOfSpeech='adjective'),
  2068: dict(chinese='其；其中；谁/哪个（关系代词）', english='whom; which; whose'),
  2081: dict(chinese='我们想想吧；让我们考虑一下', english='let us think about it', lemma='pensare', formInfo='pensare · 命令/建议式短语', partOfSpeech='verb'),
  2084: dict(chinese='我们把它传过去；我们把它度过', english='let us pass it; let us spend it', lemma='passare', partOfSpeech='verb'),
  2150: dict(chinese='下；向下', english='down'),
  2161: dict(chinese='向前；前面；继续', english='forward; ahead; go on'),
  2279: dict(chinese='胖的；肥的；脂肪', english='fat; fatty', partOfSpeech='adjective'),
  2314: dict(chinese='谁知道；也许', english='who knows; perhaps', partOfSpeech='adverb'),
  2409: dict(chinese='递给你；给你传递', english='to pass/give to you', lemma='passare', partOfSpeech='verb'),
  2456: dict(chinese='正在寻找；正在搜索', english='looking for; searching', lemma='cercare', formInfo='cercare · 现在分词/副动词', partOfSpeech='verb'),
  2466: dict(chinese='正在建造；建设中', english='building; constructing', lemma='costruire', formInfo='costruire · 现在分词/副动词', partOfSpeech='verb'),
  2472: dict(chinese='正在吃；吃着', english='eating', lemma='mangiare', formInfo='mangiare · 现在分词/副动词', partOfSpeech='verb'),
  2473: dict(chinese='正在尝试；正在试用/体验', english='trying; testing; experiencing', lemma='provare', formInfo='provare · 现在分词/副动词', partOfSpeech='verb'),
  2480: dict(chinese='正在打开', english='opening', lemma='aprire', formInfo='aprire · 现在分词/副动词', partOfSpeech='verb'),
  2485: dict(chinese='正在变成；正在成为', english='becoming; getting', lemma='diventare', formInfo='diventare · 现在分词/副动词', partOfSpeech='verb'),
  2489: dict(chinese='正在听；正在感觉', english='hearing; feeling', lemma='sentire', formInfo='sentire · 现在分词/副动词', partOfSpeech='verb'),
  2492: dict(chinese='正在拿；正在取；正在乘坐', english='taking; getting', lemma='prendere', formInfo='prendere · 现在分词/副动词', partOfSpeech='verb'),
  2499: dict(chinese='知道；明白（副动词）', english='knowing', lemma='sapere', formInfo='sapere · 现在分词/副动词', partOfSpeech='verb'),
  2505: dict(chinese='正在认识；正在了解', english='getting to know; knowing', lemma='conoscere', formInfo='conoscere · 现在分词/副动词', partOfSpeech='verb'),
  2518: dict(chinese='风格；样式', english='style', partOfSpeech='noun', gender='m', number='singular', article='lo', plural='stili'),
  2532: dict(chinese='经典的；古典的；经典作品', english='classic; classical', partOfSpeech='adjective'),
  2537: dict(chinese='复杂的；综合体；建筑群', english='complex; complicated; complex/building complex'),
  2542: dict(chinese='想象力；想象', english='imagination', partOfSpeech='noun', gender='f', number='singular', article="l'", plural='immaginazioni'),
  2551: dict(chinese='麻烦；困境；祸事', english='trouble; problem; jam', partOfSpeech='noun', gender='m', number='singular', article='il', plural='guai'),
  2563: dict(chinese='想要；愿意；需要（省略形式）', english='to want; to wish; to need', lemma='volere', partOfSpeech='verb'),
  2593: dict(chinese='发现；发现物', english='discovery; find', partOfSpeech='noun', gender='f', number='singular', article='la', plural='scoperte'),
  2613: dict(chinese='笔记；注释；音符；分数/成绩', english='note; annotation; musical note; grade', partOfSpeech='noun', gender='f', number='singular', article='la', plural='note'),
  2661: dict(chinese='会议；讲座；报告会', english='conference; lecture; talk', partOfSpeech='noun', gender='f', number='singular', article='la', plural='conferenze'),
  2662: dict(chinese='融资；资金；资助', english='financing; funding; financial backing', partOfSpeech='noun', gender='m', number='singular', article='il', plural='finanziamenti'),
  2664: dict(chinese='提议；建议；提案', english='proposal; suggestion; proposition', partOfSpeech='noun', gender='f', number='singular', article='la', plural='proposte'),
  2628: dict(chinese='晴朗的；阳光充足的', english='sunny; sunlit', partOfSpeech='adjective'),
  2703: dict(chinese='命运；宿命', english='destiny; fate', partOfSpeech='noun', gender='m', number='singular', article='il', plural='destini'),
  2713: dict(chinese='灵魂；心灵', english='soul; spirit', partOfSpeech='noun', gender='f', number='singular', article="l'", plural='anime'),
  2727: dict(chinese='攻击；袭击；进攻', english='attack; assault', partOfSpeech='noun', gender='m', number='singular', article="l'", plural='attacchi'),
  2761: dict(chinese='政府；治理；我治理', english='government; governing; I govern', partOfSpeech='noun', gender='m', number='singular', article='il', plural='governi'),
}

words=json.load(open(WORDS,encoding='utf-8'))
by={int(w['id']):w for w in words}
missing=[]
for wid,updates in PATCH.items():
    w=by.get(wid)
    if not w:
        missing.append(wid); continue
    for k,v in updates.items(): w[k]=v
if missing:
    raise SystemExit(f'missing ids: {missing}')

with open(WORDS,'w',encoding='utf-8') as f:
    json.dump(words,f,ensure_ascii=False,indent=2)
    f.write('\n')

# Keep word-family copies synchronized with the canonical learner-facing glosses.
# Some families embed a small copy of the word record, and stale copies previously
# allowed corrected words to show the old Chinese meaning in another screen.
FAMILIES=ASSETS/'word_families.json'
if FAMILIES.exists():
    families=json.load(open(FAMILIES,encoding='utf-8'))
    synced=0
    def sync_family(node):
        nonlocal_holder=None
        # Python nested traversal; return number of records changed.
        changed=0
        if isinstance(node,dict):
            try:
                wid=int(node.get('id')) if 'id' in node else None
            except Exception:
                wid=None
            if wid in PATCH and node.get('word')==by[wid].get('word'):
                new_zh=by[wid].get('chinese','')
                if node.get('chinese') != new_zh:
                    node['chinese']=new_zh
                    changed += 1
            for value in node.values():
                changed += sync_family(value)
        elif isinstance(node,list):
            for value in node:
                changed += sync_family(value)
        return changed
    synced=sync_family(families)
    with open(FAMILIES,'w',encoding='utf-8') as f:
        json.dump(families,f,ensure_ascii=False,indent=2); f.write('\n')
    print(f'Synchronized {synced} embedded word-family glosses')

ledger={
    'version':'3.1.9',
    'scannedWords':len(words),
    'correctionCount':len(PATCH),
    'method':'Full 2774-row learner-facing Italian/Chinese semantic review; high-confidence mismatches only. Ambiguous but defensible entries were left untouched.',
    'corrections':[
        {'id':wid,'word':by[wid]['word'],'chinese':by[wid]['chinese'],'english':by[wid]['english']}
        for wid in sorted(PATCH)
    ],
    'criticalRegressions':{
        'castro':'城堡；要塞；设防聚落（历史用语）',
        'bracciale':'手镯；臂环',
        'lagna':'抱怨；牢骚；烦人的抱怨',
        'destino':'命运；宿命',
        'governo':'政府；治理；我治理',
        'finanziamento':'融资；资金；资助',
        'misterioso':'神秘的',
        'stile':'风格；样式'
    }
}
with open(ASSETS/'lexical_semantic_quality_v319.json','w',encoding='utf-8') as f:
    json.dump(ledger,f,ensure_ascii=False,indent=2); f.write('\n')
print(f'Applied {len(PATCH)} v3.1.9 lexical semantic corrections to {len(words)} words')

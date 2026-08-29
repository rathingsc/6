#!/usr/bin/env python3
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
words=json.load(open(ASSETS/'words.json',encoding='utf-8'))
phrases=json.load(open(ASSETS/'frequent_phrases.json',encoding='utf-8'))
word_index={w.get('word','').lower():w for w in words}

def add_morph(out, italian, title, note):
    key=italian.lower()
    if key in word_index and key not in {x['italian'] for x in out}:
        out.append({'italian':italian,'title':title,'note':note})

morph=[]
explicit=[
 ('impossibile','im- + possibile','im- 在这里表示否定；possibile=可能的，所以 impossibile=不可能的。'),
 ('inutile','in- + utile','in- 在这里表示否定；utile=有用的，所以 inutile=没用的。'),
 ('possibilità','possibile → possibilità','把形容词 possibile“可能的”联想到名词 possibilità“可能性”。'),
 ('necessità','necessario → necessità','把 necessario“必要的”联想到名词 necessità“必要；需要”。'),
 ('felicità','felice → felicità','把 felice“开心的”联想到名词 felicità“幸福”。'),
 ('velocemente','veloce → velocemente','veloce=快的；加 -mente 变成副词，velocemente=快速地。'),
 ('lentamente','lento → lentamente','lento=慢的；加 -mente 变成副词，lentamente=慢慢地。'),
 ('sicuramente','sicuro → sicuramente','sicuro=确定的；加 -mente 后常表示“肯定地；当然”。'),
 ('probabilmente','probabile → probabilmente','probabile=可能的；加 -mente 后是 probabilmente“大概；可能”。'),
 ('fortunatamente','fortunato → fortunatamente','fortunato=幸运的；fortunatamente=幸运地；幸好。'),
 ('completamente','completo → completamente','completo=完整的；completamente=完全地。'),
 ('particolarmente','particolare → particolarmente','particolare=特别的；particolarmente=尤其；特别地。'),
 ('frequentemente','frequente → frequentemente','frequente=频繁的；frequentemente=经常地；频繁地。'),
 ('prenotazione','prenotare → prenotazione','prenotare=预订；prenotazione=预约；预订。把动词和名词一起记。'),
 ('organizzazione','organizzare → organizzazione','organizzare=组织；organizzazione=组织；安排。'),
 ('descrizione','descrivere → descrizione','descrivere=描述；descrizione=描述。'),
 ('decisione','decidere → decisione','decidere=决定；decisione=决定。'),
 ('costruzione','costruire → costruzione','costruire=建造；costruzione=建造；建筑。'),
 ('protezione','proteggere → protezione','proteggere=保护；protezione=保护；防护。'),
 ('ritornare','ri- + tornare','ri- 常带“再次 / 返回”的感觉；ritornare=返回。'),
]
for x in explicit:add_morph(morph,*x)

# Conservative recognition patterns. These are presented as word-form clues, not mechanical translations.
patterns=[
 ('zione','词尾 -zione','-zione 常见于名词，很多词和英语 -tion 有对应关系。把它当作“认形线索”，不要机械逐字拆译。'),
 ('sione','词尾 -sione','-sione 常见于名词，很多词和英语 -sion 有对应关系。把它当作“认形线索”。'),
 ('mente','词尾 -mente','-mente 常把形容词变成副词，功能很像英语 -ly。'),
 ('ità','词尾 -ità','-ità 常构成抽象名词，很多时候和英语 -ity 对应。'),
 ('abile','词尾 -abile','-abile 常有“能够……的 / 可……的”感觉，可联想英语 -able。'),
 ('ibile','词尾 -ibile','-ibile 常有“能够……的 / 可……的”感觉，可联想英语 -ible。'),
 ('atore','词尾 -atore','-atore 常出现在“做某事的人 / 某种工具”类名词里，看到词尾先判断词类。'),
]
for w in words:
    s=w.get('word','').lower()
    for suffix,title,note in patterns:
        if len(s)>len(suffix)+2 and s.endswith(suffix):
            add_morph(morph,w['word'],title,note)
            break

for s in ['artista','musicista','turista','dentista','giornalista']:
    add_morph(morph,s,'词尾 -ista','-ista 常指从事某职业、活动或具有某身份的人，可联想英语 -ist。')
for s in ['noioso','famoso','spaventoso','geloso','disgustoso','pericoloso','nervoso','delizioso','generoso','rumoroso','silenzioso','costoso','meraviglioso','ventoso']:
    add_morph(morph,s,'词尾 -oso','-oso 常见于形容词，常表达“有……性质 / 充满……感觉”。先把它识别成形容词线索。')

morph.sort(key=lambda x:x['italian'].lower())
json.dump({'version':'3.1.8','description':'High-confidence morphology and word-form memory hints. Recognition aid only; not a mechanical translation system.','count':len(morph),'entries':morph},open(ASSETS/'morphology_hints.json','w',encoding='utf-8'),ensure_ascii=False,indent=2)

# Common chunks: prefer exact surface matches so an unrelated lemma sense is not silently substituted.
stop=set("un una uno il lo la i gli le di del della dei delle degli a al allo alla ai alle agli da dal dallo dalla dai dalle dagli in nel nello nella nei nelle negli con su sul sulla sui sulle per tra fra e o ma che cosa chi come dove quando quanto quale quali questo questa questi queste quello quella quelli quelle mio mia miei mie tuo tua tuoi tue suo sua suoi sue nostro nostra nostri nostre vostro vostra vostri vostre io tu lui lei noi voi loro mi ti si ci vi me te se ne non si no è sono sei siamo siete essere ho hai ha abbiamo avete hanno posso puoi può possiamo potete possono devo devi deve dobbiamo dovete devono molto poco più meno bene male qui lì là oggi domani ieri ora già ancora sempre mai poi prima dopo".split())
def toks(s):
    return re.findall(r"[a-zàèéìòóù]+(?:'[a-zàèéìòóù]+)?",s.lower().replace('’',"'"))
phrase_by_id={p['id']:p for p in phrases}
# Manual fixed chunks already present in the curated phrase bank.
manual_ids=['v26_0284','v26_0367','v26_0368','v26_0369','v26_0370','v26_0371','v26_0372','v26_0373','v26_0374','v26_0375','v26_0376','v26_0377','v26_0378','v26_0379','v26_0380','v26_0381','v26_0382']
manual_targets={
 'v26_0284':['dipende'],
 'v26_0367':['avere','bisogno'],
 'v26_0368':['avere','voglia'],
 'v26_0369':['avere','paura'],
 'v26_0370':['accordo'],
 'v26_0371':['ritardo'],
 'v26_0372':['pronto'],
 'v26_0373':['riuscire'],
 'v26_0374':['provare'],
 'v26_0375':['smettere'],
 'v26_0376':['cominciare'],
 'v26_0377':['continuare'],
 'v26_0378':['pensare'],
 'v26_0379':['decidere'],
 'v26_0380':['cercare'],
 'v26_0381':['dimenticarsi'],
 'v26_0382':['ricordarsi'],
}
chunks={}
for pid in manual_ids:
    p=phrase_by_id.get(pid)
    if not p: continue
    for target in manual_targets.get(pid,[]):
        if target in word_index:
            chunks[target]={'italian':target,'kind':'fixed','phrase':p['it'],'chinese':p['zh'],'note':'固定搭配：把整个结构当成一个单位记。','sourceId':pid}

# Exact-token common chunks from the existing validated 430-phrase bank.
index={}
for order,p in enumerate(phrases):
    ts=toks(p['it'])
    for token in set(ts):
        if len(token)<4 or token in stop: continue
        priority=0 if '固定搭配' in p.get('note','') else (1 if any(k in p.get('note','') for k in ['最常用','必会','自然','常用']) else 4)
        score=priority*100+len(ts)+order/10000.0
        if token not in index or score<index[token][0]:index[token]=(score,p)
for w in words:
    key=w.get('word','').lower().replace('’',"'")
    if key in chunks or key not in index: continue
    p=index[key][1]
    chunks[key]={'italian':w['word'],'kind':'chunk','phrase':p['it'],'chinese':p['zh'],'note':'常用句块：把这个词放进整句一起记，避免只记孤立中文。','sourceId':p['id']}
chunk_list=sorted(chunks.values(),key=lambda x:x['italian'].lower())
json.dump({'version':'3.1.8','description':'Fixed collocations plus exact-token common chunks sourced from frequent_phrases.json.','count':len(chunk_list),'fixedCount':sum(x['kind']=='fixed' for x in chunk_list),'entries':chunk_list},open(ASSETS/'memory_chunks.json','w',encoding='utf-8'),ensure_ascii=False,indent=2)
print('morphology hints',len(morph),'memory chunks',len(chunk_list),'fixed',sum(x['kind']=='fixed' for x in chunk_list))

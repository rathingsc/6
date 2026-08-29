#!/usr/bin/env python3
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'app/src/main/assets'
words_path=AS/'words.json'
words=json.load(open(words_path,encoding='utf-8'))
by={int(w['id']):w for w in words}

# v3.2.2: manually curated corrections after auditing every existing learner-facing
# word example pair in words.json.  Do not auto-generate examples: a natural missing
# example is better than a fabricated/templated incorrect one.
fixes={
    # semantic/example mismatches and wording quality
    90:("Vorrei un’acqua naturale.","我想要一瓶无气水。"),
    168:("Cerco di seguire una dieta sana.","我尽量保持健康饮食。"),
    522:("I pomodori sono rossi.","西红柿是红色的。"),
    561:("Mi sente bene?","您听得清楚吗？"),
    603:("Vorrei una bottiglia d’acqua naturale.","我想要一瓶无气水。"),
    855:("È un paese europeo.","这是一个欧洲国家。"),
    948:("Sono le tre.","现在三点。"),
    950:("Le porgo le mie scuse per il ritardo.","我为迟到向您道歉。"),
    1391:("Il bagno è in fondo al corridoio.","洗手间在走廊尽头。"),
    1448:("Li ho già inviati.","我已经把它们发出去了。"),
    2035:("Mi serve l'originale per questa pratica.","办理这个手续我需要原件。"),
    2208:("Può scrivermi il numero della pratica?","您能把手续编号写给我吗？"),
    2616:("Vorrei chiedere un'informazione sugli orari.","我想咨询一下营业时间。"),

    # months before a- vowel: polished idiomatic form
    508:("Il corso inizia ad agosto.","课程在八月开始。"),
    876:("Il corso inizia ad aprile.","课程在四月开始。"),

    # clothing/accessories: correct agreement and word-specific Chinese instead of a generic placeholder
    145:("Vorrei provare questo zaino.","我想试背一下这个背包。"),
    149:("Vorrei provare questa camicia.","我想试穿这件衬衫。"),
    150:("Vorrei provare questa giacca.","我想试穿这件夹克。"),
    155:("Vorrei provare questo maglione.","我想试穿这件毛衣。"),
    156:("Vorrei provare questo cappello.","我想试戴这顶帽子。"),
    252:("Vorrei provare questa gonna.","我想试穿这条裙子。"),
    394:("Vorrei provare questa borsa.","我想试背一下这个包。"),
    400:("Vorrei vedere questo portafoglio.","我想看看这个钱包。"),
    422:("Questa scarpa mi sta stretta.","这只鞋我穿着有点紧。"),
    429:("Vorrei provare questa maglietta.","我想试穿这件T恤。"),
    439:("Questo guanto è troppo stretto.","这只手套太紧了。"),
    588:("Vorrei provare questo cappotto.","我想试穿这件大衣。"),
    596:("Vorrei comprare questo ombrello.","我想买这把雨伞。"),
    636:("Vorrei provare questo anello.","我想试戴这个戒指。"),
    989:("Vorrei provare questa maglia.","我想试穿这件针织衫。"),
    1053:("Cerco delle mutande di cotone.","我在找棉质内裤。"),
    1061:("Vorrei provare questa cintura.","我想试一下这条腰带。"),
    1066:("Vorrei provare questa sciarpa.","我想试戴这条围巾。"),
    1071:("Questo calzino è troppo piccolo.","这只袜子太小了。"),
    1223:("Vorrei provare questa collana.","我想试戴这条项链。"),
    1453:("Vorrei provare questo vestito.","我想试穿这件衣服。"),
    1506:("Vorrei provare questi jeans.","我想试穿这条牛仔裤。"),
    1673:("Vorrei provare questo abito.","我想试穿这套衣服。"),
    1677:("Questo sandalo mi sta bene.","这只凉鞋我穿着很合脚。"),
    1680:("Vorrei provare questo stivale.","我想试穿这只靴子。"),
    1681:("Questo pantalone è troppo lungo.","这条裤子太长了。"),

    # replace the broken 'Vorrei controllare <bare noun>' generator template with natural examples
    362:("La prossima settimana ho un appuntamento.","下周我有一个预约。"),
    379:("Vivo in Italia da un anno.","我在意大利住了一年。"),
    383:("Il contratto dura un mese.","这份合同为期一个月。"),
    404:("Il mio orologio è cinque minuti avanti.","我的手表快五分钟。"),
    409:("Sabato c'è una festa a casa di Marco.","周六马尔科家有一个聚会。"),
    410:("Domani è il mio compleanno.","明天是我的生日。"),
    484:("Ho un appuntamento tra due giorni.","我两天后有一个预约。"),
    935:("Ci vediamo a mezzogiorno.","我们中午见。"),
    982:("È la prima volta che vengo qui.","这是我第一次来这里。"),
    1002:("Qual è il suo numero di telefono?","您的电话号码是多少？"),
    1217:("Qual è la data dell'appuntamento?","预约日期是哪一天？"),
    1733:("Non ho molto tempo.","我没有太多时间。"),
    1734:("Segno l'appuntamento sul calendario.","我把预约记在日历上。"),
    1735:("Mi scusi per il ritardo.","对不起，我迟到了。"),
    1736:("Ho vissuto qui per un decennio.","我在这里住了十年。"),
    1737:("Questo edificio risale al diciannovesimo secolo.","这座建筑可以追溯到十九世纪。"),
    1738:("Aspetta un minuto, per favore.","请等一分钟。"),
    1739:("È stato un periodo difficile.","那是一段艰难的时期。"),
    1741:("Aspetti un momento, per favore.","请稍等片刻。"),
    1742:("Il treno arriva a mezzanotte.","火车午夜到达。"),
    1744:("Vorrei un paio di scarpe nere.","我想要一双黑鞋。"),
    1930:("L'età non è un problema.","年龄不是问题。"),
    1935:("Ho mangiato solo metà della pizza.","我只吃了一半披萨。"),
    2019:("In futuro vorrei lavorare in Italia.","将来我想在意大利工作。"),
    2122:("Ad agosto vado in vacanza.","我八月去度假。"),
    2167:("All'inizio era difficile.","一开始很难。"),
    2204:("Era un'altra epoca.","那是另一个时代。"),
    2357:("Con quale frequenza prende questo farmaco?","您多久服一次这种药？"),
    2397:("Qual è la durata del corso?","课程持续多长时间？"),
    2639:("Qual è la tua stagione preferita?","你最喜欢哪个季节？"),

    # replace vague/illogical medical template examples
    1654:("Ho molta fame.","我很饿。"),
    2194:("Ho paura degli aghi.","我怕针。"),
    2247:("Non ho abbastanza forza nelle braccia.","我的胳膊没有足够的力气。"),
    2452:("Qual è la sua data di nascita?","您的出生日期是什么？"),
    2544:("Ho sonno.","我困了。"),
    2595:("Oggi non ho energia.","我今天没什么精神。"),
    2686:("La mia assicurazione copre questa visita?","我的保险报销这次看诊吗？"),
    2707:("La depressione può influire sulla vita quotidiana.","抑郁会影响日常生活。"),
    2719:("La morte è un tema difficile da affrontare.","死亡是一个很难面对的话题。"),

    # improve generic meal translations while keeping natural Italian
    985:("Durante la colazione cerco di mangiare con calma.","早餐时我尽量慢慢吃。"),
    1599:("Durante la cena cerco di mangiare con calma.","晚餐时我尽量慢慢吃。"),
    1647:("Durante il pranzo cerco di mangiare con calma.","午餐时我尽量慢慢吃。"),

    # number template edge case
    1940:("Ho scritto un milione sul modulo.","我在表格上写了一百万。"),
}

changes=[]
for wid,(ex,zh) in fixes.items():
    w=by[wid]
    old=(w.get('example',''),w.get('exampleZh',''))
    if old!=(ex,zh):
        changes.append({'id':wid,'word':w['word'],'oldExample':old[0],'oldExampleZh':old[1],'newExample':ex,'newExampleZh':zh})
        w['example']=ex; w['exampleZh']=zh

# One example uncovered a missing common verb sense in the learner-facing Chinese meaning.
meaning_fixes={
    2696:'建议；劝告；委员会/理事会；我建议/劝告',
}
meaning_changes=[]
for wid,newzh in meaning_fixes.items():
    w=by[wid]; old=w.get('chinese','')
    if old!=newzh:
        w['chinese']=newzh
        meaning_changes.append({'id':wid,'word':w['word'],'oldChinese':old,'newChinese':newzh})

with open(words_path,'w',encoding='utf-8') as f:
    json.dump(words,f,ensure_ascii=False,indent=2); f.write('\n')

# Keep the v3.2.1 canonical Chinese snapshot aligned for the deliberate meaning correction.
canon_path=AS/'full_lexicon_retranslation_v321.json'
canon=json.load(open(canon_path,encoding='utf-8'))
cb={int(x['id']):x for x in canon}
for wid,newzh in meaning_fixes.items(): cb[wid]['chinese']=newzh
with open(canon_path,'w',encoding='utf-8') as f:
    json.dump(canon,f,ensure_ascii=False,indent=2); f.write('\n')

# Sync embedded family copies for learner-facing meaning fields.
fam_path=AS/'word_families.json'
fam=json.load(open(fam_path,encoding='utf-8'))
def sync(x):
    if isinstance(x,dict):
        try: wid=int(x.get('id',0))
        except: wid=0
        src=by.get(wid)
        if src and x.get('word')==src.get('word') and 'chinese' in x:
            x['chinese']=src['chinese']
        for v in x.values(): sync(v)
    elif isinstance(x,list):
        for v in x: sync(v)
sync(fam)
with open(fam_path,'w',encoding='utf-8') as f:
    json.dump(fam,f,ensure_ascii=False,indent=2); f.write('\n')

ledger={
    'version':'3.2.2',
    'allWordRowsScanned':2774,
    'existingExamplePairsAudited':sum(1 for w in words if str(w.get('example','')).strip()),
    'wordsWithoutExample':sum(1 for w in words if not str(w.get('example','')).strip()),
    'policy':'Audit every existing example pair; do not fabricate examples for empty rows. Missing is safer than templated incorrect Italian.',
    'exampleCorrectionCount':len(changes),
    'meaningCorrectionCount':len(meaning_changes),
    'exampleCorrections':changes,
    'meaningCorrections':meaning_changes,
}
with open(AS/'word_example_quality_v322.json','w',encoding='utf-8') as f:
    json.dump(ledger,f,ensure_ascii=False,indent=2); f.write('\n')
print('example corrections',len(changes),'meaning corrections',len(meaning_changes),'existing',ledger['existingExamplePairsAudited'],'missing',ledger['wordsWithoutExample'])

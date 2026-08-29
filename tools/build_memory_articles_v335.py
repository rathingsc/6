#!/usr/bin/env python3
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/assets/memory_articles.json'

articles = [
    {
      'id':'memory_01','title':'Una giornata italiana','titleZh':'第1篇 初到意大利的一天','subtitle':'咖啡馆、家庭、城市、餐厅、商店和计划','emoji':'☕','sections':[
        ('Al bar la mattina','早晨在咖啡馆',
         "Ogni mattina entro in un piccolo bar vicino a casa. Saluto il barista e ordino un cappuccino con latte e un cornetto. A volte prendo il caffè senza zucchero, altre volte scelgo un tè. Dico sempre per favore e grazie. Se incontro un amico, ci fermiamo qualche minuto a parlare prima di andare al lavoro. È un modo semplice e piacevole per cominciare la giornata.",
         '每天早上我都会走进家附近的一家小咖啡馆。我向咖啡师问好，点一杯加牛奶的卡布奇诺和一个羊角面包。有时我喝不加糖的咖啡，有时选茶。我总会说请和谢谢。如果遇到朋友，我们会停下来聊几分钟再去上班。这是开始一天的一种简单而愉快的方式。',
         ['bar','cappuccino','zucchero','grazie']),
        ('La famiglia e la città','家庭和城市',
         "Dopo colazione telefono a mia madre e chiedo come stanno mio padre, mio fratello e mia sorella. Poi esco e cammino verso il centro. Per strada vedo famiglie, bambini e persone che vanno al lavoro. Mi piace osservare la città: le piazze, le strade, il cielo e i palazzi. Quando qualcuno mi chiede di dove sono, rispondo che vivo in Italia e che sto imparando l'italiano.",
         '早餐后我给妈妈打电话，问爸爸、哥哥弟弟和姐姐妹妹怎么样。然后我出门步行去市中心。路上能看到家庭、孩子和去上班的人。我喜欢观察这座城市的广场、街道、天空和楼房。当有人问我来自哪里时，我回答说我住在意大利，而且正在学习意大利语。',
         ['madre','padre','centro','italiano']),
        ('Persone e pranzo','人物和午餐',
         "A mezzogiorno incontro due colleghi in un ristorante. Il cameriere ci porta il menu e noi scegliamo un tavolo vicino alla finestra. Io ordino pasta e tonno, un collega prende un uovo con insalata e l'altro una pizza. Parliamo delle persone che conosciamo, del lavoro e della famiglia. Alla fine chiediamo il conto e salutiamo il cameriere con un sorriso.",
         '中午我在一家餐厅和两位同事见面。服务员给我们拿来菜单，我们选了一张靠窗的桌子。我点意大利面和金枪鱼，一位同事点鸡蛋配沙拉，另一位点披萨。我们聊认识的人、工作和家庭。最后我们要账单，微笑着向服务员道别。',
         ['ristorante','menu','tavolo','conto']),
        ('Nel negozio','在商店里',
         "Nel pomeriggio passo in un negozio perché devo comprare alcune cose. Guardo i prezzi, cerco la cassa e confronto due prodotti. Una commessa mi aiuta a trovare quello che mi serve. Pago, prendo lo scontrino e metto tutto nella borsa. Prima di uscire controllo di non aver dimenticato niente. Fare acquisti diventa più facile quando conosco le parole giuste.",
         '下午我去一家商店，因为需要买些东西。我看价格、找收银台，并比较两件商品。一位女店员帮我找到需要的东西。我付款、拿小票，把东西都放进袋子里。出门前我检查没有忘东西。当我会用正确的词时，购物就变得容易多了。',
         ['negozio','prezzi','cassa','scontrino']),
        ('Chiacchiere e programmi','聊天和计划',
         "La sera incontro alcuni amici. Parliamo di quello che ci piace fare: mangiare fuori, passeggiare, cantare, nuotare o semplicemente bere qualcosa insieme. Uno di loro propone di andare al cinema nel fine settimana. Io sono libero sabato, quindi accetto. Prima di tornare a casa fissiamo l'ora e il luogo. Avere un piccolo programma mi dà qualcosa di bello da aspettare.",
         '晚上我和几个朋友见面。我们聊喜欢做的事情：在外面吃饭、散步、唱歌、游泳，或者只是一起喝点东西。其中一人提议周末去看电影。我周六有空，所以答应了。回家前我们确定了时间和地点。有一个小计划让我有所期待。',
         ['amici','passeggiare','sabato','programma'])
      ]
    },
    {
      'id':'memory_02','title':'Nuovi amici e vita quotidiana','titleZh':'第2篇 新朋友和日常生活','subtitle':'迷路、衣服、室友、邻居、旅行和礼物','emoji':'🧭','sections':[
        ('Mi sono perso','我迷路了',
         "Un giorno devo raggiungere un amico, ma prendo l'autobus sbagliato e mi perdo. Scendo vicino a una stazione e chiedo a una signora dove devo andare. Lei mi spiega che posso prendere un taxi oppure camminare per dieci minuti. Arrivo un po' in ritardo e dico: mi dispiace. Il mio amico sorride e risponde: non ti preoccupare.",
         '有一天我要去找一个朋友，但坐错了公交车，结果迷路了。我在一个车站附近下车，问一位女士应该怎么走。她告诉我可以坐出租车，也可以步行十分钟。我稍微迟到了一点，说很抱歉。朋友笑着回答别担心。',
         ['autobus','stazione','taxi','ritardo']),
        ('Comprare vestiti','买衣服',
         "Nel fine settimana vado a comprare dei vestiti. Provo una camicia, una gonna e un paio di stivali. Alcune cose sono troppo care, quindi cerco quelle in offerta. Chiedo alla commessa quanto costano e se posso provare una taglia diversa. Alla fine scelgo una maglietta semplice e uno zaino economico. Non compro tutto quello che vedo: preferisco scegliere con calma.",
         '周末我去买衣服。我试了一件衬衫、一条裙子和一双靴子。有些东西太贵，所以我找打折的。我问店员多少钱、能不能试不同尺码。最后我选了一件简单的T恤和一个便宜的背包。我不会看到什么都买，而是更喜欢慢慢挑。',
         ['camicia','stivali','offerta','zaino']),
        ('Con i coinquilini','和室友一起',
         "Vivo con due coinquilini. Ognuno ha i propri orari, ma la sera spesso ci troviamo in cucina. Prepariamo qualcosa da mangiare e raccontiamo come è andata la giornata. Un nuovo amico viene a trovarci e porta una bottiglia da condividere. Parliamo di hobby, musica e sport. Anche se siamo persone diverse, stare insieme rende la casa più allegra.",
         '我和两位室友住在一起。每个人的作息不同，但晚上我们经常在厨房碰面。我们做点吃的，讲讲这一天过得怎么样。一位新朋友来看我们，还带来一瓶饮料一起分享。我们聊爱好、音乐和运动。虽然大家性格不同，但一起生活让家里更热闹。',
         ['coinquilini','cucina','hobby','insieme']),
        ('I vicini','邻居们',
         "I miei vicini sono gentili. Quando ci incontriamo sulle scale ci salutiamo e scambiamo due parole. Una vicina anziana vive da sola, quindi ogni tanto le porto la spesa. Un altro vicino lavora molto e torna tardi. Se c'è un problema nel palazzo, cerchiamo di aiutarci. Conoscere i vicini fa sentire il quartiere più sicuro e familiare.",
         '我的邻居们很友善。我们在楼梯上遇到时会打招呼、聊两句。一位上年纪的女邻居独居，所以我偶尔帮她带些买的东西。另一位邻居工作很多，回来很晚。如果楼里有问题，我们会尽量互相帮助。认识邻居会让这个街区更安全、更有家的感觉。',
         ['vicini','scale','palazzo','quartiere']),
        ('Un viaggio e un regalo','旅行和礼物',
         "Per una breve vacanza parto in treno verso la Svizzera. Porto con me una piccola valigia e controllo più volte il biglietto. Durante il viaggio penso a un regalo per un'amica. In una città trovo un negozio con oggetti tipici e compro qualcosa di semplice. Quando torno, le consegno il pacco. Lei lo apre, sorride e dice che il regalo le piace molto.",
         '短假期间我坐火车去瑞士。我带着一个小行李箱，还反复检查车票。旅途中我想着给一位女朋友买份礼物。在一座城市里我找到一家卖当地特色物品的商店，买了一个简单的小东西。回来后我把包裹交给她，她打开后笑着说非常喜欢。',
         ['treno','Svizzera','regalo','pacco'])
      ]
    },
    {
      'id':'memory_03','title':'Un fine settimana pieno','titleZh':'第3篇 忙碌的周末','subtitle':'购物、借口、拜访、假期、米兰、天气和小谜团','emoji':'🛍️','sections':[
        ('Regali e shopping','礼物和购物',
         "Venerdì pomeriggio cerco un regalo di compleanno. Guardo orologi, scarpe, borse e guanti, ma non so subito cosa scegliere. Alcuni articoli sono belli ma costano troppo. Sul sito del negozio trovo un'offerta e decido di pagare con la carta di credito. Prima di confermare controllo il prezzo e l'indirizzo. Alla fine sono soddisfatto dell'acquisto.",
         '周五下午我寻找一份生日礼物。我看了手表、鞋、包和手套，但一开始不知道选什么。有些商品很好看但太贵。我在商店网站上找到一个优惠，决定用信用卡付款。确认前我检查价格和地址。最后我对这次购买很满意。',
         ['venerdì','regalo','offerta','confermare']),
        ('Una scusa e una visita','一个借口和一次拜访',
         "Giovedì avevo promesso di visitare un amico, ma al lavoro è arrivato un problema urgente. Gli telefono e spiego che sono occupato. Lui capisce e propone di vederci il giorno dopo. Quando finalmente arrivo a casa sua, sono stanco e ho sete. Beviamo qualcosa, parliamo a lungo e ridiamo dell'imprevisto. Una spiegazione sincera vale più di una scusa complicata.",
         '周四我本来答应去看一位朋友，但工作上突然来了一个紧急问题。我给他打电话解释自己很忙。他理解，并提议第二天见。终于到他家时我很累也很渴。我们喝了点东西，聊了很久，还拿这次意外开玩笑。真诚的解释比复杂的借口更有用。',
         ['giovedì','occupato','stanco','scusa']),
        ('Preparare le vacanze','准备假期',
         "Prima delle vacanze faccio una lista. Controllo i documenti, prenoto un posto dove dormire e decido cosa mettere in valigia. Voglio visitare una città nuova senza correre troppo. Cerco informazioni sui musei, sui mezzi pubblici e sugli orari. Porto anche una giacca perché il tempo può cambiare. Preparare bene il viaggio mi permette di rilassarmi di più.",
         '假期前我会列清单。我检查证件、预订住宿，并决定行李箱里放什么。我想去看一座新的城市，又不想太赶。我查博物馆、公共交通和时间表的信息，还带一件夹克，因为天气可能变化。把旅行准备好能让我更加放松。',
         ['vacanze','documenti','valigia','viaggio']),
        ('Una giornata a Milano','米兰的一天',
         "Arrivo a Milano la mattina e cammino verso il centro. Visito una piazza famosa, entro in un museo e poi cerco un posto tranquillo per pranzare. Nel pomeriggio incontro una persona che vive qui da molti anni e mi racconta alcuni segreti della città. Prima di ripartire compro un piccolo ricordo. Milano è grande, ma con una buona mappa diventa più facile orientarsi.",
         '我早上到米兰，步行去市中心。我参观一个著名广场、走进博物馆，然后找一个安静的地方吃午饭。下午我遇到一位在这里住了很多年的人，他告诉我这座城市的一些秘密。离开前我买了一个小纪念品。米兰很大，但有一张好地图就更容易找到方向。',
         ['Milano','centro','museo','mappa']),
        ('Pioggia e mistero','雨天和小谜团',
         "Domenica il cielo diventa scuro e comincia a piovere. Rimango in casa e sento un rumore strano vicino alla porta. Per un momento penso a un mistero, ma poi scopro che è soltanto il vento che muove una scatola sul balcone. Fuori fa freddo, quindi preparo una bevanda calda e guardo un film. A volte il tempo crea storie più grandi della realtà.",
         '周日天空变暗，开始下雨。我待在家里，听到门边有奇怪的声音。一瞬间我以为有什么谜团，后来发现只是风把阳台上的一个盒子吹动了。外面很冷，所以我做一杯热饮并看电影。有时候天气会把普通事情变成更大的故事。',
         ['domenica','piovere','mistero','vento'])
      ]
    },
    {
      'id':'memory_04','title':'Lavoro, studio e feste','titleZh':'第4篇 工作学习和庆祝','subtitle':'新工作、婚礼、大学、披萨、搬家、艺术和八卦','emoji':'💼','sections':[
        ('Il nuovo lavoro','新工作',
         "Il lunedì comincio un nuovo lavoro. Entro in ufficio, saluto il signor Rossi e mi siedo alla mia scrivania. Devo scrivere alcune email, inviare un documento e partecipare a una riunione. All'inizio sono un po' nervoso, ma i colleghi sono gentili. A pranzo parliamo del curriculum e del colloquio che ho fatto. Alla fine della giornata sono stanco ma felice.",
         '周一我开始一份新工作。我走进办公室，向罗西先生问好，然后坐到自己的办公桌前。我需要写一些邮件、发送文件并参加会议。刚开始我有点紧张，但同事们很友好。午餐时我们聊我的简历和之前的面试。一天结束时我很累，但也很开心。',
         ['lunedì','ufficio','riunione','colloquio']),
        ('Un matrimonio e l’università','婚礼和大学',
         "Mia cugina si sposa sabato. La famiglia prepara fiori, anelli e vestiti eleganti. Io però ho anche una lezione all'università e devo consegnare un lavoro. La mattina studio in biblioteca, poi torno a casa per cambiarmi. Al ricevimento incontro zii, nonni, amici e persone che non vedevo da anni. È una giornata lunga, piena di studio e di festa.",
         '我的表妹周六结婚。家人准备鲜花、戒指和正式的衣服。但我在大学还有一节课，还必须交作业。早上我在图书馆学习，然后回家换衣服。婚宴上我见到了叔叔阿姨、祖父母、朋友以及多年没见的人。这是漫长的一天，既有学习也有庆祝。',
         ['sposa','università','biblioteca','ricevimento']),
        ('Pizza dopo le lezioni','课后吃披萨',
         "Dopo le lezioni alcuni studenti propongono di andare in pizzeria. Ordiniamo pizze diverse e dividiamo una grande insalata. Il cameriere ci chiede che cosa vogliamo bere e noi scegliamo acqua e vino. Parliamo degli esami, dei professori e dei progetti per il futuro. Mangiare insieme dopo una giornata di studio ci aiuta a rilassarci e a conoscerci meglio.",
         '下课后几个学生提议去披萨店。我们点了不同的披萨，还一起分一大份沙拉。服务员问我们喝什么，我们选择水和葡萄酒。大家聊考试、教授和未来计划。学习一天后一起吃饭能让我们放松，也更了解彼此。',
         ['studenti','pizzeria','esami','professori']),
        ('Trasloco e notizie','搬家和消息',
         "Una mia amica cambia appartamento e le do una mano con il trasloco. Portiamo scatole, sedie e piccoli mobili nelle nuove stanze. Mentre lavoriamo, lei mi racconta alcune notizie sui nostri amici. Qualcuno ha cambiato lavoro, qualcuno si è fidanzato e un altro sta cercando casa. Tra una scatola e l'altra ridiamo molto. Un trasloco può diventare anche un momento per aggiornarsi.",
         '一位朋友换公寓，我帮她搬家。我们把箱子、椅子和小家具搬进新的房间。干活时她告诉我朋友们最近的一些消息：有人换了工作，有人订婚，还有人在找房子。搬箱子的间隙我们笑了很多。搬家也可以成为互相更新近况的时刻。',
         ['appartamento','trasloco','scatole','amici']),
        ('Arte e conversazioni','艺术和谈话',
         "La domenica visitiamo una mostra d'arte. Ci sono quadri, fotografie e opere moderne. Ognuno ha un'opinione diversa: a me piace un dipinto molto colorato, mentre un amico preferisce una scultura semplice. Dopo la visita prendiamo un caffè e continuiamo a parlare. L'arte ci dà nuove idee e trasforma una normale conversazione in qualcosa di più interessante.",
         '周日我们去看艺术展。那里有画、照片和现代作品。每个人的看法不同：我喜欢一幅色彩丰富的画，而一位朋友更喜欢一件简单的雕塑。参观后我们喝咖啡继续聊天。艺术给我们新的想法，也让普通谈话变得更有意思。',
         ['arte','fotografie','moderne','conversazione'])
      ]
    },
    {
      'id':'memory_05','title':'Viaggiare e vivere bene','titleZh':'第5篇 旅行和生活习惯','subtitle':'风俗、欧洲旅行、公益活动、做饭、纪念品、方向和日常','emoji':'✈️','sections':[
        ('Usi e costumi','风俗习惯',
         "Quando vivo in un paese nuovo cerco di osservare le abitudini locali. In Italia imparo quando si beve il caffè, come si saluta e quali orari sono comuni per pranzo e cena. Non tutto è uguale al mio paese, ma proprio per questo è interessante. Fare domande con rispetto e ascoltare le persone mi aiuta a capire meglio la cultura e a evitare piccoli errori.",
         '住在一个新国家时，我会观察当地习惯。在意大利，我学习什么时候喝咖啡、怎样问候，以及午饭和晚饭通常在几点。这里并不是所有事情都和我的国家一样，但正因为不同才有意思。礼貌提问并倾听别人，能帮助我更好理解文化，也避免小错误。',
         ['Italia','abitudini','pranzo','cultura']),
        ('Un giro in Europa','欧洲旅行',
         "Durante una vacanza viaggio in diversi paesi europei. Parto in aereo, poi uso treni e autobus per spostarmi. In ostello incontro tedeschi, francesi, spagnoli e persone di molte altre nazionalità. Parliamo in italiano e in inglese quando è necessario. Ogni città ha un ritmo diverso, ma ovunque trovo qualcuno disposto ad aiutare un viaggiatore che chiede informazioni con gentilezza.",
         '假期里我去几个欧洲国家旅行。我先坐飞机出发，然后用火车和公交车移动。在青年旅舍我遇到德国人、法国人、西班牙人和许多其他国家的人。需要时我们用意大利语和英语交流。每座城市节奏不同，但只要礼貌询问信息，到处都能遇到愿意帮助旅行者的人。',
         ['europei','aereo','ostello','informazioni']),
        ('Una raccolta di fondi','一次募捐活动',
         "Nel quartiere organizziamo una piccola raccolta di fondi per aiutare una famiglia. Ognuno porta qualcosa: cibo, libri, vestiti o un po' di denaro. Prepariamo un tavolo, scriviamo alcuni cartelli e invitiamo amici e vicini. Non raccogliamo una fortuna, ma il risultato è importante. Lavorare insieme per una buona causa crea fiducia e fa sentire tutti parte della comunità.",
         '社区里我们组织了一次小型募捐来帮助一个家庭。每个人都带来一些东西：食物、书、衣服或一点钱。我们摆好桌子、写一些牌子，并邀请朋友和邻居。虽然没有筹到巨款，但结果很重要。为一个好的目的共同努力会建立信任，也让大家感到自己属于这个社区。',
         ['quartiere','fondi','famiglia','comunità']),
        ('Cucinare insieme','一起做饭',
         "La sera cuciniamo insieme. Laviamo le verdure, tagliamo le cipolle e prepariamo la pasta. Sul tavolo ci sono bicchieri, piatti e pane. Uno di noi controlla il forno, un altro prepara l'insalata. Quando tutto è pronto ci sediamo e mangiamo senza fretta. Una ricetta semplice diventa un'occasione per parlare, ridere e imparare nuove parole legate alla cucina.",
         '晚上我们一起做饭。我们洗蔬菜、切洋葱并准备意大利面。桌上有杯子、盘子和面包。一个人看烤箱，另一个人做沙拉。全部准备好后我们坐下来慢慢吃。一道简单的菜也会成为聊天、欢笑和学习厨房相关新词的机会。',
         ['cuciniamo','verdure','tavolo','ricetta']),
        ('Orientarsi e ricordare','找方向和留下记忆',
         "Quando visito una città sconosciuta tengo sul telefono una mappa e l'indirizzo dell'albergo. Se non trovo una strada, chiedo indicazioni e ripeto il percorso per essere sicuro di aver capito. Prima di tornare compro un piccolo souvenir. A casa lo metto vicino a una fotografia del viaggio. Così un oggetto semplice mi ricorda le persone, i luoghi e le esperienze vissute.",
         '参观陌生城市时，我会在手机里保存地图和酒店地址。如果找不到一条街，我就询问方向，并重复路线确认自己听懂了。回去前我买一个小纪念品。到家后把它放在旅行照片旁边。这样一个简单的物品就能让我想起旅途中遇到的人、地方和经历。',
         ['mappa','albergo','indicazioni','souvenir'])
      ]
    },
    {
      'id':'memory_06','title':'Una giornata piena di impegni','titleZh':'第6篇 忙碌的一天','subtitle':'照看孩子、冰淇淋、商场、家务、侦探故事、课堂、动物园和办公室','emoji':'📚','sections':[
        ('Con i bambini e un gelato','带孩子和吃冰淇淋',
         "La mattina aiuto una famiglia a badare ai bambini. Giochiamo, prepariamo i cereali e mettiamo in ordine la stanza. Dopo pranzo usciamo e andiamo in gelateria. I bambini scelgono gusti diversi: fragola, limone, pistacchio e panna. Io prendo una piccola limonata. Prima di tornare a casa ci sediamo su una panca e raccontiamo quale gusto ci è piaciuto di più.",
         '早上我帮一个家庭照看孩子。我们一起玩、准备麦片并整理房间。午饭后我们出门去冰淇淋店。孩子们选择不同口味：草莓、柠檬、开心果和奶油。我喝一小杯柠檬水。回家前我们坐在长凳上，聊最喜欢哪种味道。',
         ['bambini','gelateria','fragola','panca']),
        ('Al centro commerciale','在商场',
         "Nel pomeriggio passo al centro commerciale. Devo comprare alcune cose per casa e voglio anche provare un paio di pantaloni. Entro nel camerino, controllo il colore e la misura e confronto due modelli. Poi compro sapone e asciugamani. Tornato a casa, non mi riposo subito: devo ancora pulire il bagno e mettere in ordine la cucina.",
         '下午我去商场。我需要买一些家用品，也想试一条裤子。我走进试衣间，检查颜色和尺码，并比较两个款式。然后我买了肥皂和毛巾。回家后我没有马上休息，因为还要打扫卫生间并整理厨房。',
         ['centro','camerino','misura','asciugamani']),
        ('Un piccolo mistero in casa','家里的小谜团',
         "Mentre faccio le faccende noto che una chiave non è al suo posto. Cerco sul tavolo, sotto il divano e vicino alla porta, ma non la trovo. Per scherzo mi sento come un detective. Seguo alcune tracce: una borsa aperta, una giacca spostata e una scatola sul pavimento. Alla fine scopro la chiave in una tasca. Il grande mistero era solo distrazione.",
         '做家务时我发现一把钥匙不在原来的地方。我在桌上、沙发下和门边找，却没找到。开玩笑地说，我感觉自己像个侦探。我顺着一些线索找：一个打开的包、一件挪动过的夹克和地上的盒子。最后在一个口袋里发现钥匙。所谓大谜团其实只是粗心。',
         ['chiave','detective','tracce','tasca']),
        ('Lezione e vecchi amici','上课和老朋友',
         "La sera frequento una lezione di italiano. L'insegnante scrive alcune frasi alla lavagna e noi leggiamo, ascoltiamo e rispondiamo alle domande. Dopo la lezione incontro per caso due vecchi compagni di corso. Non ci vedevamo da molto tempo, quindi ci fermiamo a parlare. Ricordiamo le prime lezioni e notiamo quanto siamo migliorati da allora.",
         '晚上我上一节意大利语课。老师在黑板上写一些句子，我们阅读、听并回答问题。下课后我偶然遇到两位以前的同学。我们很久没见，所以停下来聊天。我们回忆最初几节课，也发现自己从那时到现在进步了很多。',
         ['lezione','insegnante','domande','compagni']),
        ('Zoo e ufficio','动物园和办公室',
         "Il giorno dopo il mio ufficio organizza una visita allo zoo per le famiglie dei dipendenti. Vediamo cavalli, mucche, lupi, api, farfalle e molti altri animali. I bambini fanno tante domande e gli adulti scattano fotografie. È strano parlare di lavoro mentre guardiamo gli animali, ma l'atmosfera è rilassata. Una giornata diversa aiuta anche i colleghi a conoscersi meglio.",
         '第二天办公室为员工家庭组织了一次动物园参观。我们看到马、奶牛、狼、蜜蜂、蝴蝶和许多其他动物。孩子们问了很多问题，大人们拍照片。一边看动物一边聊工作有点奇怪，但气氛很放松。不同寻常的一天也能帮助同事更了解彼此。',
         ['ufficio','zoo','animali','fotografie'])
      ]
    },
    {
      'id':'memory_07','title':'Lavoro, relazioni e viaggio','titleZh':'第7篇 工作社交和出行','subtitle':'办公室、约会、音乐会、旅行、兄弟姐妹、卡普里和故事','emoji':'🎵','sections':[
        ('Messaggi e appuntamenti','消息和约会',
         "In ufficio ricevo un messaggio da una persona che mi piace. Mi propone di cenare insieme venerdì. Prima di rispondere controllo il calendario perché ho molte chiamate e un incontro con un cliente. Alla fine sono libero e accetto. Scrivo un messaggio semplice, senza pensare troppo. È meglio essere chiari e gentili che cercare una frase perfetta.",
         '在办公室里我收到一位我喜欢的人的消息，对方提议周五一起吃晚饭。回复前我查看日历，因为我有很多电话，还有一次客户会面。最后发现自己有空，就答应了。我写了一条简单的消息，没有想得太复杂。比起追求一句完美的话，清楚和友善更重要。',
         ['ufficio','messaggio','venerdì','cliente']),
        ('Una sera al concerto','音乐会之夜',
         "Sabato andiamo a un concerto. Arriviamo presto, troviamo i nostri posti e aspettiamo che inizi la musica. Il pubblico è numeroso e l'atmosfera è emozionante. Quando il cantante sale sul palco tutti applaudono. Dopo il concerto usciamo lentamente perché c'è molta gente. Parliamo delle canzoni che ci sono piaciute e decidiamo di rivederci la settimana successiva.",
         '周六我们去听音乐会。我们早早到达、找到座位，等音乐开始。观众很多，气氛令人兴奋。歌手登台时大家都鼓掌。音乐会结束后因为人多，我们慢慢离场。我们聊最喜欢的歌曲，并决定下周再见。',
         ['concerto','pubblico','cantante','settimana']),
        ('In viaggio con mia sorella','和姐姐妹妹旅行',
         "Parto per un breve viaggio con mia sorella. Prepariamo i bagagli, controlliamo i biglietti e raggiungiamo la stazione. Sul treno parliamo della nostra famiglia e dei progetti futuri. Abbiamo caratteri diversi, ma viaggiare insieme ci fa ridere molto. Quando arriviamo, cerchiamo subito l'albergo e poi usciamo a esplorare il centro.",
         '我和姐姐妹妹一起进行一次短途旅行。我们准备行李、检查车票并前往车站。火车上我们聊家庭和未来计划。我们的性格不同，但一起旅行总让我们笑很多。到达后我们先找酒店，然后出去探索市中心。',
         ['viaggio','sorella','stazione','albergo']),
        ('Due giorni a Capri','卡普里两天',
         "A Capri il mare è bellissimo. Camminiamo lungo la costa, facciamo fotografie e ci fermiamo in un piccolo bar con vista sull'acqua. Un amico racconta continuamente quanto è bravo a nuotare e quanto conosce l'isola. Noi ridiamo e gli chiediamo di mostrarci la strada migliore. Alla fine la sua sicurezza ci è utile: troviamo un punto panoramico davvero speciale.",
         '在卡普里，海非常美。我们沿海岸散步、拍照，并在一家能看到海的小咖啡馆停留。一位朋友不停地说自己游泳多厉害、对这座岛多熟。我们笑着让他带我们走最好的一条路。最后他的自信真的派上用场，我们找到了一个特别棒的观景点。',
         ['Capri','mare','fotografie','isola']),
        ('Una storia nella notte','夜里的故事',
         "La sera, nell'albergo, qualcuno racconta una storia di fantasmi. Dice che in una vecchia casa si sente una voce quando c'è vento. Nessuno ci crede davvero, ma tutti ascoltano in silenzio. Più tardi sentiamo un rumore nel corridoio e per un secondo ci guardiamo preoccupati. Poi scopriamo che era solo una porta. Ridiamo della nostra paura e andiamo a dormire.",
         '晚上在酒店里，有人讲了一个鬼故事。他说一栋老房子在起风时会听到声音。其实没人真的相信，但大家都安静地听。后来我们在走廊听到响声，一瞬间都担心地互相看。结果发现只是一扇门。我们笑自己的害怕，然后去睡觉。',
         ['fantasmi','vento','corridoio','porta'])
      ]
    },
    {
      'id':'memory_08','title':'Progetti, salute e inviti','titleZh':'第8篇 计划健康和邀请','subtitle':'兴趣圈、新年计划、喜欢的人、赠品、医生、邀请和基础表达','emoji':'❤️','sections':[
        ('Un nuovo progetto','一个新计划',
         "All'inizio dell'anno decido di imparare qualcosa di nuovo. Mi iscrivo a un piccolo gruppo di appassionati di musica e comincio a esercitarmi ogni giorno. Alcuni suonano la chitarra, altri il violino. Io voglio migliorare l'italiano e parlare con più persone. Non cerco risultati perfetti: preferisco studiare un po' ogni giorno e vedere progressi reali dopo qualche mese.",
         '年初我决定学习一些新东西。我加入了一个小型音乐爱好者小组，并开始每天练习。有些人弹吉他，有些人拉小提琴。我想提高意大利语，并和更多人交流。我不追求完美结果，更喜欢每天学一点，几个月后看到真正的进步。',
         ['musica','chitarra','violino','italiano']),
        ('Una persona speciale','一个特别的人',
         "Nel gruppo conosco una persona simpatica. Parliamo spesso dopo le prove e scopriamo di avere interessi simili. Un giorno mi invita a una festa e mi chiede se voglio portare qualcuno con me. Accetto, ma mi sento un po' nervoso. Scelgo un vestito semplice e preparo una piccola sorpresa. Alla festa capisco che non serve impressionare nessuno: basta essere se stessi.",
         '在小组里我认识了一个很讨人喜欢的人。排练后我们经常聊天，发现兴趣很相似。有一天对方邀请我参加聚会，还问我要不要带人一起去。我答应了，但有点紧张。我选了一套简单的衣服并准备一个小惊喜。到了聚会我明白，不需要刻意给别人留下印象，做自己就好。',
         ['simpatica','festa','vestito','sorpresa']),
        ('Un regalo gratuito','一份免费赠品',
         "Durante un evento c'è un piccolo concorso con alcuni premi. Compilo un modulo e partecipo senza aspettarmi niente. Qualche giorno dopo ricevo un messaggio: ho vinto un regalo gratuito. All'inizio penso che sia uno scherzo e controllo bene le informazioni. È tutto vero. Vado a ritirare il premio e lo condivido con un amico. La sorpresa rende la giornata più allegra.",
         '一次活动中有一个小比赛和几个奖品。我填表参加，并没有期待什么。几天后我收到消息，说赢得一份免费礼物。开始我以为是玩笑，于是认真核对信息，结果都是真的。我去领取奖品，并和一个朋友分享。这份惊喜让一天变得更开心。',
         ['concorso','premi','messaggio','gratuito']),
        ('Dal medico','去看医生',
         "Dopo alcuni giorni mi sento poco bene. Ho mal di pancia e sono molto stanco, quindi chiamo la clinica e prendo un appuntamento. Il medico mi fa alcune domande e mi visita con calma. Mi consiglia di riposare, bere molta acqua e prendere una medicina solo se necessario. Non è niente di grave. Esco più tranquillo e decido di ascoltare meglio il mio corpo.",
         '几天后我感觉不太舒服，肚子痛而且很累，所以给诊所打电话预约。医生问了我一些问题并认真检查。他建议我休息、多喝水，只有需要时才吃药。并不严重。我离开时安心多了，也决定以后更注意自己的身体。',
         ['clinica','appuntamento','medico','medicina']),
        ('Cena con gli amici','和朋友吃晚饭',
         "Quando sto meglio invito alcuni amici a cena. Apparecchiamo la tavola, prepariamo cibo semplice e parliamo in italiano. Se non conosco una parola, uso il dizionario oppure chiedo un esempio. Nessuno ride degli errori: tutti cercano di aiutarmi. Alla fine diciamo buonanotte e fissiamo un altro incontro. Imparare una lingua diventa più facile quando entra nella vita quotidiana.",
         '身体好些后我邀请几个朋友来吃晚饭。我们摆好餐桌、准备简单的食物，并用意大利语聊天。如果我不会一个词，就用词典或请别人举例。没人嘲笑错误，大家都想帮我。最后我们互道晚安，并约下次见面。当一门语言进入日常生活后，学习会容易很多。',
         ['cena','dizionario','buonanotte','lingua'])
      ]
    },
    {
      'id':'memory_09','title':'Cibo, famiglia e lavoro','titleZh':'第9篇 食物家庭和职业','subtitle':'动物、复数、食物、物主词、衣物、介词、时间、家庭、职业和家居','emoji':'🏠','sections':[
        ('Animali e parole al plurale','动物和复数',
         "In una fattoria didattica vediamo molti animali: cavalli, mucche, polli, anatre e perfino alcune farfalle vicino ai fiori. I bambini ripetono i nomi al singolare e al plurale. Poi facciamo merenda con mele e piccoli panini. È un esercizio semplice, ma collegare le parole a ciò che vediamo aiuta a ricordarle molto meglio.",
         '在一个教育农场里我们看到很多动物：马、奶牛、鸡、鸭，花旁边甚至还有一些蝴蝶。孩子们练习这些名称的单数和复数。之后我们用苹果和小三明治加餐。这是很简单的练习，但把单词和眼前看到的东西联系起来，会更容易记住。',
         ['cavalli','mucche','farfalle','panini']),
        ('A tavola e nell’armadio','餐桌和衣柜',
         "A pranzo preparo un pasto con patate, cipolle, carne e un po' di pepe. Sul tavolo metto olio, pane e succo d'arancia. Più tardi sistemo i vestiti: le mie camicie, i tuoi pantaloni e le nostre giacche devono andare negli armadi giusti. Parlare di cose concrete mi aiuta anche a usare meglio parole come mio, tuo, nostro e vostro.",
         '午饭我用土豆、洋葱、肉和一点胡椒做了一餐。桌上我放了油、面包和橙汁。之后我整理衣服：我的衬衫、你的裤子和我们的夹克要放进正确的衣柜。谈论具体物品也能帮助我更好使用我的、你的、我们的、你们的这些词。',
         ['patate','pepe','mie','nostre']),
        ('Dove sono le cose?','东西在哪里',
         "In casa provo a descrivere dove si trovano gli oggetti. Le chiavi sono sul tavolo, il libro è nella borsa, le scarpe sono vicino alla porta e il cappotto è nell'armadio. Poi descrivo i colori e quello che sto facendo: sto leggendo, sto scrivendo e sto preparando la cena. Le preposizioni diventano più chiare quando le uso con oggetti reali.",
         '在家里我练习描述物品的位置：钥匙在桌上，书在包里，鞋在门边，大衣在衣柜里。然后我描述颜色和正在做的事情：我在阅读、写字、准备晚饭。把介词和真实物品放在一起使用时，它们会变得更清楚。',
         ['sul','nella','porta','scrivendo']),
        ('Famiglia, tempo e collegamenti','家庭时间和连接词',
         "La domenica tutta la famiglia si riunisce. Alcuni arrivano presto, altri più tardi, ma cerchiamo di mangiare insieme. Parliamo del passato e del futuro, mentre i bambini giocano. Anche se abbiamo opinioni diverse, restiamo uniti. Quando racconto questa giornata uso parole come mentre, oppure, nonostante e finché: piccoli collegamenti che rendono le frasi più naturali.",
         '周日全家聚在一起。有些人来得早，有些晚一点，但我们尽量一起吃饭。我们聊过去和未来，孩子们在一旁玩。即使观点不同，我们仍然团结。讲述这一天时，我会用当……时、或者、尽管、直到这些词，它们能让句子更自然。',
         ['domenica','mentre','nonostante','finché']),
        ('Professioni e casa','职业和家',
         "Tra i miei conoscenti ci sono persone con lavori molto diversi: un avvocato, un ingegnere, un infermiere, un meccanico e una segretaria. Quando tornano a casa, però, tutti fanno cose normali: aprono il cancello, entrano in cortile, fanno una doccia e si siedono sul divano. Le professioni cambiano, ma molti momenti della vita quotidiana sono uguali per tutti.",
         '我认识的人里有很多不同职业：律师、工程师、护士、机械师和秘书。但回到家后，大家做的都是普通事情：打开大门、走进院子、洗澡、坐到沙发上。职业不同，但日常生活中的许多时刻对所有人都一样。',
         ['avvocato','ingegnere','cortile','divano'])
      ]
    },
    {
      'id':'memory_10','title':'Una settimana in Italia','titleZh':'第10篇 在意大利的一周','subtitle':'形容词、常用动词、副词、地点、物品、人物、数字和综合表达','emoji':'🇮🇹','sections':[
        ('Lunedì: descrivere la realtà','周一 描述现实',
         "Lunedì osservo le cose intorno a me e provo a descriverle con precisione. Un edificio può essere moderno o vecchio, una valigia leggera o pesante, una strada lunga o breve. Alcune situazioni sono normali, altre uniche. Poi uso i verbi che conosco: cercare un indirizzo, attraversare la strada, controllare un documento, scegliere un posto e spiegare un problema. Più descrivo, più le parole diventano vive.",
         '周一我观察周围的东西，并尝试准确描述它们。一栋楼可以是现代的或老旧的，一个行李箱可以轻或重，一条路可以长或短。有些情况很普通，有些则很独特。然后我使用学过的动词：找地址、过马路、检查文件、选择地点、解释问题。描述得越多，单词就越鲜活。',
         ['moderno','pesante','attraversare','spiegare']),
        ('Martedì: parlare con più naturalezza','周二 说得更自然',
         "Martedì provo a collegare meglio le frasi. Non voglio parlare soltanto con parole isolate. Dico invece, tuttavia, soprattutto, almeno, appena e ovviamente quando servono davvero. Se dimentico qualcosa, ritorno alla frase e la correggo. Se qualcuno mi fa una domanda, cerco di rispondere completamente. Parlare con calma è piuttosto utile: mi dà il tempo di scegliere la parola giusta.",
         '周二我练习把句子连接得更好，不想只说孤立的单词。我在真正需要时使用“而是、然而、尤其、至少、刚刚、显然”等词。如果忘了什么，我就回到句子重新改。如果有人问我问题，我尽量完整回答。慢慢说其实很有用，它给我时间选择正确的词。',
         ['tuttavia','soprattutto','appena','rispondere']),
        ('Mercoledì: luoghi e oggetti','周三 地点和物品',
         "Mercoledì faccio alcune commissioni. Attraverso il quartiere, passo davanti a un palazzo e raggiungo una libreria vicino al ponte. Poi vado in un ufficio e porto con me un foglio, il diario e le chiavi. Tornando a casa vedo il porto in lontananza e una nave che parte. Ogni luogo e ogni oggetto diventa un'occasione per ripassare una parola.",
         '周三我去办几件事。我穿过街区、经过一栋大楼，到桥附近的一家书店。然后去一个办公室，随身带着一张纸、记事本和钥匙。回家路上我远远看到港口和一艘出发的船。每个地点和物品都成了复习单词的机会。',
         ['quartiere','libreria','foglio','nave']),
        ('Giovedì: persone, quantità e scelte','周四 人物数量和选择',
         "Giovedì incontro una folla di persone durante un evento. Ci sono giovani, adulti, cittadini e ospiti di diverse generazioni. Conto i tavoli e noto che alcuni sono pieni, altri quasi vuoti. Non serve conoscere ogni persona: basta capire chi parla, quante persone ci sono e quale gruppo devo seguire. Numeri e determinanti sembrano piccoli, ma rendono il messaggio molto più preciso.",
         '周四我在一场活动中遇到很多人，有年轻人、成年人、市民和不同年龄的来宾。我数桌子，发现有些坐满了，有些几乎是空的。没必要认识每个人，只要弄清谁在说话、有多少人、该跟哪一组就行。数字和限定词看起来很小，却能让信息准确很多。',
         ['folla','generazioni','alcuni','quante']),
        ('Fine settimana: usare davvero la lingua','周末 真正使用语言',
         "Nel fine settimana decido di usare l'italiano il più possibile. Pianifico una passeggiata, parcheggio la macchina, incontro amici e accetto di provare qualcosa di nuovo. Camminiamo, ridiamo, facciamo domande e raccontiamo quello che è successo durante la settimana. Se sbaglio, non mi fermo: provo a migliorare. Dopo tante parole studiate, il vero obiettivo è semplice: capire gli altri e farmi capire.",
         '周末我决定尽可能多地使用意大利语。我计划散步、停好车、见朋友，并答应尝试新事物。我们走路、笑、提问，也讲这一周发生的事情。说错时我不停下来，而是继续改进。学了这么多词以后，真正的目标其实很简单：听懂别人，也让别人听懂我。',
         ['pianifico','parcheggio','migliorare','capire'])
      ]
    }
]

# Assign exactly the first 2000 word IDs: 200/article, 40/section.
for ai,a in enumerate(articles):
    a['targetWordIds']=list(range(ai*200+1,(ai+1)*200+1))
    for si,s in enumerate(a['sections']):
        title,titleZh,text,translation,cloze=s
        start=ai*200+si*40+1
        end=start+40
        a['sections'][si]={
            'id':f"{a['id']}_s{si+1}",
            'title':title,
            'titleZh':titleZh,
            'text':text,
            'translation':translation,
            'targetWordIds':list(range(start,end)),
            'clozeWords':cloze,
        }

OUT.write_text(json.dumps(articles,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'wrote {OUT} with {len(articles)} articles, {sum(len(a["sections"]) for a in articles)} sections')

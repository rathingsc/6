#!/usr/bin/env python3
"""v3.0.4 learner-facing exercise quality gate.

Blocks the exact families of low-value questions found during real-device testing:
self-referential examples, answer leakage, duplicate visible choices and answer-field mismatch.
"""
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
errors=[]
def err(x): errors.append(x)
words=json.load(open(ASSETS/'words.json',encoding='utf-8'))

bad_it=('Oggi ripasso la parola «',"Ripeto l'espressione «")
bad_zh_markers=(
    '人/角色','这个概念','这个商店、地点或工作人员','这个颜色的型号','这个身体部位不舒服或疼',
    '这个自然景观或植物','这种动物','这项手续或规定','这个学习内容','这个环境或自然话题','这个健康问题',
    '这个家居或住房相关项目','这个设备、软件或网络项目','这个购物项目','这个日常项目','这个工作或学习项目',
    '这个交通工具或设施','这个尺寸、颜色或材质','这个治疗或医疗项目','这个身体部位或指标','这个项目的信息',
    '这种情绪或人际话题','这种食物或饮品','这种状态','这个群体或社会概念','这个信息、材料或工具',
    '这个结果看起来具有这种特点','这个东西具有这种特征'
)
nonempty=0;usable=0
for w in words:
    it=(w.get('example') or '').strip();zh=(w.get('exampleZh') or '').strip();target=(w.get('word') or '').strip()
    if not it: continue
    nonempty+=1
    if any(it.startswith(x) for x in bad_it): err(f"{w.get('id')} {target}: self-referential Italian example remains: {it}")
    if any(m in zh for m in bad_zh_markers): err(f"{w.get('id')} {target}: placeholder Chinese example remains: {zh}")
    if target and target.lower() in zh.lower(): err(f"{w.get('id')} {target}: Chinese support leaks the Italian answer: {zh}")
    if target and target.lower() in it.lower() and zh:
        usable+=1
if nonempty<700: err(f'only {nonempty} non-empty examples remain; expected >=700 after quality cleanup')
if usable<600: err(f'only {usable} directly usable contextual examples remain; expected >=600')

byid={int(w['id']):w for w in words}
expected={
 385:('下一个；下一位','Qual è il prossimo treno?','下一班火车是哪一班？'),
 585:('滑雪','In inverno mi piace sciare.','冬天我喜欢滑雪。'),
 590:('读；阅读','Mi piace leggere la sera.','我喜欢晚上读书。'),
 600:('我在；我待着；我感觉','Sto cercando lavoro.','我正在找工作。'),
 601:('我喝；喝','A cena bevo acqua.','晚饭时我喝水。'),
 729:('照片；摄影','Ho scattato una fotografia del Duomo.','我拍了一张大教堂的照片。'),
}
for wid,(zh,it,itzh) in expected.items():
    w=byid.get(wid)
    if not w or (w.get('chinese'),w.get('example'),w.get('exampleZh'))!=(zh,it,itzh):
        err(f'word {wid} real-device regression not fixed')

# A four-choice meaning question must always be able to show four distinct visible labels.
unique_zh={((w.get('chinese') or w.get('english') or '').strip().lower()) for w in words}
unique_it={((w.get('word') or '').strip().lower()) for w in words}
if len(unique_zh)<4: err('dataset cannot provide 4 unique Chinese option labels')
if len(unique_it)<4: err('dataset cannot provide 4 unique Italian option labels')

practice=(JAVA/'PracticeFragment.java').read_text(encoding='utf-8')
for marker in ('makeMeaningOptions','Set<String> labels','normalizeLabel','feedbackAnswer()','mode==MODE_REVIEW||mode==MODE_WRONG?safeChinese(current):current.word'):
    if marker not in practice: err('PracticeFragment choice/feedback guard missing: '+marker)
if 'button_mode_wrong).setOnClickListener(x->((MainActivity)requireActivity()).openWrongWordRepair())' not in practice: err('legacy wrong-word button must route to the dedicated relearn flow')
engine=(JAVA/'CourseLessonEngine.java').read_text(encoding='utf-8')
if 'ExampleQuality.isUsable' not in engine: err('CourseLessonEngine does not use central example-quality guard')
quality=(JAVA/'ExampleQuality.java')
if not quality.exists(): err('ExampleQuality.java missing')
for name in ('StudySessionFragment.java','WordAdapter.java','WrongWordRepairFragment.java','StubbornWordsFragment.java','SmartClozeEngine.java','CommuteAudioService.java'):
    src=(JAVA/name).read_text(encoding='utf-8')
    if 'ExampleQuality.isUsable' not in src: err(f'{name} can still expose placeholder examples')

# v3.0.4 beginner listening contract: audio -> Chinese meaning shown under the audio cue, choices remain Italian.
engine_listen=re.search(r'private CourseQuestion listen\([^}]+?return q;}',engine,re.S)
if not engine_listen:
    err('CourseLessonEngine listen() not found')
else:
    body=engine_listen.group(0)
    for marker in ('听音频，选择你听到的意大利语','q.support=zh(w)','q.answer=w.word','q.options.addAll(italianOptions(w,u,rnd))'):
        if marker not in body: err('CourseLessonEngine listening support/Italian-choice contract missing: '+marker)
    if 'q.answer=zh(w)' in body or 'q.options.addAll(chineseOptions(w,u,rnd))' in body:
        err('CourseLessonEngine listening incorrectly uses Chinese choices')
for marker in ('听音选词 · 听力专项','question.setText("🔊")','hint.setText(safeChinese(current))','mode==MODE_REVIEW||mode==MODE_WRONG','bindAnswer(answers.get(i),options.get(i),options.get(i).word)','mode==MODE_REVIEW||mode==MODE_WRONG?safeChinese(current):current.word'):
    if marker not in practice: err('PracticeFragment Chinese-support/Italian-choice listening contract missing: '+marker)
if '听发音，选择正确中文意思' in practice:
    err('PracticeFragment still uses Chinese answer choices in basic listening mode')

level_exam=(JAVA/'LevelExamFragment.java').read_text(encoding='utf-8')
for marker in ('q.category="听音选词"','q.prompt="🔊\\n"+safeChinese(w)','q.answer=w.word','opts.add(w.word)'):
    if marker not in level_exam: err('LevelExam listening Chinese-support/Italian-choice contract missing: '+marker)

if errors:
    for x in errors[:100]: print('ERROR:',x)
    print('EXERCISE QUALITY CHECK FAILED:',len(errors),'error(s)')
    sys.exit(1)
print(f'Exercise quality OK: {len(words)} words, {nonempty} retained contextual examples, {usable} directly usable cloze/context examples, no self-referential templates or answer leaks')

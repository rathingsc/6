#!/usr/bin/env python3
"""v3.3.0 four-mode smart mastery and learner-feedback quality gate."""
from pathlib import Path
import json, re, sys, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
RES=ROOT/'app/src/main/res/layout'
ASSETS=ROOT/'app/src/main/assets'
errors=[]
def need(path,token,msg):
    p=Path(path)
    if not p.exists(): errors.append(f'MISSING: {p.relative_to(ROOT) if p.is_absolute() else p}'); return ''
    s=p.read_text(encoding='utf-8')
    if token not in s: errors.append(msg)
    return s
engine=need(JAVA/'SmartReviewModeEngine.java','MODE_CLOZE','four-mode SmartReviewModeEngine missing cloze mode')
for token in ['MODE_IT_ZH','MODE_ZH_IT','MODE_LISTENING','modeForDimension','clozeSentence','studyForm']:
    if token not in engine: errors.append(f'SmartReviewModeEngine missing {token}')
store=need(JAVA/'ProgressStore.java','smartOverallPct','four-dimensional smart mastery percentages missing')
for token in ['smartRecognitionPct','smartRecallPct','smartListeningPct','smartUsagePct','smartMastered','recordSmartWordRating(int id,int rating,long responseMs,int reviewMode)']:
    if token not in store: errors.append(f'ProgressStore missing {token}')
session=need(JAVA/'StudySessionFragment.java','setupChoices','StudySession four-mode question UI missing')
for token in ['SmartReviewModeEngine.choose','MODE_LISTENING','MODE_CLOZE','answerChoice','forcedRatingMax','button_report_word_issue','IssueReportStore.CATEGORIES']:
    if token not in session: errors.append(f'StudySession missing {token}')
issue=need(JAVA/'IssueReportStore.java','displayText','local issue queue missing')
for token in ['翻译错误','例句错误','音频错误','题目答案错误','exportText','clear()']:
    if token not in issue: errors.append(f'IssueReportStore missing {token}')
settings=need(JAVA/'SettingsFragment.java','button_issue_reports','settings pending-issue entry missing')
if 'showIssueReports' not in settings or '复制全部' not in settings: errors.append('Settings issue-list viewer/copy action missing')
home=need(JAVA/'CourseHomeFragment.java','四模式','course home does not explain four-mode smart memory')
layout=RES/'fragment_study_session.xml'
try:
    root=ET.parse(layout).getroot(); text=layout.read_text(encoding='utf-8')
except Exception as e:
    errors.append(f'fragment_study_session.xml parse failed: {e}'); text=''
for rid in ['panel_smart_choices','button_smart_choice_1','button_smart_choice_2','button_smart_choice_3','button_smart_choice_4','text_smart_choice_feedback','button_report_word_issue']:
    if f'@+id/{rid}' not in text: errors.append(f'study-session layout missing {rid}')
settings_xml=(RES/'fragment_settings.xml').read_text(encoding='utf-8')
if '@+id/button_issue_reports' not in settings_xml: errors.append('settings layout pending-issue button missing')
words=json.load(open(ASSETS/'words.json',encoding='utf-8'))
nouns=[w for w in words if w.get('partOfSpeech')=='noun']; verbs=[w for w in words if w.get('partOfSpeech')=='verb']
article=sum(bool((w.get('article') or '').strip()) for w in nouns); lemma=sum(bool((w.get('lemma') or '').strip()) for w in verbs)
if article<600: errors.append(f'not enough nouns carry article metadata: {article}')
if lemma<400: errors.append(f'not enough verbs carry lemma metadata: {lemma}')
cloze=sum(bool((w.get('example') or '').strip()) and bool(re.search(r'(?<![\w’\'])'+re.escape(w.get('word',''))+r'(?![\w’\'])',w.get('example',''),re.I)) for w in words if w.get('word'))
if cloze<600: errors.append(f'not enough safe exact-word cloze examples: {cloze}')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print(f'Smart mastery OK: 4 review modes, {article}/{len(nouns)} nouns with articles, {lemma}/{len(verbs)} verbs with lemmas, {cloze} safe cloze rows, local issue queue enabled')

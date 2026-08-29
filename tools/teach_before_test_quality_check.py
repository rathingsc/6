#!/usr/bin/env python3
"""v3.3.4 gate: vocabulary meaning choices teach the translation before testing.

Mixed listening/sentence/grammar nodes are allowed to introduce a word through Chinese support or
context, but any direct Italian->Chinese meaning choice in a learning node must keep the two-phase
teach-before-test behavior.
"""
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def read(rel):
    p=ROOT/rel
    if not p.exists(): errors.append(f'missing {rel}'); return ''
    return p.read_text(encoding='utf-8', errors='replace')
q=read('app/src/main/java/com/italiano2774/nativeapp/CourseQuestion.java')
engine=read('app/src/main/java/com/italiano2774/nativeapp/CourseLessonEngine.java')
fragment=read('app/src/main/java/com/italiano2774/nativeapp/CourseLessonFragment.java')
checks=[
 ('teachBeforeTest' in q,'CourseQuestion teachBeforeTest flag missing'),
 ('meaning(w,unit,rnd,true)' in engine,'guided learning has no teach-before-test meaning cards'),
 ('q.teachBeforeTest=teachBeforeTest' in engine,'teach-before-test flag is not copied into CourseQuestion'),
 ('q.type==CourseQuestion.MEANING&&q.teachBeforeTest' in fragment,'meaning preview condition missing'),
 ('先看一遍翻译' in fragment,'translation teaching screen copy missing'),
 ('第一次见新词不需要猜' in fragment,'beginner no-guess guidance missing'),
 ('我看过了 · 开始选择' in fragment,'teaching-to-choice action missing'),
 ('meaningPreviewPassedIndex=index' in fragment,'meaning preview does not advance to choice phase'),
 ('q.support=zh(w)' in engine,'listening learning card does not expose Chinese support'),
 ('sentenceLearning' in engine,'context-first sentence learning missing'),
]
for ok,msg in checks:
    if not ok: errors.append(msg)
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Teach-before-test OK: direct meaning choices teach first; mixed listening/sentence nodes introduce meaning through support/context instead of blind guessing.')

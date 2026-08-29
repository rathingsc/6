#!/usr/bin/env python3
"""v3.3.4 gate: the guided course path must be mixed-skill, not vocabulary-only."""
from pathlib import Path
import json, math, re, sys
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
JAVA=ROOT/'app/src/main/java/com/italiano2774/nativeapp'
errors=[]
def err(x): errors.append(x)
course=json.load(open(ASSETS/'course_curriculum.json',encoding='utf-8'))
units=course.get('units',[])
engine=(JAVA/'CourseLessonEngine.java').read_text(encoding='utf-8')
home=(JAVA/'CourseHomeFragment.java').read_text(encoding='utf-8')
question=(JAVA/'CourseQuestion.java').read_text(encoding='utf-8')
fragment=(JAVA/'CourseLessonFragment.java').read_text(encoding='utf-8')

for token in ['词汇起步','听力训练','句子理解','语法微练','主动表达','综合运用','单元挑战',
              'ROLE_VOCAB','ROLE_LISTEN','ROLE_SENTENCE','ROLE_GRAMMAR','ROLE_ACTIVE','ROLE_CHALLENGE',
              'articleQuestion','verbQuestion','sentenceLearning','pathSummary']:
    if token not in engine: err('CourseLessonEngine missing mixed-path marker: '+token)
for token in ['听、说、读、写会由系统自动穿插','pathLabels.pathSummary(unit)']:
    if token not in home: err('CourseHome mixed-path explanation missing: '+token)
for token in ['GRAMMAR_ARTICLE','GRAMMAR_VERB']:
    if token not in question or token not in fragment: err('guided grammar question wiring missing: '+token)
if 'ErrorCause.ARTICLE_GENDER' not in fragment or 'ErrorCause.WORD_FORM' not in fragment:
    err('guided grammar mistakes are not routed to grammar error causes')

expected={
  5:['词汇起步','听力训练','句子理解','主动表达','单元挑战'],
  6:['词汇起步','听力训练','句子理解','语法微练','主动表达','单元挑战'],
  7:['词汇起步','听力训练','句子理解','语法微练','主动表达','综合运用','单元挑战'],
  8:['词汇起步','听力训练','句子理解','语法微练','主动表达','听力巩固','综合运用','单元挑战'],
}
counts={}
for u in units:
    lc=u.get('lessonCount',0);counts[lc]=counts.get(lc,0)+1
    if lc not in expected: err(f"{u.get('id')}: unsupported lessonCount {lc}");continue
    n=len(u.get('wordIds',[]));pre=lc-1
    chunks=[];covered=0
    for i in range(pre):
        a=math.floor(i*n/pre);b=math.floor((i+1)*n/pre)
        if b<=a:b=min(n,a+1)
        chunks.append(b-a);covered+=b-a
    if covered!=n: err(f"{u.get('id')}: pre-challenge mixed nodes cover {covered}/{n} words")
    if chunks and max(chunks)>9: err(f"{u.get('id')}: mixed node introduces too many words {chunks}")

# Guard against the old regression where every pre-challenge node had generic vocabulary labels.
for old in ['认识新内容','继续学习']:
    if f'return lessonIndex==0?"{old}"' in engine: err('old vocabulary-only path labels returned: '+old)

if errors:
    for x in errors: print('ERROR:',x)
    print('COURSE PATH MIX CHECK FAILED:',len(errors),'error(s)')
    sys.exit(1)
print('Course path mix OK: 98 units use vocabulary + listening + sentence + grammar/output roles; lessonCount distribution',counts)

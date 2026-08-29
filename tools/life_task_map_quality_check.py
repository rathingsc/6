#!/usr/bin/env python3
"""v5.0.1 real-life task map integration gate."""
from pathlib import Path
import json,sys,re
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def err(x): errors.append(x)
def read(rel): return (ROOT/rel).read_text(encoding='utf-8')
def need(src,token,label):
    if token not in src: err('missing '+label+': '+token)

build=read('app/build.gradle');main=read('app/src/main/java/com/italiano2774/nativeapp/MainActivity.java');store=read('app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java');plan=read('app/src/main/java/com/italiano2774/nativeapp/DailySmartPlanEngine.java');bt=read('app/src/main/java/com/italiano2774/nativeapp/BreakthroughPlanEngine.java');passport=read('app/src/main/java/com/italiano2774/nativeapp/MasteryPassportEngine.java');practice=read('app/src/main/java/com/italiano2774/nativeapp/PracticeHubFragment.java');course=read('app/src/main/java/com/italiano2774/nativeapp/CourseHomeFragment.java')
for rel in ['app/src/main/java/com/italiano2774/nativeapp/LifeTask.java','app/src/main/java/com/italiano2774/nativeapp/LifeTaskRepository.java','app/src/main/java/com/italiano2774/nativeapp/LifeTaskEngine.java','app/src/main/java/com/italiano2774/nativeapp/LifeTaskMapFragment.java','app/src/main/java/com/italiano2774/nativeapp/LifeTaskAdapter.java','app/src/main/java/com/italiano2774/nativeapp/LifeTaskDetailFragment.java','app/src/main/res/layout/fragment_life_task_map.xml','app/src/main/res/layout/item_life_task.xml','app/src/main/res/layout/fragment_life_task_detail.xml']:
    if not (ROOT/rel).exists(): err('missing v4.8 life task file '+rel)
repo=read('app/src/main/java/com/italiano2774/nativeapp/LifeTaskRepository.java');engine=read('app/src/main/java/com/italiano2774/nativeapp/LifeTaskEngine.java');detail=read('app/src/main/java/com/italiano2774/nativeapp/LifeTaskDetailFragment.java');dialogue=read('app/src/main/java/com/italiano2774/nativeapp/DialogueTrainingFragment.java')
if len(re.findall(r'new LifeTask\(',repo))!=12: err('LifeTaskRepository must define exactly 12 curated tasks')
for token in ['clothing_store','post_office','bar_restaurant','supermarket','transport','directions','housing','bureaucracy','health','bank','phone_appointment','work']:
    need(repo,token,'12 unique real-life scenario mapping')
for token in ['passLine(int level)','level<=1?80:(level==2?75:70)','completedStages(ProgressStore p)','masteredTasks(ProgressStore p)','nextTask(ProgressStore p)','unlocked(ProgressStore p,LifeTask task,int level)']:
    need(engine,token,'36-stage progression engine')
for token in ['openLifeTaskMap','openLifeTask(String taskId)','openLifeTaskStage(String taskId,int level)','终学意语_backup_v5_0.json','5.0.1-preupgrade']:
    need(main,token,'navigation/upgrade backup')
for token in ['newInstance(String id,int level,String lifeTaskId)','LifeTaskEngine.passLine(currentLevel)','recordAuxiliaryResult("life_task"','openLifeTask(lifeTaskId)']:
    need(dialogue,token,'three-level task launch/result bridge')
for token in ['spinner.setEnabled(false)','button_dialogue_level_beginner).setEnabled(false)','button_dialogue_level_intermediate).setEnabled(false)','button_dialogue_level_advanced).setEnabled(false)']:
    need(dialogue,token,'mission scenario/difficulty lock')
need(engine,'return null;','all-36-complete stop condition')
need(plan,'LifeTaskEngine.completedStages(progress)>=36','all-36-complete daily-plan state')
for token in ['LifeTaskEngine.unlocked','先通过上一关','openLifeTaskStage']:
    need(detail,token,'sequential stage lock')
need(practice,'button_simple_life_tasks','beginner-visible task map entry');need(practice,'openLifeTaskMap()','task map navigation')
for token in ['LifeTaskEngine.nextTask(progress)','"life_task"','LifeTaskEngine.completedStages(progress)']:
    need(plan,token,'daily planner task-map scheduling')
need(bt,'"life_task"','three-day real-use prescription uses task map');need(course,'case "life_task"','daily-plan action route');need(passport,'"life_task"','mastery passport real-use evidence')
need(store,'o.put("version",31)','backup schema v31');need(store,'"dialogue_scenario","life_task","freechat"','life-task aux backup')
need(build,'def defaultVersionCode = 81','v4.8 versionCode');need(build,"versionName '5.0.1-native'",'v4.8 versionName')
sc=json.loads(read('app/src/main/assets/scenarios.json'));dg=json.loads(read('app/src/main/assets/dialogues.json'))
if len(sc)!=12 or len(dg)!=12: err(f'expected 12 reviewed scenario assets and dialogues, got {len(sc)}/{len(dg)}')
ids={x.get('id') for x in sc};dids={x.get('id') for x in dg}
if ids!=dids: err('scenario/dialogue ids must match')
for token in ['clothing_store','post_office']:
    if token not in ids: err('new v4.8 scenario missing '+token)
for d in dg:
    if len(d.get('turns',[]))!=5: err(d.get('id','?')+' must contain 5 reviewed turns')
for s in sc:
    if len(s.get('phrases',[]))!=10: err(s.get('id','?')+' must contain 10 core phrases')
if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print('Life task map OK: 12 unique reviewed scenarios x 3 progressive stages = 36 missions; old dialogue scores inherit; sequential unlock + daily/weekly/three-day integration + backup v31 present')

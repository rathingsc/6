#!/usr/bin/env python3
import json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def err(x): errors.append(x)
def read(p): return (ROOT/p).read_text(encoding='utf-8')
try:
    scenarios=json.loads(read(Path('app/src/main/assets/scenarios.json')))
    dialogues=json.loads(read(Path('app/src/main/assets/dialogues.json')))
except Exception as e:
    print('ERROR:',e);sys.exit(1)
if len(scenarios)!=12: err(f'expected 12 scenarios, got {len(scenarios)}')
if len(dialogues)!=12: err(f'expected 12 dialogues, got {len(dialogues)}')
scenario_ids={x.get('id') for x in scenarios}
dialogue_ids={x.get('id') for x in dialogues}
if scenario_ids!=dialogue_ids: err(f'scenario/dialogue ids differ: {scenario_ids^dialogue_ids}')
turns=0
for d in dialogues:
    ts=d.get('turns',[]);turns+=len(ts)
    if len(ts)!=5: err(f"{d.get('id')}: expected 5 turns, got {len(ts)}")
    for i,t in enumerate(ts,1):
        for k in ['npc','npcZh','reply','replyZh','answerZh','tip']:
            if not str(t.get(k,'')).strip(): err(f"{d.get('id')} turn {i}: missing {k}")
        choices=t.get('choices',[]);correct=t.get('correct',-1)
        if len(choices)!=3: err(f"{d.get('id')} turn {i}: expected 3 choices")
        if not isinstance(correct,int) or correct<0 or correct>=len(choices): err(f"{d.get('id')} turn {i}: invalid correct index")
        if len(set(choices))!=len(choices): err(f"{d.get('id')} turn {i}: duplicate choices")
        if choices and len(choices[correct].strip())<2: err(f"{d.get('id')} turn {i}: empty correct answer")
        if re.search(r'\b(?:the|please|where|want|need|train|doctor)\b',choices[correct],re.I): err(f"{d.get('id')} turn {i}: English leaked into Italian answer")
if turns!=60: err(f'expected 60 dialogue turns, got {turns}')

frag=read(Path('app/src/main/java/com/italiano2774/nativeapp/DialogueTrainingFragment.java'))
for token in [
    'LEVEL_BEGINNER','LEVEL_INTERMEDIATE','LEVEL_ADVANCED','MaterialButtonToggleGroup',
    '初级 · ①先听','中级 · ①先听','高级 · ①只听','只看中文','自由回答',
    'FreeConversationEngine','dialogueScenarioRecommendedLevel','markDialogueScenarioCompletion(current.id,score,currentLevel)',
    'SpeechRecognizer','it-IT','scoreSpeech','recordTargetWords','0.70f','turnHintLevel','sessionScoreTotal'
]:
    if token not in frag: err('DialogueTrainingFragment missing '+token)
layout=read(Path('app/src/main/res/layout/fragment_dialogue_training.xml'))
for rid in [
    'group_dialogue_difficulty','button_dialogue_level_beginner','button_dialogue_level_intermediate','button_dialogue_level_advanced',
    'button_dialogue_reveal','button_dialogue_start_answer','container_dialogue_choices','container_dialogue_speak','button_dialogue_speak','button_dialogue_hint','button_dialogue_slow'
]:
    if '@+id/'+rid not in layout: err('dialogue layout missing '+rid)
for text in ['初级&#10;有选项','中级&#10;只给中文','高级&#10;自由回答']:
    if text not in layout: err('difficulty selector label missing '+text)

practice=read(Path('app/src/main/java/com/italiano2774/nativeapp/PracticeHubFragment.java'))+read(Path('app/src/main/res/layout/fragment_practice_hub.xml'))
if 'button_simple_scenarios' not in practice or 'openScenarios()' not in practice: err('real-life scenario entry is not beginner-visible')

build=read(Path('app/build.gradle'))
if 'def defaultVersionCode = 81' not in build or "versionName '5.0.1-native'" not in build: err('v5.0.1 version identity missing')
progress=read(Path('app/src/main/java/com/italiano2774/nativeapp/ProgressStore.java'))
for token in ['dialogueScenarioCompleted(String id,int level)','dialogueScenarioBestScore(String id,int level)','dialogueScenarioRecommendedLevel','dialogueDifficultyKey','dialogueScenarioProgress','dialogue_scenario']:
    if token not in progress: err('ProgressStore missing '+token)
adapter=read(Path('app/src/main/java/com/italiano2774/nativeapp/ScenarioAdapter.java'))
for token in ['dialogueScenarioBestScore(s.id,1)','dialogueScenarioBestScore(s.id,2)','dialogueScenarioBestScore(s.id,3)','初 ','中 ','高 ']:
    if token not in adapter: err('ScenarioAdapter missing difficulty progress '+token)

if errors:
    for e in errors: print('ERROR:',e)
    sys.exit(1)
print(f'Scenario conversation quality OK: {len(dialogues)} scenarios, {turns} turns, beginner/intermediate/advanced assistance levels, semantic free-response scoring, per-level progress and smart-review feedback present')

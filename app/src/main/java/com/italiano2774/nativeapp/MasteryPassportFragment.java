package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** v4.3.0 six-skill learning passport and internal stage checkpoints. */
public class MasteryPassportFragment extends Fragment {
    private MasteryPassportEngine.Snapshot snapshot;
    private BreakthroughPlanEngine.Plan prescription;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_mastery_passport,parent,false);ProgressStore p=new ProgressStore(requireContext());WordRepository repo=WordRepository.get(requireContext());snapshot=MasteryPassportEngine.build(requireContext(),repo,p);prescription=BreakthroughPlanEngine.build(requireContext(),repo,p);
        ((TextView)v.findViewById(R.id.text_passport_stage)).setText(snapshot.currentStage+" · 内部综合掌握 "+snapshot.overall+"分");
        ((TextView)v.findViewById(R.id.text_passport_overview)).setText("课程主线 "+snapshot.coursePct+"% · 已接触路线词 "+snapshot.introduced+" · 长期掌握 "+snapshot.graduated+"\n下一关："+snapshot.nextCheckpoint);
        bindSkill(v,R.id.text_passport_meaning,R.id.progress_passport_meaning,MasteryPassportEngine.ACTION_MEANING);
        bindSkill(v,R.id.text_passport_listening,R.id.progress_passport_listening,MasteryPassportEngine.ACTION_LISTENING);
        bindSkill(v,R.id.text_passport_spelling,R.id.progress_passport_spelling,MasteryPassportEngine.ACTION_SPELLING);
        bindSkill(v,R.id.text_passport_speaking,R.id.progress_passport_speaking,MasteryPassportEngine.ACTION_SPEAKING);
        bindSkill(v,R.id.text_passport_grammar,R.id.progress_passport_grammar,MasteryPassportEngine.ACTION_GRAMMAR);
        bindSkill(v,R.id.text_passport_real,R.id.progress_passport_real,MasteryPassportEngine.ACTION_REAL_USE);
        bindCheckpoint(v,R.id.text_passport_a1,snapshot.a1);bindCheckpoint(v,R.id.text_passport_a2,snapshot.a2);bindCheckpoint(v,R.id.text_passport_b1,snapshot.b1);
        ((TextView)v.findViewById(R.id.text_passport_recommendation)).setText(snapshot.recommendation);
        ((TextView)v.findViewById(R.id.text_passport_prescription)).setText(prescription.headline+"\n"+prescription.summary+"\n\n"+prescription.threeDayText());
        com.google.android.material.button.MaterialButton prescriptionButton=v.findViewById(R.id.button_passport_prescription);prescriptionButton.setText(prescription.buttonLabel);prescriptionButton.setOnClickListener(x->openPrescriptionAction(prescription));
        prescriptionButton.setEnabled(!prescription.waitingForTomorrow&&!prescription.cycleComplete);
        com.google.android.material.button.MaterialButton action=v.findViewById(R.id.button_passport_action);action.setText(snapshot.actionLabel);action.setOnClickListener(x->openAction(snapshot.actionKey));
        v.findViewById(R.id.button_passport_exam).setOnClickListener(x->((MainActivity)requireActivity()).openLevelExam());v.findViewById(R.id.button_passport_back).setOnClickListener(x->((MainActivity)requireActivity()).openProfile());return v;
    }
    private void bindSkill(View v,int textId,int barId,String key){MasteryPassportEngine.Skill s=snapshot.skill(key);if(s==null)return;((TextView)v.findViewById(textId)).setText(s.label+"  "+s.score+"分\n"+s.evidence);((ProgressBar)v.findViewById(barId)).setProgress(s.score);}
    private void bindCheckpoint(View v,int id,MasteryPassportEngine.Checkpoint c){String head=(c.passed?"✓ ":"○ ")+c.level+" 学习检查 · "+c.completed+" / "+c.total+" 条";((TextView)v.findViewById(id)).setText(head+"\n"+c.detail+"\n"+(c.passed?"已达到本App这一阶段的内部检查线。":"下一缺口："+c.missing));}
    private void openAction(String key){MainActivity a=(MainActivity)requireActivity();if(MasteryPassportEngine.ACTION_LISTENING.equals(key))a.openListeningSpeaking();else if(MasteryPassportEngine.ACTION_SPELLING.equals(key))a.openActiveRecall(5);else if(MasteryPassportEngine.ACTION_SPEAKING.equals(key))a.openDailySpeakingChallenge();else if(MasteryPassportEngine.ACTION_GRAMMAR.equals(key))a.openMicroGrammar();else if(MasteryPassportEngine.ACTION_REAL_USE.equals(key))a.openDialogueTraining();else if(MasteryPassportEngine.ACTION_EXAM.equals(key))a.openLevelExam();else if(MasteryPassportEngine.ACTION_MEANING.equals(key))a.openReviewStudy();else a.openCourseMap();}
    private void openPrescriptionAction(BreakthroughPlanEngine.Plan plan){MainActivity a=(MainActivity)requireActivity();if("listen_speak".equals(plan.action))a.openListeningSpeaking();else if("intensive_listening".equals(plan.action))a.openIntensiveListening();else if("sentence_dictation".equals(plan.action))a.openSentenceDictation();else if("daily_speaking".equals(plan.action))a.openDailySpeakingChallenge();else if("grammar".equals(plan.action))a.openMicroGrammarLesson(plan.payload);else if("smart_cloze".equals(plan.action))a.openSmartCloze();else if("dialogue".equals(plan.action))a.openDialogueTraining(plan.payload);else if("life_task".equals(plan.action)){if(plan.payload==null||plan.payload.isEmpty())a.openLifeTaskMap();else a.openLifeTask(plan.payload);}else if("free_conversation".equals(plan.action))a.openFreeConversation(plan.payload);else if("writing".equals(plan.action))a.openWriting();else if("active_recall".equals(plan.action)){int n=8;try{n=Integer.parseInt(plan.payload);}catch(Exception ignored){}a.openActiveRecall(n);}else a.openPractice();}
}

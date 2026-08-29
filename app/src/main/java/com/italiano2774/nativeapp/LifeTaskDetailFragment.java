package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class LifeTaskDetailFragment extends Fragment {
    public static LifeTaskDetailFragment newInstance(String taskId){LifeTaskDetailFragment f=new LifeTaskDetailFragment();Bundle b=new Bundle();b.putString("taskId",taskId==null?"":taskId);f.setArguments(b);return f;}
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_life_task_detail,container,false);String id=getArguments()==null?"":getArguments().getString("taskId","");LifeTask task=LifeTaskRepository.find(id);ProgressStore p=new ProgressStore(requireContext());if(task==null){((MainActivity)requireActivity()).openLifeTaskMap();return v;}
        ((TextView)v.findViewById(R.id.text_life_detail_title)).setText(task.emoji+"  "+task.title);((TextView)v.findViewById(R.id.text_life_detail_subtitle)).setText(task.subtitle);((TextView)v.findViewById(R.id.text_life_detail_goal)).setText("任务目标\n"+task.goal+"\n\n"+LifeTaskEngine.compactProgress(p,task));
        bindStage(v,p,task,1,R.id.button_life_stage_1,R.id.text_life_stage_1);bindStage(v,p,task,2,R.id.button_life_stage_2,R.id.text_life_stage_2);bindStage(v,p,task,3,R.id.button_life_stage_3,R.id.text_life_stage_3);
        v.findViewById(R.id.button_life_core_phrases).setOnClickListener(x->((MainActivity)requireActivity()).openScenario(task.scenarioId));v.findViewById(R.id.button_life_detail_back).setOnClickListener(x->((MainActivity)requireActivity()).openLifeTaskMap());return v;
    }
    private void bindStage(View root,ProgressStore p,LifeTask task,int level,int buttonId,int textId){
        MaterialButton b=root.findViewById(buttonId);TextView txt=root.findViewById(textId);int score=LifeTaskEngine.score(p,task,level),line=LifeTaskEngine.passLine(level);boolean passed=score>=line,unlocked=LifeTaskEngine.unlocked(p,task,level);
        txt.setText((passed?"✅ ":(unlocked?"○ ":"🔒 "))+LifeTaskEngine.levelTitle(level)+" · 最佳 "+score+"% / 目标 "+line+"%\n"+LifeTaskEngine.levelDetail(level));b.setText(passed?"再练一次":(unlocked?"开始这一关":"先通过上一关"));b.setEnabled(unlocked);b.setAlpha(unlocked?1f:.58f);if(!unlocked){b.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));}if(unlocked)b.setOnClickListener(x->((MainActivity)requireActivity()).openLifeTaskStage(task.id,level));
    }
}

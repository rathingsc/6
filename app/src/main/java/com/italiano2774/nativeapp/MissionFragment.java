package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MissionFragment extends Fragment {
    private Scenario scenario;private ProgressStore progress;private AudioPlayer audio;private final List<ScenarioPhrase> questions=new ArrayList<>();private final List<MaterialButton> buttons=new ArrayList<>();private TextView prompt,status,feedback;private ProgressBar bar;private MaterialButton next;private int index=0,correct=0;private long started;
    public static MissionFragment newInstance(String id){MissionFragment f=new MissionFragment();Bundle b=new Bundle();b.putString("id",id);f.setArguments(b);return f;}
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle state){
        View v=i.inflate(R.layout.fragment_mission,c,false);String id=getArguments()==null?"":getArguments().getString("id","");scenario=ScenarioRepository.get(requireContext()).find(id);if(scenario==null){List<Scenario> all=ScenarioRepository.get(requireContext()).all();scenario=all.isEmpty()?null:all.get(0);}progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);prompt=v.findViewById(R.id.text_mission_prompt);status=v.findViewById(R.id.text_mission_status);feedback=v.findViewById(R.id.text_mission_feedback);bar=v.findViewById(R.id.progress_mission);next=v.findViewById(R.id.button_mission_next);buttons.add(v.findViewById(R.id.button_mission_a));buttons.add(v.findViewById(R.id.button_mission_b));buttons.add(v.findViewById(R.id.button_mission_c));buttons.add(v.findViewById(R.id.button_mission_d));v.findViewById(R.id.button_mission_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());
        if(scenario!=null){((TextView)v.findViewById(R.id.text_mission_title)).setText(scenario.emoji+" "+scenario.title);questions.addAll(scenario.phrases);Collections.shuffle(questions);if(questions.size()>5)questions.subList(5,questions.size()).clear();}
        for(int n=0;n<buttons.size();n++){final int k=n;buttons.get(n).setOnClickListener(x->choose(k));}next.setOnClickListener(x->{index++;if(index>=questions.size())finish();else showQuestion();});showQuestion();return v;
    }
    private void showQuestion(){if(scenario==null||questions.isEmpty()){prompt.setText("暂无可用场景句");return;}ScenarioPhrase target=questions.get(index);prompt.setText(target.zh);status.setText("任务 "+(index+1)+" / "+questions.size()+" · 已正确 "+correct);bar.setProgress((int)Math.round(index*100.0/questions.size()));feedback.setText("");next.setVisibility(View.GONE);List<ScenarioPhrase> opts=new ArrayList<>();opts.add(target);List<ScenarioPhrase> rest=new ArrayList<>(scenario.phrases);rest.remove(target);Collections.shuffle(rest);for(int n=0;n<Math.min(3,rest.size());n++)opts.add(rest.get(n));while(opts.size()<4)opts.add(target);Collections.shuffle(opts);for(int n=0;n<4;n++){MaterialButton b=buttons.get(n);b.setEnabled(true);b.setText(opts.get(n).it);b.setTag(opts.get(n)==target);}started=System.currentTimeMillis();}
    private void choose(int idx){MaterialButton b=buttons.get(idx);if(!b.isEnabled())return;boolean ok=Boolean.TRUE.equals(b.getTag());long ms=Math.max(1,System.currentTimeMillis()-started);progress.recordAuxiliaryResult("mission",ok,ms);if(ok){correct++;feedback.setText("✓ 这句话适合当前任务");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.success));audio.speak(b.getText().toString());for(MaterialButton x:buttons)x.setEnabled(false);next.setVisibility(View.VISIBLE);}else{feedback.setText("再想一下：哪一句最符合这个中文目标？");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));b.setEnabled(false);}}
    private void finish(){bar.setProgress(100);prompt.setText("🎉 场景任务完成");status.setText("答对 "+correct+" / "+questions.size()+" · 这些句子已经进入今天的真实输出训练");feedback.setText("完成后回到首页，学习路径会自动更新。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.success));for(MaterialButton b:buttons)b.setVisibility(View.GONE);next.setText("回到今日路径");next.setVisibility(View.VISIBLE);next.setOnClickListener(x->((MainActivity)requireActivity()).openToday(java.time.LocalDate.now()));}
    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}

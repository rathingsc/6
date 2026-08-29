package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

/** v5.0 practice hub: extra tools appear gradually during the first seven active study days. */
public class PracticeHubFragment extends Fragment {
    private ProgressStore progress;private MainActivity activity;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_practice_hub,container,false);activity=(MainActivity)requireActivity();progress=new ProgressStore(requireContext());
        TextView hint=v.findViewById(R.id.text_practice_hint);hint.setText(BeginnerGuideEngine.practiceHint(progress));

        MaterialButton weekly=v.findViewById(R.id.button_simple_weekly_exam);String weeklyLabel=progress.weeklyExamDue()?"🧪 每周实战考试 · 已到期":("🧪 每周实战考试 · "+Math.min(7,progress.weeklyExamActiveDaysSinceLast())+"/7活跃日");bindGuided(weekly,7,weeklyLabel,()->activity.openWeeklyExam());
        MaterialButton life=v.findViewById(R.id.button_simple_life_tasks);bindGuided(life,5,"🗺 真实生活任务地图 · "+LifeTaskEngine.completedStages(progress)+"/36关",()->activity.openLifeTaskMap());
        bindGuided(v.findViewById(R.id.button_simple_weak_story),4,"📚 今日弱词微短文",()->activity.openWeakWordStory());
        bindGuided(v.findViewById(R.id.button_simple_daily_speaking),3,"🗣 每日5句开口",()->activity.openDailySpeakingChallenge());
        bindGuided(v.findViewById(R.id.button_simple_listen),2,"🎧🗣 听力与开口",()->activity.openListeningSpeaking());
        bindGuided(v.findViewById(R.id.button_simple_scenarios),5,"🎭 真实情景会话",()->activity.openScenarios());
        bindGuided(v.findViewById(R.id.button_simple_micro_grammar),4,"🧩 微语法实战",()->activity.openMicroGrammar());
        bindGuided(v.findViewById(R.id.button_simple_pronunciation),3,"🗣 发音练习",()->activity.openPronunciation());
        bindGuided(v.findViewById(R.id.button_simple_wrong),4,"❤️ 错题复习",()->activity.openWrongWordRepair());
        MaterialButton evidence=v.findViewById(R.id.button_simple_error_evidence);bindGuided(evidence,4,"🧾 个人错句本 · 待修复 "+progress.pendingErrorRepairs()+" 条",()->activity.openErrorEvidenceRepair());
        bindGuided(v.findViewById(R.id.button_simple_spelling),3,"✍️ 拼写练习",()->activity.openPracticeMode("spell"));

        LinearLayout advanced=v.findViewById(R.id.container_advanced_practice);MaterialButton more=v.findViewById(R.id.button_more_practice);if(BeginnerGuideEngine.active(progress)){more.setVisibility(View.GONE);advanced.setVisibility(View.GONE);}else more.setOnClickListener(x->{boolean show=advanced.getVisibility()!=View.VISIBLE;advanced.setVisibility(show?View.VISIBLE:View.GONE);more.setText(show?"收起进阶工具 ▴":"更多练习工具 ▾");});
        bindAdvanced(v,R.id.button_adv_sentences,3,"💬 核心句子",()->activity.openCoreSentences());
        bindAdvanced(v,R.id.button_adv_phrases,8,"💬 高频短语",()->activity.openPhrases());
        bindAdvanced(v,R.id.button_adv_grammar,10,"🗺 语法知识地图",()->activity.openGrammarMap());
        bindAdvanced(v,R.id.button_adv_verbs,16,"🔤 动词变位",()->activity.openVerbCenter());
        bindAdvanced(v,R.id.button_adv_reading,24,"📖 分级阅读",()->activity.openReadingList());
        bindAdvanced(v,R.id.button_adv_all,32,"📊 弱项分析",()->activity.openWeaknessCenter());
        return v;
    }
    private void bindGuided(MaterialButton b,int unlockDay,String label,Runnable action){boolean unlocked=BeginnerGuideEngine.practiceUnlocked(progress,unlockDay);b.setEnabled(unlocked);b.setAlpha(unlocked?1f:.58f);b.setText(unlocked?label:BeginnerGuideEngine.lockedLabel(stripEmoji(label),unlockDay));if(unlocked)b.setOnClickListener(x->action.run());else{b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));b.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));}}
    private void bindAdvanced(View root,int id,int minUnit,String label,Runnable action){MaterialButton b=root.findViewById(id);boolean unlocked=progress.courseUnlockedUnitIndex()>=minUnit;b.setEnabled(unlocked);b.setAlpha(unlocked?1f:.58f);b.setText(unlocked?label:("🔒 "+stripEmoji(label)+" · "+unlockLabel(minUnit)+"解锁"));if(unlocked)b.setOnClickListener(x->action.run());else{b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));b.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));}}
    private String unlockLabel(int index){if(index<8)return "A0第"+(index+1)+"单元后";if(index<32)return "A1第"+(index-7)+"单元后";if(index<62)return "A2第"+(index-31)+"单元后";return "B1阶段";}
    private String stripEmoji(String s){int i=s.indexOf(' ');return i>=0?s.substring(i+1):s;}
}

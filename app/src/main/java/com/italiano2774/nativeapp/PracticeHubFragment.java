package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

/** v3.0 beginner-facing practice hub: four obvious choices, advanced tools unlock with the path. */
public class PracticeHubFragment extends Fragment {
    private ProgressStore progress;private MainActivity activity;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_practice_hub,container,false);activity=(MainActivity)requireActivity();progress=new ProgressStore(requireContext());
        v.findViewById(R.id.button_simple_listen).setOnClickListener(x->activity.openPracticeMode("listen"));
        v.findViewById(R.id.button_simple_pronunciation).setOnClickListener(x->activity.openPronunciation());
        v.findViewById(R.id.button_simple_wrong).setOnClickListener(x->activity.openWrongWordRepair());
        v.findViewById(R.id.button_simple_spelling).setOnClickListener(x->activity.openPracticeMode("spell"));
        LinearLayout advanced=v.findViewById(R.id.container_advanced_practice);MaterialButton more=v.findViewById(R.id.button_more_practice);more.setOnClickListener(x->{boolean show=advanced.getVisibility()!=View.VISIBLE;advanced.setVisibility(show?View.VISIBLE:View.GONE);more.setText(show?"收起进阶工具 ▴":"更多练习工具 ▾");});
        bindAdvanced(v,R.id.button_adv_sentences,3,"💬 核心句子",()->activity.openCoreSentences());
        bindAdvanced(v,R.id.button_adv_scenarios,5,"🎭 生活场景",()->activity.openScenarios());
        bindAdvanced(v,R.id.button_adv_phrases,8,"💬 高频短语",()->activity.openPhrases());
        bindAdvanced(v,R.id.button_adv_grammar,10,"🧩 语法与句型",()->activity.openSentencePatterns());
        bindAdvanced(v,R.id.button_adv_verbs,16,"🔤 动词变位",()->activity.openVerbCenter());
        bindAdvanced(v,R.id.button_adv_reading,24,"📖 分级阅读",()->activity.openReadingList());
        bindAdvanced(v,R.id.button_adv_all,32,"📊 弱项分析",()->activity.openWeaknessCenter());
        return v;
    }
    private void bindAdvanced(View root,int id,int minUnit,String label,Runnable action){MaterialButton b=root.findViewById(id);boolean unlocked=progress.courseUnlockedUnitIndex()>=minUnit;b.setEnabled(unlocked);b.setAlpha(unlocked?1f:.58f);b.setText(unlocked?label:("🔒 "+stripEmoji(label)+" · "+unlockLabel(minUnit)+"解锁"));if(unlocked)b.setOnClickListener(x->action.run());else{b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));b.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));}}
    private String unlockLabel(int index){if(index<8)return "A0第"+(index+1)+"单元后";if(index<32)return "A1第"+(index-7)+"单元后";if(index<62)return "A2第"+(index-31)+"单元后";return "B1阶段";}
    private String stripEmoji(String s){int i=s.indexOf(' ');return i>=0?s.substring(i+1):s;}
}

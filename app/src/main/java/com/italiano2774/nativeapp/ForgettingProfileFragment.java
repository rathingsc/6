package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.time.LocalDate;
import java.util.List;

/** v4.9 learner-facing view of the four independent forgetting tracks. */
public class ForgettingProfileFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_forgetting_profile,container,false);ProgressStore p=new ProgressStore(requireContext());List<Word> words=WordRepository.get(requireContext()).all();LocalDate today=LocalDate.now();
        int learned=p.introducedCount(words),fragile=ProgressStore.DIM_MEANING;double fragileScore=Double.MAX_VALUE;
        int[] dims={ProgressStore.DIM_MEANING,ProgressStore.DIM_LISTENING,ProgressStore.DIM_SPELLING,ProgressStore.DIM_SPEAKING};int[] ids={R.id.text_forget_meaning,R.id.text_forget_listening,R.id.text_forget_spelling,R.id.text_forget_speaking};
        for(int i=0;i<dims.length;i++){
            int dim=dims[i],due=p.dimensionDueCount(words,dim,today),avgIv=p.dimensionAverageInterval(words,dim),ret=p.dimensionAverageRetrievability(words,dim),obs=p.forgettingObservationCount(dim);double factor=p.forgettingFactor(dim);
            String text=PersonalForgettingModel.dimensionLabel(dim)+"\n今天到期 "+due+" · 平均间隔 "+avgIv+"天 · 当前保持约 "+ret+"%\n个人节奏："+p.forgettingSpeedLabel(dim)+" · 校准 "+obs+" 次"+(obs>=5?(" · 系数 "+String.format(java.util.Locale.US,"%.2f",factor)):"");((TextView)v.findViewById(ids[i])).setText(text);
            double score=(obs>=5?factor:1.0)+(ret/1000.0);if(score<fragileScore){fragileScore=score;fragile=dim;}
        }
        int totalDue=p.dueCount(words,today);String fragileName=PersonalForgettingModel.dimensionLabel(fragile);((TextView)v.findViewById(R.id.text_forgetting_summary)).setText("已学习 "+learned+" 词 · 当前到期 "+totalDue+" 词\n系统现在分别预测识义、听力、拼写、口语的遗忘时间；同一个词可能在不同日期复习不同能力。\n当前最需要留意："+fragileName+"。");
        ((TextView)v.findViewById(R.id.text_forgetting_note)).setText("校准说明：前5次有效复习只收集证据，不急着下结论。之后系统会根据“预计还记得却实际忘了”或“已经拖很久仍能答对”的情况，温和缩短或拉长该能力的未来间隔。每个词自己的FSRS仍是主调度，个人节奏只做小幅校准。\n\n这不是考试分数，也不是能力等级；它只回答一个问题：你在哪一种记忆通道上忘得更快。 ");
        v.findViewById(R.id.button_forgetting_review).setOnClickListener(x->((MainActivity)requireActivity()).openReviewStudy());v.findViewById(R.id.button_forgetting_vocabulary).setOnClickListener(x->((MainActivity)requireActivity()).openVocabulary());return v;
    }
}

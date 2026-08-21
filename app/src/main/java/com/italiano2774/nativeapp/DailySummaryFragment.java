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

public class DailySummaryFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        View v=i.inflate(R.layout.fragment_daily_summary,c,false);WordRepository repo=WordRepository.get(requireContext());ProgressStore p=new ProgressStore(requireContext());LocalDate today=LocalDate.now();
        List<Word> todayWords=repo.forDate(p.startDate(),today,p.perDay());int touched=0,strong=0;StringBuilder list=new StringBuilder();for(Word w:todayWords){int m=p.mastery(w.id);if(m>0)touched++;if(m>=4)strong++;if(list.length()<2200)list.append(m>=4?"✓ ":(m>0?"• ":"○ ")).append(w.word).append("  ").append(w.chinese==null?"":w.chinese).append("  · ").append(m).append("级\n");}
        int attempts=p.dailyAttempts(today),correct=p.dailyCorrect(today),accuracy=attempts==0?0:(int)Math.round(correct*100.0/attempts),minutes=(int)Math.round(p.dailyActiveSeconds(today)/60.0);int due=p.dueCount(repo.all(),today);
        ((TextView)v.findViewById(R.id.text_daily_summary_stats)).setText("今日课程词："+touched+" / "+todayWords.size()+" 已接触 · "+strong+" 个达到4级+\n今日学习卡："+p.dailyCards(today)+" 张卡 · 练习 "+attempts+" 次 · 正确率 "+accuracy+"%\n实际前台学习："+minutes+" 分钟 · 当前到期复习剩余 "+due+"\n长期掌握："+p.graduatedCount(repo.all())+" · 顽固词："+p.stubbornCount(repo.all()));
        ((TextView)v.findViewById(R.id.text_daily_summary_words)).setText(list.length()==0?"今天没有课程顺序新词。":list.toString().trim());
        v.findViewById(R.id.button_summary_words).setOnClickListener(x->((MainActivity)requireActivity()).openStudy(today));v.findViewById(R.id.button_summary_review).setOnClickListener(x->((MainActivity)requireActivity()).openReviewStudy());v.findViewById(R.id.button_summary_back).setOnClickListener(x->((MainActivity)requireActivity()).openToday(today));return v;
    }
}

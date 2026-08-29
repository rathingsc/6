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
import java.util.ArrayList;
import java.util.List;

/** v4.5.0 weekly report: behavior trend + practical exam + next-week automatic adjustment. */
public class WeeklyReportFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_weekly_report,container,false);ProgressStore p=new ProgressStore(requireContext());WordRepository r=WordRepository.get(requireContext());
        int att=p.weekAttempts(),cards=p.weekCards(),minutes=p.weekActualMinutes(LocalDate.now());int curCor=0;LocalDate mon=LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue()-1L);for(int d=0;d<7;d++)curCor+=p.dailyCorrect(mon.plusDays(d));int acc=att==0?0:(int)Math.round(curCor*100.0/att);int patt=p.previousWeekAttempts(),pcor=p.previousWeekCorrect(),pacc=patt==0?0:(int)Math.round(pcor*100.0/patt);int accDelta=acc-pacc;
        ((TextView)v.findViewById(R.id.text_week_report_main)).setText("本周实际前台学习 "+minutes+" 分钟\n学习卡 "+cards+" 张 · 练习 "+att+" 题\n正确率 "+acc+"% "+delta(accDelta)+"\n连续学习 "+p.activityStreak()+" 天 · 本周活跃 "+p.weekActiveDays()+" / 7 天");
        ((TextView)v.findViewById(R.id.text_week_report_memory)).setText("🎓 已毕业 "+p.graduatedCount(r.all())+" 词\n🔥 顽固词 "+p.stubbornCount(r.all())+" 词\n到期复习 "+p.dueCount(r.all(),LocalDate.now())+" · "+p.reviewPressureAdvice(r.all())+"\n当前路线 "+p.vocabularyRouteLabel()+" · "+p.routeIntroducedCount(r.all())+" / "+p.vocabularyRouteLimit());
        String top=ErrorCause.label(p.topErrorCause());((TextView)v.findViewById(R.id.text_week_report_weak)).setText("最常见错误："+top+"\n过去时 "+p.auxiliaryAccuracy("past_tense")+"% · 代词 "+p.auxiliaryAccuracy("pronouns")+"% · 介词 "+p.auxiliaryAccuracy("preposition")+"%\n写作 "+p.auxiliaryAccuracy("writing")+"% · 句子复习 "+p.auxiliaryAccuracy("sentence_fsrs")+"% · 到期语法 "+p.dueGrammarCount()+"个\n学习目标："+p.learningGoalLabel());
        ((TextView)v.findViewById(R.id.text_week_report_exam)).setText(examSummary(p));
        ((TextView)v.findViewById(R.id.text_week_report_advice)).setText(advice(p,r,acc,top));
        v.findViewById(R.id.button_week_exam).setOnClickListener(x->((MainActivity)requireActivity()).openWeeklyExam());v.findViewById(R.id.button_week_rescue).setOnClickListener(x->((MainActivity)requireActivity()).openRescueMode());v.findViewById(R.id.button_week_practice).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());return v;
    }

    private String examSummary(ProgressStore p){
        int active=p.weeklyExamActiveDaysSinceLast();if(p.weeklyExamCount()==0)return "🧪 每周实战考试\n本轮活跃学习 "+Math.min(7,active)+" / 7 天 · "+(p.weeklyExamDue()?"现在可以正式周测":"还差 "+Math.max(0,7-active)+" 个活跃日")+"\n首次周测会建立六项能力变化基线。";
        StringBuilder sb=new StringBuilder("🧪 最近一次实战周测：").append(p.lastWeeklyExamScore()).append("%\n");for(String key:WeeklyExamEngine.SKILL_KEYS){int d=p.weeklyExamDelta(key);sb.append(WeeklyExamEngine.skillLabel(key)).append(" ").append(p.weeklyExamSkillScore(key)).append("% · 护照 ").append(p.weeklyExamBeforeScore(key)).append("→").append(p.weeklyExamAfterScore(key));if(d!=0)sb.append(" (").append(d>0?"+":"").append(d).append(")");sb.append("\n");}sb.append("\n下一轮已累计 ").append(Math.min(7,active)).append(" / 7 个活跃日 · ").append(p.weeklyExamDue()?"周测已到期":"继续正常学习").append("\n").append(p.weeklyAdjustmentSummary());return sb.toString();
    }

    private String delta(int v){return v==0?"（与上周持平）":(v>0?"（比上周 +"+v+"）":"（比上周 "+v+"）");}
    private String advice(ProgressStore p,WordRepository r,int acc,String top){
        List<String> a=new ArrayList<>();if(p.weeklyAdjustmentActive())a.add("📌 周测已接管部分下周调度："+p.weeklyAdjustmentSummary()+"\n");if(p.reviewPressureLevel(r.all())>=2)a.add("下周先减少新词，优先清理高风险复习，不追求一次清空全部积压。\n");else if(acc>=90)a.add("当前状态稳定，可以维持新词节奏，但仍以周测最弱两项为优先。\n");else if(acc>0&&acc<75)a.add("正确率偏低，建议把新词量降低并增加弱项提取。\n");if(ErrorCause.WORD_FORM.equals(p.topErrorCause()))a.add("重点：过去时和动词词形专项。\n");else if(ErrorCause.GRAMMAR.equals(p.topErrorCause())||ErrorCause.ARTICLE_GENDER.equals(p.topErrorCause()))a.add("重点：语法地图、介词和代词专项。\n");else if(ErrorCause.LISTENING_CONFUSION.equals(p.topErrorCause())||ErrorCause.OMISSION.equals(p.topErrorCause()))a.add("重点：逐句精听和整句听写。\n");if(p.stubbornCount(r.all())>15)a.add("顽固词较多，优先使用多通道训练而不是机械重复。\n");a.add(p.weeklyExamDue()?"本轮已完成7个活跃学习日，建议先做实战周测再开始下一轮。":"保持每天至少一个学习节点；累计7个活跃学习日后再做一次周测。");StringBuilder sb=new StringBuilder();for(String x:a)sb.append(x);return sb.toString();
    }
}

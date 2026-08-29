package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

/**
 * Beginner-first grammar route: one tiny rule, one real sentence, then three exercises.
 * It deliberately reuses the audited sentence-pattern data instead of introducing a second
 * grammar database that could drift out of sync.
 */
public class MicroGrammarFragment extends Fragment {
    private static final String[] A1={"vorrei","bisogno","potere","dovere","c_e","piacere","quanto_costa","dove_posso","mi_serve","perche","non_ho_capito"};
    private static final String[] A2={"stare_gerundio","da_quanto","prima_dopo","passato_prossimo","imperfetto","comparativo","ci_vuole","vorrei_sapere","ce_l_ho"};
    private static final String[] B1={"se_presente","futuro_semplice","condizionale_presente","pronomi_diretti","pronomi_indiretti","imperativo","ne_partitivo"};

    private ProgressStore progress;
    private SentencePatternRepository repository;
    private LinearLayout list;
    private TextView summary,recommendedTitle,recommendedRule,recommendedExample;
    private MaterialButton recommendedStart,a1,a2,b1;
    private String level="A1";

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_micro_grammar,parent,false);
        progress=new ProgressStore(requireContext());repository=SentencePatternRepository.get(requireContext());
        list=v.findViewById(R.id.container_micro_grammar);
        summary=v.findViewById(R.id.text_micro_grammar_summary);
        recommendedTitle=v.findViewById(R.id.text_micro_recommended_title);
        recommendedRule=v.findViewById(R.id.text_micro_recommended_rule);
        recommendedExample=v.findViewById(R.id.text_micro_recommended_example);
        recommendedStart=v.findViewById(R.id.button_micro_recommended_start);
        a1=v.findViewById(R.id.button_micro_a1);a2=v.findViewById(R.id.button_micro_a2);b1=v.findViewById(R.id.button_micro_b1);
        v.findViewById(R.id.button_micro_grammar_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());
        v.findViewById(R.id.button_micro_grammar_map).setOnClickListener(x->((MainActivity)requireActivity()).openGrammarMap());
        v.findViewById(R.id.button_micro_grammar_weak).setOnClickListener(x->((MainActivity)requireActivity()).openGrammarDiagnosis());
        a1.setOnClickListener(x->{level="A1";render();});a2.setOnClickListener(x->{level="A2";render();});b1.setOnClickListener(x->{level="B1";render();});
        int unlocked=progress.courseUnlockedUnitIndex();level=unlocked>=62?"B1":(unlocked>=32?"A2":"A1");
        render();return v;
    }

    private void render(){
        renderRecommended();list.removeAllViews();String[] ids=idsForLevel(level);int started=0,stable=0,due=0,totalAttempts=0;
        for(String id:ids){SentencePattern p=find(id);if(p==null)continue;int attempts=progress.grammarAttempts(id),acc=progress.grammarAccuracy(id);if(attempts>0)started++;if(attempts>=3&&acc>=80)stable++;if(progress.grammarDue(id))due++;totalAttempts+=attempts;addCard(p,attempts,acc);}
        summary.setText(level+" · 共 "+ids.length+" 个高频语法点\n已开始 "+started+" · 基本稳定 "+stable+" · 到期复习 "+due+" · 已做 "+totalAttempts+"题\n每次只看一个规则，再做3题；错题会自动进入语法间隔复习。");
        a1.setAlpha("A1".equals(level)?1f:.62f);a2.setAlpha("A2".equals(level)?1f:.62f);b1.setAlpha("B1".equals(level)?1f:.62f);
    }

    private void renderRecommended(){
        GrammarPoint gp=GrammarDiagnostics.recommended(progress);SentencePattern p=gp==null?null:find(gp.practicePatternId);
        if(p==null)p=find("vorrei");if(p==null)return;
        recommendedTitle.setText("今天先学："+p.title);
        recommendedRule.setText(p.formula+"\n"+p.explanation);
        PatternExercise e=p.exercises.isEmpty()?null:p.exercises.get(0);
        recommendedExample.setText(e==null?"":("例句："+e.it+"\n"+e.zh));
        final String id=p.id;int attempts=progress.grammarAttempts(id),acc=progress.grammarAccuracy(id);
        recommendedStart.setText(attempts==0?"20秒看懂 · 做3题":(progress.grammarDue(id)?"到期了 · 复习3题":("继续巩固 · 当前正确率 "+acc+"%")));
        recommendedStart.setOnClickListener(x->((MainActivity)requireActivity()).openMicroGrammarLesson(id));
    }

    private void addCard(SentencePattern p,int attempts,int acc){
        MaterialCardView card=new MaterialCardView(requireContext());card.setRadius(dp(18));card.setCardElevation(0);card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),R.color.surface));
        int stroke=attempts==0?R.color.line:(acc>=80?R.color.level4:(acc>=60?R.color.level3:R.color.error));card.setStrokeColor(ContextCompat.getColor(requireContext(),stroke));card.setStrokeWidth(dp(1));
        LinearLayout box=new LinearLayout(requireContext());box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(15),dp(13),dp(15),dp(13));card.addView(box);
        TextView title=new TextView(requireContext());title.setText(p.title);title.setTextSize(17);title.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(title);
        TextView formula=new TextView(requireContext());formula.setText(p.formula);formula.setTextSize(13);formula.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue));formula.setPadding(0,dp(5),0,0);box.addView(formula);
        if(!p.exercises.isEmpty()){
            PatternExercise e=p.exercises.get(0);TextView ex=new TextView(requireContext());ex.setText(e.it+"\n"+e.zh);ex.setTextSize(12);ex.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));ex.setPadding(0,dp(7),0,0);box.addView(ex);
        }
        ProgressBar bar=new ProgressBar(requireContext(),null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);int score=attempts==0?0:(int)Math.round(acc*Math.min(1.0,attempts/3.0));bar.setProgress(score);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(6));bp.setMargins(0,dp(9),0,dp(4));box.addView(bar,bp);
        TextView stat=new TextView(requireContext());String s=attempts==0?"新语法点 · 约2分钟":("正确率 "+acc+"% · 已练 "+attempts+"题"+(progress.grammarDue(p.id)?" · 🔄 到期复习":"")+" · 间隔 "+progress.grammarIntervalDays(p.id)+"天");stat.setText(s);stat.setTextSize(11);stat.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));box.addView(stat);
        MaterialButton start=new MaterialButton(requireContext());start.setAllCaps(false);start.setText(attempts==0?"20秒规则 + 3题":"再练3题");final String id=p.id;start.setOnClickListener(x->((MainActivity)requireActivity()).openMicroGrammarLesson(id));LinearLayout.LayoutParams sb=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44));sb.setMargins(0,dp(8),0,0);box.addView(start,sb);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.setMargins(0,dp(6),0,dp(6));list.addView(card,cp);
    }

    private SentencePattern find(String id){if(id==null)return null;for(SentencePattern p:repository.all())if(id.equals(p.id))return p;return null;}
    private String[] idsForLevel(String l){return "B1".equals(l)?B1:("A2".equals(l)?A2:A1);}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
}

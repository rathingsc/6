package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
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

/** Entry page for the 10-article / 2000-word spiral-memory route. */
public class MemoryArticleListFragment extends Fragment {
    private MemoryArticleRepository repo;private ProgressStore progress;private WordRepository words;private LinearLayout container;private TextView routeProgress,wordProgress;private ProgressBar routeBar;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){View v=inflater.inflate(R.layout.fragment_memory_article_list,parent,false);repo=MemoryArticleRepository.get(requireContext());progress=new ProgressStore(requireContext());progress.migrateMemoryArticleExposureIfNeeded(repo);words=WordRepository.get(requireContext());container=v.findViewById(R.id.container_memory_articles);routeProgress=v.findViewById(R.id.text_memory_route_progress);wordProgress=v.findViewById(R.id.text_memory_word_progress);routeBar=v.findViewById(R.id.progress_memory_route);render();return v;}
    @Override public void onResume(){super.onResume();if(repo!=null)render();}
    private void render(){
        int done=progress.memoryArticleCompletedTotal(repo),total=repo.totalSections();java.util.List<Integer> routeIds=new java.util.ArrayList<>();for(int id=1;id<=Math.min(2000,words.size());id++)routeIds.add(id);int encountered=progress.memoryArticleEncounteredCount(routeIds),recognized=progress.memoryArticleRecognizedCount(routeIds),mastered=progress.memoryArticleMasteredCount(routeIds);
        routeProgress.setText("已完成 "+done+" / "+total+" 小节 · "+(done==total?"十篇全部完成 ✅":"建议按顺序往下背"));routeBar.setProgress(total==0?0:(int)Math.round(done*100.0/total));wordProgress.setText("2000词：已遇到 "+encountered+" · 已认识 "+recognized+" · 真正掌握 "+mastered+" · 待巩固 "+(2000-mastered)+"\n后面的文章会主动复现前面学过的高频词，不再一篇学完就消失");container.removeAllViews();int index=0;for(MemoryArticle article:repo.all()){index++;container.addView(articleCard(article,index),cardParams());}
    }
    private View articleCard(MemoryArticle article,int index){
        MaterialCardView card=new MaterialCardView(requireContext());card.setRadius(dp(18));card.setCardElevation(0);card.setStrokeWidth(dp(1));card.setStrokeColor(ContextCompat.getColor(requireContext(),R.color.line));card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),R.color.surface));LinearLayout box=new LinearLayout(requireContext());box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(15),dp(14),dp(15),dp(14));card.addView(box,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title=new TextView(requireContext());title.setText(article.emoji+"  "+article.titleZh);title.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));title.setTextSize(18);title.setTypeface(title.getTypeface(),android.graphics.Typeface.BOLD);box.addView(title);TextView it=new TextView(requireContext());it.setText(article.title);it.setTextColor(ContextCompat.getColor(requireContext(),R.color.blue));it.setTextSize(13);LinearLayout.LayoutParams itp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);itp.topMargin=dp(3);box.addView(it,itp);TextView sub=new TextView(requireContext());sub.setText(article.subtitle);sub.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));sub.setTextSize(12);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);box.addView(sub,sp);
        int done=progress.memoryArticleCompletedSections(article),encountered=progress.memoryArticleEncounteredCount(article.targetWordIds),recognized=progress.memoryArticleRecognizedCount(article.targetWordIds),mastered=progress.memoryArticleMasteredCount(article.targetWordIds);ProgressBar bar=new ProgressBar(requireContext(),null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress((int)Math.round(done*100.0/Math.max(1,article.sections.size())));bar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.green)));bar.setProgressBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(8));bp.topMargin=dp(10);box.addView(bar,bp);
        TextView stat=new TextView(requireContext());stat.setText("小节 "+done+" / "+article.sections.size()+" · 遇到 "+encountered+" / 200 · 认识 "+recognized+" · 掌握 "+mastered+" · 待巩固 "+(200-mastered));stat.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));stat.setTextSize(11);LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);stp.topMargin=dp(6);box.addView(stat,stp);
        MaterialButton button=new MaterialButton(requireContext());button.setAllCaps(false);button.setText(done==article.sections.size()?"复习这篇":"进入第"+index+"篇");button.setGravity(Gravity.CENTER);button.setOnClickListener(v->((MainActivity)requireActivity()).openMemoryArticle(article.id));LinearLayout.LayoutParams mbp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(48));mbp.topMargin=dp(9);box.addView(button,mbp);card.setOnClickListener(v->((MainActivity)requireActivity()).openMemoryArticle(article.id));return card;
    }
    private LinearLayout.LayoutParams cardParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(10);return p;}private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}

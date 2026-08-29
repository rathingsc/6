package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
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

/** Shows the five spiral-memory sections of one 200-word article. */
public class MemoryArticleDetailFragment extends Fragment {
    private static final String ARG_ID="article_id";private MemoryArticleRepository repo;private ProgressStore progress;private WordRepository words;private MemoryArticle article;private LinearLayout container;private TextView stats;private ProgressBar bar;
    public static MemoryArticleDetailFragment newInstance(String id){MemoryArticleDetailFragment f=new MemoryArticleDetailFragment();Bundle b=new Bundle();b.putString(ARG_ID,id);f.setArguments(b);return f;}
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_memory_article_detail,parent,false);repo=MemoryArticleRepository.get(requireContext());progress=new ProgressStore(requireContext());progress.migrateMemoryArticleExposureIfNeeded(repo);words=WordRepository.get(requireContext());article=repo.byId(getArguments()==null?null:getArguments().getString(ARG_ID));if(article==null)return v;
        ((TextView)v.findViewById(R.id.text_memory_article_badge)).setText(article.emoji+" 十篇通关 · 200词");((TextView)v.findViewById(R.id.text_memory_article_title)).setText(article.titleZh+"\n"+article.title);((TextView)v.findViewById(R.id.text_memory_article_subtitle)).setText(article.subtitle+"\n背诵流程：先逐句背文章（双语 → 听读 → 挖空 → 中文反推），再做整节5步背诵。每句可切换正常/慢速朗读，后面小节继续带回高频旧词。");stats=v.findViewById(R.id.text_memory_article_stats);bar=v.findViewById(R.id.progress_memory_article);container=v.findViewById(R.id.container_memory_sections);v.findViewById(R.id.button_memory_article_review).setOnClickListener(x->((MainActivity)requireActivity()).openMemoryArticleReview(toArray(article.targetWordIds),article.titleZh,30));render();return v;
    }
    @Override public void onResume(){super.onResume();if(article!=null)render();}
    private void render(){
        int done=progress.memoryArticleCompletedSections(article),encountered=progress.memoryArticleEncounteredCount(article.targetWordIds),recognized=progress.memoryArticleRecognizedCount(article.targetWordIds),mastered=progress.memoryArticleMasteredCount(article.targetWordIds);
        bar.setProgress((int)Math.round(done*100.0/Math.max(1,article.sections.size())));stats.setText("小节 "+done+" / "+article.sections.size()+" · 已遇到 "+encountered+" / 200 · 已认识 "+recognized+" · 真正掌握 "+mastered+" · 待巩固 "+(200-mastered));container.removeAllViews();for(int i=0;i<article.sections.size();i++)container.addView(sectionCard(article.sections.get(i),i),params());
    }
    private View sectionCard(MemoryArticleSection section,int idx){
        MaterialCardView c=new MaterialCardView(requireContext());c.setRadius(dp(16));c.setCardElevation(0);c.setStrokeWidth(dp(1));c.setStrokeColor(ContextCompat.getColor(requireContext(),R.color.line));c.setCardBackgroundColor(ContextCompat.getColor(requireContext(),progress.memoryArticleSectionDone(section.id)?R.color.surface_success:R.color.surface));
        LinearLayout b=new LinearLayout(requireContext());b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(14),dp(13),dp(14),dp(13));c.addView(b);TextView t=new TextView(requireContext());t.setText((progress.memoryArticleSectionDone(section.id)?"✓ ":(idx+1)+". ")+section.titleZh+" · "+section.title);t.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));t.setTextSize(16);t.setTypeface(t.getTypeface(),android.graphics.Typeface.BOLD);b.addView(t);
        int encountered=progress.memoryArticleEncounteredCount(section.targetWordIds),recognized=progress.memoryArticleRecognizedCount(section.targetWordIds),mastered=progress.memoryArticleMasteredCount(section.targetWordIds);boolean sentenceDone=progress.memoryArticleSentenceStudyDone(section.id);TextView s=new TextView(requireContext());s.setText("40个目标词 · 遇到 "+encountered+" · 认识 "+recognized+" · 掌握 "+mastered+" · "+section.sentences.size()+"句"+(sentenceDone?" · 逐句背诵 ✓":"")+(section.reviewWordIds.isEmpty()?"":" · 旧词复现 "+section.reviewWordIds.size()));s.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));s.setTextSize(11);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(5);b.addView(s,sp);
        ProgressBar pb=new ProgressBar(requireContext(),null,android.R.attr.progressBarStyleHorizontal);pb.setMax(100);pb.setProgress(mastered*100/40);pb.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.blue)));pb.setProgressBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(6));pp.topMargin=dp(8);b.addView(pb,pp);
        MaterialButton sentenceButton=new MaterialButton(requireContext());sentenceButton.setAllCaps(false);sentenceButton.setText(sentenceDone?"逐句再背一遍":"逐句背文章 · 正常/慢速");sentenceButton.setOnClickListener(v->((MainActivity)requireActivity()).openMemoryArticleSentenceStudy(article.id,idx));LinearLayout.LayoutParams sbp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));sbp.topMargin=dp(8);b.addView(sentenceButton,sbp);
        MaterialButton bt=new MaterialButton(requireContext());bt.setAllCaps(false);bt.setText(progress.memoryArticleSectionDone(section.id)?"整节5步再背一遍":"整节5步学习");bt.setOnClickListener(v->((MainActivity)requireActivity()).openMemoryArticleStudy(article.id,idx));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(46));bp.topMargin=dp(6);b.addView(bt,bp);c.setOnClickListener(v->((MainActivity)requireActivity()).openMemoryArticleSentenceStudy(article.id,idx));return c;
    }
    private int[] toArray(java.util.List<Integer> ids){int[] a=new int[ids.size()];for(int i=0;i<a.length;i++)a[i]=ids.get(i);return a;}private LinearLayout.LayoutParams params(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(9);return p;}private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}

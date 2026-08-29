package com.italiano2774.nativeapp;

import android.content.Context;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v4.7.0 offline weak-word micro-reading generator.
 *
 * It never invents Italian prose. It selects from the audited ten-article route first and
 * falls back to the audited word examples only when the current weak words are outside the
 * article coverage. That keeps the personalised training accurate and usable offline.
 */
public final class WeakWordStoryEngine {
    private static final Pattern TOKEN=Pattern.compile("[\\p{L}À-ÿ'’]+",Pattern.UNICODE_CASE);
    private final WordRepository words;private final ProgressStore progress;private final MemoryArticleRepository articles;

    private static class RankedWord {Word word;int score;RankedWord(Word w,int s){word=w;score=s;}}
    private static class SectionScore {MemoryArticle article;MemoryArticleSection section;int articleIndex,sectionIndex,score,overlap;double knownRatio;}

    public WeakWordStoryEngine(Context context,WordRepository words,ProgressStore progress){this.words=words;this.progress=progress;this.articles=MemoryArticleRepository.get(context);}

    public WeakWordStory build(LocalDate day,int variant){
        List<RankedWord> ranked=rankedWeakWords(day);int safeVariant=Math.max(0,variant);
        List<SectionScore> sections=rankSections(ranked);WeakWordStory sectionStory=null;if(!sections.isEmpty()){
            int usable=Math.min(4,sections.size());SectionScore pick=sections.get(Math.floorMod(variant,usable));
            if(pick.overlap>0||pick.knownRatio>=0.55){sectionStory=new WeakWordStory();sectionStory.variant=safeVariant;sectionStory.weakCandidateCount=ranked.size();fillFromSection(sectionStory,pick,ranked);if(sectionStory.targetWords.size()>=6)return sectionStory;}
        }
        WeakWordStory exampleStory=new WeakWordStory();exampleStory.variant=safeVariant;exampleStory.weakCandidateCount=ranked.size();fillFromExamples(exampleStory,ranked,variant);
        return exampleStory.targetWords.size()>=6||sectionStory==null?exampleStory:sectionStory;
    }
    public WeakWordStory build(LocalDate day){return build(day,0);}

    private List<RankedWord> rankedWeakWords(LocalDate day){
        List<RankedWord> out=new ArrayList<>();for(Word w:words.all()){
            if(progress.mastery(w.id)<=0)continue;int s=risk(w,day);if(s>0)out.add(new RankedWord(w,s));
        }
        out.sort((a,b)->{if(a.score!=b.score)return Integer.compare(b.score,a.score);return Integer.compare(a.word.id,b.word.id);});
        if(out.size()<18){Set<Integer> used=new HashSet<>();for(RankedWord r:out)used.add(r.word.id);List<Word> learned=new ArrayList<>();for(Word w:words.all())if(progress.mastery(w.id)>0&&!used.contains(w.id))learned.add(w);learned.sort((a,b)->{int wa=weakLevel(a),wb=weakLevel(b);if(wa!=wb)return Integer.compare(wa,wb);return Integer.compare(progress.mastery(a.id),progress.mastery(b.id));});for(Word w:learned){out.add(new RankedWord(w,Math.max(1,40-weakLevel(w)*6)));if(out.size()>=24)break;}}
        return out;
    }
    private int risk(Word w,LocalDate day){int s=0,wrong=progress.wrongCount(w.id);s+=Math.min(90,wrong*18);if(progress.isStubborn(w.id))s+=46;if(progress.dueForReview(w.id,day)){s+=34;long due=progress.dueEpochDay(w.id);if(due!=Long.MIN_VALUE)s+=Math.min(24,(int)Math.max(0,day.toEpochDay()-due)*3);}int weak=weakLevel(w);s+=Math.max(0,4-weak)*10;s+=Math.max(0,90-progress.memoryRetrievability(w.id))/2;s+=Math.max(0,5-progress.mastery(w.id))*4;return s;}
    private int weakLevel(Word w){return Math.min(Math.min(progress.meaningLevel(w.id),progress.listeningLevel(w.id)),Math.min(progress.spellingLevel(w.id),progress.speakingLevel(w.id)));}

    private List<SectionScore> rankSections(List<RankedWord> ranked){
        Map<Integer,Integer> risk=new HashMap<>();for(int i=0;i<Math.min(24,ranked.size());i++)risk.put(ranked.get(i).word.id,ranked.get(i).score);
        List<SectionScore> out=new ArrayList<>();List<MemoryArticle> all=articles.all();for(int ai=0;ai<all.size();ai++){MemoryArticle a=all.get(ai);for(int si=0;si<a.sections.size();si++){MemoryArticleSection s=a.sections.get(si);Set<Integer> target=new HashSet<>(s.targetWordIds),review=new HashSet<>(s.reviewWordIds);Set<Integer> union=new HashSet<>(target);union.addAll(review);int overlap=0,score=0,known=0;for(Integer id:union)if(progress.mastery(id)>0)known++;for(Map.Entry<Integer,Integer> e:risk.entrySet()){int id=e.getKey();if(target.contains(id)){overlap++;score+=e.getValue()+18;}else if(review.contains(id)){overlap++;score+=e.getValue()+10;}}double ratio=union.isEmpty()?0:known/(double)union.size();score+=(int)Math.round(ratio*55);if(ratio<0.35&&progress.introducedCount(words.all())>80)score-=45;SectionScore x=new SectionScore();x.article=a;x.section=s;x.articleIndex=ai;x.sectionIndex=si;x.score=score;x.overlap=overlap;x.knownRatio=ratio;out.add(x);}}
        out.sort((a,b)->{if(a.score!=b.score)return Integer.compare(b.score,a.score);if(a.overlap!=b.overlap)return Integer.compare(b.overlap,a.overlap);if(a.articleIndex!=b.articleIndex)return Integer.compare(a.articleIndex,b.articleIndex);return Integer.compare(a.sectionIndex,b.sectionIndex);});return out;
    }

    private void fillFromSection(WeakWordStory out,SectionScore pick,List<RankedWord> ranked){
        out.title="今日弱词微短文 · "+pick.section.titleZh;out.sourceLabel="已审校十篇通关 · 第"+(pick.articleIndex+1)+"篇 第"+(pick.sectionIndex+1)+"节";out.italian=pick.section.text;out.chinese=pick.section.translation;out.wordCount=countWords(out.italian);for(MemoryArticleSentence s:pick.section.sentences){MemoryArticleSentence copy=new MemoryArticleSentence();copy.italian=s.italian;copy.chinese=s.chinese;out.sentences.add(copy);}
        Set<Integer> allowed=new LinkedHashSet<>(pick.section.targetWordIds);allowed.addAll(pick.section.reviewWordIds);Set<Integer> used=new HashSet<>();for(RankedWord r:ranked){if(allowed.contains(r.word.id)&&occursAsKnownWord(out.italian,r.word)){out.targetWords.add(r.word);used.add(r.word.id);if(out.targetWords.size()>=10)break;}}
        if(out.targetWords.size()<6){List<Word> inText=wordsInText(out.italian);inText.sort((a,b)->Integer.compare(risk(b,LocalDate.now()),risk(a,LocalDate.now())));for(Word w:inText){if(progress.mastery(w.id)<=0||used.contains(w.id))continue;out.targetWords.add(w);used.add(w.id);if(out.targetWords.size()>=8)break;}}
        buildClozeTargets(out);
    }

    private void fillFromExamples(WeakWordStory out,List<RankedWord> ranked,int variant){
        List<RankedWord> candidates=new ArrayList<>();for(RankedWord r:ranked)if(r.word.example!=null&&!r.word.example.trim().isEmpty()&&r.word.exampleZh!=null&&!r.word.exampleZh.trim().isEmpty())candidates.add(r);
        if(candidates.isEmpty()){List<MemoryArticle> all=articles.all();MemoryArticle a=all.get(Math.floorMod(variant,all.size()));MemoryArticleSection s=a.sections.get(0);SectionScore x=new SectionScore();x.article=a;x.section=s;x.articleIndex=Math.floorMod(variant,all.size());x.sectionIndex=0;x.overlap=0;x.knownRatio=0;fillFromSection(out,x,ranked);out.sourceLabel="已审校十篇通关 · 当前弱词不足，先做可理解输入";return;}
        int start=Math.floorMod(variant*3,Math.max(1,candidates.size()));StringBuilder it=new StringBuilder(),zh=new StringBuilder();Set<Integer> used=new HashSet<>();for(int step=0;step<candidates.size()&&out.targetWords.size()<6;step++){RankedWord r=candidates.get((start+step)%candidates.size());if(!used.add(r.word.id))continue;if(it.length()>0){it.append(' ');zh.append(' ');}it.append(cleanSentence(r.word.example));zh.append(cleanSentence(r.word.exampleZh));out.targetWords.add(r.word);MemoryArticleSentence s=new MemoryArticleSentence();s.italian=cleanSentence(r.word.example);s.chinese=cleanSentence(r.word.exampleZh);out.sentences.add(s);}
        out.title="今日弱词例句微短文";out.sourceLabel="2774词库已审校例句组合 · 不联网生成";out.italian=it.toString();out.chinese=zh.toString();out.wordCount=countWords(out.italian);buildClozeTargets(out);
    }

    private void buildClozeTargets(WeakWordStory out){
        if(out.italian==null)return;Set<Integer> used=new HashSet<>();Matcher m=TOKEN.matcher(out.italian);while(m.find()&&out.clozeTargets.size()<4){String token=m.group();if(token.length()<3)continue;Word found=words.lookupSurface(token);Word target=matchTarget(found,out.targetWords);if(target==null||used.contains(target.id))continue;used.add(target.id);out.clozeTargets.add(new WeakWordStory.ClozeTarget(target.id,token,target.chinese));}
        if(out.clozeTargets.size()<4){m=TOKEN.matcher(out.italian);while(m.find()&&out.clozeTargets.size()<4){String token=m.group();if(token.length()<4)continue;Word w=words.lookupSurface(token);if(w==null||progress.mastery(w.id)<=0||used.contains(w.id))continue;used.add(w.id);if(!containsId(out.targetWords,w.id)&&out.targetWords.size()<10)out.targetWords.add(w);out.clozeTargets.add(new WeakWordStory.ClozeTarget(w.id,token,w.chinese));}}
    }

    private Word matchTarget(Word found,List<Word> targets){if(found==null)return null;for(Word t:targets){if(t.id==found.id)return t;String a=lemma(t),b=lemma(found);if(!a.isEmpty()&&a.equals(b))return t;}return null;}
    private boolean occursAsKnownWord(String text,Word target){Matcher m=TOKEN.matcher(text==null?"":text);while(m.find()){Word x=words.lookupSurface(m.group());if(matchTarget(x,Collections.singletonList(target))!=null)return true;}return false;}
    private List<Word> wordsInText(String text){List<Word> out=new ArrayList<>();Set<Integer> ids=new HashSet<>();Matcher m=TOKEN.matcher(text==null?"":text);while(m.find()){Word w=words.lookupSurface(m.group());if(w!=null&&ids.add(w.id))out.add(w);}return out;}
    private boolean containsId(List<Word> xs,int id){for(Word w:xs)if(w.id==id)return true;return false;}
    private String lemma(Word w){if(w==null)return "";String s=w.lemma==null||w.lemma.trim().isEmpty()?w.word:w.lemma;return s==null?"":s.trim().toLowerCase(Locale.ITALIAN);}
    private int countWords(String text){int n=0;Matcher m=TOKEN.matcher(text==null?"":text);while(m.find())n++;return n;}
    private String cleanSentence(String s){if(s==null)return "";String x=s.trim();if(x.isEmpty())return x;char last=x.charAt(x.length()-1);return ".!?".indexOf(last)>=0?x:x+".";}
}

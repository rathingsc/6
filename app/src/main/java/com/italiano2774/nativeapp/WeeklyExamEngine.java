package com.italiano2774.nativeapp;

import android.content.Context;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * v4.5.0 offline weekly practical exam.
 *
 * The exam deliberately samples six learner-facing skills and uses only local,
 * audited course assets. It is an internal learning diagnostic, not an official
 * CEFR exam. The same day receives the same deterministic batch so reopening the
 * page does not silently change difficulty.
 */
public final class WeeklyExamEngine {
    public static final int TYPE_CHOICE=0;
    public static final int TYPE_TYPED=1;
    public static final int TYPE_SPEAK=2;
    public static final int QUESTIONS_PER_SKILL=3;
    public static final int TOTAL_QUESTIONS=18;

    public static final String[] SKILL_KEYS={
            MasteryPassportEngine.ACTION_MEANING,
            MasteryPassportEngine.ACTION_LISTENING,
            MasteryPassportEngine.ACTION_SPELLING,
            MasteryPassportEngine.ACTION_SPEAKING,
            MasteryPassportEngine.ACTION_GRAMMAR,
            MasteryPassportEngine.ACTION_REAL_USE
    };

    public static class Item {
        public String id="",skillKey="",skillLabel="",prompt="",expected="",hint="",patternId="";
        public int type=TYPE_TYPED;
        public final List<String> choices=new ArrayList<>();
        public Word word;
        public boolean hasAudio(){return word!=null&&MasteryPassportEngine.ACTION_LISTENING.equals(skillKey);}
    }

    private final Context context;
    private final WordRepository words;
    private final ProgressStore progress;
    private final CoreSentenceRepository core;
    private final SentencePatternRepository patterns;
    private final Random random;
    private final String level;

    public WeeklyExamEngine(Context context,WordRepository words,ProgressStore progress){
        this.context=context.getApplicationContext();this.words=words;this.progress=progress;
        this.core=CoreSentenceRepository.get(context);this.patterns=SentencePatternRepository.get(context);
        MasteryPassportEngine.Snapshot snapshot=MasteryPassportEngine.build(this.context,words,progress);
        this.level=snapshot.targetLevel==null||snapshot.targetLevel.isEmpty()?"A1":snapshot.targetLevel;
        long seed=LocalDate.now().toEpochDay()*1009L+progress.weeklyExamCount()*7919L+level.hashCode();
        this.random=new Random(seed);
    }

    public String targetLevel(){return level;}

    public List<Item> build(){
        List<Item> out=new ArrayList<>();
        List<Word> pool=learnedWordPool();
        if(pool.isEmpty())return out;
        Collections.shuffle(pool,random);int cursor=0;
        for(int i=0;i<QUESTIONS_PER_SKILL;i++)out.add(wordChoice("meaning_"+i,MasteryPassportEngine.ACTION_MEANING,"识义",pool.get(cursor++%pool.size()),pool,false));
        for(int i=0;i<QUESTIONS_PER_SKILL;i++)out.add(wordChoice("listening_"+i,MasteryPassportEngine.ACTION_LISTENING,"听力",pool.get(cursor++%pool.size()),pool,true));
        for(int i=0;i<QUESTIONS_PER_SKILL;i++)out.add(spelling("spelling_"+i,pool.get(cursor++%pool.size())));

        List<CoreSentence> sentencePool=core.level(level);if(sentencePool.size()<6)sentencePool=core.all();Collections.shuffle(sentencePool,random);
        for(int i=0;i<QUESTIONS_PER_SKILL;i++)out.add(sentence("speaking_"+i,MasteryPassportEngine.ACTION_SPEAKING,"口语",sentencePool.get(i%sentencePool.size()),true));
        addGrammar(out);
        for(int i=0;i<QUESTIONS_PER_SKILL;i++)out.add(sentence("real_"+i,MasteryPassportEngine.ACTION_REAL_USE,"真实使用",sentencePool.get((i+QUESTIONS_PER_SKILL)%sentencePool.size()),false));
        // Keep skills interleaved instead of presenting six blocks. This makes the
        // exam closer to retrieval practice and reduces answer-pattern adaptation.
        Collections.shuffle(out,new Random(LocalDate.now().toEpochDay()*3571L+progress.weeklyExamCount()*17L));
        return out;
    }

    private List<Word> learnedWordPool(){
        List<Word> all=words.all(),out=new ArrayList<>();int route=Math.min(progress.vocabularyRouteLimit(),all.size());
        for(int i=0;i<route;i++){Word w=all.get(i);if(progress.mastery(w.id)>0)out.add(w);}
        // A learner can reach seven active days with few saved cards (e.g. mostly
        // listening/grammar). Fall back only to the earliest route words, never to
        // an arbitrary advanced slice.
        int fallback=Math.min(route,Math.max(60,progress.routeIntroducedCount(all)+30));
        for(int i=0;out.size()<18&&i<fallback;i++){Word w=all.get(i);if(!containsId(out,w.id))out.add(w);}
        return out;
    }

    private boolean containsId(List<Word> xs,int id){for(Word w:xs)if(w.id==id)return true;return false;}

    private Item wordChoice(String id,String skill,String label,Word target,List<Word> pool,boolean listening){
        Item q=new Item();q.id=id;q.skillKey=skill;q.skillLabel=label;q.type=TYPE_CHOICE;q.word=target;
        q.expected=listening?target.word:safeChinese(target);
        q.prompt=listening?"🔊 听音，选择你听到的意大利语单词":("选择正确中文意思：\n"+target.word);
        q.hint=listening?"只听声音，不显示中文提示":"不要凭词形猜，先主动回忆词义";
        q.choices.add(q.expected);int guard=0;while(q.choices.size()<4&&guard++<300){Word w=pool.get(random.nextInt(pool.size()));String x=listening?w.word:safeChinese(w);if(x!=null&&!x.trim().isEmpty()&&!q.choices.contains(x))q.choices.add(x);}Collections.shuffle(q.choices,random);return q;
    }

    private Item spelling(String id,Word w){Item q=new Item();q.id=id;q.skillKey=MasteryPassportEngine.ACTION_SPELLING;q.skillLabel="拼写";q.type=TYPE_TYPED;q.word=w;q.prompt="只看中文写出意大利语：\n"+safeChinese(w);q.expected=w.word;q.hint="不给首字母，不给选项";return q;}

    private Item sentence(String id,String skill,String label,CoreSentence s,boolean speaking){Item q=new Item();q.id=id;q.skillKey=skill;q.skillLabel=label;q.type=speaking?TYPE_SPEAK:TYPE_TYPED;q.prompt=(speaking?"只看中文说出完整意大利语：\n":"生活场景 · "+safeCategory(s.category)+"\n请用意大利语表达：\n")+s.chinese;q.expected=s.italian;q.hint=speaking?"可以用麦克风；语音识别不可用时可输入":"允许自然表达；本地匹配会重点检查核心词和句子完整度";return q;}

    private void addGrammar(List<Item> out){List<SentencePattern> ps=new ArrayList<>(patterns.all());Collections.shuffle(ps,random);int made=0;for(SentencePattern p:ps){if(p==null||p.exercises==null||p.exercises.isEmpty())continue;PatternExercise e=p.exercises.get(random.nextInt(p.exercises.size()));if(e==null||e.answer==null||e.answer.trim().isEmpty())continue;Item q=new Item();q.id="grammar_"+made;q.skillKey=MasteryPassportEngine.ACTION_GRAMMAR;q.skillLabel="语法";q.type=TYPE_TYPED;q.patternId=p.id;q.prompt=(p.title==null?"句型语法":p.title)+"\n"+(e.prompt==null?"补全正确表达":e.prompt);q.expected=e.answer;q.hint="不提供选项，直接提取正确结构";out.add(q);if(++made>=QUESTIONS_PER_SKILL)break;}while(made<QUESTIONS_PER_SKILL){CoreSentence s=core.all().get(made%core.all().size());Item q=sentence("grammar_fallback_"+made,MasteryPassportEngine.ACTION_GRAMMAR,"语法",s,false);q.patternId="weekly_fallback";out.add(q);made++;}}

    public int passThreshold(Item q){if(q==null)return 70;if(MasteryPassportEngine.ACTION_SPEAKING.equals(q.skillKey))return "B1".equals(level)?80:("A2".equals(level)?75:70);if(MasteryPassportEngine.ACTION_REAL_USE.equals(q.skillKey))return "B1".equals(level)?75:("A2".equals(level)?70:65);return 100;}

    public static int skillIndex(String key){for(int i=0;i<SKILL_KEYS.length;i++)if(SKILL_KEYS[i].equals(key))return i;return 0;}
    public static String skillLabel(String key){if(MasteryPassportEngine.ACTION_LISTENING.equals(key))return "听力";if(MasteryPassportEngine.ACTION_SPELLING.equals(key))return "拼写";if(MasteryPassportEngine.ACTION_SPEAKING.equals(key))return "口语";if(MasteryPassportEngine.ACTION_GRAMMAR.equals(key))return "语法";if(MasteryPassportEngine.ACTION_REAL_USE.equals(key))return "真实使用";return "识义";}
    private String safeChinese(Word w){return w.chinese==null||w.chinese.trim().isEmpty()?w.english:w.chinese;}
    private String safeCategory(String s){return s==null||s.trim().isEmpty()?"日常交流":s;}
}

package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * v3.3.4 guided mixed-skill course scheduler.
 *
 * A unit is no longer a row of vocabulary-only lessons.  New words are distributed across
 * vocabulary, listening, sentence, grammar and active-output nodes, then brought together in
 * the unit challenge.  Each node still stays short (<=12 scored/teaching cards) for beginners.
 *
 * v5.0.1 rule: fixed course lessons are topic-pure. Global due reviews must stay in the
 * daily smart/review routes and must never be injected into unrelated course units.
 */
public class CourseLessonEngine {
    private static final int ROLE_VOCAB=0,ROLE_LISTEN=1,ROLE_SENTENCE=2,ROLE_GRAMMAR=3,
            ROLE_ACTIVE=4,ROLE_LISTEN_REINFORCE=5,ROLE_INTEGRATE=6,ROLE_CHALLENGE=7;
    private static final String[] PERSONS={"io","tu","lui/lei","noi","voi","loro"};

    private final WordRepository repo;private final ProgressStore progress;
    public CourseLessonEngine(WordRepository r,ProgressStore p){repo=r;progress=p;}

    public List<CourseQuestion> build(CourseUnit unit,int lessonIndex){
        List<Word> words=unitWords(unit);if(words.isEmpty())return new ArrayList<>();
        int safeLesson=Math.max(0,Math.min(unit.lessonCount-1,lessonIndex));
        Random rnd=new Random(31L*unit.id.hashCode()+safeLesson*997L);
        int role=role(unit,safeLesson);
        if(role==ROLE_CHALLENGE)return challenge(words,unit,rnd);

        int preChallenge=Math.max(1,unit.lessonCount-1);
        List<Word> focus=chunk(words,safeLesson,preChallenge);
        if(focus.isEmpty())focus.add(words.get(Math.min(safeLesson,words.size()-1)));
        List<CourseQuestion> out=new ArrayList<>();

        switch(role){
            case ROLE_VOCAB:
                for(Word w:focus)out.add(meaning(w,unit,rnd,true));
                addSeenReinforcement(out,words,focus,unit,rnd,ROLE_LISTEN,2);
                break;
            case ROLE_LISTEN:
                for(Word w:focus)out.add(listen(w,unit,rnd));
                addSeenReinforcement(out,words,focus,unit,rnd,ROLE_SENTENCE,2);
                break;
            case ROLE_SENTENCE:
                for(Word w:focus)out.add(sentenceLearning(w,unit,rnd));
                addSeenReinforcement(out,words,focus,unit,rnd,ROLE_LISTEN,2);
                break;
            case ROLE_GRAMMAR:
                for(Word w:focus)out.add(grammarLearning(w,unit,rnd));
                addSeenReinforcement(out,words,focus,unit,rnd,ROLE_SENTENCE,2);
                break;
            case ROLE_ACTIVE:
                for(Word w:focus)out.add(spellHint(w));
                addSeenReinforcement(out,words,focus,unit,rnd,ROLE_ACTIVE,2);
                break;
            case ROLE_LISTEN_REINFORCE:
                for(Word w:focus)out.add(listen(w,unit,rnd));
                addSeenReinforcement(out,words,focus,unit,rnd,ROLE_GRAMMAR,2);
                break;
            default:
                int turn=0;
                for(Word w:focus){
                    int m=turn++%4;
                    if(m==0)out.add(listen(w,unit,rnd));
                    else if(m==1)out.add(sentenceLearning(w,unit,rnd));
                    else if(m==2)out.add(grammarLearning(w,unit,rnd));
                    else out.add(spellHint(w));
                }
                addSeenReinforcement(out,words,focus,unit,rnd,ROLE_SENTENCE,2);
                break;
        }
        if(out.size()>12)out=new ArrayList<>(out.subList(0,12));
        return out;
    }

    public String lessonTitle(CourseUnit unit,int lessonIndex){
        switch(role(unit,lessonIndex)){
            case ROLE_VOCAB:return "词汇起步";
            case ROLE_LISTEN:return "听力训练";
            case ROLE_SENTENCE:return "句子理解";
            case ROLE_GRAMMAR:return "语法微练";
            case ROLE_ACTIVE:return "主动表达";
            case ROLE_LISTEN_REINFORCE:return "听力巩固";
            case ROLE_INTEGRATE:return "综合运用";
            default:return "单元挑战";
        }
    }
    public String lessonEmoji(CourseUnit unit,int lessonIndex){
        switch(role(unit,lessonIndex)){
            case ROLE_VOCAB:return "🌱";
            case ROLE_LISTEN:return "🎧";
            case ROLE_SENTENCE:return "💬";
            case ROLE_GRAMMAR:return "🧩";
            case ROLE_ACTIVE:return "✍️";
            case ROLE_LISTEN_REINFORCE:return "🎧";
            case ROLE_INTEGRATE:return "🔄";
            default:return "🏆";
        }
    }
    public String pathSummary(CourseUnit unit){
        List<String> labels=new ArrayList<>();
        for(int i=0;i<unit.lessonCount;i++){String x=lessonTitle(unit,i);if(!labels.contains(x))labels.add(x);}
        return join(labels," → ");
    }

    private int role(CourseUnit unit,int lessonIndex){
        int n=Math.max(5,unit.lessonCount),i=Math.max(0,Math.min(n-1,lessonIndex));
        if(i==n-1)return ROLE_CHALLENGE;
        if(n==5){int[] r={ROLE_VOCAB,ROLE_LISTEN,ROLE_SENTENCE,ROLE_ACTIVE};return r[i];}
        if(n==6){int[] r={ROLE_VOCAB,ROLE_LISTEN,ROLE_SENTENCE,ROLE_GRAMMAR,ROLE_ACTIVE};return r[i];}
        if(n==7){int[] r={ROLE_VOCAB,ROLE_LISTEN,ROLE_SENTENCE,ROLE_GRAMMAR,ROLE_ACTIVE,ROLE_INTEGRATE};return r[i];}
        int[] r={ROLE_VOCAB,ROLE_LISTEN,ROLE_SENTENCE,ROLE_GRAMMAR,ROLE_ACTIVE,ROLE_LISTEN_REINFORCE,ROLE_INTEGRATE};
        return r[Math.min(i,r.length-1)];
    }

    private List<CourseQuestion> challenge(List<Word> words,CourseUnit unit,Random rnd){
        List<CourseQuestion> out=new ArrayList<>();List<Word> pool=sample(words,Math.min(10,words.size()),rnd);int turn=0;
        for(Word w:pool){
            int m=turn++%6;
            if(m==0)out.add(listen(w,unit,rnd));
            else if(m==1)out.add(active(w));
            else if(m==2)out.add(hasUsableExample(w)?exampleMeaning(w,unit,rnd):meaning(w,unit,rnd));
            else if(m==3)out.add(grammarLearning(w,unit,rnd));
            else if(m==4)out.add(hasUsableExample(w)?cloze(w,unit,rnd):spellHint(w));
            else out.add(meaning(w,unit,rnd));
        }
        if(out.size()>12)out=new ArrayList<>(out.subList(0,12));return out;
    }

    private void addSeenReinforcement(List<CourseQuestion> out,List<Word> all,List<Word> focus,CourseUnit unit,Random rnd,int mode,int max){
        if(out.size()>=12||max<=0)return;
        int firstFocus=all.indexOf(focus.get(0));if(firstFocus<=0)return;
        List<Word> seen=new ArrayList<>(all.subList(0,firstFocus));Collections.shuffle(seen,rnd);int n=0;
        for(Word w:seen){
            CourseQuestion q;
            if(mode==ROLE_LISTEN)q=listen(w,unit,rnd);
            else if(mode==ROLE_SENTENCE)q=hasUsableExample(w)?cloze(w,unit,rnd):meaning(w,unit,rnd);
            else if(mode==ROLE_GRAMMAR)q=grammarLearning(w,unit,rnd);
            else q=active(w);
            out.add(q);if(++n>=max||out.size()>=12)break;
        }
    }

    private CourseQuestion sentenceLearning(Word w,CourseUnit u,Random rnd){
        if(!hasUsableExample(w))return meaning(w,u,rnd,true);
        return (w.id%2==0)?cloze(w,u,rnd):exampleMeaning(w,u,rnd);
    }
    private CourseQuestion grammarLearning(Word w,CourseUnit u,Random rnd){
        CourseQuestion q=articleQuestion(w,rnd);if(q!=null)return q;
        q=verbQuestion(w,u,rnd);if(q!=null)return q;
        if(hasUsableExample(w))return cloze(w,u,rnd);
        return meaning(w,u,rnd,true);
    }

    private CourseQuestion articleQuestion(Word w,Random rnd){
        if(!"noun".equalsIgnoreCase(safe(w.partOfSpeech)))return null;
        String article=safe(w.article);if(article.isEmpty())return null;
        CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.GRAMMAR_ARTICLE;q.word=w;q.prompt="选择正确冠词";q.display="___ "+w.word;q.answer=article;q.dimension=ProgressStore.DIM_MEANING;
        String gender="f".equalsIgnoreCase(safe(w.gender))?"阴性":"阳性";q.support=zh(w)+" · "+gender+(safe(w.number).isEmpty()?"":" · "+("plural".equalsIgnoreCase(w.number)?"复数":"单数"));
        String[] base=("plural".equalsIgnoreCase(safe(w.number))||"i".equals(article)||"gli".equals(article)||"le".equals(article))?new String[]{"i","gli","le","dei"}:new String[]{"il","lo","la","l'"};
        q.options.add(article);for(String x:base)if(!q.options.contains(x))q.options.add(x);while(q.options.size()>4)q.options.remove(q.options.size()-1);Collections.shuffle(q.options,rnd);return q;
    }
    private CourseQuestion verbQuestion(Word w,CourseUnit u,Random rnd){
        if(!ItalianGrammar.isVerb(w))return null;String[] forms=ItalianGrammar.presentIndicative(w);if(forms==null||forms.length<6)return null;
        int person=Math.floorMod(w.id,6);String lemma=safe(w.lemma);if(lemma.isEmpty())lemma=w.word;
        CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.GRAMMAR_VERB;q.word=w;q.prompt="选择正确的现在时变位";q.display=PERSONS[person]+" ___  ("+lemma+")";q.answer=forms[person];q.dimension=ProgressStore.DIM_SPELLING;q.support=zh(w)+" · 原形 "+lemma;
        LinkedHashSet<String> opts=new LinkedHashSet<>();opts.add(q.answer);for(String f:forms)if(f!=null&&!f.trim().isEmpty())opts.add(f);
        if(opts.size()<4){List<Word> unitWords=unitWords(u);Collections.shuffle(unitWords,rnd);for(Word other:unitWords){if(other.id==w.id||!ItalianGrammar.isVerb(other))continue;String[] of=ItalianGrammar.presentIndicative(other);if(of!=null&&person<of.length)opts.add(of[person]);if(opts.size()>=4)break;}}
        if(opts.size()<4){for(Word other:repo.all()){if(!ItalianGrammar.isVerb(other))continue;String[] of=ItalianGrammar.presentIndicative(other);if(of!=null&&person<of.length)opts.add(of[person]);if(opts.size()>=4)break;}}
        q.options.addAll(opts);while(q.options.size()>4)q.options.remove(q.options.size()-1);if(q.options.size()<2)return null;Collections.shuffle(q.options,rnd);return q;
    }

    private List<Word> unitWords(CourseUnit u){List<Word> out=new ArrayList<>();for(Integer id:u.wordIds){Word w=repo.byId(id==null?0:id);if(w!=null)out.add(w);}return out;}
    private List<Word> chunk(List<Word> all,int index,int chunks){int a=(int)Math.floor(index*all.size()/(double)chunks),b=(int)Math.floor((index+1)*all.size()/(double)chunks);return new ArrayList<>(all.subList(Math.min(a,all.size()),Math.min(Math.max(a+1,b),all.size())));}
    private List<Word> sample(List<Word> all,int n,Random rnd){List<Word> c=new ArrayList<>(all);Collections.shuffle(c,rnd);return new ArrayList<>(c.subList(0,Math.min(n,c.size())));}
    private boolean hasUsableExample(Word w){return ExampleQuality.isUsable(w);}
    private String zh(Word w){return w.chinese==null||w.chinese.trim().isEmpty()?w.english:w.chinese;}

    private CourseQuestion intro(Word w){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.INTRO;q.word=w;q.prompt="先认识这个表达";q.display=w.word;boolean safe=hasUsableExample(w);q.support=zh(w)+(safe?"\n\n"+w.example+"\n"+(w.exampleZh==null?"":w.exampleZh):"");q.answer=w.word;q.autoPlayAudio=true;return q;}
    private CourseQuestion meaning(Word w,CourseUnit u,Random rnd){return meaning(w,u,rnd,false);}
    private CourseQuestion meaning(Word w,CourseUnit u,Random rnd,boolean teachBeforeTest){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.MEANING;q.word=w;q.prompt="选择正确意思";q.display=w.word;q.answer=zh(w);q.dimension=ProgressStore.DIM_MEANING;q.teachBeforeTest=teachBeforeTest;q.options.addAll(chineseOptions(w,u,rnd));return q;}
    private CourseQuestion listen(Word w,CourseUnit u,Random rnd){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.LISTEN;q.word=w;q.prompt="听音频，选择你听到的意大利语";q.display="🔊";q.support=zh(w);q.answer=w.word;q.dimension=ProgressStore.DIM_LISTENING;q.autoPlayAudio=true;q.options.addAll(italianOptions(w,u,rnd));return q;}
    private CourseQuestion spellHint(Word w){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.SPELL_HINT;q.word=w;q.prompt="根据中文和提示补出意大利语";q.display=zh(w);q.answer=w.word;q.dimension=ProgressStore.DIM_SPELLING;q.hint=mask(w.word);return q;}
    private CourseQuestion active(Word w){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.ACTIVE;q.word=w;q.prompt="不要看选项，写出意大利语";q.display=zh(w);q.answer=w.word;q.dimension=ProgressStore.DIM_SPELLING;q.hint="想不起来也没关系，提交后会看到答案";return q;}
    private CourseQuestion cloze(Word w,CourseUnit u,Random rnd){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.CLOZE;q.word=w;q.prompt="把句子补完整";q.display=replaceIgnoreCase(w.example,w.word,"_____");q.support=w.exampleZh==null?"":w.exampleZh;q.answer=w.word;q.dimension=ProgressStore.DIM_MEANING;q.options.addAll(clozeOptions(w,u,rnd));return q;}
    private CourseQuestion exampleMeaning(Word w,CourseUnit u,Random rnd){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.EXAMPLE_MEANING;q.word=w;q.prompt="这句话是什么意思？";q.display=w.example;q.answer=w.exampleZh;q.dimension=ProgressStore.DIM_MEANING;q.options.add(q.answer);for(Word d:safeExampleDistractors(w,u,rnd,8)){String z=d.exampleZh;if(!q.options.contains(z))q.options.add(z);if(q.options.size()>=4)break;}if(q.options.size()<4){for(Word d:distractors(w,u,rnd,12)){String z=zh(d);if(!q.options.contains(z))q.options.add(z);if(q.options.size()>=4)break;}}Collections.shuffle(q.options,rnd);return q;}

    private List<String> chineseOptions(Word target,CourseUnit u,Random rnd){List<String> x=new ArrayList<>();x.add(zh(target));for(Word d:distractors(target,u,rnd,8)){String z=zh(d);if(!x.contains(z))x.add(z);if(x.size()>=4)break;}if(x.size()<4){for(Word d:repo.all()){String z=zh(d);if(d.id!=target.id&&!x.contains(z))x.add(z);if(x.size()>=4)break;}}Collections.shuffle(x,rnd);return x;}
    private List<String> italianOptions(Word target,CourseUnit u,Random rnd){List<String> x=new ArrayList<>();x.add(target.word);for(Word d:distractors(target,u,rnd,8)){if(!x.contains(d.word))x.add(d.word);if(x.size()>=4)break;}if(x.size()<4){for(Word d:repo.all()){if(d.id!=target.id&&!x.contains(d.word))x.add(d.word);if(x.size()>=4)break;}}Collections.shuffle(x,rnd);return x;}
    private List<String> clozeOptions(Word target,CourseUnit u,Random rnd){
        List<String> x=new ArrayList<>();x.add(target.word);
        List<Word> pool=unitWords(u);Collections.shuffle(pool,rnd);
        for(Word d:pool){if(d.id==target.id||!sameGrammarClass(target,d)||x.contains(d.word))continue;x.add(d.word);if(x.size()>=4)break;}
        if(x.size()<4)for(Word d:distractors(target,u,rnd,12)){if(!x.contains(d.word))x.add(d.word);if(x.size()>=4)break;}
        if(x.size()<4)for(Word d:repo.all()){if(d.id!=target.id&&!x.contains(d.word))x.add(d.word);if(x.size()>=4)break;}
        Collections.shuffle(x,rnd);return x;
    }
    private boolean sameGrammarClass(Word a,Word b){
        String ap=safe(a.partOfSpeech),bp=safe(b.partOfSpeech);if(!ap.equals(bp))return false;
        if("noun".equals(ap)){String ag=safe(a.gender),bg=safe(b.gender);if(!ag.isEmpty()&&!bg.isEmpty()&&!ag.equals(bg))return false;}
        return true;
    }
    private List<Word> safeExampleDistractors(Word target,CourseUnit u,Random rnd,int max){List<Word> pool=unitWords(u);if(pool.size()<6)pool=new ArrayList<>(repo.all());Collections.shuffle(pool,rnd);List<Word> out=new ArrayList<>();for(Word w:pool){if(w.id==target.id||!ExampleQuality.isUsable(w))continue;out.add(w);if(out.size()>=max)break;}return out;}
    private List<Word> distractors(Word target,CourseUnit u,Random rnd,int max){List<Word> pool=unitWords(u);if(pool.size()<4)pool=new ArrayList<>(repo.all().subList(0,Math.min(250,repo.size())));Collections.shuffle(pool,rnd);List<Word> out=new ArrayList<>();for(Word w:pool){if(w.id==target.id)continue;out.add(w);if(out.size()>=max)break;}return out;}

    private String mask(String s){if(s==null||s.isEmpty())return "";StringBuilder b=new StringBuilder();int letters=0;for(int i=0;i<s.length();i++){char c=s.charAt(i);if(Character.isLetter(c)){b.append(letters++%2==0?c:'_');}else b.append(c);}return b.toString();}
    private String replaceIgnoreCase(String src,String needle,String repl){int i=src.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));if(i<0)return src;return src.substring(0,i)+repl+src.substring(i+needle.length());}
    private String safe(String s){return s==null?"":s.trim();}
    private String join(List<String> xs,String sep){StringBuilder b=new StringBuilder();for(String x:xs){if(b.length()>0)b.append(sep);b.append(x);}return b.toString();}
}

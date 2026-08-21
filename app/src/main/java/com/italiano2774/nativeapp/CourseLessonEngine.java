package com.italiano2774.nativeapp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * v3.0 lesson scheduler: introduce -> recognize -> listen -> use in a sentence -> active recall.
 * The hard FSRS/error machinery remains underneath, but the learner only sees short progressive lessons.
 */
public class CourseLessonEngine {
    private final WordRepository repo;private final ProgressStore progress;
    public CourseLessonEngine(WordRepository r,ProgressStore p){repo=r;progress=p;}

    public List<CourseQuestion> build(CourseUnit unit,int lessonIndex){
        List<Word> words=unitWords(unit);if(words.isEmpty())return new ArrayList<>();
        int learningNodes=Math.max(1,unit.lessonCount-2);Random rnd=new Random(31L*unit.id.hashCode()+lessonIndex*997L);
        List<CourseQuestion> out=new ArrayList<>();
        if(lessonIndex<learningNodes){
            List<Word> focus=chunk(words,lessonIndex,learningNodes);
            if(focus.isEmpty())focus.add(words.get(Math.min(lessonIndex,words.size()-1)));
            // Every focus word must appear at least once before optional reinforcement is added.
            // Large B1 chunks therefore use one intro + one recognition task per word,
            // while smaller beginner chunks get extra audio/sentence practice within the 12-card cap.
            int introCount=focus.size()<=6?Math.min(2,focus.size()):1;
            for(int i=0;i<introCount;i++)out.add(intro(focus.get(i)));
            for(Word w:focus)out.add(meaning(w,unit,rnd));
            for(int i=0;i<Math.min(2,focus.size())&&out.size()<11;i++)out.add(listen(focus.get((i+lessonIndex)%focus.size()),unit,rnd));
            if(!focus.isEmpty()&&out.size()<12){Word w=focus.get(focus.size()-1);out.add(hasUsableExample(w)?cloze(w,unit,rnd):spellHint(w));}
        }else if(lessonIndex==unit.lessonCount-2){
            List<Word> pool=sample(words,Math.min(8,words.size()),rnd);int turn=0;
            for(Word w:pool){out.add(turn%3==0?listen(w,unit,rnd):(turn%3==1?(hasUsableExample(w)?cloze(w,unit,rnd):meaning(w,unit,rnd)):spellHint(w)));turn++;}
            addDueReview(out,unit,rnd,2);
        }else{
            List<Word> pool=sample(words,Math.min(10,words.size()),rnd);int turn=0;
            for(Word w:pool){int m=turn++%5;if(m==0)out.add(listen(w,unit,rnd));else if(m==1)out.add(active(w));else if(m==2)out.add(hasUsableExample(w)?exampleMeaning(w,unit,rnd):meaning(w,unit,rnd));else if(m==3)out.add(spellHint(w));else out.add(hasUsableExample(w)?cloze(w,unit,rnd):meaning(w,unit,rnd));}
            addDueReview(out,unit,rnd,2);
        }
        // Lessons are intentionally short. Intro cards count as teaching, not testing.
        if(out.size()>12)out=new ArrayList<>(out.subList(0,12));
        return out;
    }

    public String lessonTitle(CourseUnit unit,int lessonIndex){
        int learn=Math.max(1,unit.lessonCount-2);
        if(lessonIndex<learn)return lessonIndex==0?"认识新内容":"继续学习";
        if(lessonIndex==unit.lessonCount-2)return "混合复习";
        return "单元挑战";
    }
    public String lessonEmoji(CourseUnit unit,int lessonIndex){int learn=Math.max(1,unit.lessonCount-2);if(lessonIndex<learn)return lessonIndex==0?"🌱":"●";if(lessonIndex==unit.lessonCount-2)return "⚡";return "🏆";}

    private List<Word> unitWords(CourseUnit u){List<Word> out=new ArrayList<>();for(Integer id:u.wordIds){Word w=repo.byId(id==null?0:id);if(w!=null)out.add(w);}return out;}
    private List<Word> chunk(List<Word> all,int index,int chunks){int a=(int)Math.floor(index*all.size()/(double)chunks),b=(int)Math.floor((index+1)*all.size()/(double)chunks);return new ArrayList<>(all.subList(Math.min(a,all.size()),Math.min(Math.max(a+1,b),all.size())));}
    private List<Word> sample(List<Word> all,int n,Random rnd){List<Word> c=new ArrayList<>(all);Collections.shuffle(c,rnd);return new ArrayList<>(c.subList(0,Math.min(n,c.size())));}
    private boolean hasUsableExample(Word w){return ExampleQuality.isUsable(w);}
    private String zh(Word w){return w.chinese==null||w.chinese.trim().isEmpty()?w.english:w.chinese;}

    private CourseQuestion intro(Word w){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.INTRO;q.word=w;q.prompt="先认识这个表达";q.display=w.word;boolean safe=hasUsableExample(w);q.support=zh(w)+(safe?"\n\n"+w.example+"\n"+(w.exampleZh==null?"":w.exampleZh):"");q.answer=w.word;q.autoPlayAudio=true;return q;}
    private CourseQuestion meaning(Word w,CourseUnit u,Random rnd){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.MEANING;q.word=w;q.prompt="选择正确意思";q.display=w.word;q.answer=zh(w);q.dimension=ProgressStore.DIM_MEANING;q.options.addAll(chineseOptions(w,u,rnd));return q;}
    private CourseQuestion listen(Word w,CourseUnit u,Random rnd){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.LISTEN;q.word=w;q.prompt="听音频，选择你听到的意大利语";q.display="🔊";q.support=zh(w);q.answer=w.word;q.dimension=ProgressStore.DIM_LISTENING;q.autoPlayAudio=true;q.options.addAll(italianOptions(w,u,rnd));return q;}
    private CourseQuestion spellHint(Word w){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.SPELL_HINT;q.word=w;q.prompt="把意大利语补完整";q.display=zh(w);q.answer=w.word;q.dimension=ProgressStore.DIM_SPELLING;q.hint=mask(w.word);return q;}
    private CourseQuestion active(Word w){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.ACTIVE;q.word=w;q.prompt="不要看选项，写出意大利语";q.display=zh(w);q.answer=w.word;q.dimension=ProgressStore.DIM_SPELLING;q.hint="想不起来也没关系，提交后会看到答案";return q;}
    private CourseQuestion cloze(Word w,CourseUnit u,Random rnd){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.CLOZE;q.word=w;q.prompt="把句子补完整";q.display=replaceIgnoreCase(w.example,w.word,"_____");q.support=w.exampleZh==null?"":w.exampleZh;q.answer=w.word;q.dimension=ProgressStore.DIM_MEANING;q.options.addAll(clozeOptions(w,u,rnd));return q;}
    private CourseQuestion exampleMeaning(Word w,CourseUnit u,Random rnd){CourseQuestion q=new CourseQuestion();q.type=CourseQuestion.EXAMPLE_MEANING;q.word=w;q.prompt="这句话是什么意思？";q.display=w.example;q.answer=w.exampleZh;q.dimension=ProgressStore.DIM_MEANING;q.options.add(q.answer);for(Word d:safeExampleDistractors(w,u,rnd,8)){String z=d.exampleZh;if(!q.options.contains(z))q.options.add(z);if(q.options.size()>=4)break;}if(q.options.size()<4){for(Word d:distractors(w,u,rnd,12)){String z=zh(d);if(!q.options.contains(z))q.options.add(z);if(q.options.size()>=4)break;}}Collections.shuffle(q.options,rnd);return q;}

    private List<String> chineseOptions(Word target,CourseUnit u,Random rnd){List<String> x=new ArrayList<>();x.add(zh(target));for(Word d:distractors(target,u,rnd,5)){String z=zh(d);if(!x.contains(z))x.add(z);if(x.size()>=4)break;}Collections.shuffle(x,rnd);return x;}
    private List<String> italianOptions(Word target,CourseUnit u,Random rnd){List<String> x=new ArrayList<>();x.add(target.word);for(Word d:distractors(target,u,rnd,5)){if(!x.contains(d.word))x.add(d.word);if(x.size()>=4)break;}Collections.shuffle(x,rnd);return x;}
    private List<String> clozeOptions(Word target,CourseUnit u,Random rnd){
        List<String> x=new ArrayList<>();x.add(target.word);
        List<Word> pool=unitWords(u);Collections.shuffle(pool,rnd);
        for(Word d:pool){if(d.id==target.id||!sameGrammarClass(target,d)||x.contains(d.word))continue;x.add(d.word);if(x.size()>=4)break;}
        if(x.size()<4)for(Word d:distractors(target,u,rnd,8)){if(!x.contains(d.word))x.add(d.word);if(x.size()>=4)break;}
        Collections.shuffle(x,rnd);return x;
    }
    private boolean sameGrammarClass(Word a,Word b){
        String ap=a.partOfSpeech==null?"":a.partOfSpeech,bp=b.partOfSpeech==null?"":b.partOfSpeech;
        if(!ap.equals(bp))return false;
        if("noun".equals(ap)){String ag=a.gender==null?"":a.gender,bg=b.gender==null?"":b.gender;if(!ag.isEmpty()&&!bg.isEmpty()&&!ag.equals(bg))return false;}
        return true;
    }
    private List<Word> safeExampleDistractors(Word target,CourseUnit u,Random rnd,int max){List<Word> pool=unitWords(u);if(pool.size()<6)pool=new ArrayList<>(repo.all());Collections.shuffle(pool,rnd);List<Word> out=new ArrayList<>();for(Word w:pool){if(w.id==target.id||!ExampleQuality.isUsable(w))continue;out.add(w);if(out.size()>=max)break;}return out;}

    private List<Word> distractors(Word target,CourseUnit u,Random rnd,int max){List<Word> pool=unitWords(u);if(pool.size()<4)pool=new ArrayList<>(repo.all().subList(0,Math.min(250,repo.size())));Collections.shuffle(pool,rnd);List<Word> out=new ArrayList<>();for(Word w:pool){if(w.id==target.id)continue;out.add(w);if(out.size()>=max)break;}return out;}

    private void addDueReview(List<CourseQuestion> out,CourseUnit unit,Random rnd,int max){List<Word> due=repo.reviewDue(progress,LocalDate.now());Set<Integer> unitIds=new LinkedHashSet<>(unit.wordIds);int n=0;for(Word w:due){if(unitIds.contains(w.id))continue;out.add(n%2==0?meaning(w,unit,rnd):listen(w,unit,rnd));if(++n>=max)break;}}
    private String mask(String s){if(s==null||s.isEmpty())return "";StringBuilder b=new StringBuilder();int letters=0;for(int i=0;i<s.length();i++){char c=s.charAt(i);if(Character.isLetter(c)){b.append(letters++%2==0?c:'_');}else b.append(c);}return b.toString();}
    private String replaceIgnoreCase(String src,String needle,String repl){int i=src.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));if(i<0)return src;return src.substring(0,i)+repl+src.substring(i+needle.length());}
}

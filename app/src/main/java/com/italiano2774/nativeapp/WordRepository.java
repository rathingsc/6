package com.italiano2774.nativeapp;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class WordRepository {
    private static WordRepository instance;
    private final List<Word> words=new ArrayList<>();
    private final List<String> topics=new ArrayList<>();
    // v2.7.8: O(1) lookup indexes. Reading/listening screens used to linearly scan
    // all 2774 words for every token, which made dashboard recommendation work expensive.
    private final Map<Integer,Word> idIndex=new HashMap<>();
    private final Map<String,Word> surfaceIndex=new HashMap<>();
    private final Map<String,Word> lemmaIndex=new HashMap<>();

    private WordRepository(Context context){
        try{
            BufferedReader br=new BufferedReader(new InputStreamReader(context.getAssets().open("words.json"),StandardCharsets.UTF_8));
            StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);
            JSONArray arr=new JSONArray(sb.toString());Set<String> topicSet=new LinkedHashSet<>();
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i);Word w=new Word();
                w.id=o.getInt("id");w.num=o.optString("num");w.path=o.optString("path");w.level=o.optString("level");
                w.word=o.optString("word");w.english=o.optString("english");w.chinese=o.optString("chinese");
                w.ipa=o.optString("ipa");w.zhPron=o.optString("zhPron");w.duoAudio=o.optString("duoAudio");w.localAudio=o.optString("localAudio");
                w.example=o.optString("example");w.exampleZh=o.optString("exampleZh");w.lemma=o.optString("lemma");w.formInfo=o.optString("formInfo");
                w.partOfSpeech=o.optString("partOfSpeech");w.gender=o.optString("gender");w.number=o.optString("number");w.article=o.optString("article");w.plural=o.optString("plural");
                w.levelOrder=o.optInt("levelOrder");w.sectionOrder=o.optInt("sectionOrder");
                words.add(w);idIndex.put(w.id,w);
                String surface=normalizeLookup(w.word);if(!surface.isEmpty()&&!surfaceIndex.containsKey(surface))surfaceIndex.put(surface,w);
                String lemma=normalizeLookup(w.lemma);if(!lemma.isEmpty()&&!lemmaIndex.containsKey(lemma))lemmaIndex.put(lemma,w);
                if(!w.level.isEmpty())topicSet.add(w.level);
            }
            topics.addAll(topicSet);
        }catch(Exception e){throw new RuntimeException("Failed to load words.json",e);}
    }
    public static synchronized WordRepository get(Context c){if(instance==null)instance=new WordRepository(c.getApplicationContext());return instance;}
    public List<Word> all(){return words;}
    public List<String> topics(){return topics;}
    public int size(){return words.size();}
    public int totalDays(LocalDate start,int perDay){return (int)Math.ceil(words.size()/(double)perDay);}
    public int dayIndex(LocalDate start,LocalDate date){return (int)ChronoUnit.DAYS.between(start,date);}
    public List<Word> forDate(LocalDate start,LocalDate date,int perDay){
        int idx=dayIndex(start,date);if(idx<0||idx>=totalDays(start,perDay))return new ArrayList<>();
        int from=idx*perDay,to=Math.min(from+perDay,words.size());return new ArrayList<>(words.subList(from,to));
    }
    public List<Word> reviewDue(ProgressStore progress,LocalDate date){
        List<Word> out=new ArrayList<>();for(Word w:words)if(progress.dueForReview(w.id,date))out.add(w);
        out.sort((a,b)->{long da=progress.dueEpochDay(a.id),db=progress.dueEpochDay(b.id);if(da!=db)return Long.compare(da,db);int wa=progress.wrongCount(a.id),wb=progress.wrongCount(b.id);if(wa!=wb)return Integer.compare(wb,wa);return Integer.compare(progress.mastery(a.id),progress.mastery(b.id));});
        return out;
    }
    public List<Word> wrongWords(ProgressStore progress){List<Word> out=new ArrayList<>();for(Word w:words)if(progress.wrongCount(w.id)>0)out.add(w);return out;}
    public List<Word> stubbornWords(ProgressStore progress){List<Word> out=new ArrayList<>();for(Word w:words)if(progress.isStubborn(w.id))out.add(w);out.sort((a,b)->{int wa=progress.wrongCount(a.id),wb=progress.wrongCount(b.id);if(wa!=wb)return Integer.compare(wb,wa);return Integer.compare(progress.mastery(a.id),progress.mastery(b.id));});return out;}
    public List<Word> quizPool(ProgressStore progress){
        List<Word> out=new ArrayList<>();for(Word w:words)if(progress.mastery(w.id)>0)out.add(w);
        if(out.size()<20){out.clear();out.addAll(words.subList(0,Math.min(200,words.size())));}
        return out;
    }


    public Word byId(int id){return id<=0?null:idIndex.get(id);}
    public Word byWord(String value){if(value==null)return null;return surfaceIndex.get(normalizeLookup(value));}
    public Word lookupSurface(String value){
        if(value==null)return null;String v=normalizeLookup(value);if(v.isEmpty())return null;
        Word direct=surfaceIndex.get(v);if(direct!=null)return direct;
        if(v.contains("'")){String tail=v.substring(v.lastIndexOf("'")+1);Word t=surfaceIndex.get(tail);if(t!=null)return t;}
        return lemmaIndex.get(v);
    }
    private static String normalizeLookup(String value){
        if(value==null)return "";
        String v=value.trim().toLowerCase(java.util.Locale.ROOT).replace('’','\'');
        return v.replaceAll("^[^\\p{L}]+|[^\\p{L}']+$","");
    }

    /** 40 conservative placement-test samples: 5 evenly spread items from each of 8 course bands. */
    public List<Word> placementSample(){
        List<Word> out=new ArrayList<>();int bands=8,perBand=5;
        for(int b=0;b<bands;b++){
            int start=(int)Math.floor(b*words.size()/(double)bands),end=(int)Math.floor((b+1)*words.size()/(double)bands);
            int span=Math.max(1,end-start);
            for(int j=0;j<perBand;j++){int idx=start+(int)Math.floor((j+0.5)*span/perBand);idx=Math.max(start,Math.min(end-1,idx));out.add(words.get(idx));}
        }
        return out;
    }

    public int placementBoundaryForPassedBands(int passedBands){int bands=8;return Math.min(words.size(),(int)Math.round(words.size()*passedBands/(double)bands));}

    /** Adaptive daily plan based on time budget. Already-known words are not re-added as new. */
    public DailyPlan adaptivePlan(ProgressStore progress){
        DailyPlan plan=new DailyPlan();plan.minutes=progress.sessionMinutes();plan.newQuota=progress.recommendedNewWords(words);
        int routeLimit=Math.min(progress.vocabularyRouteLimit(),words.size());
        int reviewPressureCap=progress.protectedReviewCap(words);
        int target=plan.minutes<=10?16:(plan.minutes<=20?28:(plan.minutes<=30?48:(plan.minutes<=45?60:72)));
        LinkedHashSet<Integer> used=new LinkedHashSet<>();Set<Integer> recent=progress.recentWordIds();

        // Stubborn words first: repeatedly failed items get a short multi-channel repair slot.
        List<Word> stubborn=stubbornWords(progress);int stubbornCap=Math.max(1,(int)Math.round(target*0.12));
        for(Word w:stubborn){if(plan.words.size()>=stubbornCap)break;if(used.add(w.id)){plan.words.add(w);plan.wrongCount++;}}

        // Wrong words next, but cap them so the session is not dominated by failures.
        List<Word> wrong=wrongWords(progress);wrong.sort((a,b)->Integer.compare(progress.wrongCount(b.id),progress.wrongCount(a.id)));
        int wrongCap=Math.max(2,(int)Math.round(target*0.20));
        for(Word w:wrong){if(plan.words.size()>=wrongCap)break;if(used.add(w.id)){plan.words.add(w);plan.wrongCount++;}}

        // Due reviews are the backbone of the session.
        List<Word> due=reviewDue(progress,LocalDate.now());int reviewCap=Math.min(reviewPressureCap,Math.max(3,(int)Math.round(target*0.48)));
        for(Word w:due){if(plan.reviewCount>=reviewCap||plan.words.size()>=target)break;if(used.add(w.id)){plan.words.add(w);plan.reviewCount++;}}

        // Weak-dimension words: prioritize the lowest of meaning/listening/spelling/speaking.
        List<Word> weak=new ArrayList<>();for(Word w:words)if(progress.mastery(w.id)>0&&progress.mastery(w.id)<5&&!progress.isGraduated(w.id))weak.add(w);
        weak.sort((a,b)->{int wa=progress.dimensionLevel(a.id,progress.weakestDimension(a.id)),wb=progress.dimensionLevel(b.id,progress.weakestDimension(b.id));if(wa!=wb)return Integer.compare(wa,wb);return Integer.compare(progress.wrongCount(b.id),progress.wrongCount(a.id));});
        int weakCap=Math.max(2,(int)Math.round(target*0.20));
        for(int pass=0;pass<2&&plan.weakCount<weakCap&&plan.words.size()<target;pass++)for(Word w:weak){if(plan.weakCount>=weakCap||plan.words.size()>=target)break;if(pass==0&&recent.contains(w.id))continue;if(pass==1&&!recent.contains(w.id))continue;if(used.add(w.id)){plan.words.add(w);plan.weakCount++;switch(progress.weakestDimension(w.id)){case ProgressStore.DIM_LISTENING:plan.listeningWeak++;break;case ProgressStore.DIM_SPELLING:plan.spellingWeak++;break;case ProgressStore.DIM_SPEAKING:plan.speakingWeak++;break;default:plan.meaningWeak++;}}}

        // Fresh words are capped dynamically. Prefer words not shown earlier today;
        // recently exposed words are only used when there are not enough alternatives.
        for(int pass=0;pass<2&&plan.newCount<plan.newQuota&&plan.words.size()<target;pass++)for(int wi=0;wi<routeLimit;wi++){Word w=words.get(wi);if(plan.words.size()>=target||plan.newCount>=plan.newQuota)break;if(pass==0&&recent.contains(w.id))continue;if(pass==1&&!recent.contains(w.id))continue;if(progress.mastery(w.id)==0&&used.add(w.id)){plan.words.add(w);plan.newCount++;}}

        // Fill remaining time with studied low-mastery words rather than forcing more new material.
        if(plan.words.size()<target){for(Word w:weak){if(plan.words.size()>=target)break;if(used.add(w.id)){plan.words.add(w);plan.weakCount++;}}}
        if(plan.words.size()<target){for(Word w:due){if(plan.words.size()>=target)break;if(used.add(w.id)){plan.words.add(w);plan.reviewCount++;}}}
        return plan;
    }

    /** v3.1 Momo-inspired queue: due reviews first, then a user-sized batch of fresh words. */
    public DailyPlan smartMemoryPlan(ProgressStore progress){
        DailyPlan plan=new DailyPlan();plan.minutes=progress.sessionMinutes();int routeLimit=Math.min(progress.vocabularyRouteLimit(),words.size());
        LinkedHashSet<Integer> used=new LinkedHashSet<>();List<Word> due=reviewDue(progress,LocalDate.now());int reviewCap=Math.min(due.size(),progress.protectedReviewCap(words));
        for(Word w:due){if(plan.reviewCount>=reviewCap)break;if(used.add(w.id)){plan.words.add(w);plan.reviewCount++;}}
        int unknown=0;for(int i=0;i<routeLimit;i++)if(progress.mastery(words.get(i).id)==0)unknown++;plan.newQuota=Math.min(progress.perDay(),unknown);Set<Integer> recent=progress.recentWordIds();
        for(int pass=0;pass<2&&plan.newCount<plan.newQuota;pass++)for(int i=0;i<routeLimit&&plan.newCount<plan.newQuota;i++){Word w=words.get(i);if(progress.mastery(w.id)>0||used.contains(w.id))continue;if(pass==0&&recent.contains(w.id))continue;if(pass==1&&!recent.contains(w.id))continue;if(used.add(w.id)){plan.words.add(w);plan.newCount++;}}
        return plan;
    }

    /** v2.5: smallest high-yield queue for days when the learner only has five minutes. */
    public List<Word> rescuePlan(ProgressStore progress){
        List<Word> candidates=new ArrayList<>();for(Word w:words)if(progress.mastery(w.id)>0&&!progress.isGraduated(w.id))candidates.add(w);
        candidates.sort((a,b)->{
            int sa=rescueRisk(a,progress),sb=rescueRisk(b,progress);if(sa!=sb)return Integer.compare(sb,sa);
            long da=progress.dueEpochDay(a.id),db=progress.dueEpochDay(b.id);return Long.compare(da,db);
        });
        List<Word> out=new ArrayList<>();for(Word w:candidates){if(out.size()>=12)break;if(rescueRisk(w,progress)>0)out.add(w);}
        if(out.size()<8){int limit=Math.min(progress.vocabularyRouteLimit(),words.size());for(int i=0;i<limit&&out.size()<8;i++){Word w=words.get(i);if(progress.mastery(w.id)>0&&!out.contains(w)&&!progress.isGraduated(w.id))out.add(w);}}
        return out;
    }
    private int rescueRisk(Word w,ProgressStore p){
        int risk=0;if(p.dueForReview(w.id,LocalDate.now())){long due=p.dueEpochDay(w.id);long late=due==Long.MIN_VALUE?1:Math.max(0,LocalDate.now().toEpochDay()-due);risk+=40+(int)Math.min(30,late*3);}
        risk+=Math.min(30,p.wrongCount(w.id)*8);if(p.isStubborn(w.id))risk+=30;risk+=Math.max(0,90-p.memoryRetrievability(w.id))/3;int weak=p.dimensionLevel(w.id,p.weakestDimension(w.id));risk+=Math.max(0,3-weak)*8;return risk;
    }
    public List<Word> routeWords(ProgressStore progress){int limit=Math.min(progress.vocabularyRouteLimit(),words.size());return new ArrayList<>(words.subList(0,limit));}

    public List<Word> weaknessPool(ProgressStore progress,int dim){List<Word> out=new ArrayList<>();for(Word w:words)if(progress.mastery(w.id)>0&&progress.dimensionLevel(w.id,dim)<4)out.add(w);out.sort((a,b)->Integer.compare(progress.dimensionLevel(a.id,dim),progress.dimensionLevel(b.id,dim)));return out;}

    private static final String[][] CURATED_CONFUSIONS={
            {"buono","bene"},{"essere","stare"},{"sapere","conoscere"},{"questo","quello"},{"qui","qua"},{"lì","là"},
            {"molto","molti"},{"qualche","alcuni"},{"perché","per"},{"da","di"},{"a","in"},{"tra","fra"},
            {"sera","notte"},{"casa","appartamento"},{"sentire","ascoltare"},{"guardare","vedere"},{"chiedere","domandare"},
            {"parlare","dire"},{"andare","venire"},{"portare","prendere"}
    };

    public List<Word> confusionPool(ProgressStore progress){
        LinkedHashSet<Integer> ids=new LinkedHashSet<>();
        for(int[] c:progress.learnedConfusions()){ids.add(c[0]);ids.add(c[1]);if(ids.size()>=30)break;}
        for(String[] pair:CURATED_CONFUSIONS){Word a=byWord(pair[0]),b=byWord(pair[1]);if(a!=null&&b!=null){ids.add(a.id);ids.add(b.id);}}
        List<Word> out=new ArrayList<>();for(Integer id:ids){Word w=byId(id);if(w!=null)out.add(w);}return out;
    }
    public Word confusionPartner(Word target,ProgressStore progress){
        if(target==null)return null;int best=0;Word bestWord=null;
        for(int[] c:progress.learnedConfusions()){if(c[0]==target.id||c[1]==target.id){int other=c[0]==target.id?c[1]:c[0];if(c[2]>best){best=c[2];bestWord=byId(other);}}}
        if(bestWord!=null)return bestWord;
        for(String[] pair:CURATED_CONFUSIONS){if(target.word.equalsIgnoreCase(pair[0]))return byWord(pair[1]);if(target.word.equalsIgnoreCase(pair[1]))return byWord(pair[0]);}
        return null;
    }

}

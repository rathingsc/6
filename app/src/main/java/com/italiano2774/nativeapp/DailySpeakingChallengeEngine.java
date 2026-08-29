package com.italiano2774.nativeapp;

import android.content.Context;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** v4.0.0: deterministic five-sentence daily active speaking set from audited core sentences. */
public class DailySpeakingChallengeEngine {
    public static final int DAILY_TARGET=5;
    private final CoreSentenceRepository repo;
    private final CourseCurriculumRepository curriculum;
    private final ProgressStore progress;

    public DailySpeakingChallengeEngine(Context context,ProgressStore progress){
        this.repo=CoreSentenceRepository.get(context);this.curriculum=CourseCurriculumRepository.get(context);this.progress=progress;
    }

    public String targetLevel(){
        CourseUnit u=curriculum.current(progress);String stage=u==null?"A1":u.stage;
        if("B1".equals(stage))return "B1";
        if("A2".equals(stage))return "A2";
        return "A1";
    }

    public int passScore(){String level=targetLevel();return "B1".equals(level)?80:("A2".equals(level)?75:70);}

    public List<CoreSentence> todayBatch(){
        LocalDate today=LocalDate.now();int completed=progress.dailyAuxiliaryAttempts("daily_speaking",today);int batch=Math.max(0,completed/DAILY_TARGET);
        String level=targetLevel();List<CoreSentence> levelPool=repo.level(level);if(levelPool.isEmpty())levelPool=repo.all();
        Map<String,List<CoreSentence>> byCategory=new LinkedHashMap<>();for(CoreSentence s:levelPool)byCategory.computeIfAbsent(s.category,k->new ArrayList<>()).add(s);
        List<String> categories=new ArrayList<>();for(Map.Entry<String,List<CoreSentence>> e:byCategory.entrySet())if(e.getValue().size()>=DAILY_TARGET)categories.add(e.getKey());
        Collections.sort(categories);if(categories.isEmpty())return fallback(levelPool,today,batch);
        long seed=today.toEpochDay()*1009L+batch*7919L+level.hashCode();String category=categories.get(Math.floorMod((int)(seed^(seed>>>32)),categories.size()));
        List<CoreSentence> pool=new ArrayList<>(byCategory.get(category));Collections.shuffle(pool,new Random(seed));
        return new ArrayList<>(pool.subList(0,Math.min(DAILY_TARGET,pool.size())));
    }

    public String todayTheme(){List<CoreSentence> xs=todayBatch();return xs.isEmpty()?"日常交流":xs.get(0).category;}

    private List<CoreSentence> fallback(List<CoreSentence> pool,LocalDate today,int batch){
        List<CoreSentence> copy=new ArrayList<>(pool);copy.sort(Comparator.comparing(x->x.id));Collections.shuffle(copy,new Random(today.toEpochDay()*1009L+batch*7919L));
        return new ArrayList<>(copy.subList(0,Math.min(DAILY_TARGET,copy.size())));
    }
}

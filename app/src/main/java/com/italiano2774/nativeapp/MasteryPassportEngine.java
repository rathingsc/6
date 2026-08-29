package com.italiano2774.nativeapp;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * v4.1.0 internal learning passport. This is deliberately an App learning
 * diagnostic, not an official CEFR assessment or certificate.
 */
public final class MasteryPassportEngine {
    public static final String ACTION_COURSE="course";
    public static final String ACTION_MEANING="meaning";
    public static final String ACTION_LISTENING="listening";
    public static final String ACTION_SPELLING="spelling";
    public static final String ACTION_SPEAKING="speaking";
    public static final String ACTION_GRAMMAR="grammar";
    public static final String ACTION_REAL_USE="real_use";
    public static final String ACTION_EXAM="exam";

    public static class Skill {
        public String key,label,evidence;
        public int score;
        Skill(String key,String label,int score,String evidence){this.key=key;this.label=label;this.score=clamp(score);this.evidence=evidence;}
    }
    public static class Checkpoint {
        public String level;
        public int completed,total,progressPct;
        public boolean passed;
        public String detail,missing;
    }
    public static class Snapshot {
        public int overall,coursePct,introduced,graduated;
        public String currentStage,nextCheckpoint,recommendation,actionKey,actionLabel,targetLevel,focusSkillKey,focusSkillLabel;
        public int focusSkillScore,focusSkillTarget;
        public final List<Skill> skills=new ArrayList<>();
        public Checkpoint a1,a2,b1;
        public Skill skill(String key){for(Skill s:skills)if(s.key.equals(key))return s;return null;}
    }

    private MasteryPassportEngine(){}

    public static Snapshot build(Context context,WordRepository repo,ProgressStore p){
        Snapshot out=new Snapshot();List<Word> words=repo.all();ProgressStore.DashboardStats ds=p.dashboardStats(words,java.time.LocalDate.now());
        CourseCurriculumRepository curriculum=CourseCurriculumRepository.get(context);
        int route=Math.max(1,Math.min(p.vocabularyRouteLimit(),words.size()));int coverage=clamp((int)Math.round(ds.routeIntroduced*100.0/route));
        int meaning=calibrateMeaning(clamp((int)Math.round(ds.learnedDimensionPct[ProgressStore.DIM_MEANING]*0.85+coverage*0.15)),coverage);
        int listening=blended(ds.learnedDimensionPct[ProgressStore.DIM_LISTENING],coverage,p,new String[]{"listen_speak","listening_course","intensive_listening"});
        int spelling=blended(ds.learnedDimensionPct[ProgressStore.DIM_SPELLING],coverage,p,new String[]{"active_recall","cloze","sentence_dictation","writing"});
        int speaking=blended(ds.learnedDimensionPct[ProgressStore.DIM_SPEAKING],coverage,p,new String[]{"daily_speaking","dialogue_speaking","freechat","shadowing","pronunciation"});
        int grammar=grammarScore(p);
        int realUse=realUseScore(p);
        out.skills.add(new Skill(ACTION_MEANING,"识义",meaning,"已接触 "+ds.routeIntroduced+" / "+route+" 个路线词"));
        out.skills.add(new Skill(ACTION_LISTENING,"听力",listening,evidenceText(p,new String[]{"listen_speak","listening_course","intensive_listening"}))); 
        out.skills.add(new Skill(ACTION_SPELLING,"拼写",spelling,evidenceText(p,new String[]{"active_recall","cloze","sentence_dictation","writing"}))); 
        out.skills.add(new Skill(ACTION_SPEAKING,"口语",speaking,"每日5句 "+p.auxiliaryAttempts("daily_speaking")+" 次 · "+evidenceText(p,new String[]{"dialogue_speaking","freechat","shadowing"}))); 
        out.skills.add(new Skill(ACTION_GRAMMAR,"语法",grammar,grammarEvidence(p)));
        out.skills.add(new Skill(ACTION_REAL_USE,"真实使用",realUse,evidenceText(p,new String[]{"life_task","reading","dialogue","dialogue_speaking","freechat","writing","memory_article_sentence","weak_story"})));
        out.overall=clamp((int)Math.round(meaning*0.20+listening*0.18+spelling*0.14+speaking*0.20+grammar*0.14+realUse*0.14));
        out.coursePct=courseOverallPct(curriculum,p);out.introduced=ds.routeIntroduced;out.graduated=ds.graduated;
        CourseUnit current=curriculum.current(p);out.currentStage=current==null?"A0 入门":CourseCurriculumRepository.stageName(current.stage);
        out.a1=checkpoint("A1",stagePct(curriculum,p,"A1"),p.bestExamScore("A1"),meaning,listening,spelling,speaking,grammar,realUse,new int[]{50,40,35,35,40,35});
        out.a2=checkpoint("A2",stagePct(curriculum,p,"A2"),p.bestExamScore("A2"),meaning,listening,spelling,speaking,grammar,realUse,new int[]{65,55,50,50,55,50});
        out.b1=checkpoint("B1",stagePct(curriculum,p,"B1"),p.bestExamScore("B1"),meaning,listening,spelling,speaking,grammar,realUse,new int[]{75,68,60,65,65,60});
        Checkpoint target=!out.a1.passed?out.a1:(!out.a2.passed?out.a2:out.b1);out.nextCheckpoint=target.level+" 内部学习检查";out.targetLevel=target.level;
        chooseFocusSkill(out,target);
        chooseRecommendation(out,target,p,curriculum);
        return out;
    }

    private static int blended(int base,int coverage,ProgressStore p,String[] types){
        int att=0,cor=0;for(String t:types){att+=p.auxiliaryAttempts(t);cor+=p.auxiliaryCorrect(t);}int raw;if(att<=0)raw=clamp((int)Math.round(base*0.88));else{int accuracy=clamp((int)Math.round(cor*100.0/att));double evidence=Math.min(1.0,att/30.0);double auxWeight=0.12+0.23*evidence;raw=clamp((int)Math.round(base*(1.0-auxWeight)+accuracy*auxWeight));}
        return Math.min(raw,wordEvidenceCap(coverage,att));
    }
    private static int calibrateMeaning(int raw,int coverage){int cap=clamp((int)Math.round(40+60*Math.sqrt(Math.max(0,coverage)/100.0)));return Math.min(raw,cap);}
    private static int wordEvidenceCap(int coverage,int auxAttempts){int cap=(int)Math.round(30+55*Math.sqrt(Math.max(0,coverage)/100.0)+Math.min(15,auxAttempts/2.0));return clamp(cap);}

    private static int grammarScore(ProgressStore p){
        int att=0,cor=0,points=0,total=GrammarDiagnostics.all().size();for(GrammarPoint g:GrammarDiagnostics.all()){int a=p.grammarAttempts(g.id);if(a>0){points++;att+=a;cor+=p.grammarCorrect(g.id);}}
        for(String t:new String[]{"pattern","past_tense","preposition","pronouns","verb_center"}){att+=p.auxiliaryAttempts(t);cor+=p.auxiliaryCorrect(t);}
        if(att<=0)return 0;int acc=clamp((int)Math.round(cor*100.0/att));int cov=clamp((int)Math.round(points*100.0/Math.max(1,total)));return clamp((int)Math.round(acc*0.82+cov*0.18));
    }
    private static int realUseScore(ProgressStore p){
        String[] types={"life_task","reading","dialogue","dialogue_speaking","freechat","writing","memory_article_sentence","weak_story"};int att=0,cor=0,channels=0;for(String t:types){int a=p.auxiliaryAttempts(t);if(a>0)channels++;att+=a;cor+=p.auxiliaryCorrect(t);}if(att<=0)return 0;int acc=clamp((int)Math.round(cor*100.0/att));int channelPct=clamp((int)Math.round(channels*100.0/types.length));double evidence=Math.min(1.0,att/35.0);return clamp((int)Math.round(acc*(0.62+0.18*evidence)+channelPct*(0.38-0.18*evidence)));
    }
    private static String evidenceText(ProgressStore p,String[] types){int att=0,cor=0,channels=0;for(String t:types){int a=p.auxiliaryAttempts(t);if(a>0)channels++;att+=a;cor+=p.auxiliaryCorrect(t);}if(att<=0)return "还没有足够练习证据";return att+" 次练习 · "+channels+" 类任务 · 正确率 "+Math.round(cor*100.0/att)+"%";}
    private static String grammarEvidence(ProgressStore p){int att=0,cor=0,points=0;for(GrammarPoint g:GrammarDiagnostics.all()){int a=p.grammarAttempts(g.id);if(a>0){points++;att+=a;cor+=p.grammarCorrect(g.id);}}return att==0?"还没有足够语法诊断记录":points+" 个语法点有记录 · "+att+" 次 · "+Math.round(cor*100.0/att)+"%";}
    private static int stagePct(CourseCurriculumRepository c,ProgressStore p,String stage){int total=0,done=0;for(CourseUnit u:c.all())if(stage.equals(u.stage)){total+=u.lessonCount;done+=c.completedLessons(u,p);}return total==0?0:clamp((int)Math.round(done*100.0/total));}
    private static int courseOverallPct(CourseCurriculumRepository c,ProgressStore p){int total=0,done=0;for(CourseUnit u:c.all()){total+=u.lessonCount;done+=c.completedLessons(u,p);}return total==0?0:clamp((int)Math.round(done*100.0/total));}
    private static Checkpoint checkpoint(String level,int course,int exam,int meaning,int listening,int spelling,int speaking,int grammar,int real,int[] target){
        Checkpoint c=new Checkpoint();c.level=level;String[] names={"识义","听力","拼写","口语","语法","真实使用"};int[] actual={meaning,listening,spelling,speaking,grammar,real};StringBuilder detail=new StringBuilder();c.total=8;
        if(course>=100)c.completed++;if(exam>=70)c.completed++;for(int i=0;i<6;i++)if(actual[i]>=target[i])c.completed++;
        c.progressPct=(int)Math.round(c.completed*100.0/c.total);c.passed=c.completed==c.total;
        detail.append("课程段 ").append(course).append("% ").append(course>=100?"✓":"△").append(" · 阶段自测 ").append(exam).append("% ").append(exam>=70?"✓":"△").append("\n");
        for(int i=0;i<6;i++){if(i>0)detail.append(" · ");detail.append(names[i]).append(" ").append(actual[i]).append("/").append(target[i]).append(actual[i]>=target[i]?"✓":"△");}
        c.detail=detail.toString();
        if(course<100)c.missing="先完成 "+level+" 课程段（当前 "+course+"%）";else if(exam<70)c.missing="做一次 "+level+" 阶段自测，目标至少70%";else{double worst=2;int wi=0;for(int i=0;i<6;i++){double ratio=actual[i]/(double)Math.max(1,target[i]);if(ratio<worst){worst=ratio;wi=i;}}c.missing=actual[wi]>=target[wi]?"本阶段检查已通过":names[wi]+"还差 "+Math.max(0,target[wi]-actual[wi])+" 分";}
        return c;
    }
    private static void chooseFocusSkill(Snapshot out,Checkpoint target){
        String[] keys={ACTION_MEANING,ACTION_LISTENING,ACTION_SPELLING,ACTION_SPEAKING,ACTION_GRAMMAR,ACTION_REAL_USE};
        int[] targets=target.level.equals("A1")?new int[]{50,40,35,35,40,35}:(target.level.equals("A2")?new int[]{65,55,50,50,55,50}:new int[]{75,68,60,65,65,60});
        double worst=9;Skill pick=null;int targetValue=0;
        for(int i=0;i<keys.length;i++){Skill s=out.skill(keys[i]);if(s==null)continue;double r=s.score/(double)Math.max(1,targets[i]);if(r<worst){worst=r;pick=s;targetValue=targets[i];}}
        if(pick==null){out.focusSkillKey=ACTION_MEANING;out.focusSkillLabel="识义";out.focusSkillScore=0;out.focusSkillTarget=targets[0];return;}
        out.focusSkillKey=pick.key;out.focusSkillLabel=pick.label;out.focusSkillScore=pick.score;out.focusSkillTarget=targetValue;
    }

    private static void chooseRecommendation(Snapshot out,Checkpoint target,ProgressStore p,CourseCurriculumRepository curriculum){
        int stage=target.level.equals("A1")?stagePct(curriculum,p,"A1"):(target.level.equals("A2")?stagePct(curriculum,p,"A2"):stagePct(curriculum,p,"B1"));
        if(stage<100){out.actionKey=ACTION_COURSE;out.actionLabel="继续当前课程";out.recommendation="先把课程主线推进，不用同时补所有弱项。当前最值钱的是完成 "+target.level+" 课程段；每日计划会同时穿插最弱的"+out.focusSkillLabel+"，不需要你手工安排。";return;}
        int exam=p.bestExamScore(target.level);if(exam<70){out.actionKey=ACTION_EXAM;out.actionLabel="做阶段自测";out.recommendation="课程段已经完成，但缺少稳定的阶段检查证据。先做一次 "+target.level+" 自测；每日计划仍会继续补最弱的"+out.focusSkillLabel+"。";return;}
        Skill pick=out.skill(out.focusSkillKey);if(pick==null){out.actionKey=ACTION_COURSE;out.actionLabel="继续课程";out.recommendation="继续按每日智能计划推进。";return;}
        out.actionKey=pick.key;out.actionLabel=actionLabel(pick.key);out.recommendation="下一步只补一个最短板："+pick.label+"。当前 "+pick.score+" 分，"+target.level+" 内部检查目标 "+out.focusSkillTarget+" 分；先集中练这一项，比平均用力更有效。";
    }
    public static String actionLabel(String key){if(ACTION_LISTENING.equals(key))return "练听力";if(ACTION_SPELLING.equals(key))return "做主动回忆";if(ACTION_SPEAKING.equals(key))return "完成每日5句";if(ACTION_GRAMMAR.equals(key))return "补微语法";if(ACTION_REAL_USE.equals(key))return "推进真实生活任务";if(ACTION_EXAM.equals(key))return "做阶段自测";if(ACTION_MEANING.equals(key))return "复习词义";return "继续当前课程";}
    public static int targetFor(String level,String key){
        int[] target="A1".equals(level)?new int[]{50,40,35,35,40,35}:("A2".equals(level)?new int[]{65,55,50,50,55,50}:new int[]{75,68,60,65,65,60});
        String[] keys={ACTION_MEANING,ACTION_LISTENING,ACTION_SPELLING,ACTION_SPEAKING,ACTION_GRAMMAR,ACTION_REAL_USE};
        for(int i=0;i<keys.length;i++)if(keys[i].equals(key))return target[i];return target[0];
    }
    private static int clamp(int v){return Math.max(0,Math.min(100,v));}
}

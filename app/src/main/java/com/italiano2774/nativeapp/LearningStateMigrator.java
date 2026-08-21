package com.italiano2774.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** v2.7 compatibility migration: Room v5 becomes the primary history store while legacy preferences remain a safe compatibility mirror. */
public final class LearningStateMigrator {
    private static final String PREF="zhongxue_migrations",KEY="room_v5_done";
    private static final String[] SKILLS={"pattern","dialogue","freechat","shadowing","pronunciation","mission","level_exam","reading","sentence_dictation","error_repair","verb_center","cloze","word_family","intensive_listening","stubborn","phrases","preposition","past_tense","pronouns","family_train","rescue","writing","sentence_fsrs","active_recall","listening_course","focus_mode","core_sentences","pron_double","pron_r","pron_gli","pron_gn","pron_hard_soft"};
    private LearningStateMigrator(){}
    public static void migrateIfNeeded(Context context,WordRepository repo,ProgressStore progress){Context app=context.getApplicationContext();SharedPreferences p=app.getSharedPreferences(PREF,Context.MODE_PRIVATE);if(p.getBoolean(KEY,false))return;mirrorAll(context,repo,progress,true);}
    public static void mirrorAll(Context context,WordRepository repo,ProgressStore progress,boolean markDone){Context app=context.getApplicationContext();new Thread(()->{try{LearningStateDao dao=LearningDatabase.get(app).learningStateDao();List<WordProgressEntity> batch=new ArrayList<>();for(Word w:repo.all()){if(progress.mastery(w.id)<=0&&!progress.favorite(w.id)&&progress.wrongCount(w.id)<=0&&progress.attempts(w.id)<=0)continue;batch.add(progress.snapshotWordState(w.id));if(batch.size()>=250){dao.upsertWords(new ArrayList<>(batch));batch.clear();}}if(!batch.isEmpty())dao.upsertWords(batch);LocalDate today=LocalDate.now();for(int i=0;i<180;i++){LocalDate d=today.minusDays(i);if(progress.dailyActivity(d)>0)dao.upsertDaily(progress.snapshotDailyState(d));}for(GrammarPoint gp:GrammarDiagnostics.all())if(progress.grammarAttempts(gp.id)>0)dao.upsertGrammar(progress.snapshotGrammarState(gp.id));for(String id:SKILLS){int a=progress.auxiliaryAttempts(id);if(a<=0)continue;SkillProgressEntity x=new SkillProgressEntity();x.skillId=id;x.attempts=a;x.correct=progress.auxiliaryCorrect(id);x.lastEpochDay=today.toEpochDay();x.updatedAt=System.currentTimeMillis();dao.upsertSkill(x);}SentenceFsrsRepository.seedPhrases(app,null);if(markDone)app.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putBoolean(KEY,true).apply();}catch(Exception ignored){}} ,"room-v5-migration").start();}
}

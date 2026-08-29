package com.italiano2774.nativeapp;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {StudyEvent.class,CustomStudyItem.class,WordProgressEntity.class,DailyStatEntity.class,ErrorRecordEntity.class,SentenceProgressEntity.class,GrammarProgressEntity.class,SkillProgressEntity.class}, version = 6, exportSchema = false)
public abstract class LearningDatabase extends RoomDatabase {
    public abstract StudyEventDao studyEventDao();
    public abstract CustomStudyItemDao customStudyItemDao();
    public abstract LearningStateDao learningStateDao();
    private static volatile LearningDatabase INSTANCE;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static final Migration MIGRATION_1_2 = new Migration(1,2){
        @Override public void migrate(SupportSQLiteDatabase db){
            db.execSQL("CREATE TABLE IF NOT EXISTS `custom_study_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kind` TEXT NOT NULL, `italian` TEXT NOT NULL, `chinese` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `intervalDays` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `correct` INTEGER NOT NULL, `stability` REAL NOT NULL, `difficulty` REAL NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_study_items_dueEpochDay` ON `custom_study_items` (`dueEpochDay`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_study_items_kind_italian` ON `custom_study_items` (`kind`, `italian`)");
        }
    };
    private static final Migration MIGRATION_2_3 = new Migration(2,3){
        @Override public void migrate(SupportSQLiteDatabase db){
            db.execSQL("CREATE TABLE IF NOT EXISTS `word_progress` (`wordId` INTEGER NOT NULL, `mastery` INTEGER NOT NULL, `meaning` INTEGER NOT NULL, `listening` INTEGER NOT NULL, `spelling` INTEGER NOT NULL, `speaking` INTEGER NOT NULL, `wrongCount` INTEGER NOT NULL, `correctStreak` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `correctAnswers` INTEGER NOT NULL, `avgResponseMs` INTEGER NOT NULL, `lastEpochDay` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `intervalDays` INTEGER NOT NULL, `stability` REAL NOT NULL, `difficulty` REAL NOT NULL, `favorite` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`wordId`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `daily_stats` (`date` TEXT NOT NULL, `cards` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `correct` INTEGER NOT NULL, `responseMs` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`date`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `error_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, `wordId` INTEGER NOT NULL, `mode` TEXT NOT NULL, `cause` TEXT NOT NULL, `expected` TEXT NOT NULL, `actual` TEXT NOT NULL, `detail` TEXT NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_error_records_createdAt` ON `error_records` (`createdAt`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_error_records_cause` ON `error_records` (`cause`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_error_records_wordId` ON `error_records` (`wordId`)");
        }
    };


    private static final Migration MIGRATION_3_4 = new Migration(3,4){
        @Override public void migrate(SupportSQLiteDatabase db){
            db.execSQL("ALTER TABLE `daily_stats` ADD COLUMN `activeSeconds` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE TABLE IF NOT EXISTS `sentence_progress` (`sentenceId` TEXT NOT NULL, `source` TEXT NOT NULL, `italian` TEXT NOT NULL, `chinese` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `correct` INTEGER NOT NULL, `lastEpochDay` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `intervalDays` INTEGER NOT NULL, `stability` REAL NOT NULL, `difficulty` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`sentenceId`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `grammar_progress` (`grammarId` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `correct` INTEGER NOT NULL, `lastEpochDay` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `intervalDays` INTEGER NOT NULL, `stability` REAL NOT NULL, `difficulty` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`grammarId`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sentence_progress_dueEpochDay` ON `sentence_progress` (`dueEpochDay`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_grammar_progress_dueEpochDay` ON `grammar_progress` (`dueEpochDay`)");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4,5){
        @Override public void migrate(SupportSQLiteDatabase db){
            db.execSQL("ALTER TABLE `sentence_progress` ADD COLUMN `meaningLevel` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `sentence_progress` ADD COLUMN `listeningLevel` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `sentence_progress` ADD COLUMN `recallLevel` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `sentence_progress` ADD COLUMN `speakingLevel` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `sentence_progress` ADD COLUMN `lapses` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `sentence_progress` ADD COLUMN `lastScore` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE TABLE IF NOT EXISTS `skill_progress` (`skillId` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `correct` INTEGER NOT NULL, `responseMs` INTEGER NOT NULL, `lastEpochDay` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`skillId`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_skill_progress_updatedAt` ON `skill_progress` (`updatedAt`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_progress_dueEpochDay` ON `word_progress` (`dueEpochDay`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_progress_mastery` ON `word_progress` (`mastery`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_stats_updatedAt` ON `daily_stats` (`updatedAt`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sentence_progress_source_dueEpochDay` ON `sentence_progress` (`source`, `dueEpochDay`)");
        }
    };


    private static final Migration MIGRATION_5_6 = new Migration(5,6){
        @Override public void migrate(SupportSQLiteDatabase db){
            db.execSQL("ALTER TABLE `error_records` ADD COLUMN `repairedAt` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `error_records` ADD COLUMN `repairAttempts` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_error_records_repairedAt` ON `error_records` (`repairedAt`)");
        }
    };

    public static LearningDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (LearningDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), LearningDatabase.class,
                            "italiano2774_learning.db").setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).addMigrations(MIGRATION_1_2,MIGRATION_2_3,MIGRATION_3_4,MIGRATION_4_5,MIGRATION_5_6).build();
                }
            }
        }
        return INSTANCE;
    }

    public static void clear(Context context) {
        final Context app = context.getApplicationContext();
        IO.execute(() -> { try { LearningDatabase db=get(app);db.studyEventDao().clear();db.learningStateDao().clearWords();db.learningStateDao().clearDaily();db.learningStateDao().clearErrors();db.learningStateDao().clearSentences();db.learningStateDao().clearGrammar();db.learningStateDao().clearSkills(); } catch (Exception ignored) {} });
    }

    public static void log(Context context, String type, String itemId, int dimension,
                           boolean correct, long responseMs, String detail) {
        final Context app = context.getApplicationContext();
        IO.execute(() -> {
            try {
                StudyEvent e = new StudyEvent();e.createdAt = System.currentTimeMillis();e.itemType = type == null ? "" : type;e.itemId = itemId == null ? "" : itemId;e.dimension = dimension;e.correct = correct;e.responseMs = responseMs;e.detail = detail == null ? "" : detail;get(app).studyEventDao().insert(e);
            } catch (Exception ignored) {}
        });
    }

    public static void mirrorWord(Context context,WordProgressEntity item){if(item==null)return;final Context app=context.getApplicationContext();IO.execute(()->{try{get(app).learningStateDao().upsertWord(item);}catch(Exception ignored){}});}
    public static void mirrorWords(Context context,List<WordProgressEntity> items){if(items==null||items.isEmpty())return;final Context app=context.getApplicationContext();IO.execute(()->{try{get(app).learningStateDao().upsertWords(items);}catch(Exception ignored){}});}
    public static void mirrorDaily(Context context,DailyStatEntity item){if(item==null)return;final Context app=context.getApplicationContext();IO.execute(()->{try{get(app).learningStateDao().upsertDaily(item);}catch(Exception ignored){}});}
    public static void logError(Context context,int wordId,String mode,String cause,String expected,String actual,String detail){final Context app=context.getApplicationContext();IO.execute(()->{try{ErrorRecordEntity e=new ErrorRecordEntity();e.createdAt=System.currentTimeMillis();e.wordId=wordId;e.mode=mode==null?"":mode;e.cause=cause==null?ErrorCause.RECALL:cause;e.expected=expected==null?"":expected;e.actual=actual==null?"":actual;e.detail=detail==null?"":detail;get(app).learningStateDao().insertError(e);}catch(Exception ignored){}});}

    public static void mirrorSkill(Context context,String skillId,boolean correct,long responseMs){
        if(skillId==null||skillId.trim().isEmpty())return;final Context app=context.getApplicationContext();IO.execute(()->{try{LearningStateDao dao=get(app).learningStateDao();SkillProgressEntity x=dao.skill(skillId);if(x==null){x=new SkillProgressEntity();x.skillId=skillId;}x.attempts++;if(correct)x.correct++;if(responseMs>0)x.responseMs+=responseMs;x.lastEpochDay=java.time.LocalDate.now().toEpochDay();x.updatedAt=System.currentTimeMillis();dao.upsertSkill(x);}catch(Exception ignored){}});
    }
}

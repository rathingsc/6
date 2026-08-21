package com.italiano2774.nativeapp;

import android.content.Context;
import android.database.Cursor;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.io.File;
import java.util.concurrent.TimeUnit;

public final class DatabaseHealthManager {
    private DatabaseHealthManager(){}
    public static class Snapshot{
        public boolean healthy;public String quickCheck="unknown";public long dbBytes,errorLogBytes;public long events;public int words,daily,errors,sentences,grammar,skills,custom,backups;public String backupLabel="";
        public String summary(){return (healthy?"✅ 数据库正常":"⚠️ 数据库需要检查")+"\nquick_check："+quickCheck+"\n数据库大小："+formatBytes(dbBytes)+"\n学习事件："+events+" · 单词状态："+words+" · 句子状态："+sentences+"\n语法："+grammar+" · 技能："+skills+" · 每日统计："+daily+"\n错误记录："+errors+" · 我的词句："+custom+"\n"+backupLabel+"\n本地错误日志："+formatBytes(errorLogBytes);}
    }
    public static Snapshot inspect(Context c){Snapshot s=new Snapshot();LearningDatabase db=LearningDatabase.get(c);try{SupportSQLiteDatabase raw=db.getOpenHelper().getReadableDatabase();try(Cursor cur=raw.query("PRAGMA quick_check")){if(cur.moveToFirst())s.quickCheck=cur.getString(0);}s.healthy="ok".equalsIgnoreCase(s.quickCheck);}catch(Exception e){s.healthy=false;s.quickCheck="检查失败";LocalErrorLog.write(c,"Database quick_check",e);}try{s.events=db.studyEventDao().count();s.words=db.learningStateDao().wordCount();s.daily=db.learningStateDao().dailyCount();s.errors=db.learningStateDao().errorCount();s.sentences=db.learningStateDao().sentenceCount();s.grammar=db.learningStateDao().grammarCount();s.skills=db.learningStateDao().skillCount();s.custom=db.customStudyItemDao().count();}catch(Exception e){LocalErrorLog.write(c,"Database counts",e);}File f=c.getDatabasePath("italiano2774_learning.db");s.dbBytes=f.exists()?f.length():0;s.backups=LocalBackupManager.count(c);s.backupLabel=LocalBackupManager.latestLabel(c);s.errorLogBytes=LocalErrorLog.size(c);return s;}
    public static int cleanOldLogs(Context c){long now=System.currentTimeMillis();int n=0;try{n+=LearningDatabase.get(c).studyEventDao().deleteBefore(now-TimeUnit.DAYS.toMillis(365));n+=LearningDatabase.get(c).learningStateDao().deleteErrorsBefore(now-TimeUnit.DAYS.toMillis(180));}catch(Exception e){LocalErrorLog.write(c,"Clean old logs",e);}return n;}
    public static String formatBytes(long b){if(b<1024)return b+" B";if(b<1024*1024)return String.format(java.util.Locale.US,"%.1f KB",b/1024.0);return String.format(java.util.Locale.US,"%.1f MB",b/(1024.0*1024.0));}
}

package com.italiano2774.nativeapp;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small private on-device error log. No network upload. */
public final class LocalErrorLog {
    private static final Object LOCK=new Object();private static final int MAX_BYTES=96*1024;private static volatile boolean installed=false;
    private LocalErrorLog(){}
    private static File file(Context c){return new File(c.getFilesDir(),"local_error_log.txt");}
    public static void install(Context context){
        if(installed)return;installed=true;final Context app=context.getApplicationContext();final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{try{write(app,"UNCAUGHT "+thread.getName(),error);}catch(Exception ignored){}if(previous!=null)previous.uncaughtException(thread,error);});
    }
    public static void write(Context c,String where,Throwable error){
        if(c==null||error==null)return;synchronized(LOCK){try{File f=file(c);if(f.exists()&&f.length()>MAX_BYTES)trim(f);StringBuilder sb=new StringBuilder();sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date())).append(" | ").append(where==null?"unknown":where).append(" | ").append(error.getClass().getSimpleName()).append(": ").append(error.getMessage()==null?"":error.getMessage()).append('\n');StackTraceElement[] st=error.getStackTrace();for(int i=0;i<Math.min(12,st.length);i++)sb.append("  at ").append(st[i]).append('\n');try(FileOutputStream out=new FileOutputStream(f,true)){out.write(sb.toString().getBytes(StandardCharsets.UTF_8));}}catch(Exception ignored){}}
    }
    public static String recent(Context c){synchronized(LOCK){try{File f=file(c);if(!f.exists())return "暂无本地错误记录";byte[] all;try(FileInputStream in=new FileInputStream(f)){all=new byte[(int)Math.min(f.length(),MAX_BYTES)];int read=in.read(all);if(read<=0)return "暂无本地错误记录";}String s=new String(all,StandardCharsets.UTF_8);return s.length()>6000?s.substring(s.length()-6000):s;}catch(Exception e){return "错误日志读取失败";}}}
    public static long size(Context c){File f=file(c);return f.exists()?f.length():0;}
    public static void clear(Context c){synchronized(LOCK){File f=file(c);if(f.exists())f.delete();}}
    private static void trim(File f)throws Exception{byte[] all;try(FileInputStream in=new FileInputStream(f)){all=new byte[(int)f.length()];int n=in.read(all);if(n<=0){f.delete();return;}}int keep=Math.min(all.length,MAX_BYTES/2);try(FileOutputStream out=new FileOutputStream(f,false)){out.write(all,all.length-keep,keep);}}
}

package com.italiano2774.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Lightweight local queue for learner-reported content problems. */
public final class IssueReportStore {
    public static final String[] CATEGORIES={"翻译错误","例句错误","音频错误","题目答案错误","其他"};
    private static final String PREF="italiano2774_issue_reports";
    private static final String KEY="reports";
    private final SharedPreferences p;

    public IssueReportStore(Context context){p=context.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public void add(Word w,String category,String mode){
        if(w==null)return;
        try{
            JSONArray a=loadArray();JSONObject o=new JSONObject();
            o.put("createdAt",System.currentTimeMillis());o.put("wordId",w.id);o.put("num",w.num==null?"":w.num);o.put("word",w.word==null?"":w.word);o.put("chinese",w.chinese==null?"":w.chinese);o.put("category",category==null?"其他":category);o.put("mode",mode==null?"":mode);o.put("example",w.example==null?"":w.example);o.put("exampleZh",w.exampleZh==null?"":w.exampleZh);
            a.put(o);while(a.length()>300){JSONArray b=new JSONArray();for(int i=1;i<a.length();i++)b.put(a.get(i));a=b;}
            p.edit().putString(KEY,a.toString()).apply();
        }catch(Exception ignored){}
    }

    public int count(){return loadArray().length();}

    public String displayText(){
        JSONArray a=loadArray();if(a.length()==0)return "目前没有待检查内容。";
        StringBuilder sb=new StringBuilder();for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i);if(o==null)continue;sb.append(a.length()-i).append(". ").append(o.optString("word")).append(" → ").append(o.optString("chinese")).append("\n   ").append(o.optString("category"));String mode=o.optString("mode");if(!mode.isEmpty())sb.append(" · ").append(mode);sb.append("\n");}
        return sb.toString().trim();
    }

    public String exportText(){
        JSONArray a=loadArray();StringBuilder sb=new StringBuilder("终学意语 待检查清单\n");for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;sb.append(i+1).append("\t").append(o.optInt("wordId")).append("\t").append(o.optString("word")).append("\t").append(o.optString("chinese")).append("\t").append(o.optString("category")).append("\t").append(o.optString("mode")).append("\n");}return sb.toString();
    }

    public void clear(){p.edit().remove(KEY).apply();}

    private JSONArray loadArray(){try{return new JSONArray(p.getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
}

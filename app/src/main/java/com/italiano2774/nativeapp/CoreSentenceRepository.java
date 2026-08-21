package com.italiano2774.nativeapp;
import android.content.Context;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class CoreSentenceRepository{
 private static CoreSentenceRepository instance;private final List<CoreSentence> all=new ArrayList<>();
 private CoreSentenceRepository(Context c){try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open("core_sentences.json"),StandardCharsets.UTF_8))){StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);JSONArray a=new JSONArray(sb.toString());for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);CoreSentence x=new CoreSentence();x.id=o.optString("id","cs"+i);x.level=o.optString("level","A1");x.tier=o.optString("tier","扩展430");x.category=o.optString("category","日常");x.italian=o.optString("it");x.chinese=o.optString("zh");x.note=o.optString("note");if(!x.italian.isEmpty())all.add(x);}}catch(Exception e){throw new RuntimeException("Failed to load core_sentences.json",e);}}
 public static synchronized CoreSentenceRepository get(Context c){if(instance==null)instance=new CoreSentenceRepository(c.getApplicationContext());return instance;}
 public List<CoreSentence> all(){return new ArrayList<>(all);}public List<CoreSentence> tier(int max){return new ArrayList<>(all.subList(0,Math.min(max,all.size())));}
 public List<CoreSentence> level(String level){List<CoreSentence> out=new ArrayList<>();for(CoreSentence s:all)if(level==null||level.isEmpty()||level.equals(s.level))out.add(s);return out;}
 public CoreSentence find(String id){for(CoreSentence s:all)if(s.id.equals(id))return s;return null;}
}

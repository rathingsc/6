package com.italiano2774.nativeapp;
import android.content.Context;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class SentenceDictationRepository{
 private final List<SentenceDictationItem> all=new ArrayList<>();
 public SentenceDictationRepository(Context c){loadScenarios(c);loadEmergency(c);}
 public List<SentenceDictationItem> all(){return new ArrayList<>(all);}
 public List<SentenceDictationItem> session(int count){List<SentenceDictationItem> x=all();Collections.shuffle(x);return new ArrayList<>(x.subList(0,Math.min(count,x.size())));}
 private String read(Context c,String file)throws Exception{BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open(file),StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);return sb.toString();}
 private void loadScenarios(Context c){try{JSONArray arr=new JSONArray(read(c,"scenarios.json"));for(int i=0;i<arr.length();i++){JSONObject s=arr.getJSONObject(i);JSONArray ps=s.getJSONArray("phrases");for(int j=0;j<ps.length();j++){JSONObject p=ps.getJSONObject(j);all.add(new SentenceDictationItem("sc_"+s.getString("id")+"_"+j,"生活场景 · "+s.optString("title"),p.optString("it"),p.optString("zh"),p.optString("note")));}}}catch(Exception ignored){}}
 private void loadEmergency(Context c){try{JSONArray arr=new JSONArray(read(c,"emergency_phrases.json"));for(int i=0;i<arr.length();i++){JSONObject s=arr.getJSONObject(i);JSONArray ps=s.getJSONArray("phrases");for(int j=0;j<ps.length();j++){JSONObject p=ps.getJSONObject(j);all.add(new SentenceDictationItem("em_"+s.getString("id")+"_"+j,"救急意语 · "+s.optString("category"),p.optString("it"),p.optString("zh"),""));}}}catch(Exception ignored){}}
}

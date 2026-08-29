package com.italiano2774.nativeapp;
import android.content.Context;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class WritingRepository{
 private static WritingRepository instance;private final List<WritingPrompt> all=new ArrayList<>();
 private WritingRepository(Context c){try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getAssets().open("writing_prompts.json"),StandardCharsets.UTF_8))){StringBuilder sb=new StringBuilder();String l;while((l=br.readLine())!=null)sb.append(l);JSONArray a=new JSONArray(sb.toString());for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);WritingPrompt p=new WritingPrompt();p.id=o.optString("id");p.level=o.optString("level","A1");p.title=o.optString("title");p.prompt=o.optString("prompt");p.reference=o.optString("reference");p.tip=o.optString("tip");p.minWords=o.optInt("minWords",8);JSONArray k=o.optJSONArray("keywords");if(k!=null)for(int j=0;j<k.length();j++)p.keywords.add(k.optString(j));all.add(p);}}catch(Exception e){throw new RuntimeException(e);}}
 public static synchronized WritingRepository get(Context c){if(instance==null)instance=new WritingRepository(c.getApplicationContext());return instance;}public List<WritingPrompt> all(){return new ArrayList<>(all);}
}

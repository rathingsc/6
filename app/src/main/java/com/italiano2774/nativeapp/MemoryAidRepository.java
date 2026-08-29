package com.italiano2774.nativeapp;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * v3.1.8 beginner memory aids: morphology recognition + common chunks.
 * All teaching text is curated/generated at build time and shipped in assets so
 * the app never invents etymology or collocations at runtime.
 */
public final class MemoryAidRepository {
    public static final class MorphologyHint {
        public String italian="",title="",note="";
        public String displayText(){return "🧩 "+title+"\n"+note;}
    }
    public static final class MemoryChunk {
        public String italian="",kind="",phrase="",chinese="",note="";
        public String displayText(){
            String head="fixed".equals(kind)?"🧱 固定搭配":"🧱 常用句块";
            return head+"\n"+phrase+(chinese.isEmpty()?"":"\n"+chinese)+(note.isEmpty()?"":"\n"+note);
        }
    }

    private static MemoryAidRepository instance;
    private final Map<String,MorphologyHint> morphology=new HashMap<>();
    private final Map<String,MemoryChunk> chunks=new HashMap<>();

    private MemoryAidRepository(Context context){
        loadMorphology(context);
        loadChunks(context);
    }

    public static synchronized MemoryAidRepository get(Context c){
        if(instance==null)instance=new MemoryAidRepository(c.getApplicationContext());
        return instance;
    }

    private void loadMorphology(Context context){
        try{
            JSONObject root=new JSONObject(readAsset(context,"morphology_hints.json"));
            JSONArray arr=root.getJSONArray("entries");
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i);MorphologyHint h=new MorphologyHint();
                h.italian=o.optString("italian");h.title=o.optString("title");h.note=o.optString("note");
                String key=normalize(h.italian);if(!key.isEmpty()&&!morphology.containsKey(key))morphology.put(key,h);
            }
        }catch(Exception e){throw new RuntimeException("Failed to load morphology_hints.json",e);}
    }

    private void loadChunks(Context context){
        try{
            JSONObject root=new JSONObject(readAsset(context,"memory_chunks.json"));
            JSONArray arr=root.getJSONArray("entries");
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i);MemoryChunk h=new MemoryChunk();
                h.italian=o.optString("italian");h.kind=o.optString("kind");h.phrase=o.optString("phrase");h.chinese=o.optString("chinese");h.note=o.optString("note");
                String key=normalize(h.italian);if(!key.isEmpty()&&!chunks.containsKey(key))chunks.put(key,h);
            }
        }catch(Exception e){throw new RuntimeException("Failed to load memory_chunks.json",e);}
    }

    public MorphologyHint morphologyFor(Word w){
        if(w==null)return null;MorphologyHint h=morphology.get(normalize(w.word));
        if(h!=null)return h;return morphology.get(normalize(w.lemma));
    }

    public MemoryChunk chunkFor(Word w){
        if(w==null)return null;MemoryChunk h=chunks.get(normalize(w.word));
        if(h!=null)return h;return chunks.get(normalize(w.lemma));
    }

    public int morphologyCount(){return morphology.size();}
    public int chunkCount(){return chunks.size();}

    private static String readAsset(Context context,String name)throws Exception{
        BufferedReader br=new BufferedReader(new InputStreamReader(context.getAssets().open(name),StandardCharsets.UTF_8));
        StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);return sb.toString();
    }

    private static String normalize(String value){
        if(value==null)return "";
        String s=Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        return s.replace('’','\'').replaceAll("[^a-z']","");
    }
}

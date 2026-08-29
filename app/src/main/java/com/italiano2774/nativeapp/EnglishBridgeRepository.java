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
 * v3.1.7: conservative English memory bridges.
 * Data is curated rather than guessed at runtime so false cognates do not silently become teaching errors.
 */
public class EnglishBridgeRepository {
    private static EnglishBridgeRepository instance;
    private final Map<String,EnglishBridgeEntry> index=new HashMap<>();

    private EnglishBridgeRepository(Context context){
        try{
            BufferedReader br=new BufferedReader(new InputStreamReader(context.getAssets().open("english_bridges.json"),StandardCharsets.UTF_8));
            StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);
            JSONObject root=new JSONObject(sb.toString());JSONArray arr=root.getJSONArray("entries");
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i);EnglishBridgeEntry e=new EnglishBridgeEntry();
                e.italian=o.optString("italian");e.english=o.optString("english");e.kind=o.optString("kind");e.note=o.optString("note");
                String key=normalize(e.italian);if(!key.isEmpty()&&!index.containsKey(key))index.put(key,e);
            }
        }catch(Exception e){throw new RuntimeException("Failed to load english_bridges.json",e);}
    }

    public static synchronized EnglishBridgeRepository get(Context c){
        if(instance==null)instance=new EnglishBridgeRepository(c.getApplicationContext());return instance;
    }

    public EnglishBridgeEntry forWord(Word w){
        if(w==null)return null;EnglishBridgeEntry e=index.get(normalize(w.word));
        if(e!=null)return e;return index.get(normalize(w.lemma));
    }

    public int size(){return index.size();}

    private static String normalize(String value){
        if(value==null)return "";
        String s=Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        return s.replaceAll("[^a-z]","");
    }
}

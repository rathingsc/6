package com.italiano2774.nativeapp;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads the ten-article / 2000-word spiral-memory route from assets. */
public class MemoryArticleRepository {
    private static MemoryArticleRepository instance;
    private final List<MemoryArticle> articles=new ArrayList<>();

    private MemoryArticleRepository(Context context){
        try(BufferedReader br=new BufferedReader(new InputStreamReader(context.getAssets().open("memory_articles.json"), StandardCharsets.UTF_8))){
            StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);
            JSONArray arr=new JSONArray(sb.toString());
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i);MemoryArticle a=new MemoryArticle();
                a.id=o.optString("id");a.title=o.optString("title");a.titleZh=o.optString("titleZh");a.subtitle=o.optString("subtitle");a.emoji=o.optString("emoji","📖");
                copyInts(o.optJSONArray("targetWordIds"),a.targetWordIds);
                JSONArray sections=o.optJSONArray("sections");
                if(sections!=null)for(int j=0;j<sections.length();j++){
                    JSONObject s=sections.getJSONObject(j);MemoryArticleSection x=new MemoryArticleSection();
                    x.id=s.optString("id");x.title=s.optString("title");x.titleZh=s.optString("titleZh");x.text=s.optString("text");x.translation=s.optString("translation");
                    copyInts(s.optJSONArray("targetWordIds"),x.targetWordIds);copyInts(s.optJSONArray("reviewWordIds"),x.reviewWordIds);
                    JSONArray cw=s.optJSONArray("clozeWords");if(cw!=null)for(int k=0;k<cw.length();k++)x.clozeWords.add(cw.optString(k));
                    JSONArray ri=s.optJSONArray("reinforcementItems");if(ri!=null)for(int k=0;k<ri.length();k++){
                        JSONObject r=ri.optJSONObject(k);if(r==null)continue;MemoryArticleReinforcement item=new MemoryArticleReinforcement();
                        item.wordId=r.optInt("wordId");item.sentence=r.optString("sentence");item.translation=r.optString("translation");x.reinforcementItems.add(item);
                    }
                    JSONArray sentencePairs=s.optJSONArray("sentences");if(sentencePairs!=null)for(int k=0;k<sentencePairs.length();k++){
                        JSONObject pair=sentencePairs.optJSONObject(k);if(pair==null)continue;MemoryArticleSentence sentence=new MemoryArticleSentence();
                        sentence.italian=pair.optString("italian");sentence.chinese=pair.optString("chinese");if(!sentence.italian.isEmpty()&&!sentence.chinese.isEmpty())x.sentences.add(sentence);
                    }
                    a.sections.add(x);
                }
                articles.add(a);
            }
        }catch(Exception e){throw new RuntimeException("Failed to load memory_articles.json",e);}
    }
    private static void copyInts(JSONArray arr,List<Integer> out){if(arr!=null)for(int i=0;i<arr.length();i++)out.add(arr.optInt(i));}
    public static synchronized MemoryArticleRepository get(Context c){if(instance==null)instance=new MemoryArticleRepository(c.getApplicationContext());return instance;}
    public List<MemoryArticle> all(){return articles;}
    public MemoryArticle byId(String id){if(id==null)return null;for(MemoryArticle a:articles)if(id.equals(a.id))return a;return null;}
    public MemoryArticleSection section(String articleId,int sectionIndex){MemoryArticle a=byId(articleId);return a==null||sectionIndex<0||sectionIndex>=a.sections.size()?null:a.sections.get(sectionIndex);}
    public int totalTargetWords(){int n=0;for(MemoryArticle a:articles)n+=a.targetWordIds.size();return n;}
    public int totalSections(){int n=0;for(MemoryArticle a:articles)n+=a.sections.size();return n;}
}

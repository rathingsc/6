package com.italiano2774.nativeapp;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads the fixed A0 -> A1 -> A2 -> B1 path. The order is derived from the 2774-word source course. */
public class CourseCurriculumRepository {
    private static CourseCurriculumRepository instance;
    private final List<CourseUnit> units=new ArrayList<>();
    private final Map<String,CourseUnit> byId=new HashMap<>();

    private CourseCurriculumRepository(Context context){
        try{
            BufferedReader br=new BufferedReader(new InputStreamReader(context.getAssets().open("course_curriculum.json"),StandardCharsets.UTF_8));
            StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);
            JSONObject root=new JSONObject(sb.toString());JSONArray arr=root.getJSONArray("units");
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i);CourseUnit u=new CourseUnit();
                u.id=o.getString("id");u.index=o.getInt("index");u.stage=o.getString("stage");u.stageUnit=o.getInt("stageUnit");u.titleZh=o.getString("titleZh");u.titleIt=o.optString("titleIt");u.subtitle=o.optString("subtitle");u.lessonCount=o.getInt("lessonCount");
                JSONArray ids=o.getJSONArray("wordIds");for(int j=0;j<ids.length();j++)u.wordIds.add(ids.getInt(j));
                JSONArray skills=o.optJSONArray("sourceSkills");if(skills!=null)for(int j=0;j<skills.length();j++)u.sourceSkills.add(skills.getString(j));
                units.add(u);byId.put(u.id,u);
            }
        }catch(Exception e){throw new RuntimeException("Failed to load course_curriculum.json",e);}
    }
    public static synchronized CourseCurriculumRepository get(Context c){if(instance==null)instance=new CourseCurriculumRepository(c.getApplicationContext());return instance;}
    public List<CourseUnit> all(){return Collections.unmodifiableList(units);}
    public CourseUnit byId(String id){return id==null?null:byId.get(id);}
    public CourseUnit byIndex(int index){return index<0||index>=units.size()?null:units.get(index);}
    public int size(){return units.size();}

    public CourseUnit current(ProgressStore p){return byIndex(Math.min(Math.max(0,p.courseUnlockedUnitIndex()),Math.max(0,units.size()-1)));}
    public boolean isUnlocked(CourseUnit u,ProgressStore p){return u!=null&&u.index<=p.courseUnlockedUnitIndex();}
    public boolean isComplete(CourseUnit u,ProgressStore p){return u!=null&&(u.index<p.courseUnlockedUnitIndex()||p.courseCompletedLessons(u.id)>=u.lessonCount);}
    public int completedLessons(CourseUnit u,ProgressStore p){if(u==null)return 0;if(u.index<p.courseUnlockedUnitIndex())return u.lessonCount;return Math.min(u.lessonCount,p.courseCompletedLessons(u.id));}
    public int firstIncompleteLesson(CourseUnit u,ProgressStore p){int done=completedLessons(u,p);return done>=u.lessonCount?Math.max(0,u.lessonCount-1):done;}

    /** One-time v2 -> v3 bridge: approximate current path position from already introduced words without deleting history. */
    public void migrateLegacyPositionIfNeeded(ProgressStore p,WordRepository words){
        if(p.courseInitialized())return;
        p.initializeCourseProgress(unitIndexForKnownWords(p.routeIntroducedCount(words.all())));
    }

    /** Placement tests may move a learner forward, but never erase or move existing course progress backward. */
    public void advanceFromPlacement(ProgressStore p,int knownWords){p.advanceCourseUnlockedUnit(unitIndexForKnownWords(knownWords));}
    private int unitIndexForKnownWords(int knownWords){
        if(units.isEmpty()||knownWords<=0)return 0;int cumulative=0;
        for(CourseUnit u:units){cumulative+=u.wordIds.size();if(knownWords<cumulative)return u.index;if(knownWords==cumulative)return Math.min(units.size()-1,u.index+1);}
        return units.size()-1;
    }

    public static String stageName(String stage){
        if("A0".equals(stage))return "A0 入门";
        if("A1".equals(stage))return "A1 基础";
        if("A2".equals(stage))return "A2 日常交流";
        return "B1 独立交流";
    }
}

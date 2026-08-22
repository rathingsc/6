package com.italiano2774.nativeapp;

import android.Manifest;
import android.content.Intent;
import android.content.Context;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {
    private static final int EXPORT=401,IMPORT=402,NOTIFY=403;

    @Override protected void attachBaseContext(Context newBase){
        android.content.SharedPreferences sp=newBase.getSharedPreferences("italiano2774_native",Context.MODE_PRIVATE);int mode=Math.max(0,Math.min(3,sp.getInt("font_scale_mode",1)));float scale=mode==0?0.90f:(mode==2?1.15f:(mode==3?1.30f:1.00f));Configuration cfg=new Configuration(newBase.getResources().getConfiguration());cfg.fontScale=scale;super.attachBaseContext(newBase.createConfigurationContext(cfg));
    }
    private ProgressStore progress;private WordRepository repo;private String pending;private StudyTimeTracker studyTimeTracker;
    private int lastSystemBottomInset=0;private boolean focusUi=false;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);LocalErrorLog.install(this);setContentView(R.layout.activity_main);
        progress=new ProgressStore(this);repo=WordRepository.get(this);studyTimeTracker=new StudyTimeTracker(this);ReminderScheduler.createChannel(this);LocalBackupManager.ensureVersionBackupThen(this,repo,progress,"3.1.3-preupgrade",()->LearningStateMigrator.migrateIfNeeded(this,repo,progress));if(progress.onboardingCompleted()||progress.hasLearningHistory())CourseCurriculumRepository.get(this).migrateLegacyPositionIfNeeded(progress,repo);new Thread(()->{try{progress.repairCorruptState(repo.all());}catch(Exception e){LocalErrorLog.write(this,"v3.0.1 data self-repair",e);}},"state-repair").start();

        View root=findViewById(R.id.root_main);
        BottomNavigationView nav=findViewById(R.id.bottom_navigation);
        final int navLeft=nav.getPaddingLeft(),navTop=nav.getPaddingTop(),navRight=nav.getPaddingRight(),navBottom=nav.getPaddingBottom();
        final int baseNavHeight=(int)(64*getResources().getDisplayMetrics().density+0.5f);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            lastSystemBottomInset=bars.bottom;
            // 普通页面的底部安全区由 BottomNavigationView 承担；专注学习页隐藏底栏后，
            // 则把系统导航栏高度留给 fragment_container，防止“检查/继续”按钮被三键导航覆盖。
            view.setPadding(bars.left,bars.top,bars.right,0);
            ViewGroup.LayoutParams lp=nav.getLayoutParams();
            lp.height=baseNavHeight+bars.bottom;
            nav.setLayoutParams(lp);
            nav.setPadding(navLeft,navTop,navRight,navBottom+bars.bottom);
            applyFocusBottomInset();
            return insets;
        });

        nav.setOnItemSelectedListener(item->{
            int id=item.getItemId();Fragment current=getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if(id==R.id.nav_today){if(!(current instanceof CourseHomeFragment))show(CourseHomeFragment.newInstance());return true;}
            if(id==R.id.nav_calendar){if(!(current instanceof CourseMapFragment))show(new CourseMapFragment());return true;}
            if(id==R.id.nav_practice){if(!(current instanceof PracticeHubFragment))show(new PracticeHubFragment());return true;}
            if(id==R.id.nav_settings){if(!(current instanceof ProfileFragment))show(new ProfileFragment());return true;}
            return false;
        });
        if(s==null){if(!progress.onboardingCompleted()&&!progress.hasLearningHistory()){setFocusUi(true);show(new OnboardingFragment());}else{progress.markOnboardingCompleted();nav.setSelectedItemId(R.id.nav_today);}}
    }

    private void show(Fragment f){try{getSupportFragmentManager().beginTransaction().setReorderingAllowed(true).replace(R.id.fragment_container,f).commit();}catch(Exception e){LocalErrorLog.write(this,"Fragment navigation "+f.getClass().getSimpleName(),e);Toast.makeText(this,"页面暂时无法打开，请重试",Toast.LENGTH_SHORT).show();}}
    public void openToday(LocalDate d){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_today).setChecked(true);show(CourseHomeFragment.newInstance());}
    public void openCourseUnit(String unitId){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_today).setChecked(true);show(CourseHomeFragment.newInstance(unitId));}
    public void openCourseLesson(String unitId,int lesson){show(CourseLessonFragment.newInstance(unitId,lesson));}
    public void openCourseMap(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_calendar).setChecked(true);show(new CourseMapFragment());}
    public void openProfile(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_settings).setChecked(true);show(new ProfileFragment());}
    public void openVocabulary(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_settings).setChecked(true);show(new VocabularyFragment());}
    public void openStudy(LocalDate d){show(StudySessionFragment.newInstance(d));}
    public void openAdaptiveStudy(){show(StudySessionFragment.newAdaptiveInstance());}
    public void openAdaptiveStudy(int maxCards){show(StudySessionFragment.newAdaptiveInstance(maxCards));}
    public void openSmartMemory(){show(StudySessionFragment.newSmartMemoryInstance());}
    public void openPlacementTest(){show(new PlacementTestFragment());}
    public void openPractice(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new PracticeHubFragment());}
    public void openScenarios(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new ScenarioFragment());}
    public void openScenario(String id){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(ScenarioDetailFragment.newInstance(id));}
    public void openMission(String id){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(MissionFragment.newInstance(id));}
    public void openSentencePatterns(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new SentencePatternFragment());}
    public void openSentencePatterns(String patternId){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(SentencePatternFragment.newInstance(patternId));}
    public void openGrammarDiagnosis(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new GrammarDiagnosisFragment());}
    public void openFreeConversation(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new FreeConversationFragment());}
    public void openFreeConversation(String scenarioId){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(FreeConversationFragment.newInstance(scenarioId));}
    public void openPersonalCourse(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_today).setChecked(true);show(new PersonalCourseFragment());}
    public void openDialogueTraining(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new DialogueTrainingFragment());}
    public void openDialogueTraining(String id){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(DialogueTrainingFragment.newInstance(id));}
    public void openCommute(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new CommuteFragment());}
    public void openShadowing(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new ShadowingFragment());}
    public void openPronunciation(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new PronunciationFragment());}
    public void openLevelExam(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new LevelExamFragment());}
    public void openReadingList(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new ReadingFragment());}
    public void openReading(String id){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(ReadingDetailFragment.newInstance(id));}
    public void openWeaknessCenter(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new WeaknessCenterFragment());}
    public void openEmergency(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new EmergencyFragment());}
    public void openCustomLibrary(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_settings).setChecked(true);show(new CustomLibraryFragment());}
    public void openPracticeMode(String mode){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(PracticeFragment.newInstance(mode));}
    public void openSentenceDictation(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new SentenceDictationFragment());}
    public void openWordFamilies(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new WordFamilyFragment());}
    public void openVerbCenter(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new VerbCenterFragment());}
    public void openGrammarMap(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new GrammarMapFragment());}
    public void openSmartCloze(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new SmartClozeFragment());}
    public void openIntensiveListening(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new IntensiveListeningFragment());}
    public void openStubbornWords(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new StubbornWordsFragment());}
    public void openPhrases(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new PhraseFragment());}
    public void openPrepositions(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new PrepositionFragment());}
    public void openPastTense(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new PastTenseFragment());}
    public void openPronouns(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new PronounFragment());}
    public void openFamilyTraining(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new WordFamilyTrainingFragment());}
    public void openWeeklyReport(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_settings).setChecked(true);show(new WeeklyReportFragment());}
    public void openRescueMode(){show(StudySessionFragment.newRescueInstance());}
    public void openReviewStudy(){show(StudySessionFragment.newReviewInstance());}
    public void openDailySummary(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_settings).setChecked(true);show(new DailySummaryFragment());}
    public void openWrongWordRepair(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new WrongWordRepairFragment());}
    public void openDatabaseHealth(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_settings).setChecked(true);show(new DatabaseHealthFragment());}
    public void openWriting(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new WritingFragment());}
    public void openSentenceReview(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new SentenceReviewFragment());}
    public void openPronunciationMap(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new PronunciationMapFragment());}
    public void openListeningCourse(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new ListeningCourseFragment());}
    public void openActiveRecall(int max){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(ActiveRecallFragment.newInstance(max));}
    public void openCoreSentences(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_practice).setChecked(true);show(new CoreSentenceFragment());}
    public void openFocusMode(){show(new FocusModeFragment());}
    public void openSettings(){BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.getMenu().findItem(R.id.nav_settings).setChecked(true);show(new SettingsFragment());}
    public void setFocusUi(boolean focus){focusUi=focus;View nav=findViewById(R.id.bottom_navigation);if(nav!=null)nav.setVisibility(focus?View.GONE:View.VISIBLE);applyFocusBottomInset();}
    private void applyFocusBottomInset(){View container=findViewById(R.id.fragment_container);if(container!=null)container.setPadding(container.getPaddingLeft(),container.getPaddingTop(),container.getPaddingRight(),focusUi?lastSystemBottomInset:0);}
    public void finishOnboarding(boolean openPlacement){progress.markOnboardingCompleted();setFocusUi(false);if(openPlacement)openPlacementTest();else{BottomNavigationView nav=findViewById(R.id.bottom_navigation);nav.setSelectedItemId(R.id.nav_today);}}


    public void createLocalBackup(){
        Toast.makeText(this,"正在创建本地备份…",Toast.LENGTH_SHORT).show();
        new Thread(()->{try{LocalBackupManager.create(this,repo,progress);runOnUiThread(()->Toast.makeText(this,"本地备份已创建",Toast.LENGTH_SHORT).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"本地备份失败："+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();
    }
    public void restoreLatestLocalBackup(){
        java.io.File f=LocalBackupManager.latest(this);if(f==null){Toast.makeText(this,"没有可恢复的本地备份",Toast.LENGTH_SHORT).show();return;}
        Toast.makeText(this,"正在恢复最近备份…",Toast.LENGTH_SHORT).show();new Thread(()->{try{JSONObject root=LocalBackupManager.read(f);progress.importJson(root);LearningDatabase db=LearningDatabase.get(this);JSONArray custom=root.optJSONArray("customItems");if(custom!=null){db.customStudyItemDao().clear();for(int i=0;i<custom.length();i++){JSONObject o=custom.getJSONObject(i);CustomStudyItem x=new CustomStudyItem();x.kind=o.optString("kind","word");x.italian=o.optString("italian");x.chinese=o.optString("chinese");x.note=o.optString("note");x.createdAt=o.optLong("createdAt",System.currentTimeMillis());x.dueEpochDay=o.optLong("dueEpochDay",LocalDate.now().toEpochDay());x.intervalDays=o.optInt("intervalDays",0);x.attempts=o.optInt("attempts",0);x.correct=o.optInt("correct",0);x.stability=o.optDouble("stability",1.0);x.difficulty=o.optDouble("difficulty",5.0);db.customStudyItemDao().insert(x);}}JSONArray errors=root.optJSONArray("errorHistory");if(errors!=null){db.learningStateDao().clearErrors();for(int i=0;i<errors.length();i++){JSONObject o=errors.getJSONObject(i);ErrorRecordEntity er=new ErrorRecordEntity();er.createdAt=o.optLong("createdAt",System.currentTimeMillis());er.wordId=o.optInt("wordId",0);er.mode=o.optString("mode");er.cause=o.optString("cause",ErrorCause.RECALL);er.expected=o.optString("expected");er.actual=o.optString("actual");er.detail=o.optString("detail");db.learningStateDao().insertError(er);}}LocalBackupManager.restoreRoomExtras(this,root);LearningStateMigrator.mirrorAll(this,repo,progress,false);runOnUiThread(()->{Toast.makeText(this,"已恢复最近本地备份",Toast.LENGTH_SHORT).show();show(new ProfileFragment());});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"恢复失败："+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();
    }

    public void requestNotificationPermissionIfNeeded(){
        if(Build.VERSION.SDK_INT>=33&&ActivityCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY);
    }

    @Override protected void onResume(){super.onResume();if(studyTimeTracker!=null)studyTimeTracker.resume();}
    @Override protected void onPause(){if(studyTimeTracker!=null)studyTimeTracker.pause();super.onPause();}
    @Override protected void onStop(){super.onStop();if(repo!=null&&progress!=null)LocalBackupManager.maybeBackup(this,repo,progress);}

    public void exportProgress(){
        Toast.makeText(this,"正在准备备份…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            try{
                JSONObject root=progress.exportJson(repo.all());JSONArray custom=new JSONArray();
                for(CustomStudyItem x:LearningDatabase.get(this).customStudyItemDao().all()){JSONObject o=new JSONObject();o.put("id",x.id);o.put("kind",x.kind);o.put("italian",x.italian);o.put("chinese",x.chinese);o.put("note",x.note);o.put("createdAt",x.createdAt);o.put("dueEpochDay",x.dueEpochDay);o.put("intervalDays",x.intervalDays);o.put("attempts",x.attempts);o.put("correct",x.correct);o.put("stability",x.stability);o.put("difficulty",x.difficulty);custom.put(o);}
                root.put("customItems",custom);JSONArray errors=new JSONArray();for(ErrorRecordEntity er:LearningDatabase.get(this).learningStateDao().recentErrors(1000)){JSONObject o=new JSONObject();o.put("createdAt",er.createdAt);o.put("wordId",er.wordId);o.put("mode",er.mode);o.put("cause",er.cause);o.put("expected",er.expected);o.put("actual",er.actual);o.put("detail",er.detail);errors.put(o);}root.put("errorHistory",errors);LocalBackupManager.appendAdvancedProgress(this,root);String json=root.toString(2);
                runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"终学意语_backup_v3_1.json");pending=json;startActivityForResult(i,EXPORT);});
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"导出失败："+e.getMessage(),Toast.LENGTH_LONG).show());}
        }).start();
    }

    public void importProgress(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,IMPORT);
    }

    @Override protected void onActivityResult(int req,int result,@Nullable Intent data){
        super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        try{
            if(req==EXPORT&&pending!=null){
                try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(pending.getBytes(StandardCharsets.UTF_8));}
                pending=null;Toast.makeText(this,"进度已导出",Toast.LENGTH_SHORT).show();
            }else if(req==IMPORT){
                StringBuilder sb=new StringBuilder();
                try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))){
                    String line;while((line=br.readLine())!=null)sb.append(line);
                }
                JSONObject root=new JSONObject(sb.toString());progress.importJson(root);
                JSONArray custom=root.optJSONArray("customItems");JSONArray errors=root.optJSONArray("errorHistory");
                new Thread(()->{try{LearningDatabase db=LearningDatabase.get(this);CustomStudyItemDao dao=db.customStudyItemDao();if(custom!=null){dao.clear();for(int i=0;i<custom.length();i++){JSONObject o=custom.getJSONObject(i);CustomStudyItem x=new CustomStudyItem();x.kind=o.optString("kind","word");x.italian=o.optString("italian");x.chinese=o.optString("chinese");x.note=o.optString("note");x.createdAt=o.optLong("createdAt",System.currentTimeMillis());x.dueEpochDay=o.optLong("dueEpochDay",LocalDate.now().toEpochDay());x.intervalDays=o.optInt("intervalDays",0);x.attempts=o.optInt("attempts",0);x.correct=o.optInt("correct",0);x.stability=o.optDouble("stability",1.0);x.difficulty=o.optDouble("difficulty",5.0);dao.insert(x);}}if(errors!=null){db.learningStateDao().clearErrors();for(int i=0;i<errors.length();i++){JSONObject o=errors.getJSONObject(i);ErrorRecordEntity er=new ErrorRecordEntity();er.createdAt=o.optLong("createdAt",System.currentTimeMillis());er.wordId=o.optInt("wordId",0);er.mode=o.optString("mode");er.cause=o.optString("cause",ErrorCause.RECALL);er.expected=o.optString("expected");er.actual=o.optString("actual");er.detail=o.optString("detail");db.learningStateDao().insertError(er);}}LocalBackupManager.restoreRoomExtras(this,root);LearningStateMigrator.mirrorAll(this,repo,progress,false);}catch(Exception ignored){}}).start();
                if(progress.reminderEnabled())ReminderScheduler.schedule(this,progress.reminderHour(),progress.reminderMinute());
                Toast.makeText(this,"进度和个人词句库已导入",Toast.LENGTH_SHORT).show();show(new SettingsFragment());
            }
        }catch(Exception e){Toast.makeText(this,"文件处理失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
}

package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

/** v3.0 primary screen: one visible path, one obvious next action. */
public class CourseHomeFragment extends Fragment {
    private static final String ARG_UNIT="unit";
    private ProgressStore progress;private WordRepository words;private CourseCurriculumRepository curriculum;private CourseUnit unit;
    private LinearLayout path;private TextView stage,title,subtitle,daily,streak,xp,nextUnit,smartSummary;private ProgressBar unitProgress;private MaterialButton continueButton,smartButton;

    public static CourseHomeFragment newInstance(){return new CourseHomeFragment();}
    public static CourseHomeFragment newInstance(String unitId){CourseHomeFragment f=new CourseHomeFragment();Bundle b=new Bundle();b.putString(ARG_UNIT,unitId);f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_course_home,container,false);progress=new ProgressStore(requireContext());words=WordRepository.get(requireContext());curriculum=CourseCurriculumRepository.get(requireContext());curriculum.migrateLegacyPositionIfNeeded(progress,words);
        stage=v.findViewById(R.id.text_course_stage);title=v.findViewById(R.id.text_course_unit_title);subtitle=v.findViewById(R.id.text_course_unit_subtitle);daily=v.findViewById(R.id.text_course_daily);streak=v.findViewById(R.id.text_course_streak);xp=v.findViewById(R.id.text_course_xp);nextUnit=v.findViewById(R.id.text_course_next_unit);unitProgress=v.findViewById(R.id.progress_course_unit);path=v.findViewById(R.id.container_course_path);continueButton=v.findViewById(R.id.button_course_continue);smartSummary=v.findViewById(R.id.text_smart_memory_summary);smartButton=v.findViewById(R.id.button_smart_memory);smartButton.setOnClickListener(x->((MainActivity)requireActivity()).openSmartMemory());
        String requested=getArguments()==null?null:getArguments().getString(ARG_UNIT);CourseUnit candidate=curriculum.byId(requested);unit=candidate!=null&&curriculum.isUnlocked(candidate,progress)?candidate:curriculum.current(progress);render();return v;
    }

    @Override public void onResume(){super.onResume();if(progress!=null&&unit!=null){CourseUnit current=curriculum.current(progress);String requested=getArguments()==null?null:getArguments().getString(ARG_UNIT);if(requested==null&&current!=null)unit=current;render();}}

    private void render(){if(unit==null)return;int done=curriculum.completedLessons(unit,progress);boolean complete=curriculum.isComplete(unit,progress);stage.setText(CourseCurriculumRepository.stageName(unit.stage)+" · 第"+unit.stageUnit+"单元");title.setText(unit.titleZh);subtitle.setText(unit.subtitle+"\n不用选择学习模式，沿着圆点往下学就可以。");unitProgress.setProgress((int)Math.round(done*100.0/Math.max(1,unit.lessonCount)));long activeMin=Math.round(progress.dailyActiveSeconds(java.time.LocalDate.now())/60.0);daily.setText("今天目标 "+progress.sessionMinutes()+" 分钟 · 已学习 "+activeMin+" 分钟 · 自动混入需要复习的旧内容");streak.setText("🔥 "+progress.activityStreak()+"天");xp.setText("⭐ "+progress.courseXp());renderSmartMemory();renderPath(done,complete);
        CourseUnit next=curriculum.byIndex(unit.index+1);nextUnit.setText(next==null?"你已经走到完整课程的最后一站":"下一单元："+next.titleZh);
        int lesson=curriculum.firstIncompleteLesson(unit,progress);if(complete){CourseUnit c=curriculum.current(progress);boolean hasNext=c!=null&&c.index!=unit.index;continueButton.setText(hasNext?"继续下一单元":"复习这个单元");continueButton.setOnClickListener(x->{if(hasNext)((MainActivity)requireActivity()).openCourseUnit(c.id);else ((MainActivity)requireActivity()).openCourseLesson(unit.id,Math.max(0,unit.lessonCount-1));});}
        else{continueButton.setText(done>0?"继续学习 · 第"+(lesson+1)+"关":"开始第1关");continueButton.setOnClickListener(x->((MainActivity)requireActivity()).openCourseLesson(unit.id,lesson));}
    }


    private void renderSmartMemory(){int due=0,unknown=0,mastered=0,route=Math.min(progress.vocabularyRouteLimit(),words.size());java.time.LocalDate today=java.time.LocalDate.now();java.util.List<Word> all=words.all();for(int i=0;i<all.size();i++){Word w=all.get(i);int m=progress.mastery(w.id);if(m>0&&progress.dueForReview(w.id,today))due++;if(m>=4)mastered++;if(i<route&&m==0)unknown++;}int fresh=Math.min(progress.perDay(),unknown);smartSummary.setText("今日复习 "+due+" · 新词 "+fresh+" · 已掌握 "+mastered+"\n忘记的词会在本轮后面自动再出现，不用自己排复习表。");smartButton.setEnabled(due+fresh>0);smartButton.setText(due+fresh>0?"开始智能背词":"今日已完成");}

    private void renderPath(int done,boolean complete){path.removeAllViews();CourseLessonEngine labels=new CourseLessonEngine(words,progress);int firstIncomplete=Math.min(done,unit.lessonCount-1);for(int i=0;i<unit.lessonCount;i++){boolean migrated=unit.index<progress.courseUnlockedUnitIndex();boolean finished=migrated||progress.courseLessonDone(unit.id,i);boolean unlocked=finished||complete||i<=firstIncomplete;LinearLayout row=new LinearLayout(requireContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rp.setMargins(dp(10),dp(5),dp(10),dp(5));path.addView(row,rp);
            Space left=new Space(requireContext()),right=new Space(requireContext());int offset=i%3==0?2:(i%3==1?3:1);row.addView(left,new LinearLayout.LayoutParams(0,dp(1),offset));LinearLayout nodeBox=new LinearLayout(requireContext());nodeBox.setOrientation(LinearLayout.VERTICAL);nodeBox.setGravity(Gravity.CENTER);row.addView(nodeBox,new LinearLayout.LayoutParams(dp(128),ViewGroup.LayoutParams.WRAP_CONTENT));row.addView(right,new LinearLayout.LayoutParams(0,dp(1),4-offset));
            MaterialButton b=new MaterialButton(requireContext());b.setAllCaps(false);b.setMinWidth(0);b.setCornerRadius(dp(34));b.setText(finished?"✓":(unlocked?labels.lessonEmoji(unit,i):"🔒"));b.setTextSize(20);b.setTextColor(ContextCompat.getColor(requireContext(),android.R.color.white));int bg=finished?R.color.green:(unlocked?R.color.blue:R.color.level0);b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),bg)));b.setEnabled(unlocked);final int lesson=i;b.setOnClickListener(x->((MainActivity)requireActivity()).openCourseLesson(unit.id,lesson));nodeBox.addView(b,new LinearLayout.LayoutParams(dp(68),dp(68)));
            TextView label=new TextView(requireContext());label.setText(labels.lessonTitle(unit,i));label.setGravity(Gravity.CENTER);label.setTextSize(11);label.setTextColor(ContextCompat.getColor(requireContext(),unlocked?R.color.text_primary:R.color.text_secondary));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(128),ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=dp(4);nodeBox.addView(label,lp);
            if(i<unit.lessonCount-1){TextView line=new TextView(requireContext());line.setText("│");line.setGravity(Gravity.CENTER);line.setTextColor(ContextCompat.getColor(requireContext(),R.color.line));path.addView(line,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(18)));}
        }}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}

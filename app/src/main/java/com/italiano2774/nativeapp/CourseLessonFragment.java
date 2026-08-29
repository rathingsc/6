package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One short guided lesson. It intentionally exposes only one task at a time. */
public class CourseLessonFragment extends Fragment {
    private static final String ARG_UNIT="unit",ARG_LESSON="lesson";
    private ProgressStore progress;private WordRepository repo;private CourseCurriculumRepository curriculum;private CourseUnit unit;private CourseLessonEngine engine;private AudioPlayer audio;
    private List<CourseQuestion> questions=new ArrayList<>();private int lesson,index,correct,wrong;private boolean checked=false,finished=false,teachingMeaning=false;private int meaningPreviewPassedIndex=-1;private String selectedOption="";private long shownAt;
    private ProgressBar bar;private TextView xp,header,prompt,display,support,hint,feedback,finishTitle,finishSummary;private LinearLayout choices,finishPanel;private TextInputLayout inputLayout;private TextInputEditText input;private MaterialCardView feedbackCard;private MaterialButton audioButton,checkButton;

    public static CourseLessonFragment newInstance(String unitId,int lesson){CourseLessonFragment f=new CourseLessonFragment();Bundle b=new Bundle();b.putString(ARG_UNIT,unitId);b.putInt(ARG_LESSON,lesson);f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_course_lesson,container,false);((MainActivity)requireActivity()).setFocusUi(true);progress=new ProgressStore(requireContext());repo=WordRepository.get(requireContext());curriculum=CourseCurriculumRepository.get(requireContext());String id=getArguments()==null?"":getArguments().getString(ARG_UNIT,"");lesson=getArguments()==null?0:getArguments().getInt(ARG_LESSON,0);unit=curriculum.byId(id);if(unit==null){((MainActivity)requireActivity()).openToday(java.time.LocalDate.now());return v;}lesson=Math.max(0,Math.min(unit.lessonCount-1,lesson));engine=new CourseLessonEngine(repo,progress);questions=engine.build(unit,lesson);audio=new AudioPlayer(requireContext(),progress);bind(v);restore();render();return v;
    }

    private void bind(View v){bar=v.findViewById(R.id.progress_course_lesson);xp=v.findViewById(R.id.text_course_lesson_xp);header=v.findViewById(R.id.text_course_lesson_header);prompt=v.findViewById(R.id.text_course_lesson_prompt);display=v.findViewById(R.id.text_course_lesson_display);support=v.findViewById(R.id.text_course_lesson_support);hint=v.findViewById(R.id.text_course_hint);feedback=v.findViewById(R.id.text_course_feedback);finishTitle=v.findViewById(R.id.text_course_finish_title);finishSummary=v.findViewById(R.id.text_course_finish_summary);choices=v.findViewById(R.id.container_course_choices);finishPanel=v.findViewById(R.id.panel_course_finish);inputLayout=v.findViewById(R.id.layout_course_answer);input=v.findViewById(R.id.input_course_answer);feedbackCard=v.findViewById(R.id.card_course_feedback);audioButton=v.findViewById(R.id.button_course_lesson_audio);checkButton=v.findViewById(R.id.button_course_check);v.findViewById(R.id.button_course_lesson_close).setOnClickListener(x->close());audioButton.setOnClickListener(x->{CourseQuestion q=current();if(q!=null&&q.word!=null)audio.play(q.word);});checkButton.setOnClickListener(x->onAction());}
    private void restore(){if(progress.hasCourseResume(unit.id,lesson)){index=Math.min(progress.courseResumeQuestion(),questions.size());correct=progress.courseResumeCorrect();wrong=progress.courseResumeWrong();}}
    private CourseQuestion current(){return index>=0&&index<questions.size()?questions.get(index):null;}

    private void render(){
        if(index>=questions.size()){showFinish();return;}finished=false;checked=false;teachingMeaning=false;selectedOption="";CourseQuestion q=current();bar.setProgress((int)Math.round(index*100.0/Math.max(1,questions.size())));xp.setText("⭐ "+progress.courseXp());header.setText(CourseCurriculumRepository.stageName(unit.stage)+" · "+unit.titleZh+" · "+engine.lessonTitle(unit,lesson));prompt.setText(q.prompt);display.setText(q.display);support.setText(q.support==null?"":q.support);support.setVisibility(q.support==null||q.support.trim().isEmpty()?View.GONE:View.VISIBLE);hint.setText(q.hint==null?"":q.hint);hint.setVisibility(q.hint==null||q.hint.isEmpty()?View.GONE:View.VISIBLE);feedbackCard.setVisibility(View.GONE);finishPanel.setVisibility(View.GONE);choices.removeAllViews();input.setEnabled(true);input.setText("");inputLayout.setVisibility(View.GONE);audioButton.setVisibility(q.word!=null&&(q.type==CourseQuestion.INTRO||q.type==CourseQuestion.LISTEN)?View.VISIBLE:View.GONE);
        if(q.type==CourseQuestion.MEANING&&q.teachBeforeTest&&meaningPreviewPassedIndex!=index){renderMeaningTeaching(q);}else if(q.type==CourseQuestion.INTRO){checkButton.setText("继续");checkButton.setEnabled(true);}else if(q.type==CourseQuestion.MEANING||q.type==CourseQuestion.LISTEN||q.type==CourseQuestion.CLOZE||q.type==CourseQuestion.EXAMPLE_MEANING||q.type==CourseQuestion.GRAMMAR_ARTICLE||q.type==CourseQuestion.GRAMMAR_VERB){renderChoices(q);checkButton.setText("检查");checkButton.setEnabled(false);}else{inputLayout.setVisibility(View.VISIBLE);checkButton.setText("检查");checkButton.setEnabled(true);input.requestFocus();}
        if(!teachingMeaning)shownAt=SystemClock.elapsedRealtime();if(q.autoPlayAudio&&q.word!=null)audioButton.postDelayed(()->{if(isAdded()&&current()==q)audio.play(q.word);},180);if(index+1<questions.size()){CourseQuestion n=questions.get(index+1);if(n.word!=null)audio.preload(n.word);}
    }

    /** New vocabulary is taught once immediately before the Chinese-meaning choices. */
    private void renderMeaningTeaching(CourseQuestion q){
        teachingMeaning=true;prompt.setText("先看一遍翻译");display.setText(q.word==null?q.display:q.word.word);StringBuilder lessonText=new StringBuilder(q.answer==null?"":q.answer);if(q.word!=null&&q.word.ipa!=null&&!q.word.ipa.trim().isEmpty())lessonText.append("\n").append(q.word.ipa);if(q.word!=null&&ExampleQuality.isUsable(q.word))lessonText.append("\n\n").append(q.word.example).append("\n").append(q.word.exampleZh==null?"":q.word.exampleZh);support.setText(lessonText.toString());support.setVisibility(View.VISIBLE);hint.setText("先记住这个中文意思，下一步再做选择。第一次见新词不需要猜。");hint.setVisibility(View.VISIBLE);choices.removeAllViews();inputLayout.setVisibility(View.GONE);audioButton.setVisibility(q.word==null?View.GONE:View.VISIBLE);checkButton.setText("我看过了 · 开始选择");checkButton.setEnabled(true);if(q.word!=null)audioButton.postDelayed(()->{if(isAdded()&&current()==q&&teachingMeaning)audio.play(q.word);},180);
    }

    private void renderChoices(CourseQuestion q){for(String opt:q.options){MaterialButton b=new MaterialButton(requireContext());b.setAllCaps(false);b.setText(opt);b.setTextSize(14);b.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.surface)));b.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));b.setStrokeWidth(dp(1));b.setCornerRadius(dp(14));b.setOnClickListener(x->{if(checked)return;selectedOption=opt;for(int i=0;i<choices.getChildCount();i++){View c=choices.getChildAt(i);if(c instanceof MaterialButton){MaterialButton mb=(MaterialButton)c;boolean sel=mb==b;mb.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),sel?R.color.blue:R.color.line)));mb.setStrokeWidth(dp(sel?2:1));}}checkButton.setEnabled(true);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(54));lp.bottomMargin=dp(8);choices.addView(b,lp);}}

    private void onAction(){if(finished){closeToPath();return;}CourseQuestion q=current();if(q==null){showFinish();return;}if(teachingMeaning&&!checked){meaningPreviewPassedIndex=index;teachingMeaning=false;render();return;}if(q.type==CourseQuestion.INTRO&&!checked){index++;saveResume();render();return;}if(!checked){evaluate(q);return;}index++;saveResume();render();}
    private void evaluate(CourseQuestion q){String actual=isChoice(q)?selectedOption:(input.getText()==null?"":input.getText().toString());boolean ok=equalsAnswer(actual,q.answer);checked=true;if(ok)correct++;else wrong++;long ms=Math.max(0,SystemClock.elapsedRealtime()-shownAt);if(q.type!=CourseQuestion.INTRO&&q.word!=null){if(q.type==CourseQuestion.ACTIVE)progress.recordDimensionResults(q.word.id,new int[]{ProgressStore.DIM_MEANING,ProgressStore.DIM_SPELLING},ok,ms);else if(q.type!=CourseQuestion.GRAMMAR_ARTICLE&&q.type!=CourseQuestion.GRAMMAR_VERB)progress.recordDimensionResult(q.word.id,q.dimension,ok,ms);if(!ok){String cause=q.type==CourseQuestion.LISTEN?ErrorCause.LISTENING_CONFUSION:(q.type==CourseQuestion.GRAMMAR_ARTICLE?ErrorCause.ARTICLE_GENDER:(q.type==CourseQuestion.GRAMMAR_VERB?ErrorCause.WORD_FORM:(q.type==CourseQuestion.SPELL_HINT||q.type==CourseQuestion.ACTIVE?ErrorCause.SPELLING:ErrorCause.RECALL)));progress.recordErrorCause(cause,q.word.id,"course_v334",q.answer,actual,"unit="+unit.id+",lesson="+lesson);}}
        feedbackCard.setVisibility(View.VISIBLE);feedback.setText(ok?"✓ 正确":"答案："+q.answer+"\n没关系，后面的课程还会再遇到它。");feedback.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));checkButton.setText("继续");checkButton.setEnabled(true);disableInputs();hideKeyboard();saveResume();}
    private void disableInputs(){for(int i=0;i<choices.getChildCount();i++)choices.getChildAt(i).setEnabled(false);input.setEnabled(false);}
    private boolean isChoice(CourseQuestion q){return q.type==CourseQuestion.MEANING||q.type==CourseQuestion.LISTEN||q.type==CourseQuestion.CLOZE||q.type==CourseQuestion.EXAMPLE_MEANING||q.type==CourseQuestion.GRAMMAR_ARTICLE||q.type==CourseQuestion.GRAMMAR_VERB;}

    private void showFinish(){finished=true;bar.setProgress(100);choices.removeAllViews();inputLayout.setVisibility(View.GONE);audioButton.setVisibility(View.GONE);hint.setVisibility(View.GONE);support.setVisibility(View.GONE);feedbackCard.setVisibility(View.GONE);display.setText("🎉");prompt.setText("这一小课完成了");finishPanel.setVisibility(View.VISIBLE);int tested=correct+wrong;int score=tested==0?100:(int)Math.round(correct*100.0/tested);boolean challenge=lesson==unit.lessonCount-1;boolean pass=!challenge||score>=70;int gain=pass?(challenge?20:10):0;if(pass){progress.markCourseLessonComplete(unit.id,unit.index,lesson,unit.lessonCount,gain);progress.clearCourseResume();finishTitle.setText(challenge?"🏆 单元挑战完成":"🎉 完成一课");boolean finalUnit=challenge&&unit.index>=curriculum.size()-1;finishSummary.setText("正确率 "+score+"% · +"+gain+" 学习点\n"+(challenge?(finalUnit?"完整课程已完成，可以继续复习巩固。":"下一单元已经解锁。"):"继续下一关，系统会自动混入复习。"));checkButton.setText("回到学习路径");}else{finishTitle.setText("再练一次就可以过关");finishSummary.setText("本次正确率 "+score+"%\n挑战需要达到70%。不会扣除任何进度。");checkButton.setText("重新挑战");}checkButton.setEnabled(true);checkButton.setOnClickListener(x->{if(pass)closeToPath();else{progress.clearCourseResume();((MainActivity)requireActivity()).openCourseLesson(unit.id,lesson);}});}
    private void saveResume(){if(!finished)progress.saveCourseResume(unit.id,lesson,index,correct,wrong);}
    private void close(){saveResume();closeToPath();}
    private void closeToPath(){((MainActivity)requireActivity()).setFocusUi(false);((MainActivity)requireActivity()).openCourseUnit(unit.id);}
    private void hideKeyboard(){View v=getView();if(v==null)return;InputMethodManager imm=(InputMethodManager)requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(imm!=null)imm.hideSoftInputFromWindow(v.getWindowToken(),0);}
    private boolean equalsAnswer(String a,String b){return normalize(a).equals(normalize(b));}
    private String normalize(String s){if(s==null)return "";String x=Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT).replace('’','\''),Normalizer.Form.NFC);return x.replaceAll("[\\s\\p{Punct}]+$","").replaceAll("\\s+"," ");}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    @Override public void onDestroyView(){if(audio!=null)audio.release();if(getActivity() instanceof MainActivity)((MainActivity)getActivity()).setFocusUi(false);super.onDestroyView();}
}

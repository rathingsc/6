package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/** v4.5.0 retained error-evidence repair queue used by daily practice and weekly diagnosis. */
public class ErrorEvidenceRepairFragment extends Fragment {
    private final List<ErrorRecordEntity> items=new ArrayList<>();
    private ProgressStore progress;private WordRepository words;private int index=0,totalPending=0;private long startedAt=0L;
    private TextView stats,source,actual,detail,feedback,expected;private EditText answer;private MaterialButton check,reveal,skip,next;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_error_evidence_repair,container,false);progress=new ProgressStore(requireContext());words=WordRepository.get(requireContext());
        stats=v.findViewById(R.id.text_error_evidence_stats);source=v.findViewById(R.id.text_error_evidence_source);actual=v.findViewById(R.id.text_error_evidence_actual);detail=v.findViewById(R.id.text_error_evidence_detail);feedback=v.findViewById(R.id.text_error_evidence_feedback);expected=v.findViewById(R.id.text_error_evidence_expected);answer=v.findViewById(R.id.edit_error_evidence_answer);check=v.findViewById(R.id.button_error_evidence_check);reveal=v.findViewById(R.id.button_error_evidence_reveal);skip=v.findViewById(R.id.button_error_evidence_skip);next=v.findViewById(R.id.button_error_evidence_next);
        v.findViewById(R.id.button_error_evidence_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());check.setOnClickListener(x->checkRepair());reveal.setOnClickListener(x->revealAnswer());skip.setOnClickListener(x->advance(false));next.setOnClickListener(x->advance(false));load();return v;
    }

    private ErrorRecordEntity current(){return items.isEmpty()||index<0||index>=items.size()?null:items.get(index);}
    private void load(){
        stats.setText("正在整理最近的真实错误…");final android.content.Context app=requireContext().getApplicationContext();
        new Thread(()->{LearningStateDao dao=LearningDatabase.get(app).learningStateDao();List<ErrorRecordEntity> x=dao.unresolvedPracticeErrors(40);int count=dao.unresolvedPracticeErrorCount();if(!isAdded())return;requireActivity().runOnUiThread(()->{items.clear();items.addAll(x);totalPending=count;progress.setPendingErrorRepairs(count);index=0;render();});},"error-evidence-load").start();
    }

    private void render(){
        ErrorRecordEntity e=current();boolean empty=e==null;answer.setText("");answer.setEnabled(!empty);check.setEnabled(!empty);reveal.setEnabled(!empty);skip.setEnabled(!empty);next.setVisibility(View.GONE);expected.setVisibility(View.GONE);feedback.setText("");
        if(empty){stats.setText("待修复 0 条 · 最近记录都已经重新做对");source.setText("🎉 个人错句本已清空");actual.setText("继续做听力、写作、每日5句或自由会话；新的真实错误会自动进入这里。");detail.setText("显示答案不会被算作修复。以后出现错误时，重新写对一次才会从队列消失，并进入句子间隔复习。");startedAt=System.currentTimeMillis();return;}
        stats.setText("待修复 "+totalPending+" 条 · 当前 "+(index+1)+" / "+items.size()+" · 正确重写后才移出队列");source.setText(ErrorCause.label(e.cause)+" · "+modeLabel(e.mode)+" · 已回炉 "+e.repairAttempts+" 次");actual.setText(e.actual==null||e.actual.trim().isEmpty()?"（当时没有完整作答）":e.actual);detail.setText((e.detail==null||e.detail.trim().isEmpty()?"根据当时的错误，把这句话重新写正确。":e.detail)+"\n提示：先修改自己的错误表达，不要直接照抄答案。");startedAt=System.currentTimeMillis();
    }

    private void checkRepair(){
        ErrorRecordEntity e=current();if(e==null)return;String typed=answer.getText()==null?"":answer.getText().toString().trim();if(typed.isEmpty()){answer.setError("先自己重写一次");return;}
        ErrorCauseAnalyzer.SentenceAnalysis a=ErrorCauseAnalyzer.analyzeSentence(e.expected,typed,words);int threshold="writing".equals(e.mode)?70:(wordCount(e.expected)<=2?96:86);boolean ok=a.score>=threshold;long ms=Math.max(1,System.currentTimeMillis()-startedAt);progress.recordAuxiliaryResult("error_evidence_repair",ok,ms);
        SentenceFsrsRepository.recordDimension(requireContext(),"个人错句本 · "+modeLabel(e.mode),e.expected,repairHint(e),SentenceFsrsRepository.DIM_RECALL,ok,a.score,null);
        final android.content.Context app=requireContext().getApplicationContext();new Thread(()->{LearningStateDao dao=LearningDatabase.get(app).learningStateDao();if(ok)dao.markMatchingErrorRepaired(e.mode,e.expected,e.actual,System.currentTimeMillis());else dao.incrementMatchingErrorRepairAttempt(e.mode,e.expected,e.actual);},"error-evidence-grade").start();
        if(ok){progress.markEvidenceRepairComplete();totalPending=Math.max(0,totalPending-1);feedback.setText("✓ 修复成功 · 匹配度 "+a.score+"%\n这条正确表达已经进入句子间隔复习，之后还会再次出现。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.success));expected.setText("正确表达\n"+e.expected);expected.setVisibility(View.VISIBLE);check.setEnabled(false);reveal.setEnabled(false);skip.setVisibility(View.GONE);next.setVisibility(View.VISIBLE);}else{e.repairAttempts++;feedback.setText("△ 还没有修复成功 · 匹配度 "+a.score+"%\n"+a.summary()+"\n"+a.diff+"\n先根据提示再改一次；这条仍会留在待修复队列里。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));startedAt=System.currentTimeMillis();}
    }

    private void revealAnswer(){
        ErrorRecordEntity e=current();if(e==null)return;expected.setText("正确表达\n"+e.expected);expected.setVisibility(View.VISIBLE);feedback.setText("这次只算查看答案，不算修复。请看完后遮住答案，再重新输入到匹配线以上。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));progress.recordAuxiliaryResult("error_evidence_repair",false,Math.max(1,System.currentTimeMillis()-startedAt));SentenceFsrsRepository.recordDimension(requireContext(),"个人错句本 · "+modeLabel(e.mode),e.expected,repairHint(e),SentenceFsrsRepository.DIM_RECALL,false,0,null);e.repairAttempts++;final String mode=e.mode,target=e.expected,old=e.actual;final android.content.Context app=requireContext().getApplicationContext();new Thread(()->LearningDatabase.get(app).learningStateDao().incrementMatchingErrorRepairAttempt(mode,target,old),"error-evidence-reveal").start();startedAt=System.currentTimeMillis();
    }

    private void advance(boolean reload){
        if(reload){load();return;}ErrorRecordEntity e=current();if(e!=null&&e.repairedAt==0&&next.getVisibility()==View.VISIBLE){e.repairedAt=System.currentTimeMillis();items.remove(index);if(index>=items.size())index=0;}else if(!items.isEmpty())index=(index+1)%items.size();render();
    }

    private int wordCount(String s){String b=ErrorCauseAnalyzer.basic(s);return b.isEmpty()?0:b.split("\\s+").length;}
    private String repairHint(ErrorRecordEntity e){String d=e.detail==null?"":e.detail.trim();return d.isEmpty()?ErrorCause.label(e.cause):ErrorCause.label(e.cause)+" · "+d;}
    private String modeLabel(String mode){if("writing".equals(mode))return "真实写作";if("freechat".equals(mode))return "自由会话";if("daily_speaking".equals(mode))return "每日5句";if("listen_speak".equals(mode))return "听说训练";if("intensive_listening".equals(mode))return "精听听写";if("listening_course".equals(mode))return "听力课程";if("dialogue".equals(mode)||"dialogue_speaking".equals(mode))return "情景会话";if("active_recall".equals(mode))return "主动回忆";return mode==null||mode.isEmpty()?"练习":mode;}
}

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

/** v2.8: multi-channel relearn flow for wrong/stubborn words. */
public class WrongWordRepairFragment extends Fragment {
    private final List<Word> queue=new ArrayList<>();private WordRepository repo;private ProgressStore progress;private AudioPlayer audio;private int index=0,stage=0;private long started;
    private TextView counter,word,chinese,example,feedback,stageText;private EditText input;private MaterialButton main,next,audioBtn;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        View v=i.inflate(R.layout.fragment_wrong_word_repair,c,false);repo=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);counter=v.findViewById(R.id.text_repair_counter);word=v.findViewById(R.id.text_repair_word);chinese=v.findViewById(R.id.text_repair_chinese);example=v.findViewById(R.id.text_repair_example);feedback=v.findViewById(R.id.text_repair_feedback);stageText=v.findViewById(R.id.text_repair_stage);input=v.findViewById(R.id.edit_repair_answer);main=v.findViewById(R.id.button_repair_main);next=v.findViewById(R.id.button_repair_next);audioBtn=v.findViewById(R.id.button_repair_audio);
        for(Word w:repo.wrongWords(progress)){queue.add(w);if(queue.size()>=10)break;}if(queue.isEmpty())for(Word w:repo.stubbornWords(progress)){queue.add(w);if(queue.size()>=10)break;}
        main.setOnClickListener(x->advance());next.setOnClickListener(x->{index++;stage=0;show();});audioBtn.setOnClickListener(x->{Word w=current();if(w!=null)audio.play(w);});v.findViewById(R.id.button_repair_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());show();return v;
    }
    private Word current(){return index<queue.size()?queue.get(index):null;}
    private void show(){Word w=current();feedback.setText("");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));input.setText("");input.setEnabled(true);input.setVisibility(View.GONE);next.setVisibility(View.GONE);if(w==null){counter.setText(queue.isEmpty()?"没有需要重学的错词":"本轮完成");word.setText("🎉");chinese.setText(queue.isEmpty()?"当前错词本和顽固词队列都是空的。":"这一轮错词重学已经完成。");example.setVisibility(View.GONE);audioBtn.setVisibility(View.GONE);main.setVisibility(View.GONE);stageText.setText("可以回到练习中心继续其他弱项。");return;}main.setVisibility(View.VISIBLE);audioBtn.setVisibility(View.VISIBLE);counter.setText((index+1)+" / "+queue.size()+" · 错词 "+progress.wrongCount(w.id)+"次");word.setText(w.word);chinese.setVisibility(View.INVISIBLE);example.setVisibility(View.GONE);stageText.setText("1/3 先主动回忆意思，不急着看答案");main.setText("显示释义");started=System.currentTimeMillis();}
    private void advance(){Word w=current();if(w==null)return;if(stage==0){stage=1;chinese.setText(w.chinese);chinese.setVisibility(View.VISIBLE);stageText.setText("2/3 听一次发音，再从中文主动拼写");audio.play(w);main.setText("开始拼写回忆");return;}if(stage==1){stage=2;word.setText("中文 → 意大利语");input.setVisibility(View.VISIBLE);input.requestFocus();stageText.setText("3/3 不看原词，完整写出意大利语");main.setText("检查答案");return;}String typed=input.getText()==null?"":input.getText().toString().trim();if(typed.isEmpty()){input.setError("请先写出答案");return;}boolean ok=ErrorCauseAnalyzer.basic(typed).equals(ErrorCauseAnalyzer.basic(w.word));long ms=Math.max(1,System.currentTimeMillis()-started);progress.recordDimensionResults(w.id,new int[]{ProgressStore.DIM_MEANING,ProgressStore.DIM_SPELLING},ok,ms);progress.recordAuxiliaryResult("error_repair",ok,ms);if(!ok){String cause=ErrorCauseAnalyzer.forTypedWord(w.word,typed,w,repo);progress.recordErrorCause(cause,w.id,"wrong_repair",w.word,typed,"错词重学拼写");}
        word.setText(w.word);chinese.setVisibility(View.VISIBLE);boolean hasExample=ExampleQuality.isUsable(w);example.setVisibility(hasExample?View.VISIBLE:View.GONE);if(hasExample)example.setText(w.example+"\n"+w.exampleZh);feedback.setText(ok?"✓ 回忆正确 · 错词计数会逐步下降":"△ 正确答案："+w.word+"\n这次已重新安排复习");feedback.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));input.setEnabled(false);main.setVisibility(View.GONE);next.setVisibility(View.VISIBLE);next.setText(index+1>=queue.size()?"完成":"下一个");}
    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}

package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** v2.8: debounced/background vocabulary filtering so typing and chip changes stay responsive. */
public class VocabularyFragment extends Fragment{
    private static final ExecutorService FILTER_EXECUTOR=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private WordRepository repo;private ProgressStore progress;private AudioPlayer audio;private WordAdapter adapter;
    private EditText search;private Spinner topic;private Chip route,fav,low,wrong,stubborn,graduated;private TextView count;private int generation=0;private Runnable pending;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        View v=i.inflate(R.layout.fragment_vocabulary,c,false);repo=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        search=v.findViewById(R.id.edit_search);topic=v.findViewById(R.id.spinner_topic);route=v.findViewById(R.id.check_route);fav=v.findViewById(R.id.check_favorite);low=v.findViewById(R.id.check_low);wrong=v.findViewById(R.id.check_wrong);stubborn=v.findViewById(R.id.check_stubborn);graduated=v.findViewById(R.id.check_graduated);count=v.findViewById(R.id.text_count);
        route.setCheckable(true);fav.setCheckable(true);low.setCheckable(true);wrong.setCheckable(true);stubborn.setCheckable(true);graduated.setCheckable(true);
        RecyclerView rv=v.findViewById(R.id.recycler_vocabulary);rv.setLayoutManager(new LinearLayoutManager(requireContext()));rv.setItemAnimator(null);rv.setItemViewCacheSize(10);adapter=new WordAdapter(requireContext(),progress,audio,this::scheduleFilter);rv.setAdapter(adapter);
        List<String> ts=new ArrayList<>();ts.add("全部主题");ts.addAll(repo.topics());ArrayAdapter<String>a=new ArrayAdapter<>(requireContext(),R.layout.item_spinner_text,ts);a.setDropDownViewResource(R.layout.item_spinner_dropdown);topic.setAdapter(a);
        topic.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?>p){}public void onItemSelected(android.widget.AdapterView<?>p,View x,int n,long id){scheduleFilter();}});
        route.setOnCheckedChangeListener((b,x)->scheduleFilter());fav.setOnCheckedChangeListener((b,x)->scheduleFilter());low.setOnCheckedChangeListener((b,x)->scheduleFilter());wrong.setOnCheckedChangeListener((b,x)->scheduleFilter());stubborn.setOnCheckedChangeListener((b,x)->scheduleFilter());graduated.setOnCheckedChangeListener((b,x)->scheduleFilter());
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){scheduleFilter();}public void afterTextChanged(Editable e){}});
        v.findViewById(R.id.button_reset_vocab_filters).setOnClickListener(x->{search.setText("");route.setChecked(false);fav.setChecked(false);low.setChecked(false);wrong.setChecked(false);stubborn.setChecked(false);graduated.setChecked(false);topic.setSelection(0);scheduleFilterNow();});
        v.findViewById(R.id.button_open_custom_library).setOnClickListener(x->((MainActivity)requireActivity()).openCustomLibrary());scheduleFilterNow();return v;
    }

    private void scheduleFilter(){if(pending!=null)main.removeCallbacks(pending);pending=this::filterAsync;main.postDelayed(pending,160);}
    private void scheduleFilterNow(){if(pending!=null)main.removeCallbacks(pending);filterAsync();}
    private void filterAsync(){
        if(adapter==null||search==null)return;final android.content.Context app=requireContext().getApplicationContext();final int g=++generation;final String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);final String t=topic.getSelectedItem()==null?"全部主题":topic.getSelectedItem().toString();final boolean fRoute=route.isChecked(),fFav=fav.isChecked(),fLow=low.isChecked(),fWrong=wrong.isChecked(),fStubborn=stubborn.isChecked(),fGraduated=graduated.isChecked();final int routeLimit=progress.vocabularyRouteLimit();
        count.setText("正在筛选…");
        FILTER_EXECUTOR.execute(()->{
            try{
                List<Word> out=new ArrayList<>();int matched=0;
                for(Word w:repo.all()){
                    if(!t.equals("全部主题")&&!w.level.equals(t))continue;if(fRoute&&w.id>routeLimit)continue;if(fFav&&!progress.favorite(w.id))continue;if(fLow&&progress.mastery(w.id)>=4)continue;if(fWrong&&progress.wrongCount(w.id)<=0)continue;if(fStubborn&&!progress.isStubborn(w.id))continue;if(fGraduated&&!progress.isGraduated(w.id))continue;
                    String hay=(safe(w.word)+" "+safe(w.english)+" "+safe(w.chinese)+" "+safe(w.zhPron)+" "+safe(w.lemma)+" "+safe(w.formInfo)).toLowerCase(Locale.ROOT);if(!q.isEmpty()&&!hay.contains(q))continue;matched++;if(out.size()<400)out.add(w);
                }
                final int fm=matched;main.post(()->{if(!isAdded()||getView()==null||g!=generation)return;count.setText((fRoute?progress.vocabularyRouteLabel()+" · ":"")+(fm<=400?"找到 "+fm+" 项":"找到 "+fm+" 项 · 当前显示前400项，请继续搜索或筛选"));adapter.submit(out);});
            }catch(Exception e){LocalErrorLog.write(app,"Vocabulary filter",e);main.post(()->{if(isAdded()&&getView()!=null&&g==generation)count.setText("筛选暂时失败，请清除筛选后重试");});}
        });
    }
    private static String safe(String s){return s==null?"":s;}
    @Override public void onDestroyView(){generation++;if(pending!=null)main.removeCallbacks(pending);if(audio!=null)audio.release();super.onDestroyView();}
}

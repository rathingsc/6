package com.italiano2774.nativeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ScenarioAdapter extends RecyclerView.Adapter<ScenarioAdapter.Holder>{
    public interface Listener{void onOpen(Scenario s);}
    private final List<Scenario> items;private final ProgressStore progress;private final Listener listener;
    public ScenarioAdapter(List<Scenario> items,ProgressStore progress,Listener listener){this.items=items;this.progress=progress;this.listener=listener;}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scenario,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int pos){
        Scenario s=items.get(pos);h.emoji.setText(s.emoji);h.title.setText(s.title);h.subtitle.setText(s.subtitle);
        int done=progress==null?0:progress.dialogueScenarioCompleted(s.id);
        int b=progress==null?0:progress.dialogueScenarioBestScore(s.id,1),m=progress==null?0:progress.dialogueScenarioBestScore(s.id,2),a=progress==null?0:progress.dialogueScenarioBestScore(s.id,3);
        h.count.setText(done>0?s.phrases.size()+" 个核心句 · 初 "+b+"% · 中 "+m+"% · 高 "+a+"%":s.phrases.size()+" 个核心句 · 初级→中级→高级三档会话");
        h.itemView.setOnClickListener(v->listener.onOpen(s));
    }
    @Override public int getItemCount(){return items.size();}
    static class Holder extends RecyclerView.ViewHolder{TextView emoji,title,subtitle,count;Holder(View v){super(v);emoji=v.findViewById(R.id.text_scenario_emoji);title=v.findViewById(R.id.text_scenario_title);subtitle=v.findViewById(R.id.text_scenario_subtitle);count=v.findViewById(R.id.text_scenario_count);}}
}

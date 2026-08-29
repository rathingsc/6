package com.italiano2774.nativeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LifeTaskAdapter extends RecyclerView.Adapter<LifeTaskAdapter.Holder>{
    public interface Listener{void onOpen(LifeTask task);}
    private final List<LifeTask> items;private final ProgressStore progress;private final Listener listener;
    public LifeTaskAdapter(List<LifeTask> items,ProgressStore progress,Listener listener){this.items=items;this.progress=progress;this.listener=listener;}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_life_task,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int pos){LifeTask t=items.get(pos);int done=LifeTaskEngine.completedStages(progress,t),next=LifeTaskEngine.nextLevel(progress,t);h.emoji.setText(t.emoji);h.title.setText(t.title);h.subtitle.setText(t.subtitle);h.score.setText(LifeTaskEngine.compactProgress(progress,t));h.status.setText(done>=3?"✅ 已掌握":("下一关 · "+LifeTaskEngine.levelTitle(next)+" · "+LifeTaskEngine.passLine(next)+"%通过"));h.itemView.setOnClickListener(v->listener.onOpen(t));}
    @Override public int getItemCount(){return items.size();}
    static class Holder extends RecyclerView.ViewHolder{TextView emoji,title,subtitle,score,status;Holder(View v){super(v);emoji=v.findViewById(R.id.text_life_task_emoji);title=v.findViewById(R.id.text_life_task_title);subtitle=v.findViewById(R.id.text_life_task_subtitle);score=v.findViewById(R.id.text_life_task_score);status=v.findViewById(R.id.text_life_task_status);}}
}

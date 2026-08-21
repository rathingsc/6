package com.italiano2774.nativeapp;
import android.view.*;import android.widget.TextView;import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;import java.util.*;
public class ReadingAdapter extends RecyclerView.Adapter<ReadingAdapter.H>{
    public interface Listener{void open(ReadingPassage passage);} private final Listener listener;private final ProgressStore progress;private final ReadingComprehensibilityEngine engine;private final List<ReadingPassage> items=new ArrayList<>();
    public ReadingAdapter(ProgressStore p,ReadingComprehensibilityEngine e,Listener l){progress=p;engine=e;listener=l;} public void submit(List<ReadingPassage> data){items.clear();items.addAll(data);notifyDataSetChanged();}
    @NonNull public H onCreateViewHolder(@NonNull ViewGroup p,int v){return new H(LayoutInflater.from(p.getContext()).inflate(R.layout.item_reading,p,false));}
    public void onBindViewHolder(@NonNull H h,int pos){ReadingPassage r=items.get(pos);h.level.setText(r.level);h.title.setText(r.title);h.zh.setText(r.titleZh);int best=progress.readingBest(r.id);ReadingComprehensibilityEngine.Score sc=engine.score(r);h.status.setText("已知 "+sc.coverage+"% · "+sc.label()+(best>0?(" · 最好 "+best+"%"):""));h.itemView.setOnClickListener(v->listener.open(r));}
    public int getItemCount(){return items.size();}
    static class H extends RecyclerView.ViewHolder{TextView level,title,zh,status;H(View v){super(v);level=v.findViewById(R.id.text_read_level);title=v.findViewById(R.id.text_read_title);zh=v.findViewById(R.id.text_read_title_zh);status=v.findViewById(R.id.text_read_status);}}
}

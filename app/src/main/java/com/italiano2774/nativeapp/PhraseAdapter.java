package com.italiano2774.nativeapp;
import android.view.*;import android.widget.*;import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;import java.util.*;
public class PhraseAdapter extends RecyclerView.Adapter<PhraseAdapter.H>{
 public interface Listener{void speak(Phrase p);void reviewed(Phrase p);}private final Listener listener;private final List<Phrase> items=new ArrayList<>();
 public PhraseAdapter(Listener l){listener=l;}public void submit(List<Phrase> x){items.clear();items.addAll(x);notifyDataSetChanged();}
 @NonNull public H onCreateViewHolder(@NonNull ViewGroup p,int v){return new H(LayoutInflater.from(p.getContext()).inflate(R.layout.item_phrase,p,false));}
 public void onBindViewHolder(@NonNull H h,int pos){Phrase p=items.get(pos);h.it.setText(p.italian);h.zh.setText(p.chinese);h.meta.setText(p.category+(p.note.isEmpty()?"":" · "+p.note));h.audio.setOnClickListener(v->listener.speak(p));h.done.setOnClickListener(v->listener.reviewed(p));}
 public int getItemCount(){return items.size();}
 static class H extends RecyclerView.ViewHolder{TextView it,zh,meta;ImageButton audio;com.google.android.material.button.MaterialButton done;H(View v){super(v);it=v.findViewById(R.id.text_phrase_it);zh=v.findViewById(R.id.text_phrase_zh);meta=v.findViewById(R.id.text_phrase_meta);audio=v.findViewById(R.id.button_phrase_audio);done=v.findViewById(R.id.button_phrase_done);}}
}

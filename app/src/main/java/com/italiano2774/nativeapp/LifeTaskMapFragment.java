package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** v4.8.0 12 real-life missions x three progressive difficulty stages. */
public class LifeTaskMapFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_life_task_map,container,false);ProgressStore p=new ProgressStore(requireContext());
        int stages=LifeTaskEngine.completedStages(p),mastered=LifeTaskEngine.masteredTasks(p);LifeTask next=LifeTaskEngine.nextTask(p);
        ((TextView)v.findViewById(R.id.text_life_map_progress)).setText("已通过 "+stages+" / 36 关 · 已掌握 "+mastered+" / 12 个生活任务"+(next==null?"":"\n下一步："+next.emoji+" "+next.title+" · "+LifeTaskEngine.levelTitle(LifeTaskEngine.nextLevel(p,next))));
        RecyclerView rv=v.findViewById(R.id.recycler_life_tasks);rv.setLayoutManager(new LinearLayoutManager(requireContext()));rv.setAdapter(new LifeTaskAdapter(LifeTaskRepository.all(),p,t->((MainActivity)requireActivity()).openLifeTask(t.id)));
        v.findViewById(R.id.button_life_map_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());return v;
    }
}

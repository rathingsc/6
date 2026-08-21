package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHealthFragment extends Fragment {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private TextView status,log;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        View v=i.inflate(R.layout.fragment_database_health,c,false);status=v.findViewById(R.id.text_database_health);log=v.findViewById(R.id.text_local_error_log);
        v.findViewById(R.id.button_health_refresh).setOnClickListener(x->refresh());
        v.findViewById(R.id.button_health_backup).setOnClickListener(x->{((MainActivity)requireActivity()).createLocalBackup();v.postDelayed(this::refresh,800);});
        v.findViewById(R.id.button_health_clean).setOnClickListener(x->{android.content.Context app=requireContext().getApplicationContext();IO.execute(()->{int n=DatabaseHealthManager.cleanOldLogs(app);if(!isAdded())return;requireActivity().runOnUiThread(()->{Toast.makeText(requireContext(),"已清理 "+n+" 条过旧详细日志",Toast.LENGTH_SHORT).show();refresh();});});});
        v.findViewById(R.id.button_health_repair).setOnClickListener(x->{android.content.Context app=requireContext().getApplicationContext();IO.execute(()->{int n=0;try{n=new ProgressStore(app).repairCorruptState(WordRepository.get(app).all());}catch(Exception e){LocalErrorLog.write(app,"Manual state repair",e);}final int fixed=n;if(!isAdded())return;requireActivity().runOnUiThread(()->{Toast.makeText(requireContext(),fixed==0?"未发现异常学习状态":"已修复 "+fixed+" 项异常状态",Toast.LENGTH_SHORT).show();refresh();});});});
        v.findViewById(R.id.button_error_log_clear).setOnClickListener(x->{LocalErrorLog.clear(requireContext());refresh();});
        v.findViewById(R.id.button_health_back).setOnClickListener(x->((MainActivity)requireActivity()).openSettings());refresh();return v;
    }
    private void refresh(){status.setText("正在检查数据库…");android.content.Context app=requireContext().getApplicationContext();IO.execute(()->{DatabaseHealthManager.Snapshot snap=DatabaseHealthManager.inspect(app);String recent=LocalErrorLog.recent(app);if(!isAdded())return;requireActivity().runOnUiThread(()->{status.setText(snap.summary());log.setText(recent);});});}
}

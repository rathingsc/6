package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.time.LocalDate;

/** Hides advanced maintenance features behind a calm "My" screen. */
public class ProfileFragment extends Fragment {
    private ProgressStore progress;private CourseCurriculumRepository curriculum;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){View v=inflater.inflate(R.layout.fragment_profile,container,false);progress=new ProgressStore(requireContext());curriculum=CourseCurriculumRepository.get(requireContext());curriculum.migrateLegacyPositionIfNeeded(progress,WordRepository.get(requireContext()));bind(v);return v;}
    private void bind(View v){CourseUnit u=curriculum.current(progress);((TextView)v.findViewById(R.id.text_profile_stage)).setText(u==null?"A0 入门":CourseCurriculumRepository.stageName(u.stage));((TextView)v.findViewById(R.id.text_profile_summary)).setText("连续学习 "+progress.activityStreak()+" 天 · ⭐ "+progress.courseXp());((TextView)v.findViewById(R.id.text_profile_detail)).setText((u==null?"从第一单元开始":("当前：第"+u.stageUnit+"单元 · "+u.titleZh))+"\n今天学习 "+Math.round(progress.dailyActiveSeconds(LocalDate.now())/60.0)+" 分钟");MainActivity a=(MainActivity)requireActivity();v.findViewById(R.id.button_profile_vocabulary).setOnClickListener(x->a.openVocabulary());v.findViewById(R.id.button_profile_daily).setOnClickListener(x->a.openDailySummary());v.findViewById(R.id.button_profile_weekly).setOnClickListener(x->a.openWeeklyReport());v.findViewById(R.id.button_profile_passport).setOnClickListener(x->a.openMasteryPassport());v.findViewById(R.id.button_profile_forgetting).setOnClickListener(x->a.openForgettingProfile());v.findViewById(R.id.button_profile_settings).setOnClickListener(x->a.openSettings());v.findViewById(R.id.button_profile_custom).setOnClickListener(x->a.openCustomLibrary());v.findViewById(R.id.button_profile_database).setOnClickListener(x->a.openDatabaseHealth());}
}

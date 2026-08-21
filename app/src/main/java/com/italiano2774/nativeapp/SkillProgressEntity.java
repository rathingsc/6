package com.italiano2774.nativeapp;
import androidx.annotation.NonNull;import androidx.room.Entity;import androidx.room.Index;import androidx.room.PrimaryKey;
@Entity(tableName="skill_progress",indices={@Index(value={"updatedAt"})})
public class SkillProgressEntity{
 @PrimaryKey @NonNull public String skillId="";public int attempts;public int correct;public long responseMs;public long lastEpochDay=Long.MIN_VALUE;public long updatedAt;
}

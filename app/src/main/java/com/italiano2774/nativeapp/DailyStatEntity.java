package com.italiano2774.nativeapp;
import androidx.annotation.NonNull;import androidx.room.Entity;import androidx.room.Index;import androidx.room.PrimaryKey;
@Entity(tableName="daily_stats",indices={@Index(value={"updatedAt"})})
public class DailyStatEntity{
 @PrimaryKey @NonNull public String date="";public int cards;public int attempts;public int correct;public long responseMs;public long activeSeconds;public long updatedAt;
}

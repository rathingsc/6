package com.italiano2774.nativeapp;
import androidx.annotation.NonNull;import androidx.room.Entity;import androidx.room.Index;import androidx.room.PrimaryKey;
@Entity(tableName="sentence_progress",indices={@Index(value={"dueEpochDay"}),@Index(value={"source","dueEpochDay"})})
public class SentenceProgressEntity{
 @PrimaryKey @NonNull public String sentenceId="";
 @NonNull public String source="";
 @NonNull public String italian="";
 @NonNull public String chinese="";
 public int attempts;public int correct;public long lastEpochDay=Long.MIN_VALUE;public long dueEpochDay=Long.MIN_VALUE;public int intervalDays;public double stability=1.0;public double difficulty=5.0;public long updatedAt;
 // v2.7 sentence dimensions. 0-5 levels, independent from the aggregate FSRS state.
 public int meaningLevel;public int listeningLevel;public int recallLevel;public int speakingLevel;public int lapses;public int lastScore;
}

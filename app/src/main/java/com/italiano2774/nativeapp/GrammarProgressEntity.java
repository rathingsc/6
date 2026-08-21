package com.italiano2774.nativeapp;
import androidx.annotation.NonNull;import androidx.room.Entity;import androidx.room.Index;import androidx.room.PrimaryKey;
@Entity(tableName="grammar_progress",indices={@Index(value={"dueEpochDay"})})
public class GrammarProgressEntity{
 @PrimaryKey @NonNull public String grammarId="";
 public int attempts;public int correct;public long lastEpochDay=Long.MIN_VALUE;public long dueEpochDay=Long.MIN_VALUE;public int intervalDays;public double stability=1.0;public double difficulty=5.0;public long updatedAt;
}

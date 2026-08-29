package com.italiano2774.nativeapp;
import androidx.room.Entity;import androidx.room.Index;import androidx.room.PrimaryKey;
@Entity(tableName="word_progress",indices={@Index(value={"dueEpochDay"}),@Index(value={"mastery"})})
public class WordProgressEntity{
 @PrimaryKey public int wordId;public int mastery;public int meaning;public int listening;public int spelling;public int speaking;public int wrongCount;public int correctStreak;public int attempts;public int correctAnswers;public long avgResponseMs;public long lastEpochDay=Long.MIN_VALUE;public long dueEpochDay=Long.MIN_VALUE;public int intervalDays;public double stability;public double difficulty;public boolean favorite;public long updatedAt;
}

package com.italiano2774.nativeapp;
import androidx.annotation.NonNull;import androidx.room.Entity;import androidx.room.Index;import androidx.room.PrimaryKey;
@Entity(tableName="error_records",indices={@Index(value={"createdAt"}),@Index(value={"cause"}),@Index(value={"wordId"})})
public class ErrorRecordEntity{
 @PrimaryKey(autoGenerate=true) public long id;public long createdAt;public int wordId;@NonNull public String mode="";@NonNull public String cause="";@NonNull public String expected="";@NonNull public String actual="";@NonNull public String detail="";
}

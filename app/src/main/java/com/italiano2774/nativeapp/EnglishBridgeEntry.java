package com.italiano2774.nativeapp;

/** Curated Italian-English memory bridge shown only after the learner reveals an answer. */
public class EnglishBridgeEntry {
    public String italian;
    public String english;
    public String kind;
    public String note;

    public boolean isFalseFriend(){return "false_friend".equals(kind);}

    public String displayText(){
        if(isFalseFriend()) return "⚠️ 英意假朋友  "+italian+" ≠ "+english+"\n"+note;
        return "🇬🇧 英语助记  "+italian+" ↔ "+english+"\n"+note;
    }
}

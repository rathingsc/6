package com.italiano2774.nativeapp;

/** Stable error categories used by diagnosis, Room history and adaptive routing. */
public final class ErrorCause {
    private ErrorCause() {}
    public static final String MEANING_CONFUSION="meaning_confusion";
    public static final String LISTENING_CONFUSION="listening_confusion";
    public static final String SPELLING="spelling";
    public static final String ACCENT="accent";
    public static final String WORD_FORM="word_form";
    public static final String ARTICLE_GENDER="article_gender";
    public static final String WORD_ORDER="word_order";
    public static final String OMISSION="omission";
    public static final String PRONUNCIATION="pronunciation";
    public static final String GRAMMAR="grammar";
    public static final String RECALL="recall";

    public static String label(String key){
        if(key==null)return "未分类";
        switch(key){
            case MEANING_CONFUSION:return "词义混淆";
            case LISTENING_CONFUSION:return "听音辨词";
            case SPELLING:return "拼写错误";
            case ACCENT:return "重音/符号";
            case WORD_FORM:return "词形/动词变位";
            case ARTICLE_GENDER:return "冠词/阴阳性";
            case WORD_ORDER:return "词序";
            case OMISSION:return "漏词";
            case PRONUNCIATION:return "发音/口语";
            case GRAMMAR:return "语法";
            default:return "回忆失败";
        }
    }
}

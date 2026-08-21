package com.italiano2774.nativeapp;

import java.util.Locale;

/**
 * Central guard for learner-facing examples.
 * Early dataset versions filled missing examples with self-referential templates such as
 * "Oggi ripasso la parola ...". Those are technically sentences but teach no real usage.
 * v3.0.2 treats them as missing data instead of turning them into exercises.
 */
public final class ExampleQuality {
    private ExampleQuality() {}

    private static final String[] BAD_IT_PREFIXES={
            "Oggi ripasso la parola «",
            "Ripeto l'espressione «"
    };

    private static final String[] BAD_ZH_MARKERS={
            "人/角色","这个概念","这个商店、地点或工作人员","这个颜色的型号",
            "这个身体部位不舒服或疼","这个自然景观或植物","这种动物",
            "这项手续或规定","这个学习内容","这个环境或自然话题","这个健康问题",
            "这个家居或住房相关项目","这个设备、软件或网络项目","这个购物项目",
            "这个日常项目","这个工作或学习项目","这个交通工具或设施",
            "这个尺寸、颜色或材质","这个治疗或医疗项目","这个身体部位或指标",
            "这个项目的信息","这种情绪或人际话题","这种食物或饮品","这种状态",
            "这个群体或社会概念","这个信息、材料或工具","这个结果看起来具有这种特点",
            "这个东西具有这种特征"
    };
    public static boolean isUsable(Word w){
        if(w==null)return false;
        String it=s(w.example),zh=s(w.exampleZh),target=s(w.word);
        if(it.isEmpty()||zh.isEmpty()||target.isEmpty())return false;
        if(isBoilerplate(it,zh))return false;
        return containsTarget(it,target);
    }

    public static boolean isBoilerplate(String it,String zh){
        String i=s(it),z=s(zh);
        for(String p:BAD_IT_PREFIXES)if(i.startsWith(p))return true;
        if(z.equals("我每天都努力把这件事做得更好一点。")||z.equals("做决定前，我会先认真想清楚。")||z.equals("做这件事时，冷静和表达清楚很重要。"))return true;
        for(String m:BAD_ZH_MARKERS)if(z.contains(m))return true;
        return false;
    }

    public static boolean containsTarget(String sentence,String target){
        String a=s(sentence).toLowerCase(Locale.ROOT),b=s(target).toLowerCase(Locale.ROOT);
        return !a.isEmpty()&&!b.isEmpty()&&a.contains(b);
    }

    private static String s(String x){return x==null?"":x.trim();}
}

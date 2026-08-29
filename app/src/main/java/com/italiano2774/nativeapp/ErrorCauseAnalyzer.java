package com.italiano2774.nativeapp;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lightweight local error diagnosis. No cloud/API is required. */
public final class ErrorCauseAnalyzer {
    private ErrorCauseAnalyzer() {}
    private static final List<String> ARTICLES=Arrays.asList("il","lo","la","l","i","gli","le","un","uno","una");

    public static String forChoice(String mode){
        if("listen".equals(mode)||"dictation".equals(mode))return ErrorCause.LISTENING_CONFUSION;
        if("confusion".equals(mode))return ErrorCause.MEANING_CONFUSION;
        return ErrorCause.MEANING_CONFUSION;
    }

    public static String forTypedWord(String expected,String actual,Word target,WordRepository repo){
        String e=basic(expected),a=basic(actual);if(a.isEmpty())return ErrorCause.RECALL;
        if(e.equals(a)&&!safe(expected).equalsIgnoreCase(safe(actual)))return ErrorCause.ACCENT;
        Word entered=repo==null?null:repo.lookupSurface(actual);
        if(target!=null&&entered!=null){
            String tl=safe(target.lemma).isEmpty()?target.word:target.lemma;
            String el=safe(entered.lemma).isEmpty()?entered.word:entered.lemma;
            if(!target.word.equalsIgnoreCase(entered.word)&&tl.equalsIgnoreCase(el))return ErrorCause.WORD_FORM;
        }
        int d=levenshtein(e,a);if(d<=Math.max(1,e.length()/5))return ErrorCause.SPELLING;
        return ErrorCause.RECALL;
    }

    public static SentenceAnalysis analyzeSentence(String expected,String actual,WordRepository repo){
        SentenceAnalysis out=new SentenceAnalysis();out.expected=safe(expected);out.actual=safe(actual);
        List<String> e=tokens(expected),a=tokens(actual);List<String> er=rawTokens(expected),ar=rawTokens(actual);if(e.isEmpty()){out.score=100;return out;}
        int lcs=lcs(e,a);out.score=(int)Math.max(0,Math.min(100,Math.round(100.0*lcs/Math.max(e.size(),a.size()))));
        Map<String,Integer> eb=bag(e),ab=bag(a);int missing=0,extra=0;for(String k:eb.keySet())missing+=Math.max(0,eb.get(k)-ab.getOrDefault(k,0));for(String k:ab.keySet())extra+=Math.max(0,ab.get(k)-eb.getOrDefault(k,0));
        if(missing>0){out.causes.add(ErrorCause.OMISSION);out.notes.add("漏词 "+missing+" 个");}
        boolean sameBag=missing==0&&extra==0&&e.size()==a.size();if(sameBag&&lcs<e.size()){out.causes.add(ErrorCause.WORD_ORDER);out.notes.add("单词基本齐全，但词序需要调整");}
        int articleErrors=0,formErrors=0,spellingErrors=0,accentErrors=0;
        int n=Math.min(e.size(),a.size());
        for(int i=0;i<n;i++){
            String ew=e.get(i),aw=a.get(i);if(ew.equals(aw)){if(i<er.size()&&i<ar.size()&&!er.get(i).equals(ar.get(i))&&basic(er.get(i)).equals(basic(ar.get(i))))accentErrors++;continue;}
            if(ARTICLES.contains(stripApostrophe(ew))||ARTICLES.contains(stripApostrophe(aw))){articleErrors++;continue;}
            String ebasic=basic(ew),abasic=basic(aw);if(ebasic.equals(abasic)){accentErrors++;continue;}
            Word we=repo==null?null:repo.lookupSurface(ew),wa=repo==null?null:repo.lookupSurface(aw);
            if(we!=null&&wa!=null){String le=safe(we.lemma).isEmpty()?we.word:we.lemma;String la=safe(wa.lemma).isEmpty()?wa.word:wa.lemma;if(!we.word.equalsIgnoreCase(wa.word)&&le.equalsIgnoreCase(la)){formErrors++;continue;}}
            if(levenshtein(ebasic,abasic)<=Math.max(1,ebasic.length()/5))spellingErrors++;
        }
        if(articleErrors>0){out.causes.add(ErrorCause.ARTICLE_GENDER);out.notes.add("冠词/阴阳性 "+articleErrors+" 处");}
        if(formErrors>0){out.causes.add(ErrorCause.WORD_FORM);out.notes.add("词形/变位 "+formErrors+" 处");}
        if(accentErrors>0){out.causes.add(ErrorCause.ACCENT);out.notes.add("重音或符号 "+accentErrors+" 处");}
        if(spellingErrors>0){out.causes.add(ErrorCause.SPELLING);out.notes.add("拼写接近但不准确 "+spellingErrors+" 处");}
        if(out.causes.isEmpty()&&out.score<100){out.causes.add(ErrorCause.RECALL);out.notes.add("句子内容与目标差异较大");}
        out.diff=buildDiff(e,a);
        return out;
    }

    private static String buildDiff(List<String> e,List<String> a){
        StringBuilder sb=new StringBuilder();int n=Math.max(e.size(),a.size());
        for(int i=0;i<n;i++){String ew=i<e.size()?e.get(i):"∅",aw=i<a.size()?a.get(i):"∅";if(i>0)sb.append("  ");sb.append(ew.equals(aw)?"✓ ":"✗ ").append(aw).append(ew.equals(aw)?"":" → "+ew);}return sb.toString();
    }
    private static String stripApostrophe(String s){return s.replace("'","");}
    private static String safe(String s){return s==null?"":s.trim();}
    public static String basic(String s){String n=Normalizer.normalize(safe(s).toLowerCase(Locale.ITALIAN).replace('’','\''),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return n.replaceAll("[^a-z0-9' ]","").replaceAll("\\s+"," ").trim();}

    private static List<String> rawTokens(String s){String n=safe(s).toLowerCase(Locale.ITALIAN).replace('’','\'').replaceAll("[^\\p{L}0-9' ]"," ").replaceAll("\\s+"," ").trim();return n.isEmpty()?new ArrayList<>():new ArrayList<>(Arrays.asList(n.split(" ")));}
    private static List<String> tokens(String s){String b=basic(s);return b.isEmpty()?new ArrayList<>():new ArrayList<>(Arrays.asList(b.split(" ")));}
    private static Map<String,Integer> bag(List<String> xs){Map<String,Integer> m=new HashMap<>();for(String x:xs)m.put(x,m.getOrDefault(x,0)+1);return m;}
    private static int lcs(List<String>a,List<String>b){int[][] d=new int[a.size()+1][b.size()+1];for(int i=1;i<=a.size();i++)for(int j=1;j<=b.size();j++)d[i][j]=a.get(i-1).equals(b.get(j-1))?d[i-1][j-1]+1:Math.max(d[i-1][j],d[i][j-1]);return d[a.size()][b.size()];}
    public static int levenshtein(String a,String b){int n=a.length(),m=b.length();int[] p=new int[m+1],c=new int[m+1];for(int j=0;j<=m;j++)p[j]=j;for(int i=1;i<=n;i++){c[0]=i;for(int j=1;j<=m;j++){int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;c[j]=Math.min(Math.min(c[j-1]+1,p[j]+1),p[j-1]+cost);}int[] t=p;p=c;c=t;}return p[m];}

    public static class SentenceAnalysis{
        public int score;public String expected="",actual="",diff="";public final List<String> causes=new ArrayList<>();public final List<String> notes=new ArrayList<>();
        public String primaryCause(){return causes.isEmpty()?ErrorCause.RECALL:causes.get(0);}
        public String summary(){return notes.isEmpty()?(score>=95?"很好，句子基本完整。":"继续再听一遍。"):String.join(" · ",notes);}
    }
}

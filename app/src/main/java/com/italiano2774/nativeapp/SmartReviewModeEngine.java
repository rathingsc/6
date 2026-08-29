package com.italiano2774.nativeapp;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * v3.3 chooses the most useful recall channel for each word.
 * New words begin with recognition; reviewed words are routed toward their weakest skill.
 */
public final class SmartReviewModeEngine {
    public static final int MODE_IT_ZH = 0;
    public static final int MODE_ZH_IT = 1;
    public static final int MODE_LISTENING = 2;
    public static final int MODE_CLOZE = 3;

    private SmartReviewModeEngine() {}

    public static int choose(Word w, ProgressStore progress, WordRepository repo) {
        if (w == null || progress.attempts(w.id) == 0) return MODE_IT_ZH;
        // v4.9: when one memory channel is actually due, train that channel first.
        // If nothing is due yet, fall back to the weakest mastery dimension.
        int weak = progress.priorityReviewDimension(w.id, java.time.LocalDate.now());
        int preferred = modeForDimension(weak);
        int rotate = Math.floorMod(w.id + progress.attempts(w.id), 4);
        int[] candidates = {preferred, rotate, MODE_IT_ZH, MODE_LISTENING, MODE_ZH_IT, MODE_CLOZE};
        for (int mode : candidates) if (isAvailable(mode, w, repo)) return mode;
        return MODE_IT_ZH;
    }

    public static int modeForDimension(int dim) {
        switch (dim) {
            case ProgressStore.DIM_LISTENING: return MODE_LISTENING;
            case ProgressStore.DIM_SPELLING: return MODE_ZH_IT;
            case ProgressStore.DIM_SPEAKING: return MODE_CLOZE;
            default: return MODE_IT_ZH;
        }
    }

    public static int dimensionForMode(int mode) {
        switch (mode) {
            case MODE_LISTENING: return ProgressStore.DIM_LISTENING;
            case MODE_ZH_IT: return ProgressStore.DIM_SPELLING;
            case MODE_CLOZE: return ProgressStore.DIM_SPEAKING;
            default: return ProgressStore.DIM_MEANING;
        }
    }

    public static String modeLabel(int mode) {
        switch (mode) {
            case MODE_ZH_IT: return "中 → 意主动回忆";
            case MODE_LISTENING: return "听音 → 认词";
            case MODE_CLOZE: return "句子 → 会使用";
            default: return "意 → 中识义";
        }
    }

    public static String dimensionLabel(int dim) {
        switch (dim) {
            case ProgressStore.DIM_LISTENING: return "听力";
            case ProgressStore.DIM_SPELLING: return "主动回忆";
            case ProgressStore.DIM_SPEAKING: return "使用";
            default: return "认词";
        }
    }

    public static boolean isAvailable(int mode, Word w, WordRepository repo) {
        if (w == null) return false;
        if (mode == MODE_ZH_IT) return repo != null && repo.isChinesePromptUnique(w);
        if (mode == MODE_CLOZE) return canCloze(w);
        return true;
    }

    public static boolean canCloze(Word w) {
        if (w == null || !ExampleQuality.isUsable(w) || w.word == null || w.word.trim().isEmpty()) return false;
        String regex = "(?i)(?<![\\p{L}’'])" + Pattern.quote(w.word.trim()) + "(?![\\p{L}’'])";
        return Pattern.compile(regex).matcher(w.example == null ? "" : w.example).find();
    }

    public static String clozeSentence(Word w) {
        if (!canCloze(w)) return "";
        String regex = "(?i)(?<![\\p{L}’'])" + Pattern.quote(w.word.trim()) + "(?![\\p{L}’'])";
        return (w.example == null ? "" : w.example).replaceFirst(regex, "____");
    }

    /** Smart-memory display keeps noun articles attached so gender becomes part of the word memory. */
    public static String studyForm(Word w) {
        if (w == null) return "";
        String word = safe(w.word);
        if ("noun".equalsIgnoreCase(safe(w.partOfSpeech)) && !safe(w.article).isEmpty()) {
            String article = safe(w.article);
            return article.endsWith("'") || article.endsWith("’") ? article + word : article + " " + word;
        }
        return word;
    }

    public static String answerNote(Word w) {
        if (w == null) return "";
        if ("noun".equalsIgnoreCase(safe(w.partOfSpeech)) && !safe(w.article).isEmpty()) return "名词请把冠词一起记住";
        if (ItalianGrammar.isVerb(w)) {
            String lemma = safe(w.lemma);
            if (lemma.isEmpty()) lemma = safe(w.word);
            return "动词与原形一起记：" + lemma;
        }
        return "";
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}

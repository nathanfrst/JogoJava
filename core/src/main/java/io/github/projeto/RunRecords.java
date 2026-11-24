package io.github.projeto;

import com.badlogic.gdx.Preferences;
import java.util.Locale;

/** Personal records live in LibGDX preferences, independently of a restarted run. */
public final class RunRecords {
    public static final String PREFERENCES_NAME = "JogoJava-records";
    private static final String SCORE_KEY = "bestScore";
    private static final String TIME_KEY = "bestSurvivalSeconds";
    private final Preferences preferences;
    private int bestScore;
    private long bestSurvivalSeconds;

    public RunRecords(Preferences preferences) {
        this.preferences = preferences;
        bestScore = Math.max(0, preferences.getInteger(SCORE_KEY, 0));
        bestSurvivalSeconds = Math.max(0L, preferences.getLong(TIME_KEY, 0L));
    }

    /** Save only improvements. Returns whether this run beat the score record. */
    public boolean submit(int score, double survivalSeconds) {
        boolean improvedScore = score > bestScore;
        long seconds = Double.isFinite(survivalSeconds) ? (long)Math.max(0d, survivalSeconds) : 0L;
        boolean improvedTime = seconds > bestSurvivalSeconds;
        if (!improvedScore && !improvedTime) return false;
        if (improvedScore) bestScore = score;
        if (improvedTime) bestSurvivalSeconds = seconds;
        preferences.putInteger(SCORE_KEY, bestScore);
        preferences.putLong(TIME_KEY, bestSurvivalSeconds);
        preferences.flush();
        return improvedScore;
    }

    public int getBestScore() { return bestScore; }
    public long getBestSurvivalSeconds() { return bestSurvivalSeconds; }
    public String getBestTimeDisplay() {
        return String.format(Locale.ROOT, "%02d:%02d", bestSurvivalSeconds / 60, bestSurvivalSeconds % 60);
    }
}

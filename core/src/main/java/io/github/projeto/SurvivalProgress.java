package io.github.projeto;

import java.util.Locale;

/** Simulation time drives both the survival HUD and the gradual spawn difficulty. */
public final class SurvivalProgress {
    public static final float INITIAL_SPAWN_INTERVAL=.75f;
    public static final float MIN_SPAWN_INTERVAL=.25f;
    public static final float RAMP_SECONDS=300f;
    private double elapsedSeconds;
    private long displayedSeconds=-1;
    private String displayTime="00:00";

    public void update(float deltaTime) {
        if(Float.isFinite(deltaTime)&&deltaTime>0)elapsedSeconds+=deltaTime;
    }
    public void reset() {
        elapsedSeconds=0;displayedSeconds=-1;displayTime="00:00";
    }
    public double getElapsedSeconds() { return elapsedSeconds; }
    public float getSpawnInterval() {
        double progress=Math.min(1d,elapsedSeconds/RAMP_SECONDS);
        return (float)(INITIAL_SPAWN_INTERVAL-(INITIAL_SPAWN_INTERVAL-MIN_SPAWN_INTERVAL)*progress);
    }
    public String getDisplayTime() {
        long seconds=(long)elapsedSeconds;
        if(seconds!=displayedSeconds) {
            displayTime=String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);
            displayedSeconds=seconds;
        }
        return displayTime;
    }
}

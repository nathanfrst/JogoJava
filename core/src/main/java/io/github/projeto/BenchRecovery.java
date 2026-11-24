package io.github.projeto;

/** One recovery timer per run, shared by all benches, to prevent bench hopping. */
public final class BenchRecovery {
    public static final int COST=10;
    public static final float COOLDOWN_SECONDS=20f;
    private double remaining;

    public void update(float deltaTime) {
        if(Float.isFinite(deltaTime)&&deltaTime>0)remaining=Math.max(0,remaining-deltaTime);
    }
    public void reset() { remaining=0; }
    public float getRemaining() { return (float)remaining; }
    public boolean canUse(Player player,int score) {
        return player.isAlive() && player.getHealth()<player.getMaxHealth() && remaining<=0 && score>=COST;
    }
    /** Returns the score to deduct only when a heal actually succeeded. */
    public int tryHeal(Player player,int score) {
        if(!canUse(player,score)||!player.healToFull())return 0;
        remaining=COOLDOWN_SECONDS;
        return COST;
    }
    public String prompt(Player player,int score) {
        if(remaining>0)return "Recarga: "+(int)Math.ceil(remaining)+"s";
        if(player.getHealth()>=player.getMaxHealth())return "Vida cheia";
        if(score<COST)return "Curar: 10 pontos (saldo: "+score+")";
        return "Curar: 10 pontos";
    }
}

package io.github.projeto;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Player extends GameObject {
    private static final int LIFE = 5;
    private static final float ATTACK_COOLDOWN = 1.6f;
    private Array<Enemy> targetEnemies;
    private Array<Obstacle> targetObstacles;
    private final com.badlogic.gdx.math.Rectangle targetView=new com.badlogic.gdx.math.Rectangle();
    private boolean limitToView;
    private final ObstacleWorld obstacleWorld;
    private final PlayerAnimations visuals;
    private final PlayerMotion motion = new PlayerMotion();
    private float life = LIFE;
    private final Vector2 attackDirection = new Vector2(0,-1);
    private float attackTimer;
    private final Array<Attack> attacks = new Array<>();
    private final Animation<Texture> attackAnimation;
    private final Sound attackSfx;

    public Player(float x,float y,PlayerAnimations visuals,Animation<Texture> attackAnimation,
                  Sound slashSfx,ObstacleWorld obstacleWorld) {
        // Keep the existing gameplay footprint when swapping the artwork.
        super(x,y,20/32f,26/32f);
        this.visuals=visuals;
        this.attackAnimation=attackAnimation;
        this.attackSfx=slashSfx;
        this.obstacleWorld=obstacleWorld;
    }
    public void reset(float x,float y) {
        rect.setPosition(x,y);
        life=LIFE;
        attackTimer=ATTACK_COOLDOWN;
        attacks.clear();
        targetEnemies=null;targetObstacles=null;limitToView=false;
        motion.reset();
        attackDirection.set(0,-1);
    }
    @Override void update(float deltaTime) {
        motion.step(deltaTime);
        if(isDead())return;
        obstacleWorld.move(rect,motion.getDisplacementX(),motion.getDisplacementY());
        GameObject target=AttackTargeting.closest(rect,targetEnemies,targetObstacles,limitToView?targetView:null);
        attackTimer=Math.max(0f,attackTimer-deltaTime);
        if(target!=null&&attackTimer<=0) {
            attackDirection.set(target.rect.x+target.rect.width/2f-rect.x-rect.width/2f,
                target.rect.y+target.rect.height/2f-rect.y-rect.height/2f);
            if(attackDirection.isZero())attackDirection.set(motion.getLastX(),motion.getLastY());
            attackDirection.nor();
            attackTimer=ATTACK_COOLDOWN;
            attackSfx.play();
            attacks.add(new Attack(getCenter(TMP_VEC2),attackDirection,attackAnimation,rect));
        }
        var iterator=attacks.iterator();
        while(iterator.hasNext()) {
            Attack attack=iterator.next();attack.update(deltaTime);
            if(attack.isDone())iterator.remove();
        }
    }
    public boolean requestDash() { return isAlive()&&motion.requestDash(); }
    public PlayerMotion getMotion() { return motion; }
    public boolean isDeathAnimationFinished() { return motion.isDeathFinished(); }
    public void aimAtNearestEnemy(Array<Enemy> enemies) { aimAtTargets(enemies,null); }
    public void aimAtTargets(Array<Enemy> enemies,Array<Obstacle> obstacles) {
        aimAtTargets(enemies,obstacles,null);
    }
    public void aimAtTargets(Array<Enemy> enemies,Array<Obstacle> obstacles,
                             com.badlogic.gdx.math.Rectangle visibleArea) {
        targetEnemies=enemies;targetObstacles=obstacles;
        limitToView=visibleArea!=null;
        if(limitToView)targetView.set(visibleArea);
    }
    public void changeDirection(Vector2 direction) { motion.setDirection(direction.x,direction.y); }
    public void subLife(float amount) {
        if(isDead())return;
        life=Math.max(0f,life-amount);
        if(isDead()) { motion.die();attacks.clear(); }
    }
    public float getHealth() { return Math.max(0f,Math.min(life,LIFE)); }
    public float getMaxHealth() { return LIFE; }
    public float getHealthRatio() { return getHealth()/LIFE; }
    public boolean healToFull() {
        if(!isAlive() || life>=LIFE)return false;
        life=LIFE;
        return true;
    }
    public Object getLife() { return getHealth(); }
    public boolean isAlive() { return life>0f; }
    public boolean isDead() { return life<=0f; }
    ObstacleWorld getObstacleWorld() { return obstacleWorld; }
    public Array<Attack> getAttacks() { return attacks; }
    public void drawShadow(Batch batch) { visuals.drawShadow(batch,rect,motion); }
    @Override public void draw(Batch batch) { visuals.draw(batch,rect,motion); }
}

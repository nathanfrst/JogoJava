package io.github.projeto;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;

public class Attack extends GameObject{
    private static final float DURATION = 0.4f;
    private static final float SIZE = 1f;

    private float lifeSpan = DURATION;
    private final Animation<Texture> animation;
    private final float originX,originY;

    public Attack(Vector2 position, Vector2 direction, Animation<Texture> animation){
        this(position,direction,animation,null);
    }
    public Attack(Vector2 position,Vector2 direction,Animation<Texture> animation,com.badlogic.gdx.math.Rectangle source){
        float spawnX = position.x - SIZE / 2 + direction.x * SIZE * 0.75f;
        float spawnY = position.y - SIZE / 2 + direction.y * SIZE * 0.75f;
        super(spawnX, spawnY, SIZE, SIZE);
        this.animation = animation;
        originX=source==null?position.x:source.x+source.width/2f;
        originY=source==null?position.y:source.y+Math.min(.05f,source.height/2f);
    }


    @Override
    void update(float deltaTime){
        this.lifeSpan -= deltaTime;
    }

    public boolean canHit(GameObject target) {
        return !isDone() && overlaps(target) && CombatGeometry.canReach(originX,originY,target.rect);
    }

    public boolean isDone(){
        return lifeSpan <= 0;
    }

    @Override
    public void draw(Batch batch){
        float animationDuration = animation.getAnimationDuration();
        float animationPerc = 1f - (Math.max(0f, lifeSpan / DURATION));
        float stateTime = animationDuration * animationPerc;

        texture = animation.getKeyFrame(stateTime, false);
        super.draw(batch);
    }

}

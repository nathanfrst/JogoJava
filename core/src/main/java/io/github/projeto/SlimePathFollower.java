package io.github.projeto;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Each slime retains its waypoint, while route searches are shared by the whole group. */
public final class SlimePathFollower {
    private final SlimeNavigation navigation;
    private final ObstacleWorld world;
    private final Vector2 waypoint=new Vector2();
    private boolean hasWaypoint;
    private float retryTimer;
    private float replanTimer;
    public SlimePathFollower(SlimeNavigation navigation,ObstacleWorld world) {
        this.navigation=navigation;this.world=world;
    }
    public void update(Rectangle body,float deltaTime,float speed) {
        retryTimer-=deltaTime;
        replanTimer-=deltaTime;
        if(replanTimer<=0) { hasWaypoint=false;replanTimer=.45f; }
        float remaining=speed*deltaTime;
        for(int iteration=0;iteration<16&&remaining>0;iteration++) {
            float cx=body.x+body.width/2f,cy=body.y+body.height/2f;
            if(!hasWaypoint) {
                if(retryTimer>0)return;
                hasWaypoint=navigation.nextWaypoint(body,waypoint);
                if(!hasWaypoint) { retryTimer=.3f;return; }
            }
            float dx=waypoint.x-cx,dy=waypoint.y-cy;
            float distance=(float)Math.sqrt(dx*dx+dy*dy);
            // Finish tiny alignment moves: skipping them can leave a body below a
            // narrow clearance edge even though the next cell center is valid.
            if(distance<.00001f) { hasWaypoint=false;continue; }
            float step=Math.min(remaining,distance);
            float oldX=body.x,oldY=body.y;
            world.move(body,dx/distance*step,dy/distance*step);
            remaining-=step;
            if(Vector2.dst2(oldX,oldY,body.x,body.y)<step*step*.1f) {
                hasWaypoint=false;retryTimer=.15f;return;
            }
            if(step>=distance)hasWaypoint=false;
        }
    }
}

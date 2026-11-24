package io.github.projeto;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** Contextual interaction measured from the player's body to a bench's solid base. */
public final class BenchHealing {
    public static final float RANGE=1.2f;
    private static final float LEVEL_TOLERANCE=.15f;
    private static final float SIGHT_SIZE=.04f;
    private BenchHealing() {}

    public static Obstacle findNearby(Rectangle playerBody,Array<Obstacle> obstacles) {
        Obstacle nearest=null;
        float nearestDistance=Float.POSITIVE_INFINITY;
        float startX=playerBody.x+playerBody.width/2f;
        float startY=playerBody.y+.05f;
        float scale=TerrainLayout.TILE_SIZE;
        float playerLevel=RuinLayout.elevation(startX/scale,startY/scale);
        for(int index=0;index<obstacles.size;index++) {
            Obstacle obstacle=obstacles.get(index);
            if(obstacle.getKind()!=Obstacle.Kind.BENCH)continue;
            Rectangle bench=obstacle.rect;
            float dx=Math.max(0f,Math.max(bench.x-playerBody.x-playerBody.width,
                playerBody.x-bench.x-bench.width));
            float dy=Math.max(0f,Math.max(bench.y-playerBody.y-playerBody.height,
                playerBody.y-bench.y-bench.height));
            float distance=dx*dx+dy*dy;
            if(distance>RANGE*RANGE||distance>=nearestDistance)continue;
            float benchLevel=RuinLayout.elevation((bench.x+bench.width/2f)/scale,(bench.y+.05f)/scale);
            if(Math.abs(playerLevel-benchLevel)>LEVEL_TOLERANCE)continue;
            float endX=MathUtils.clamp(startX,bench.x,bench.x+bench.width);
            float endY=MathUtils.clamp(startY,bench.y,bench.y+bench.height);
            if(!clearSight(startX,startY,endX,endY,obstacle,obstacles))continue;
            nearestDistance=distance;nearest=obstacle;
        }
        return nearest;
    }

    private static boolean clearSight(float startX,float startY,float endX,float endY,
                                      Obstacle bench,Array<Obstacle> obstacles) {
        float dx=endX-startX,dy=endY-startY;
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(dx),Math.abs(dy))/.05f));
        Rectangle probe=new Rectangle(0,0,SIGHT_SIZE,SIGHT_SIZE);
        for(int i=0;i<=steps;i++) {
            float fraction=(float)i/steps;
            probe.setPosition(startX+dx*fraction-SIGHT_SIZE/2f,startY+dy*fraction-SIGHT_SIZE/2f);
            if(RuinLayout.clipAxis(probe.x,probe.y,probe.width,probe.height,1f,true)!=probe.x)return false;
            for(int index=0;index<obstacles.size;index++) {
                Obstacle obstacle=obstacles.get(index);
                // Benches remain solid for movement, but their own base is the interaction target.
                if(obstacle==bench||!obstacle.isSolid())continue;
                if(obstacle.rect.overlaps(probe))return false;
            }
        }
        return true;
    }
}

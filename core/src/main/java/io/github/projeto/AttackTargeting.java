package io.github.projeto;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** Range and camera bounds gate automatic attacks; nearby chests remain valid targets. */
public final class AttackTargeting {
    public static final float SLIME_RANGE=2f;
    public static final float CHEST_RANGE=1.6f;
    private AttackTargeting() {}
    public static GameObject closest(Rectangle player,Array<? extends GameObject> enemies,
                                     Array<Obstacle> obstacles,Rectangle view) {
        float x=player.x+player.width/2f,y=player.y+player.height/2f;
        GameObject best=null;float nearest=Float.POSITIVE_INFINITY;
        if(enemies!=null)for(int i=0;i<enemies.size;i++) {
            GameObject enemy=enemies.get(i);
            if(view!=null&&!view.overlaps(enemy.rect))continue;
            float dx=enemy.rect.x+enemy.rect.width/2f-x,dy=enemy.rect.y+enemy.rect.height/2f-y;
            float distance=dx*dx+dy*dy;
            if(distance<=SLIME_RANGE*SLIME_RANGE&&distance<nearest&&CombatGeometry.canReach(player,enemy.rect)) {best=enemy;nearest=distance;}
        }
        if(obstacles!=null)for(int i=0;i<obstacles.size;i++) {
            Obstacle chest=obstacles.get(i);
            if(!chest.isBreakable()||view!=null&&!view.overlaps(chest.rect))continue;
            float dx=chest.rect.x+chest.rect.width/2f-x,dy=chest.rect.y+chest.rect.height/2f-y;
            float distance=dx*dx+dy*dy;
            if(distance<=CHEST_RANGE*CHEST_RANGE&&distance<nearest&&CombatGeometry.canReach(player,chest.rect)) {best=chest;nearest=distance;}
        }
        return best;
    }
}

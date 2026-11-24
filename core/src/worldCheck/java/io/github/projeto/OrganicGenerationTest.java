package io.github.projeto;
import com.badlogic.gdx.math.Rectangle;
import java.util.*;
public class OrganicGenerationTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        float scale=TerrainLayout.TILE_SIZE;
        Set<Integer> areas=new HashSet<>();int count=0;
        ObstacleWorld world=new ObstacleWorld(8,4.5f);
        for(int ry=-2;ry<=2;ry++)for(int rx=-2;rx<=2;rx++) {
            RuinLayout.Template region=RuinLayout.region(rx,ry);
            check(region.landmarks().length>0,"No landmarks");
            check(Arrays.stream(region.landmarks()).anyMatch(m->m.kind()==Obstacle.Kind.CHEST),"Missing chest "+rx+","+ry);
            for(RuinLayout.Terrace t:region.terraces()) {
                int area=0;for(boolean occupied:t.surface())if(occupied)area++;
                check(area<(t.right()-t.left())*(t.top()-t.front()-2),"Rectangular footprint "+rx+","+ry);
                areas.add(area);count++;
                float cx=(rx*64+(t.stairLeft()+t.stairRight())/2f)*scale;
                float start=(ry*64+t.front()-2)*scale;
                Rectangle body=new Rectangle(cx-.3125f,start,.625f,.8125f);
                world.move(body,0,6*scale);
                check(Math.abs(body.y-start-6*scale)<.002,"Blocked stair ascent "+rx+","+ry+" "+t.front()+" stopped "+body);
                world.move(body,0,-6*scale);
                check(Math.abs(body.y-start)<.002,"Blocked descent");
            }
        }
        check(areas.size()>15,"Insufficient size variation");
        world.dispose();System.out.println("PASS: "+count+" nonrectangular terraces, "+areas.size()+" sizes, stair traversal and authored chests");
    }
}

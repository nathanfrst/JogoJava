package io.github.projeto;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Runs without OpenGL or an audio device via Gradle checkWorldLayout. */
public class WorldLayoutTest {
    private static void require(boolean value,String message) {
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        float t=TerrainLayout.TILE_SIZE;
        ObstacleWorld world=new ObstacleWorld(8,4.5f);
        require(world.canStand(new Rectangle(8,4.5f,20/32f,26/32f)),"Spawn blocked");
        java.util.Set<Integer> variants=new java.util.HashSet<>();
        for(int ry=-2;ry<=2;ry++)for(int rx=-2;rx<=2;rx++) {
            variants.add(RuinLayout.variant(rx,ry));
            for(RuinLayout.Terrace terrace:RuinLayout.region(rx,ry).terraces()) {
                float x=(rx*64+(terrace.stairLeft()+terrace.stairRight())/2f)*t-.3125f;
                float startY=(ry*64+terrace.front()-2)*t;
                Rectangle climber=new Rectangle(x,startY,20/32f,26/32f);
                world.move(climber,0,6*t);
                require(Math.abs(climber.y-startY-6*t)<.002,"Cannot ascend variant stairs");
                world.move(climber,0,-6*t);
                require(Math.abs(climber.y-startY)<.002,"Cannot descend variant stairs");
            }
        }
        require(variants.size()==5,"Insufficient ruin variety");
        Rectangle body=new Rectangle(8*t,9*t,.625f,.8125f);
        world.move(body,0,5*t);
        require(body.y+body.height<=11*t+.001,"Tunneled through retaining wall");
        body.set(5*t,20*t,.625f,.8125f);
        world.move(body,-5*t,0);
        require(body.x>=3.3f*t-.001,"Passed through side wall");
        body.set(20*t,38*t,.625f,.8125f);
        world.move(body,0,5*t);
        require(body.y+body.height<=39.7f*t+.001,"Passed through north wall");
        require(RuinLayout.elevation(12,14)==1,"Wrong lower elevation");
        require(RuinLayout.elevation(25,31)==2,"Wrong upper elevation");
        require(RuinLayout.elevation(12,11)>0&&RuinLayout.elevation(12,11)<1,"Stairs must change elevation gradually");
        for(int y=-80;y<80;y++)for(int x=-80;x<80;x++) {
            require(!TerrainLayout.isPath(x,y)||!RuinLayout.isBlocked(x,y),"Road crosses a wall");
            for(int q=0;q<4;q++) {
                int source=TerrainLayout.pathQuarter(x,y,q);
                require((source&255)<256&&(source>>>8)>=128&&(source>>>8)<256,"Paver outside grass atlas");
            }
        }
        for(int ry=-2;ry<=2;ry++)for(int rx=-2;rx<=2;rx++) {
            int eastWest=0,northSouth=0;
            for(int y=1;y<10;y++)if(RuinLayout.isRoad(rx*64,ry*64+y)&&RuinLayout.isRoad(rx*64-1,ry*64+y))eastWest++;
            for(int x=54;x<62;x++)if(RuinLayout.isRoad(rx*64+x,ry*64)&&RuinLayout.isRoad(rx*64+x,ry*64-1))northSouth++;
            require(eastWest>=2,"Disconnected east/west road at region seam");
            require(northSouth>=2,"Disconnected north/south road at region seam");
        }
        Viewport view=new Viewport(){};
        view.setCamera(new OrthographicCamera());view.setWorldSize(20,14);
        for(int i=-10;i<=10;i++) {
            view.getCamera().position.set(i*8,i*4,0);
            world.refresh(view);
            for(Obstacle o:world.getObstacles()) {
                require(TerrainLayout.canPlace(o.rect.x,o.rect.y,o.rect.width,o.rect.height,
                    o.getKind().plantSheet),"Object on path, masonry or landing");
            }
        }
        view.getCamera().position.set(8,4.5f,0);world.refresh(view);
        String before=snapshot(world);
        view.getCamera().position.set(-200,200,0);world.refresh(view);
        view.getCamera().position.set(8,4.5f,0);world.refresh(view);
        require(before.equals(snapshot(world)),"Terrain changed after revisiting");
        world.dispose();
        OrganicGenerationTest.main(args);
        PlayerMotionTest.main(args);
        SurvivalProgressTest.main(args);
        RunRecordsTest.main(args);
        AttackTargetingTest.main(args);
        CombatWallsTest.main(args);
        HealingTest.main(args);
        BenchRecoveryTest.main(args);
        WorldPropsTest.main(args);
        SlimeNavigationTest.main(args);
        System.out.println("World layout checks passed.");
    }
    private static String snapshot(ObstacleWorld world) {
        StringBuilder result=new StringBuilder();
        for(Obstacle o:world.getObstacles())result.append(o.rect).append(o.getKind());
        return result.toString();
    }
}

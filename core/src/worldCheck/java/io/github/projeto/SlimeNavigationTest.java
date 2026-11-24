package io.github.projeto;
import com.badlogic.gdx.math.Rectangle;

public class SlimeNavigationTest {
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    private static void chase(ObstacleWorld world,Rectangle body,Rectangle target,String name) {
        SlimeNavigation nav=new SlimeNavigation(world,body.width,body.height);
        SlimePathFollower follower=new SlimePathFollower(nav,world);
        boolean lower=false,upper=false,arrived=false;
        long before=System.nanoTime();
        for(int frame=0;frame<3600;frame++) {
            nav.update(target,1/30f);
            follower.update(body,1/30f,1.5f);
            require(world.canStand(body),"Route crossed a solid: "+name+" "+body);
            float level=RuinLayout.elevation((body.x+body.width/2)/TerrainLayout.TILE_SIZE,
                (body.y+body.height/2)/TerrainLayout.TILE_SIZE);
            if(level>.2f&&level<.8f)lower=true;
            if(level>1.2f&&level<1.8f)upper=true;
            if(body.overlaps(target)) { arrived=true;break; }
        }
        require(arrived,"Chase failed: "+name+" stopped at "+body+" target "+target);
        if(name.equals("two terraces")||name.equals("small slime, two terraces"))require(lower&&upper,"Chase skipped stairs");
        System.out.println("Navigation passed: "+name+" ("+(System.nanoTime()-before)/1000000+" ms)");
    }
    public static void main(String[] args) {
        float t=TerrainLayout.TILE_SIZE;
        ObstacleWorld world=new ObstacleWorld(8,4.5f);
        chase(world,new Rectangle(12*t,9*t,Enemy.HITBOX_WIDTH,Enemy.HITBOX_HEIGHT),
            new Rectangle(25*t,32*t,.625f,.8125f),"small slime, two terraces");
        chase(world,new Rectangle(25*t,32*t,Enemy.HITBOX_WIDTH,Enemy.HITBOX_HEIGHT),
            new Rectangle(12*t,9*t,.625f,.8125f),"small slime descending both terraces");
        chase(world,new Rectangle(12*t,9*t,1,1),new Rectangle(25*t,32*t,.625f,.8125f),"two terraces");
        chase(world,new Rectangle(25*t,32*t,1,1),new Rectangle(12*t,9*t,.625f,.8125f),"descending both terraces");
        // The direct line intersects a retaining wall, forcing a detour to its staircase.
        chase(world,new Rectangle(6*t,6*t,1,1),new Rectangle(7*t,21*t,.625f,.8125f),"around wall");
        java.util.Set<Integer> tested=new java.util.HashSet<>();
        for(int ry=-2;ry<=2;ry++)for(int rx=-2;rx<=2;rx++) {
            if(!tested.add(RuinLayout.variant(rx,ry)))continue;
            RuinLayout.Terrace[] terraces=RuinLayout.region(rx,ry).terraces();
            if(terraces.length==2&&terraces[0].level()==terraces[1].level()) {
                RuinLayout.Terrace a=terraces[0],b=terraces[1];
                chase(world,new Rectangle((rx*64+a.stairLeft()+1)*t,(ry*64+a.front()+5)*t,1,1),
                    new Rectangle((rx*64+b.stairLeft()+1)*t,(ry*64+b.front()+5)*t,.625f,.8125f),
                    "between separate terraces "+RuinLayout.variant(rx,ry));
            }
            for(RuinLayout.Terrace terrace:terraces) {
                float cx=(rx*64+(terrace.stairLeft()+terrace.stairRight())/2f)*t;
                chase(world,new Rectangle(cx-.5f,(ry*64+terrace.front()-3)*t,1,1),
                    new Rectangle(cx-.3f,(ry*64+terrace.front()+5)*t,.625f,.8125f),"variant "+RuinLayout.variant(rx,ry));
            }
        }
        com.badlogic.gdx.utils.viewport.Viewport view=new com.badlogic.gdx.utils.viewport.Viewport(){};
        view.setCamera(new com.badlogic.gdx.graphics.OrthographicCamera());
        view.setWorldSize(40,30);view.getCamera().position.set(15,15,0);world.refresh(view);
        int detours=0;
        for(Obstacle obstacle:world.getObstacles()) {
            if(!obstacle.isSolid())continue;
            float cy=obstacle.rect.y+obstacle.rect.height/2f;
            Rectangle start=new Rectangle(obstacle.rect.x-1.2f,cy-.5f,1,1);
            Rectangle end=new Rectangle(obstacle.rect.x+obstacle.rect.width+.2f,cy-.5f,1,1);
            if(!world.canStand(start)||!world.canStand(end))continue;
            require(!world.canTravel(start,end.x+.5f,end.y+.5f),"Expected a blocked direct path");
            chase(world,start,end,"detour around "+obstacle.getKind());
            if(++detours==8)break;
        }
        require(detours>=5,"Insufficient obstacle detour coverage");
        // Moving destination must invalidate the reverse search without recreating the world.
        Rectangle target=new Rectangle(12*t,17*t,.625f,.8125f),body=new Rectangle(12*t,9*t,1,1);
        SlimeNavigation nav=new SlimeNavigation(world,1,1);
        SlimePathFollower follower=new SlimePathFollower(nav,world);
        boolean arrived=false;
        for(int frame=0;frame<2400;frame++) {
            if(frame==80)target.setPosition(25*t,32*t);
            nav.update(target,1/30f);follower.update(body,1/30f,1.5f);
            require(world.canStand(body),"Moving target caused collision");
            if(frame>80&&body.overlaps(target)) { arrived=true;break; }
        }
        require(arrived,"Did not replan for moving player");
        world.dispose();
        System.out.println("Slime navigation checks passed.");
    }
}

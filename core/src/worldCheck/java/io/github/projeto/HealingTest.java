package io.github.projeto;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public final class HealingTest {
    private static void require(boolean value,String message) {if(!value)throw new AssertionError(message);}
    private static void close(float actual,float expected,String message) {
        require(Math.abs(actual-expected)<.0001f,message+": "+actual);
    }
    public static void main(String[] args) {
        ObstacleWorld world=new ObstacleWorld(8,4.5f);
        Player player=new Player(8,4.5f,null,null,null,world);
        close(player.getHealth(),5,"Initial health");
        require(!player.healToFull(),"Healing full health should be a no-op");
        player.subLife(1.25f);
        close(player.getHealthRatio(),.75f,"Partial health ratio");
        require(player.healToFull(),"Injured living player was not healed");
        close(player.getHealth(),player.getMaxHealth(),"Healing did not reach maximum");
        require(!player.healToFull(),"Repeated healing exceeded maximum");
        player.subLife(100);
        require(player.isDead()&&!player.healToFull(),"Healing resurrected dead player");
        close(player.getHealthRatio(),0,"Dead health ratio");
        player.reset(8,4.5f);
        require(player.isAlive(),"Restart did not restore life");
        close(player.getHealthRatio(),1,"Restart did not restore health ratio");

        Rectangle body=new Rectangle(-6,-6,.625f,.8125f);
        Obstacle bench=new Obstacle(-4.5f,-6,Obstacle.Kind.BENCH);
        Array<Obstacle> obstacles=new Array<>();obstacles.add(bench);
        require(BenchHealing.findNearby(body,obstacles)==bench,"Nearby bench not found from side");
        Rectangle contact=new Rectangle(bench.rect.x-.625f,bench.rect.y,.625f,.8125f);
        require(BenchHealing.findNearby(contact,obstacles)==bench,"Bench's own solid base blocked interaction");
        Rectangle justOutside=new Rectangle(bench.rect.x-.625f-BenchHealing.RANGE-.01f,bench.rect.y,.625f,.8125f);
        require(BenchHealing.findNearby(justOutside,obstacles)==null,"Healed beyond interaction range");
        justOutside.x+=.02f;
        require(BenchHealing.findNearby(justOutside,obstacles)==bench,"Range edge rejected valid interaction");
        Obstacle farther=new Obstacle(-6,-4.05f,Obstacle.Kind.BENCH);
        obstacles.add(farther);
        require(BenchHealing.findNearby(body,obstacles)==bench,"Did not select nearest bench");
        Obstacle blocker=new Obstacle(-5.2f,-6,Obstacle.Kind.CRATE);
        obstacles.add(blocker);
        require(BenchHealing.findNearby(body,obstacles)==farther,"Blocked nearest bench hid reachable farther bench");
        obstacles.removeValue(farther,true);
        require(BenchHealing.findNearby(body,obstacles)==null,"Interaction passed through another solid prop");
        obstacles.clear();obstacles.add(new Obstacle(-4.5f,-6,Obstacle.Kind.CRATE));
        require(BenchHealing.findNearby(body,obstacles)==null,"Non-bench offered healing");

        float t=TerrainLayout.TILE_SIZE;
        RuinLayout.Terrace terrace=RuinLayout.region(0,0).terraces()[0];
        Obstacle elevated=new Obstacle((terrace.left()+.5f)*t,(terrace.front()+5)*t,Obstacle.Kind.BENCH);
        Rectangle outside=new Rectangle(terrace.left()*t-.7f,elevated.rect.y,.625f,.8125f);
        obstacles.clear();obstacles.add(elevated);
        require(BenchHealing.findNearby(outside,obstacles)==null,"Healed across a retaining wall/from another level");
        world.dispose();
        System.out.println("Bench healing checks passed: health caps/death/reset, range, side access, nearest reachable target, walls and levels.");
    }
}

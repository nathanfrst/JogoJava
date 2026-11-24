package io.github.projeto;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public final class CombatWallsTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static final class Slime extends GameObject {
        Slime(Rectangle r){super(r.x,r.y,r.width,r.height);}
        @Override void update(float delta){}
    }
    public static void main(String[] args) {
        float t=TerrainLayout.TILE_SIZE;
        Rectangle outside=new Rectangle(3*t-.625f-.03f,20*t,.625f,.8125f);
        Slime protectedSlime=new Slime(new Rectangle(3.4f*t,20*t,1,1));
        Obstacle protectedChest=new Obstacle(3.4f*t,20*t,Obstacle.Kind.CHEST);
        check(!CombatGeometry.canReach(outside,protectedSlime.rect),"Attack crossed thin retaining rail");
        check(!CombatGeometry.canReach(protectedSlime.rect,outside),"Wall only blocks in one direction");
        Array<GameObject> slimes=new Array<>();slimes.add(protectedSlime);
        Array<Obstacle> props=new Array<>();props.add(protectedChest);
        Rectangle view=new Rectangle(-5,-5,30,30);
        check(AttackTargeting.closest(outside,slimes,props,view)==null,"Auto aim selected protected enemy/chest");
        Attack attack=new Attack(outside.getCenter(new Vector2()),new Vector2(1,0),null,outside);
        check(attack.overlaps(protectedSlime)&&attack.overlaps(protectedChest),"Fixture must overlap both protected targets");
        check(!attack.canHit(protectedSlime)&&!attack.canHit(protectedChest),"Damage bypassed terrain despite valid hitbox");
        Slime reachable=new Slime(new Rectangle(outside.x-1f,outside.y,.5f,.5f));slimes.add(reachable);
        check(AttackTargeting.closest(outside,slimes,props,view)==reachable,"Blocked nearest hid reachable target");
        Rectangle a=new Rectangle(.8f*t,22*t,.3f,.4f),b=new Rectangle(6*t,28*t,.3f,.4f);
        check(RuinLayout.elevation((a.x+a.width/2)/t,(a.y+.05f)/t)==0
            &&RuinLayout.elevation((b.x+b.width/2)/t,(b.y+.05f)/t)==0,"Corner fixture must have same height");
        check(!CombatGeometry.canReach(a,b),"Same-height ray cut across corner of terrace");
        Rectangle clear=new Rectangle(8,4.5f,.625f,.8125f);
        Obstacle openChest=new Obstacle(8.8f,4.5f,Obstacle.Kind.CHEST);
        Attack clearAttack=new Attack(clear.getCenter(new Vector2()),new Vector2(1,0),null,clear);
        check(clearAttack.canHit(openChest),"Clear nearby chest cannot be hit");
        check(CombatGeometry.canReach(new Rectangle(12*t,11*t,.625f,.8125f),
            new Rectangle(12*t,12*t,.625f,.8125f)),"Attack along open stairs incorrectly blocked");
        float x=clear.x,y=clear.y;clear.setPosition(4,4);
        check(clearAttack.canHit(openChest),"Moving player changed the attack's fixed origin");
        ObstacleWorld world=new ObstacleWorld(8,4.5f);
        com.badlogic.gdx.utils.viewport.Viewport camera=new com.badlogic.gdx.utils.viewport.Viewport(){};
        camera.setCamera(new com.badlogic.gdx.graphics.OrthographicCamera());camera.setWorldSize(16,9);
        camera.getCamera().position.set(30*t,32*t,0);world.refresh(camera);
        Obstacle chest=null;for(Obstacle prop:world.getObstacles())if(prop.isBreakable()){chest=prop;break;}
        check(chest!=null,"Actual chest missing");
        // Hitbox intentionally overlaps a real chest while its stored source lies below the level.
        Rectangle source=new Rectangle(chest.rect.x,chest.rect.y-10,.625f,.8125f);
        Attack invalid=new Attack(chest.rect.getCenter(new Vector2()),Vector2.Zero,null,source);
        check(world.breakChests(invalid)==0,"Chest destruction path bypassed line-of-sight");
        check(world.breakChests(new Rectangle(chest.rect))==1,"Rejected hit incorrectly destroyed/persisted chest");
        world.dispose();
        System.out.println("Combat walls checks passed: target selection, thin rails, corners, damage, chest destruction, stairs and fixed origin.");
    }
}

package io.github.projeto;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import java.lang.reflect.Proxy;

public final class AttackTargetingTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static final class Target extends GameObject {
        Target(float x,float y,float width,float height){super(x,y,width,height);}
        @Override void update(float delta){}
    }
    public static void main(String[] args) {
        Rectangle body=new Rectangle(0,0,1,1),view=new Rectangle(-5,-5,10,10);
        Array<GameObject> enemies=new Array<>();Array<Obstacle> props=new Array<>();
        check(AttackTargeting.closest(body,enemies,props,view)==null,"No enemies still gave a target");
        Target far=new Target(5,0,1,1);enemies.add(far);
        check(AttackTargeting.closest(body,enemies,props,view)==null,"Distant slime triggered slash");
        Target nearby=new Target(1.2f,0,1,1);enemies.add(nearby);
        check(AttackTargeting.closest(body,enemies,props,view)==nearby,"Nearby visible slime not selected");
        view.set(0,0,1,1);
        check(AttackTargeting.closest(body,enemies,props,view)==null,"Off-camera nearby slime triggered slash");
        Target edge=new Target(.9f,0,.2f,.2f);enemies.add(edge);
        check(AttackTargeting.closest(body,enemies,props,view)==edge,"Partially visible slime ignored");
        enemies.clear();view.set(-5,-5,10,10);
        Obstacle chest=new Obstacle(1,0,Obstacle.Kind.CHEST);props.add(chest);
        check(AttackTargeting.closest(body,enemies,props,view)==chest,"Chest exception lost");
        props.clear();props.add(new Obstacle(1,0,Obstacle.Kind.BENCH));
        check(AttackTargeting.closest(body,enemies,props,view)==null,"Bench triggered slash");
        ObstacleWorld world=new ObstacleWorld(8,4.5f);int[] sounds={0};
        Sound sound=(Sound)Proxy.newProxyInstance(Sound.class.getClassLoader(),new Class<?>[]{Sound.class},(proxy,method,values)-> {
            if(method.getName().equals("play"))sounds[0]++;
            return method.getReturnType()==long.class?0L:null;
        });
        Player player=new Player(8,4.5f,null,null,sound,world);
        player.reset(8,4.5f);
        player.aimAtTargets(new Array<Enemy>(),new Array<>(),view);
        player.update(2f);
        check(player.getAttacks().size==0&&sounds[0]==0,"Empty slash/sound continued");
        props.clear();props.add(new Obstacle(8.8f,4.5f,Obstacle.Kind.CHEST));view.set(0,0,16,9);
        player.aimAtTargets(new Array<Enemy>(),props,view);player.update(.01f);
        check(player.getAttacks().size==1&&sounds[0]==1,"Ready slash did not hit nearby chest");
        player.update(.01f);check(sounds[0]==1,"Attack cooldown bypassed");
        world.dispose();
        PlayerMotion motion=new PlayerMotion();
        check(motion.getDashCooldown()==0&&motion.requestDash(),"Initial dash unavailable");
        motion.step(.6f);
        check(Math.abs(motion.getDashCooldown()-.6f)<.0001,"Dash meter does not follow cooldown");
        motion.step(.6f);check(motion.requestDash(),"Full dash meter not ready");
        motion.reset();check(motion.getDashCooldown()==0,"Restart did not restore dash bar");
        System.out.println("Attack targeting checks passed: range, viewport, edge visibility, chest exception, no idle slash/audio and dash timer.");
    }
}

package io.github.projeto;

public final class PlayerMotionTest {
    private static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
    private static void close(float actual,float expected,String message) {
        check(Math.abs(actual-expected)<.0002f,message+": "+actual+" expected "+expected);
    }
    public static void main(String[] args) {
        PlayerMotion motion=new PlayerMotion();
        motion.setDirection(1,1);
        motion.step(.5f);
        close((float)Math.hypot(motion.getDisplacementX(),motion.getDisplacementY()),1f,"Diagonal walking changed speed");
        check(motion.getFacing()==PlayerMotion.Facing.RIGHT_UP,"Diagonal facing");
        motion.setDirection(0,0);
        check(motion.getState()==PlayerMotion.State.IDLE,"Releasing input did not idle");
        check(motion.getFacing()==PlayerMotion.Facing.RIGHT_UP,"Idle lost facing");
        motion.reset();motion.setDirection(1,0);check(motion.requestDash(),"First dash rejected");
        motion.step(.14f);float first=motion.getDisplacementX();
        motion.setDirection(0,1);
        check(motion.getFacing()==PlayerMotion.Facing.RIGHT_DOWN,"Dash changed facing halfway");
        motion.step(.14f);
        close(first+motion.getDisplacementX(),2.24f,"Dash distance must be frame independent");
        close(motion.getDisplacementY(),0f,"Turning redirected active dash");
        check(!motion.requestDash(),"Dash ignored cooldown");
        motion.step(.93f);check(motion.requestDash(),"Dash did not recharge");
        motion.die();motion.step(.4f);
        close(motion.getDisplacementX(),0f,"Dead player moved");
        close(motion.getDisplacementY(),0f,"Dead player moved");
        check(!motion.requestDash(),"Dead player dashed");
        check(!motion.isDeathFinished(),"Death ended before final frame");
        motion.step(.5f);check(motion.isDeathFinished(),"Death never finished");
        check(PlayerAnimations.frameIndex(PlayerMotion.State.DEAD,motion.getStateTime())==7,"Death did not hold final frame");
        motion.step(10f);check(PlayerAnimations.frameIndex(PlayerMotion.State.DEAD,motion.getStateTime())==7,"Death looped");
        motion.reset();check(motion.getState()==PlayerMotion.State.IDLE,"Reset kept death state");
        check(motion.requestDash(),"Reset kept cooldown");
        motion.step(1f);
        close(motion.getDisplacementY(),-2.24f,"Stationary dash ignored last/default direction or exceeded duration");
        motion.reset();motion.setDirection(-1,0);motion.requestDash();motion.step(.4f);
        close(motion.getDisplacementX(),-(.28f*8+.12f*2),"Long frame changed dash distance");
        check(PlayerMotion.directionFacing(0,1)==PlayerMotion.Facing.UP,"Up facing");
        check(PlayerMotion.directionFacing(0,-1)==PlayerMotion.Facing.DOWN,"Down facing");
        check(PlayerMotion.directionFacing(-1,-1)==PlayerMotion.Facing.LEFT_DOWN,"Left-down facing");
        check(PlayerMotion.directionFacing(-1,1)==PlayerMotion.Facing.LEFT_UP,"Left-up facing");
        // Simulate sustained SD and AS input across a full walk cycle.
        for(int x:new int[]{-1,1}) {
            motion.reset();motion.setDirection(x,0);motion.step(.2f);
            motion.setDirection(x,-1);
            PlayerMotion.Facing expected=x<0?PlayerMotion.Facing.LEFT_DOWN:PlayerMotion.Facing.RIGHT_DOWN;
            java.util.Set<Integer> frames=new java.util.HashSet<>();
            for(int i=0;i<48;i++) {
                motion.setDirection(x,-1);motion.step(1/60f);
                check(motion.getState()==PlayerMotion.State.WALK&&motion.getFacing()==expected,"Downward diagonal lost walk/facing");
                check(Math.signum(motion.getDisplacementX())==x&&motion.getDisplacementY()<0,"Diagonal lost movement axis");
                frames.add(PlayerAnimations.frameIndex(motion.getState(),motion.getStateTime()));
            }
            check(frames.size()==8,"Diagonal did not play the full walk sequence");
        }
        // Adding the second key in either order must preserve the walking clock.
        for(boolean horizontalFirst:new boolean[]{true,false}) {
            motion.reset();motion.setDirection(horizontalFirst?-1:0,horizontalFirst?0:-1);
            motion.step(.23f);
            float before=motion.getStateTime();
            motion.setDirection(-1,-1);
            close(motion.getStateTime(),before,"Second key restarted walking animation");
            for(int i=0;i<40;i++){motion.setDirection(-1,-1);motion.step(1/60f);}
            check(motion.getFacing()==PlayerMotion.Facing.LEFT_DOWN,"Key order changed facing");
            check(motion.getStateTime()>before+.6f,"Key order froze animation time");
            before=motion.getStateTime();motion.setDirection(-1,0);
            close(motion.getStateTime(),before,"Releasing one key restarted walking animation");
        }
        for(boolean horizontalFirst:new boolean[]{true,false}) {
            motion.reset();motion.setDirection(horizontalFirst?-1:0,horizontalFirst?0:-1);
            for(int i=0;i<300;i++){motion.setDirection(horizontalFirst?-1:0,horizontalFirst?0:-1);motion.step(1/60f);}
            int before=motion.getWalkFrame();motion.setDirection(-1,-1);
            check(motion.getWalkFrame()==before,"Key transition changed walk frame");
            java.util.Set<Integer> actualFrames=new java.util.HashSet<>();
            for(int i=0;i<120;i++){motion.setDirection(-1,-1);motion.step(1/60f);actualFrames.add(motion.getWalkFrame());}
            check(actualFrames.size()==8,"Independent walk clock froze after adding second key");
        }
        System.out.println("Player motion checks passed.");
    }
}

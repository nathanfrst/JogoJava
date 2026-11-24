package io.github.projeto;

/** Input, facing and timed movement, independent of textures and rendering. */
public final class PlayerMotion {
    public enum State { IDLE, WALK, DASH, DEAD }
    /** These values match the row order in every character atlas. */
    public enum Facing { DOWN, LEFT_DOWN, LEFT_UP, UP, RIGHT_UP, RIGHT_DOWN }
    public static final float SPEED = 2f;
    public static final float DASH_SPEED = 8f;
    public static final float DASH_DURATION = .28f;
    public static final float DASH_COOLDOWN = 1.2f;
    public static final float DEATH_DURATION = .88f;
    private State state = State.IDLE;
    private Facing facing = Facing.DOWN;
    private float inputX, inputY, lastX, lastY = -1f;
    private float dashX, dashY, dashRemaining, dashCooldown, stateTime;
    private float displacementX, displacementY;
    private static final float WALK_FRAME_DURATION=.08f;
    private int walkFrame;
    private double walkFrameTime;

    public void reset() {
        state = State.IDLE;
        walkFrame=0;walkFrameTime=0;
        facing = Facing.DOWN;
        inputX = inputY = lastX = dashX = dashY = 0f;
        lastY = -1f;
        dashRemaining = dashCooldown = stateTime = displacementX = displacementY = 0f;
    }
    public void setDirection(float x, float y) {
        if (state == State.DEAD) return;
        float length = (float)Math.sqrt(x*x + y*y);
        inputX = length > 0 ? x/length : 0;
        inputY = length > 0 ? y/length : 0;
        if (state != State.DASH) {
            if (length > 0) {
                // Keep the walking cycle continuous when adding/releasing a direction key.
                lastX = inputX; lastY = inputY;
                facing = directionFacing(inputX,inputY);
            }
            changeState(length > 0 ? State.WALK : State.IDLE);
        }
    }
    public static Facing directionFacing(float x,float y) {
        if(x<0 && y<0)return Facing.LEFT_DOWN;
        if(x>0 && y<0)return Facing.RIGHT_DOWN;
        if (Math.abs(x) < Math.abs(y)*.58f) return y > 0 ? Facing.UP : Facing.DOWN;
        if (x < 0) return y > 0 ? Facing.LEFT_UP : Facing.LEFT_DOWN;
        return y > 0 ? Facing.RIGHT_UP : Facing.RIGHT_DOWN;
    }
    public boolean requestDash() {
        if (state == State.DEAD || state == State.DASH || dashCooldown > 0f) return false;
        dashX = lastX; dashY = lastY;
        dashRemaining = DASH_DURATION;
        dashCooldown = DASH_COOLDOWN;
        changeState(State.DASH);
        return true;
    }
    public void step(float deltaTime) {
        float delta = Math.max(0f,deltaTime);
        displacementX = displacementY = 0f;
        stateTime += delta;
        if (state == State.DEAD) return;
        dashCooldown = Math.max(0f,dashCooldown-delta);
        if (state == State.DASH) {
            float dashDelta = Math.min(delta,dashRemaining);
            displacementX = dashX*DASH_SPEED*dashDelta;
            displacementY = dashY*DASH_SPEED*dashDelta;
            dashRemaining = Math.max(0f,dashRemaining-dashDelta);
            delta -= dashDelta;
            if (dashRemaining <= .00001f) {
                dashRemaining = 0f;
                if (inputX != 0 || inputY != 0) {
                    lastX = inputX; lastY = inputY;
                    facing = directionFacing(lastX,lastY);
                }
                changeState(inputX != 0 || inputY != 0 ? State.WALK : State.IDLE);
                stateTime = delta;
            }
        }
        if(state==State.WALK) {
            // Only time advances the walk frames. Key order and facing never reset this clock.
            walkFrameTime+=delta;
            int advances=(int)((walkFrameTime+1e-9)/WALK_FRAME_DURATION);
            if(advances>0) {
                walkFrame=(walkFrame+advances)%8;
                walkFrameTime-=advances*WALK_FRAME_DURATION;
            }
        }
        if (state != State.DASH) {
            displacementX += inputX*SPEED*delta;
            displacementY += inputY*SPEED*delta;
        }
    }
    private void changeState(State next) {
        if (state != next) { state=next;stateTime=0f; }
    }
    public void die() {
        if (state == State.DEAD) return;
        changeState(State.DEAD);
        inputX = inputY = dashRemaining = displacementX = displacementY = 0f;
    }
    public int getWalkFrame() { return walkFrame; }
    public State getState() { return state; }
    public Facing getFacing() { return facing; }
    public float getStateTime() { return stateTime; }
    public float getDashRemaining() { return dashRemaining; }
    public float getDashCooldown() { return dashCooldown; }
    public float getDisplacementX() { return displacementX; }
    public float getDisplacementY() { return displacementY; }
    public float getLastX() { return lastX; }
    public float getLastY() { return lastY; }
    public boolean isDeathFinished() { return state == State.DEAD && stateTime >= DEATH_DURATION; }
}

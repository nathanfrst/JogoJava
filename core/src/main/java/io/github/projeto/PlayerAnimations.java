package io.github.projeto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/** One texture per atlas, with six directional rows and eight 48x64 frames. */
public final class PlayerAnimations implements Disposable {
    public static final int FRAME_WIDTH = 48, FRAME_HEIGHT = 64, FRAME_COUNT = 8;
    private static final float PIXEL_SCALE = 1f/32f;
    private static final float FOOT_X = 24f, FOOT_Y = 43f;
    private final Array<Texture> textures = new Array<>();
    private final TextureRegion[][] idle = load("Idle/idle.png",6);
    private final TextureRegion[][] walk = loadWalkDirections();
    private final TextureRegion[][] dash = load("Dash/Dash.png",6);
    private final TextureRegion[][] death = load("Death/death_normal.png",6);
    private final TextureRegion[][] dust = load("Dash/Dust/Dash_Dust.png",6);
    private final TextureRegion[][] deathShadow = load("Death Shadow/death_normal.png",6);
    private final TextureRegion[][] dashShadow = load("Dash_Shadow.png",1);
    private final TextureRegion shadow = loadSingle("Shadow.png");

    private TextureRegion[][] loadWalkDirections() {
        // Bind the named strips explicitly, including the two downward diagonals.
        String[] names={"down","left_down","left_up","up","right_up","right_down"};
        TextureRegion[][] result=new TextureRegion[names.length][];
        for(int row=0;row<names.length;row++) {
            result[row]=load("Walk/walk_"+names[row]+".png",1)[0];
        }
        return result;
    }

    private TextureRegion[][] load(String relative,int rows) {
        Texture texture = texture(relative);
        if (texture.getWidth() != FRAME_WIDTH*FRAME_COUNT || texture.getHeight() != FRAME_HEIGHT*rows)
            throw new IllegalArgumentException("Unexpected player atlas dimensions: "+relative);
        return TextureRegion.split(texture,FRAME_WIDTH,FRAME_HEIGHT);
    }
    private TextureRegion loadSingle(String relative) {
        Texture texture=texture(relative);
        if (texture.getWidth()!=FRAME_WIDTH || texture.getHeight()!=FRAME_HEIGHT)
            throw new IllegalArgumentException("Unexpected player shadow dimensions: "+relative);
        return new TextureRegion(texture);
    }
    private Texture texture(String relative) {
        Texture texture=new Texture(Gdx.files.internal("Player/"+relative));
        texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        textures.add(texture);
        return texture;
    }
    public static int frameIndex(PlayerMotion.State state,float time) {
        float duration = switch(state) {
            case IDLE -> 1.12f;
            case WALK -> .64f;
            case DASH -> PlayerMotion.DASH_DURATION;
            case DEAD -> PlayerMotion.DEATH_DURATION;
        };
        int frame=(int)(Math.max(0f,time)/duration*FRAME_COUNT);
        return state==PlayerMotion.State.DEAD || state==PlayerMotion.State.DASH
            ? Math.min(frame,FRAME_COUNT-1) : frame%FRAME_COUNT;
    }
    public TextureRegion getFrame(PlayerMotion motion) {
        int row=motion.getFacing().ordinal(),frame=frameIndex(motion.getState(),motion.getStateTime());
        return switch(motion.getState()) {
            case IDLE -> idle[row][frame];
            case WALK -> walk[row][motion.getWalkFrame()];
            case DASH -> dash[row][frame];
            case DEAD -> death[row][frame];
        };
    }
    private void drawFrame(Batch batch,TextureRegion frame,Rectangle body) {
        batch.draw(frame,body.x+body.width/2f-FOOT_X*PIXEL_SCALE,
            body.y-(FRAME_HEIGHT-FOOT_Y)*PIXEL_SCALE,
            FRAME_WIDTH*PIXEL_SCALE,FRAME_HEIGHT*PIXEL_SCALE);
    }
    public void drawShadow(Batch batch,Rectangle body,PlayerMotion motion) {
        int frame=frameIndex(motion.getState(),motion.getStateTime());
        TextureRegion image=switch(motion.getState()) {
            case DEAD -> deathShadow[motion.getFacing().ordinal()][frame];
            case DASH -> dashShadow[0][frame];
            default -> shadow;
        };
        drawFrame(batch,image,body);
    }
    public void draw(Batch batch,Rectangle body,PlayerMotion motion) {
        if(motion.getState()==PlayerMotion.State.DASH) {
            drawFrame(batch,dust[motion.getFacing().ordinal()][frameIndex(motion.getState(),motion.getStateTime())],body);
        }
        drawFrame(batch,getFrame(motion),body);
    }
    @Override public void dispose() { for(Texture texture:textures)texture.dispose();textures.clear(); }
}

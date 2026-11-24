package io.github.projeto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** HUD sprites are cropped from the supplied UI atlases, with no generated textures. */
public final class GameHud implements Disposable {
    public static final int HEALTH_NATIVE_WIDTH=12;
    public static final int HEALTH_NATIVE_HEIGHT=6;
    public static final int KEY_NATIVE_SIZE=16;
    public static final float PROMPT_KEY_SIZE=48f;
    public static final float PROMPT_HEIGHT=PROMPT_KEY_SIZE;
    private static final float PROMPT_GAP=8f;
    private final Texture healthTexture=load("UI/healthbar.png");
    private final Texture keyboardTexture=load("UI/keyboard.png");
    private final TextureRegion[][] emptyFrame=new TextureRegion[3][3];
    private final TextureRegion redFill;
    private final TextureRegion blueFill;
    private final TextureRegion keyE;
    private final GlyphLayout promptLabel=new GlyphLayout();
    private final Color savedFontColor=new Color();

    public GameHud() {
        if(healthTexture.getWidth()<28||healthTexture.getHeight()<168)
            throw new IllegalArgumentException("UI/healthbar.png does not contain the expected red bar region");
        if(keyboardTexture.getWidth()<32||keyboardTexture.getHeight()<80)
            throw new IllegalArgumentException("UI/keyboard.png does not contain the expected E key region");
        // Continuous red style: full bar x0/y162, matching empty frame x16/y162.
        int[] sourceX={16,17,27},sourceY={162,163,167};
        int[] widths={1,10,1},heights={1,4,1};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
            emptyFrame[row][col]=new TextureRegion(healthTexture,sourceX[col],sourceY[row],widths[col],heights[row]);
        }
        redFill=new TextureRegion(healthTexture,1,163,10,4);
        blueFill=new TextureRegion(healthTexture,1,171,10,4);
        // Highlighted raised E, fifth alphabet row, second 16x16 tile.
        keyE=new TextureRegion(keyboardTexture,16,64,KEY_NATIVE_SIZE,KEY_NATIVE_SIZE);
    }
    private static Texture load(String path) {
        Texture texture=new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        return texture;
    }
    public static float healthFraction(float current,float maximum) {
        if(!Float.isFinite(current)||!Float.isFinite(maximum)||maximum<=0)return 0f;
        return Math.max(0f,Math.min(1f,current/maximum));
    }
    public void drawHealth(Batch batch,float current,float maximum,float x,float y,float width,float height) {
        drawBar(batch,redFill,healthFraction(current,maximum),x,y,width,height);
    }
    /** Full means Q is available; filling follows the real dash cooldown. */
    public void drawDash(Batch batch,float remaining,float cooldown,float x,float y,float width,float height) {
        drawBar(batch,blueFill,1f-healthFraction(remaining,cooldown),x,y,width,height);
    }
    private void drawBar(Batch batch,TextureRegion fill,float fraction,float x,float y,float width,float height) {
        if(!Float.isFinite(width)||!Float.isFinite(height)||width<=0||height<=0)return;
        float packedColor=batch.getPackedColor();
        batch.setColor(Color.WHITE);
        // Preserve the one-pixel source corners; stretch only the center and edge strips.
        float border=Math.min(height/HEALTH_NATIVE_HEIGHT,width/2f);
        float[] destX={x,x+border,x+width-border};
        float[] destY={y+height-border,y+border,y};
        float[] destW={border,Math.max(0,width-border*2),border};
        float[] destH={border,Math.max(0,height-border*2),border};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
            if(destW[col]>0&&destH[row]>0)
                batch.draw(emptyFrame[row][col],destX[col],destY[row],destW[col],destH[row]);
        }
        float innerWidth=Math.max(0,width-border*2);
        float fillWidth=innerWidth*fraction;
        if(fillWidth>0&&height>border*2)
            batch.draw(fill,x+border,y+border,fillWidth,height-border*2);
        batch.setPackedColor(packedColor);
    }
    private static String promptText(boolean needsHealing) { return needsHealing?"Curar":"Vida cheia"; }
    public float getBenchPromptWidth(BitmapFont font,boolean needsHealing) {
        return getBenchPromptWidth(font,promptText(needsHealing));
    }
    public float getBenchPromptWidth(BitmapFont font,String text) {
        promptLabel.setText(font,text);
        return PROMPT_KEY_SIZE+PROMPT_GAP+promptLabel.width;
    }
    public void drawBenchPrompt(Batch batch,BitmapFont font,float centerX,float bottomY,boolean needsHealing) {
        drawBenchPrompt(batch,font,centerX,bottomY,promptText(needsHealing),needsHealing);
    }
    public void drawBenchPrompt(Batch batch,BitmapFont font,float centerX,float bottomY,String text,boolean enabled) {
        float width=getBenchPromptWidth(font,text);
        float left=centerX-width/2f;
        float packedColor=batch.getPackedColor();
        savedFontColor.set(font.getColor());
        batch.setColor(Color.WHITE);
        if(!enabled)batch.setColor(.65f,.65f,.65f,1f);
        batch.draw(keyE,left,bottomY,PROMPT_KEY_SIZE,PROMPT_KEY_SIZE);
        batch.setColor(Color.WHITE);
        float textX=left+PROMPT_KEY_SIZE+PROMPT_GAP;
        float textY=bottomY+(PROMPT_KEY_SIZE+promptLabel.height)/2f;
        font.setColor(0f,0f,0f,.85f);
        promptLabel.setText(font,text);
        font.draw(batch,promptLabel,textX+2,textY-2);
        font.setColor(enabled?Color.WHITE:Color.LIGHT_GRAY);
        promptLabel.setText(font,text);
        font.draw(batch,promptLabel,textX,textY);
        font.setColor(savedFontColor);
        batch.setPackedColor(packedColor);
    }
    @Override public void dispose() {
        healthTexture.dispose();keyboardTexture.dispose();
    }
}

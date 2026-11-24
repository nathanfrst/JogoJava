package io.github.projeto;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Sprite rectangles are measured in pixels from the top-left of each sheet. */
public class Obstacle extends GameObject {
    public enum Kind {
        TREE_A(true, true, 16, 8, 128, 152, .5f, .4f),
        TREE_B(true, true, 152, 8, 112, 152, .5f, .4f),
        TREE_C(true, true, 288, 24, 96, 136, .5f, .4f),
        CRATE(false, true, 160, 16, 32, 24, .6f, .4f),
        BARREL(false, true, 160, 152, 32, 40, .5f, .4f),
        VASE(false, true, 160, 216, 32, 40, .4f, .3f),
        CHEST(false, true, 96, 24, 32, 40, .6f, .4f),
        BENCH(false, true, 288, 16, 64, 48, 1.15f, .5f),
        WELL(false, true, 416, 352, 64, 64, 1.05f, .7f),
        FOUNTAIN(false, true, 352, 256, 96, 96, 1.6f, 1f),
        STATUE(false, true, 448, 16, 40, 80, .65f, .5f),
        GRAVE(false, true, 224, 240, 32, 40, .5f, .35f),
        CROSS(false, true, 224, 288, 32, 56, .4f, .3f),
        OBELISK(false, true, 224, 8, 32, 64, .5f, .4f),
        TOMB(false, true, 288, 160, 32, 64, .55f, .45f),
        CRATE_STACK(false, true, 160, 16, 32, 48, .6f, .4f),
        SIGN(false, true, 96, 160, 32, 32, .3f, .25f),
        BUSH(true, true, 208, 176, 64, 56, .8f, .5f),
        SMALL_BUSH(true, false, 32, 192, 32, 32, .4f, .25f),
        GRASS_A(true, false, 0, 384, 32, 32, .3f, .2f),
        GRASS_B(true, false, 32, 384, 32, 32, .3f, .2f),
        GRASS_C(true, false, 64, 416, 32, 32, .3f, .2f);

        final boolean plantSheet;
        final boolean solid;
        final int x, y, width, height;
        final float bodyWidth, bodyHeight;

        Kind(boolean plantSheet, boolean solid, int x, int y, int width, int height,
             float bodyWidth, float bodyHeight) {
            this.plantSheet = plantSheet;
            this.solid = solid;
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.bodyWidth = bodyWidth; this.bodyHeight = bodyHeight;
        }
    }

    private final Kind kind;
    private TextureRegion sprite;
    private TextureRegion shadow;

    public Obstacle(float x, float y, Kind kind) {
        super(x, y, kind.bodyWidth, kind.bodyHeight);
        this.kind = kind;
    }

    public Kind getKind() { return kind; }
    public boolean isBreakable() { return kind == Kind.CHEST; }
    public boolean isSolid() { return kind.solid; }
    public void setSprite(TextureRegion sprite) { this.sprite = sprite; }
    public void setShadow(TextureRegion shadow) { this.shadow = shadow; }

    public void drawShadow(Batch batch) {
        if (shadow == null) return;
        float color = batch.getPackedColor();
        batch.setColor(0f, 0f, 0f, .3f);
        float width = shadow.getRegionWidth() / 48f;
        float height = shadow.getRegionHeight() / 48f;
        batch.draw(shadow, rect.x + (rect.width - width) / 2f,
            rect.y - .12f, width, height);
        batch.setPackedColor(color);
    }

    @Override
    void update(float deltaTime) {}

    @Override
    public void draw(Batch batch) {
        if (sprite == null) return;
        float width = sprite.getRegionWidth() / 48f;
        float height = sprite.getRegionHeight() / 48f;
        batch.draw(sprite, rect.x + (rect.width - width) / 2f,
            rect.y - .12f, width, height);
    }
}

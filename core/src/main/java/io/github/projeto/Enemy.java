package io.github.projeto;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Enemy extends GameObject {
    private static final float SCALE = 1 / 32f;
    public static final float HITBOX_WIDTH = 12 * SCALE;
    public static final float HITBOX_HEIGHT = 8 * SCALE;
    // The 32x32 sprite has 11 transparent rows below the slime's feet.
    private static final float FOOT_OFFSET = 11 * SCALE;
    private static final float SPEED = 1.5f;
    private final SlimePathFollower pathFollower;

    public Enemy(float x, float y, Texture texture, Player player, SlimeNavigation navigation){
        super(x, y, HITBOX_WIDTH, HITBOX_HEIGHT, texture);
        this.pathFollower = new SlimePathFollower(navigation, player.getObstacleWorld());
    }

    @Override
    public void draw(Batch batch) {
        float width = texture.getWidth() * SCALE;
        float height = texture.getHeight() * SCALE;
        batch.draw(texture, rect.x + rect.width / 2f - width / 2f,
            rect.y - FOOT_OFFSET, width, height);
    }

    @Override
    void update(float deltaTime){
        pathFollower.update(rect, deltaTime, SPEED);
    }

    public static Enemy spawn(Viewport viewport, Texture texture, Player player, SlimeNavigation navigation){
        int edge = MathUtils.random(3);
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();
        float left = viewport.getCamera().position.x - width / 2f;
        float bottom = viewport.getCamera().position.y - height / 2f;
        float enemyWidth = texture.getWidth() * SCALE;
        float enemyHeight = texture.getHeight() * SCALE;
        float horizontalMargin = (enemyWidth - HITBOX_WIDTH) / 2f;
        float x, y;

        // Spawn outside the current view as the camera follows the player.
        switch (edge) {
            case 0: {
                x = MathUtils.random(left, left + width);
                y = bottom + height + FOOT_OFFSET;
                break;
            }
            case 1: {
                x = left + width + horizontalMargin;
                y = MathUtils.random(bottom, bottom + height);
                break;
            }
            case 2: {
                x = MathUtils.random(left, left + width);
                y = bottom - enemyHeight + FOOT_OFFSET;
                break;
            }
            default: {
                x = left - enemyWidth + horizontalMargin;
                y = MathUtils.random(bottom, bottom + height);
                break;
            }
        }
        return new Enemy(x, y, texture, player, navigation);
    }
}

package io.github.projeto;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Disposable;

/** Centered results text over a subtly dimmed world, without a decorative panel. */
public final class GameOverPanel implements Disposable {
    private final Texture pixel;
    private final GlyphLayout label = new GlyphLayout();
    private final Color savedFontColor = new Color();

    public GameOverPanel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
    }

    /** Draw before the HUD so only the world is darkened. */
    public void drawDimBackground(Batch batch, float width, float height) {
        float packedColor = batch.getPackedColor();
        batch.setColor(0f, 0f, 0f, .5f);
        batch.draw(pixel, 0f, 0f, width, height);
        batch.setPackedColor(packedColor);
    }

    public void draw(Batch batch, BitmapFont font, float width, float height,
                     int score, String time, RunRecords records, boolean newScoreRecord) {
        float packedColor = batch.getPackedColor();
        float oldScaleX = font.getData().scaleX, oldScaleY = font.getData().scaleY;
        savedFontColor.set(font.getColor());
        float scale = Math.max(.1f, Math.min(1f, Math.min((width - 32f) / 600f, (height - 32f) / 380f)));
        float centerX = width / 2f, centerY = height / 2f;
        batch.setColor(Color.WHITE);

        centered(batch, font, "FIM DE JOGO", centerX, centerY + 150f * scale, 1.35f * scale, Color.WHITE);
        if (newScoreRecord) {
            centered(batch, font, "NOVO RECORDE!", centerX, centerY + 108f * scale, .65f * scale, Color.LIGHT_GRAY);
        }
        centered(batch, font, "Score final: " + score, centerX, centerY + 62f * scale, .9f * scale, Color.WHITE);
        centered(batch, font, "Tempo sobrevivido: " + time, centerX, centerY + 22f * scale, .9f * scale, Color.WHITE);
        centered(batch, font, "Recorde de score: " + records.getBestScore(), centerX, centerY - 38f * scale, .75f * scale, Color.LIGHT_GRAY);
        centered(batch, font, "Melhor tempo: " + records.getBestTimeDisplay(), centerX, centerY - 72f * scale, .75f * scale, Color.LIGHT_GRAY);
        centered(batch, font, "Aperte R para jogar novamente", centerX, centerY - 132f * scale, .75f * scale, Color.WHITE);

        font.getData().setScale(oldScaleX, oldScaleY);
        font.setColor(savedFontColor);
        batch.setPackedColor(packedColor);
    }

    private void centered(Batch batch, BitmapFont font, String text, float centerX, float top, float scale, Color color) {
        font.getData().setScale(scale);
        // A shadow on the letters keeps text readable over the map without a panel.
        font.setColor(0f, 0f, 0f, .9f);
        label.setText(font, text);
        font.draw(batch, label, centerX - label.width / 2f + 2f * scale, top - 2f * scale);
        font.setColor(color);
        label.setText(font, text);
        font.draw(batch, label, centerX - label.width / 2f, top);
    }
    @Override public void dispose() { pixel.dispose(); }
}

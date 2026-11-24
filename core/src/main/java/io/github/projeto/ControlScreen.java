package io.github.projeto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ControlScreen extends ScreenAdapter {
    private final GdxGame game;
    private final Batch batch;
    private final BitmapFont font;
    private final Viewport viewport = new ScreenViewport();
    private final GlyphLayout layout = new GlyphLayout();

    public ControlScreen(GdxGame game){
        this.game = game;
        this.batch = game.getBatch();
        this.font = game.getFont();

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
            game.setScreen(new GameScreen(game));
            dispose();
            return;
        }

        ScreenUtils.clear(Color.BLACK);

        float centerX = viewport.getWorldWidth() / 2;
        float y = viewport.getWorldHeight() / 2 + 180;

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        layout.setText(font, "CONTROLES");
        font.draw(batch,layout, centerX - layout.width / 2,y);
        y -=50;

        layout.setText(font,"W / A / S / D - Mover");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y-=40;

        layout.setText(font, "Q - Dash");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 40;

        layout.setText(font, "E - Curar no banco (10 pontos, recarga de 20s)");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 40;

        layout.setText(font, "Ataque automático - Slimes e baús próximos");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 40;

        layout.setText(font, "R - Reiniciar (quando morto)");
        font.draw(batch,layout, centerX - layout.width / 2, y);
        y-=70;

        layout.setText(font, "Aperte ESPAÇO para iniciar. ");
        font.draw(batch, layout, centerX - layout.width / 2,y);

        y -= 60;
        layout.setText(font, "Recorde de score: " + game.getRecords().getBestScore());
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 36;
        layout.setText(font, "Melhor tempo: " + game.getRecords().getBestTimeDisplay());
        font.draw(batch, layout, centerX - layout.width / 2, y);

        batch.end();
    }
}

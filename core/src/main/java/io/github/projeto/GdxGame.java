package io.github.projeto;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class GdxGame extends Game {
    private SpriteBatch batch;
    private BitmapFont font;

    @Override
    public void create() {
        batch = new SpriteBatch();

        var generator = new FreeTypeFontGenerator(Gdx.files.internal("CherryCreamSoda-Regular.ttf"));
        var fontParams = new FreeTypeFontGenerator.FreeTypeFontParameter();
        fontParams.size = 24;
        fontParams.color = Color.WHITE;
        font = generator.generateFont(fontParams);
        generator.dispose();

        setScreen(new ControlScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        font.dispose();
    }

    public Batch getBatch(){

        return batch;
    }

    public BitmapFont getFont(){
        return font;
    }
}

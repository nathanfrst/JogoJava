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
    private RunRecords records;

    @Override
    public void create() {
        records = new RunRecords(Gdx.app.getPreferences(RunRecords.PREFERENCES_NAME));
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
        if (screen != null) screen.dispose();
        batch.dispose();
        font.dispose();
    }

    public Batch getBatch(){

        return batch;
    }

    public BitmapFont getFont(){
        return font;
    }

    public RunRecords getRecords() { return records; }
}

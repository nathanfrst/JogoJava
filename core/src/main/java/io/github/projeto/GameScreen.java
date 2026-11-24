package io.github.projeto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter{
    private static final float WORLD_WIDTH = 16f;
    private static final float WORLD_HEIGHT = 9f;
    private static final float DAMAGE_PER_SECOND = 1f;
    private static final int CHEST_SCORE = 15;

    private final Batch batch;
    private final BitmapFont font;
    private final RunRecords records;
    private final GameOverPanel gameOverPanel = new GameOverPanel();

    private final TerrainRenderer terrain = new TerrainRenderer();
    private final RuinRenderer ruins = new RuinRenderer();
    private final GameHud hud = new GameHud();
    private final PlayerAnimations playerAnimations = new PlayerAnimations();
    private final Texture enemyTexture = new Texture(Gdx.files.internal("slime.png"));
    private final Array<Texture> attackTexture = loadAttackTexture();
    private final Animation<Texture> attackAnimation = new Animation<>(1/12f, attackTexture);
    private final Music music = Gdx.audio.newMusic(Gdx.files.internal("Audio/nightsplitter.mp3"));
    private final Sound slashSfx = Gdx.audio.newSound(Gdx.files.internal("Audio/slash.wav"));

    private final Viewport gameViewport = new ExtendViewport(WORLD_WIDTH,WORLD_HEIGHT);
    private final Viewport uiViewport = new ScreenViewport();
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 inputMovement = new Vector2();
    private final Vector2 cameraCenter = new Vector2();
    private final Vector2 interactionScreen = new Vector2();
    private final com.badlogic.gdx.math.Rectangle attackView = new com.badlogic.gdx.math.Rectangle();
    private Obstacle nearbyBench;
    private final BenchRecovery benchRecovery = new BenchRecovery();

    private final ObstacleWorld obstacleWorld = new ObstacleWorld(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
    private final SlimeNavigation navigation = new SlimeNavigation(obstacleWorld,
        Enemy.HITBOX_WIDTH, Enemy.HITBOX_HEIGHT);
    private final Array<GameObject> worldObjects = new Array<>();

    private final Player player = new Player(WORLD_WIDTH/2f, WORLD_HEIGHT/2f, playerAnimations, attackAnimation, slashSfx, obstacleWorld);

    private final Array<Enemy> enemies = new Array<>();
    private float enemySpawnTimer;
    private int score;
    private final SurvivalProgress survival = new SurvivalProgress();
    private boolean resultSaved;
    private boolean newScoreRecord;
    private boolean disposed;

    public GameScreen(GdxGame game){
        this.batch = game.getBatch();
        this.font = game.getFont();
        this.records = game.getRecords();

    }
    private Array<Texture> loadAttackTexture(){
        var textures = new Array<Texture>();
        for(int i =0; i < 14; ++i){
            textures.add(new Texture(Gdx.files.internal(String.format("Player/Slash/slash_%02d.png",i))));
        }
        return textures;
    }
    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, false);
        updateCamera();
        uiViewport.update(width, height, true);
    }

    @Override
    public void show() {
        resetGame();
    }
    private void resetGame(){
        resultSaved = false;
        newScoreRecord = false;
        survival.reset();
        benchRecovery.reset();
        obstacleWorld.reset();
        player.reset(WORLD_WIDTH/2, WORLD_HEIGHT / 2);
        updateCamera();
        refreshBenchInteraction();

        enemies.clear();
        enemySpawnTimer = 0f;
        score = 0;

        music.stop();
        music.setLooping(true);
        music.play();
    }

    private void processInput() {
        inputMovement.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            inputMovement.y += 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            inputMovement.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            inputMovement.x -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            inputMovement.x += 1;
        }

        inputMovement.nor();
        player.changeDirection(inputMovement);
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) player.requestDash();
    }


    @Override
    public void render(float deltaTime) {
        benchRecovery.update(deltaTime);
        if(player.isAlive()) {
            survival.update(deltaTime);
            processInput();
            updateLogic(deltaTime);
            checkCollision(deltaTime);
            if (player.isDead()) finishRun();
            refreshBenchInteraction();
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) tryHealAtBench();
        } else {
            finishRun();
            nearbyBench = null;
            player.update(deltaTime);
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) resetGame();
        }
        draw();
    }

    private void finishRun() {
        if (resultSaved) return;
        newScoreRecord = records.submit(score, survival.getElapsedSeconds());
        resultSaved = true;
        music.stop();
    }

    @Override
    public void hide() {
        // Also keep a new record if the window is closed during a living run.
        finishRun();
    }

    private void refreshBenchInteraction() {
        nearbyBench = player.isAlive()
            ? BenchHealing.findNearby(player.rect, obstacleWorld.getObstacles()) : null;
    }

    private void tryHealAtBench() {
        // Recheck the position on the interaction frame; old prompts cannot heal at a distance.
        refreshBenchInteraction();
        if (nearbyBench != null) score -= benchRecovery.tryHeal(player, score);
    }

    private void drawBenchPrompt() {
        if (nearbyBench == null || player.isDead()) return;
        interactionScreen.set(nearbyBench.rect.x + nearbyBench.rect.width / 2f, nearbyBench.rect.y + .98f);
        gameViewport.project(interactionScreen);
        interactionScreen.y = Gdx.graphics.getHeight() - interactionScreen.y;
        uiViewport.unproject(interactionScreen);
        String prompt = benchRecovery.prompt(player, score);
        boolean enabled = benchRecovery.canUse(player, score);
        float margin = hud.getBenchPromptWidth(font, prompt) / 2f + 8f;
        float x = uiViewport.getWorldWidth() < margin * 2f ? uiViewport.getWorldWidth() / 2f
            : Math.max(margin, Math.min(uiViewport.getWorldWidth() - margin, interactionScreen.x));
        float y = Math.max(12f, Math.min(uiViewport.getWorldHeight() - GameHud.PROMPT_HEIGHT - 12f, interactionScreen.y));
        hud.drawBenchPrompt(batch, font, x, y, prompt, enabled);
    }

    private void checkCollision(float deltaTime) {
        for (Attack attack : player.getAttacks()) {
            score += CHEST_SCORE * obstacleWorld.breakChests(attack);
            var iterator = enemies.iterator();
            while(iterator.hasNext()){
                Enemy enemy = iterator.next();
                if(attack.canHit(enemy)){
                    iterator.remove();
                    ++score;
                }
            }

        }

        int numHits = 0;
        for (Enemy enemy : enemies) {
            if(player.overlaps(enemy)){
                ++numHits;
            }
        }
        if (numHits>0){
            player.subLife(DAMAGE_PER_SECOND * numHits * deltaTime);
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);

        gameViewport.apply();
        batch.setProjectionMatrix(gameViewport.getCamera().combined);
        batch.begin();

        drawBackground();
        obstacleWorld.prepareSprites();
        for (Obstacle obstacle : obstacleWorld.getObstacles()) {
            obstacle.drawShadow(batch);
        }
        player.drawShadow(batch);
        for (Attack attack : player.getAttacks()) {
            attack.draw(batch);
        }
        worldObjects.clear();
        ruins.addWalls(worldObjects);
        for (Obstacle obstacle : obstacleWorld.getObstacles()) {
            if (obstacle.isSolid()) worldObjects.add(obstacle);
            else obstacle.draw(batch); // Small plants stay underneath characters.
        }
        worldObjects.addAll(enemies);
        worldObjects.add(player);
        // Higher bases draw first; tree canopies can cover characters behind them.
        worldObjects.sort((a, b) -> Float.compare(b.rect.y, a.rect.y));
        for (GameObject object : worldObjects) {
            object.draw(batch);
        }

        batch.end();

        // render UI
        uiViewport.apply();
        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        batch.begin();
        hud.drawHealth(batch, player.getHealth(), player.getMaxHealth(),
            20f, uiViewport.getWorldHeight() - 88f,
            Math.min(216f, Math.max(48f, uiViewport.getWorldWidth() - 40f)), 36f);
        float dashWidth=Math.min(216f,Math.max(48f,uiViewport.getWorldWidth()-40f));
        hud.drawDash(batch,player.getMotion().getDashCooldown(),PlayerMotion.DASH_COOLDOWN,
            20f,uiViewport.getWorldHeight()-122f,dashWidth,24f);
        font.draw(batch,"Q",28f+dashWidth,uiViewport.getWorldHeight()-101f);
        drawBenchPrompt();
        font.draw(batch, "Score: " + score,20f, uiViewport.getWorldHeight() - 20);
        layout.setText(font,"Tempo: "+survival.getDisplayTime());
        font.draw(batch,layout,(uiViewport.getWorldWidth()-layout.width)/2f,uiViewport.getWorldHeight()-20f);
        layout.setText(font, "Recorde: " + records.getBestScore());
        font.draw(batch, layout, uiViewport.getWorldWidth() - layout.width - 20f, uiViewport.getWorldHeight() - 20f);
        if(player.isDead() && player.isDeathAnimationFinished()){
            gameOverPanel.draw(batch, font, uiViewport.getWorldWidth(), uiViewport.getWorldHeight(),
                score, survival.getDisplayTime(), records, newScoreRecord);
        }
        batch.end();
    }

    private void updateLogic(float deltaTime) {
        attackView.set(gameViewport.getCamera().position.x-gameViewport.getWorldWidth()/2f,
            gameViewport.getCamera().position.y-gameViewport.getWorldHeight()/2f,
            gameViewport.getWorldWidth(),gameViewport.getWorldHeight());
        player.aimAtTargets(enemies, obstacleWorld.getObstacles(), attackView);
        player.update(deltaTime);
        updateCamera();

        enemySpawnTimer += deltaTime;
        float spawnInterval=survival.getSpawnInterval();
        if(enemySpawnTimer >= spawnInterval){
            enemySpawnTimer -= spawnInterval;
            // Retry blocked edges without ever spawning inside terrain or props.
            for(int attempt=0;attempt<8;attempt++) {
                Enemy enemy = Enemy.spawn(gameViewport, enemyTexture, player, navigation);
                if (obstacleWorld.canStand(enemy.rect)) { enemies.add(enemy);break; }
            }
        }
        navigation.update(player.rect, deltaTime);
        for (Enemy enemy :enemies){
            enemy.update(deltaTime);
        }

    }

    private void updateCamera() {
        Vector2 center = player.getCenter(cameraCenter);
        gameViewport.getCamera().position.set(center.x, center.y, 0f);
        gameViewport.getCamera().update();
        obstacleWorld.refresh(gameViewport);
        ruins.refresh(gameViewport);
    }

    private void drawBackground() {
        terrain.draw(batch, gameViewport);
        ruins.drawStairs(batch);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        finishRun();
        obstacleWorld.dispose();
        terrain.dispose();
        ruins.dispose();
        playerAnimations.dispose();
        hud.dispose();
        enemyTexture.dispose();
        attackTexture.forEach(Texture::dispose);
        music.dispose();
        slashSfx.dispose();
    }
}

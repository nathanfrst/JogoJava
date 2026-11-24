package io.github.projeto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Deterministic regions: unload distant regions and recreate them on return. */
public class ObstacleWorld implements Disposable {
    private static final float CHUNK_SIZE = 8f;
    private static final int OBSTACLES_PER_CHUNK = 2;
    private static final long WORLD_SEED = 20261123L;
    private static final float MOVE_STEP = 0.1f;
    private final LongMap<Array<Obstacle>> chunks = new LongMap<>();
    private final Array<Obstacle> visibleObstacles = new Array<>();
    private final Array<Long> expiredChunks = new Array<>();
    private final Rectangle spawnClearing;
    private record ChestId(int xBits,int yBits) {}
    private final java.util.Set<ChestId> brokenChests=new java.util.HashSet<>();
    private long revision;
    private static ChestId chestId(Obstacle obstacle) {
        return new ChestId(Float.floatToIntBits(obstacle.rect.x),Float.floatToIntBits(obstacle.rect.y));
    }
    private Texture plantTexture;
    private Texture propTexture;
    private Texture plantShadowTexture;
    private Texture propShadowTexture;
    private final ObjectMap<Obstacle.Kind, TextureRegion> shadows = new ObjectMap<>();
    private final ObjectMap<Obstacle.Kind, TextureRegion> sprites = new ObjectMap<>();

    public ObstacleWorld(float spawnX, float spawnY) {
        spawnClearing = new Rectangle(spawnX - 2f, spawnY - 2f, 5f, 5f);
    }

    private static int chunkAt(float position) {
        // floor is necessary: -0.5 belongs to chunk -1, not chunk 0.
        return MathUtils.floor(position / CHUNK_SIZE);
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private Array<Obstacle> chunk(int x, int y) {
        long key = key(x, y);
        Array<Obstacle> existing = chunks.get(key);
        if (existing != null) return existing;

        // Separate randomness from enemy spawning; visiting order never changes terrain.
        RandomXS128 random = new RandomXS128(WORLD_SEED
            ^ ((long) x * 73856093L) ^ ((long) y * 19349663L));
        Array<Obstacle> generated = new Array<>();
        // Authored landmarks establish each region's theme before scattered decoration.
        float regionWorldSize=64*TerrainLayout.TILE_SIZE;
        int rx=(int)Math.floor(x*CHUNK_SIZE/regionWorldSize);
        int ry=(int)Math.floor(y*CHUNK_SIZE/regionWorldSize);
        for(int regionY=ry-1;regionY<=ry+1;regionY++)for(int regionX=rx-1;regionX<=rx+1;regionX++) {
            for(RuinLayout.Landmark mark:RuinLayout.region(regionX,regionY).landmarks()) {
                float px=(regionX*64+mark.x())*TerrainLayout.TILE_SIZE;
                float py=(regionY*64+mark.y())*TerrainLayout.TILE_SIZE;
                if(chunkAt(px)!=x||chunkAt(py)!=y)continue;
                Obstacle landmark=new Obstacle(px,py,mark.kind());
                if(!landmark.rect.overlaps(spawnClearing)&&hasValidGround(landmark)
                    && (!landmark.isBreakable()||!brokenChests.contains(chestId(landmark))))generated.add(landmark);
            }
        }
        for (int i = 0; i < OBSTACLES_PER_CHUNK; i++) {
            // One candidate in each 4x4 cell, with space between neighbors.
            float px = x * CHUNK_SIZE + 1f + random.nextFloat() * 6f;
            float py = y * CHUNK_SIZE + 1f + random.nextFloat() * 6f;
            // Wilderness only gets sparse vegetation. Every prop has an authored context.
            Obstacle.Kind[] solidKinds = {
                Obstacle.Kind.TREE_A,Obstacle.Kind.TREE_B,Obstacle.Kind.TREE_C,Obstacle.Kind.BUSH
            };
            Obstacle obstacle = new Obstacle(px, py, solidKinds[random.nextInt(solidKinds.length)]);
            if (!obstacle.rect.overlaps(spawnClearing) && hasValidGround(obstacle) && !overlapsAny(obstacle,generated)) {
                generated.add(obstacle);
            }
        }
        // Decorative plants have no solid collision rectangle.
        Obstacle.Kind[] plants = { Obstacle.Kind.SMALL_BUSH, Obstacle.Kind.GRASS_A,
            Obstacle.Kind.GRASS_B, Obstacle.Kind.GRASS_C };
        for (int i = 0; i < 4; i++) {
            float px = x * CHUNK_SIZE + random.nextFloat() * CHUNK_SIZE;
            float py = y * CHUNK_SIZE + random.nextFloat() * CHUNK_SIZE;
            Obstacle plant = new Obstacle(px, py, plants[random.nextInt(plants.length)]);
            boolean overlapsSolid = false;
            for (Obstacle other : generated) {
                if (other.isSolid() && plant.rect.overlaps(other.rect)) overlapsSolid = true;
            }
            if (!overlapsSolid && hasValidGround(plant)) generated.add(plant);
        }
        chunks.put(key, generated);
        return generated;
    }

    private boolean overlapsAny(Obstacle candidate,Array<Obstacle> existing) {
        for(Obstacle obstacle:existing)if(obstacle.isSolid()&&candidate.rect.overlaps(obstacle.rect))return true;
        return false;
    }

    private boolean hasValidGround(Obstacle obstacle) {
        Rectangle base = obstacle.rect;
        Obstacle.Kind kind=obstacle.getKind();
        if(kind.plantSheet) {
            // Reserve the visible plant footprint too, so canopies do not bury the paths.
            float width=kind.width/48f,height=kind.height/48f;
            float left=base.x+(base.width-width)/2f;
            return TerrainLayout.canPlace(left,base.y-.12f,width,height,true);
        }
        return TerrainLayout.canPlace(base.x,base.y,base.width,base.height,false);
    }

    /** Generate a padded area for drawing, and discard distant regions. */
    public void refresh(Viewport viewport) {
        float cx = viewport.getCamera().position.x;
        float cy = viewport.getCamera().position.y;
        int minX = chunkAt(cx - viewport.getWorldWidth() / 2f - 3f);
        int maxX = chunkAt(cx + viewport.getWorldWidth() / 2f + 3f);
        int minY = chunkAt(cy - viewport.getWorldHeight() / 2f - 3f);
        int maxY = chunkAt(cy + viewport.getWorldHeight() / 2f + 3f);
        visibleObstacles.clear();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) visibleObstacles.addAll(chunk(x, y));
        }
        expiredChunks.clear();
        for (LongMap.Entry<Array<Obstacle>> entry : chunks) {
            int x = (int) (entry.key >> 32);
            int y = (int) entry.key;
            if (x < minX || x > maxX || y < minY || y > maxY) expiredChunks.add(entry.key);
        }
        for (Long expired : expiredChunks) chunks.remove(expired);
    }

    /** Small steps prevent crossing a thin trunk during a long frame. */
    public void move(Rectangle body, float dx, float dy) {
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / MOVE_STEP));
        for (int step = 0; step < steps; step++) {
            moveAxis(body, dx / steps, true);
            moveAxis(body, dy / steps, false);
        }
    }

    private void moveAxis(Rectangle body, float amount, boolean horizontal) {
        if (amount == 0f) return;
        if (horizontal) body.x += amount;
        else body.y += amount;

        resolveTerrain(body,amount,horizontal);
        int minX = chunkAt(body.x - 1f);
        int maxX = chunkAt(body.x + body.width + 1f);
        int minY = chunkAt(body.y - 1f);
        int maxY = chunkAt(body.y + body.height + 1f);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (Obstacle obstacle : chunk(x, y)) {
                    if (!obstacle.isSolid()) continue;
                    Rectangle solid = obstacle.rect;
                    if (!body.overlaps(solid)) continue;
                    if (horizontal) {
                        body.x = amount > 0f ? solid.x - body.width : solid.x + solid.width;
                    } else {
                        body.y = amount > 0f ? solid.y - body.height : solid.y + solid.height;
                    }
                }
            }
        }
    }

    private void resolveTerrain(Rectangle body,float amount,boolean horizontal) {
        float result=RuinLayout.clipAxis(body.x,body.y,body.width,body.height,amount,horizontal);
        if(horizontal)body.x=result;else body.y=result;
    }

    public boolean canStand(Rectangle body) {
        if(RuinLayout.clipAxis(body.x,body.y,body.width,body.height,1f,true)!=body.x)return false;
        for(int y=chunkAt(body.y-1);y<=chunkAt(body.y+body.height+1);y++) {
            for(int x=chunkAt(body.x-1);x<=chunkAt(body.x+body.width+1);x++) {
                for(Obstacle obstacle:chunk(x,y)) {
                    if(obstacle.isSolid() && body.overlaps(obstacle.rect))return false;
                }
            }
        }
        return true;
    }

    /** Test a swept footprint, not just a center ray, so routes cannot cut corners. */
    public boolean canTravel(Rectangle body,float centerX,float centerY) {
        float startX=body.x+body.width/2f,startY=body.y+body.height/2f;
        float dx=centerX-startX,dy=centerY-startY;
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(dx),Math.abs(dy))/.12f));
        Rectangle probe=new Rectangle(body);
        for(int i=0;i<=steps;i++) {
            float fraction=(float)i/steps;
            probe.setPosition(body.x+dx*fraction,body.y+dy*fraction);
            if(!canStand(probe))return false;
        }
        return true;
    }

    public long getRevision() { return revision; }

    /** New run restores destructible props; unloading a region alone does not. */
    public void reset() {
        chunks.clear();visibleObstacles.clear();brokenChests.clear();revision++;
    }

    public int breakChests(Attack attack) { return breakChests(attack.rect,attack); }

    /** Returns new destructions only, allowing the caller to award score once. */
    public int breakChests(Rectangle hitbox) { return breakChests(hitbox,null); }

    private int breakChests(Rectangle hitbox,Attack attack) {
        int count=0;
        for(LongMap.Entry<Array<Obstacle>> entry:chunks) {
            java.util.Iterator<Obstacle> iterator=entry.value.iterator();
            while(iterator.hasNext()) {
                Obstacle obstacle=iterator.next();
                if(!obstacle.isBreakable()||!obstacle.rect.overlaps(hitbox))continue;
                if(attack!=null&&!attack.canHit(obstacle))continue;
                if(brokenChests.add(chestId(obstacle)))count++;
                iterator.remove();
                visibleObstacles.removeValue(obstacle,true);
            }
        }
        if(count>0)revision++;
        return count;
    }

    public Array<Obstacle> getObstacles() {
        return visibleObstacles;
    }

    /** Regions share their source textures instead of loading an image per object. */
    public void prepareSprites() {
        if (plantTexture == null) {
            plantTexture = new Texture(Gdx.files.internal("Mapa/TX Plant.png"));
            propTexture = new Texture(Gdx.files.internal("Mapa/TX Props.png"));
            plantTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            propTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            plantShadowTexture = new Texture(Gdx.files.internal("Mapa/TX Shadow Plant.png"));
            propShadowTexture = new Texture(Gdx.files.internal("Mapa/TX Shadow.png"));
            plantShadowTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            propShadowTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            for (Obstacle.Kind kind : Obstacle.Kind.values()) {
                TextureRegion region = new TextureRegion(kind.plantSheet ? plantTexture : propTexture,
                        kind.x, kind.y, kind.width, kind.height);
                sprites.put(kind, region);
                if (kind.solid) {
                    shadows.put(kind, new TextureRegion(
                        kind.plantSheet ? plantShadowTexture : propShadowTexture,
                        kind.x, kind.y, kind.width, kind.height));
                }
            }
        }
        for (Obstacle obstacle : visibleObstacles) {
            obstacle.setSprite(sprites.get(obstacle.getKind()));
            obstacle.setShadow(shadows.get(obstacle.getKind()));
        }
    }

    @Override
    public void dispose() {
        if (plantTexture != null) plantTexture.dispose();
        if (propTexture != null) propTexture.dispose();
        if (plantShadowTexture != null) plantShadowTexture.dispose();
        if (propShadowTexture != null) propShadowTexture.dispose();
        shadows.clear();
        sprites.clear();
        chunks.clear();
        visibleObstacles.clear();
    }
}

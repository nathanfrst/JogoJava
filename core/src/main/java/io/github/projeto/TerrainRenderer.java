package io.github.projeto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Visible terrain only: shared tiles plus a bounded cache of generated regions. */
public class TerrainRenderer implements Disposable {
    private static final float TILE_SIZE = TerrainLayout.TILE_SIZE;
    private static final int REGION_SIZE = 16;
    private final Texture grassTexture = load("Mapa/TX Tileset Grass.png");
    private final TextureRegion[] grass = new TextureRegion[32];
    private final TextureRegion[] pavers = new TextureRegion[4];
    private final TextureRegion[][] pathQuarters = new TextureRegion[16][16];
    private final LongMap<int[]> regions = new LongMap<>();
    private final Array<Long> expired = new Array<>();

    public TerrainRenderer() {
        for (int i = 0; i < grass.length; i++) {
            grass[i] = new TextureRegion(grassTexture, (i % 8) * 32, (i / 8) * 32, 32, 32);
        }
        int[][] paverSources = {{128, 192}, {176, 192}, {192, 208}, {240, 208}};
        for (int i = 0; i < pavers.length; i++) {
            pavers[i] = new TextureRegion(grassTexture, paverSources[i][0], paverSources[i][1], 16, 16);
        }
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                pathQuarters[x][y] = new TextureRegion(grassTexture, x * 16, y * 16, 16, 16);
            }
        }
    }

    private static Texture load(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private int[] region(int rx, int ry) {
        long key = key(rx, ry);
        int[] data = regions.get(key);
        if (data != null) return data;
        // Six integers per tile: grass, four paving quarters, optional broken paver.
        data = new int[REGION_SIZE * REGION_SIZE * 6];
        for (int y = 0; y < REGION_SIZE; y++) {
            for (int x = 0; x < REGION_SIZE; x++) {
                int wx = rx * REGION_SIZE + x, wy = ry * REGION_SIZE + y;
                int offset = (y * REGION_SIZE + x) * 6;
                data[offset] = TerrainLayout.grassVariant(wx, wy);
                data[offset + 5] = TerrainLayout.scatteredPaver(wx, wy);
                boolean path = TerrainLayout.isPath(wx, wy);
                for (int q = 0; q < 4; q++) {
                    data[offset + 1 + q] = path ? TerrainLayout.pathQuarter(wx, wy, q) : -1;
                }
            }
        }
        regions.put(key, data);
        return data;
    }

    public void draw(Batch batch, Viewport viewport) {
        float cx = viewport.getCamera().position.x, cy = viewport.getCamera().position.y;
        int minX = (int) Math.floor((cx - viewport.getWorldWidth() / 2f) / TILE_SIZE) - 1;
        int maxX = (int) Math.floor((cx + viewport.getWorldWidth() / 2f) / TILE_SIZE) + 1;
        int minY = (int) Math.floor((cy - viewport.getWorldHeight() / 2f) / TILE_SIZE) - 1;
        int maxY = (int) Math.floor((cy + viewport.getWorldHeight() / 2f) / TILE_SIZE) + 1;
        // Grass underlay: drawn separately to avoid switching textures for every tile.
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int[] data = region(Math.floorDiv(x, REGION_SIZE), Math.floorDiv(y, REGION_SIZE));
                int offset = (Math.floorMod(y, REGION_SIZE) * REGION_SIZE + Math.floorMod(x, REGION_SIZE)) * 6;
                batch.draw(grass[data[offset]], x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                if (data[offset + 5] >= 0) {
                    batch.draw(pavers[data[offset + 5]], (x + .25f) * TILE_SIZE,
                        (y + .25f) * TILE_SIZE, TILE_SIZE / 2f, TILE_SIZE / 2f);
                }
            }
        }
        float half = TILE_SIZE / 2f;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int[] data = region(Math.floorDiv(x, REGION_SIZE), Math.floorDiv(y, REGION_SIZE));
                int offset = (Math.floorMod(y, REGION_SIZE) * REGION_SIZE + Math.floorMod(x, REGION_SIZE)) * 6;
                for (int q = 0; q < 4; q++) {
                    int source = data[offset + q + 1];
                    if (source < 0) continue;
                    batch.draw(pathQuarters[(source & 255) / 16][(source >>> 8) / 16],
                        x * TILE_SIZE + (q % 2) * half,
                        y * TILE_SIZE + (q < 2 ? half : 0), half, half);
                }
            }
        }
        expired.clear();
        for (LongMap.Entry<int[]> entry : regions) {
            int x = (int) (entry.key >> 32), y = (int) entry.key;
            if (x < Math.floorDiv(minX, REGION_SIZE) || x > Math.floorDiv(maxX, REGION_SIZE)
                || y < Math.floorDiv(minY, REGION_SIZE) || y > Math.floorDiv(maxY, REGION_SIZE)) {
                expired.add(entry.key);
            }
        }
        for (Long key : expired) regions.remove(key);
    }

    @Override
    public void dispose() {
        grassTexture.dispose();
        regions.clear();
    }
}

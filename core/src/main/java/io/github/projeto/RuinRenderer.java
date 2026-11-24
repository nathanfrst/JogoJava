package io.github.projeto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Structures share the object depth ordering so walls can occlude characters. */
public class RuinRenderer implements Disposable {
    private final Texture[] sheets={load("Mapa/TX Tileset Wall.png"),
        load("Mapa/TX Struct.png"),load("Mapa/TX Props.png")};
    private final LongMap<Array<Piece>> regions=new LongMap<>();
    private final Array<Long> expired=new Array<>();
    private final Array<Piece> visible=new Array<>();
    private static Texture load(String path) {
        Texture texture=new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        return texture;
    }
    private static long key(int x,int y) { return ((long)x<<32)|(y&0xffffffffL); }
    private class Piece extends GameObject {
        final TextureRegion sprite;
        final boolean floor;
        Piece(RuinArt.Piece p) {
            super(p.x()*TerrainLayout.TILE_SIZE,p.y()*TerrainLayout.TILE_SIZE,
                p.width()*TerrainLayout.TILE_SIZE,p.height()*TerrainLayout.TILE_SIZE);
            sprite=new TextureRegion(sheets[p.sheet()],p.sx(),p.sy(),p.sw(),p.sh());
            floor=p.floor();
        }
        @Override void update(float deltaTime) {}
        @Override public void draw(Batch batch) { batch.draw(sprite,rect.x,rect.y,rect.width,rect.height); }
    }
    public void refresh(Viewport viewport) {
        float scale=TerrainLayout.TILE_SIZE*RuinLayout.REGION_SIZE;
        int minX=(int)Math.floor((viewport.getCamera().position.x-viewport.getWorldWidth()/2f-2f)/scale);
        int maxX=(int)Math.floor((viewport.getCamera().position.x+viewport.getWorldWidth()/2f+2f)/scale);
        int minY=(int)Math.floor((viewport.getCamera().position.y-viewport.getWorldHeight()/2f-2f)/scale);
        int maxY=(int)Math.floor((viewport.getCamera().position.y+viewport.getWorldHeight()/2f+2f)/scale);
        visible.clear();
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++) {
            long key=key(x,y);
            Array<Piece> pieces=regions.get(key);
            if(pieces==null) {
                pieces=new Array<>();
                Array<Piece> target=pieces;
                RuinArt.pieces(x,y,p->target.add(new Piece(p)));
                regions.put(key,pieces);
            }
            visible.addAll(pieces);
        }
        expired.clear();
        for(LongMap.Entry<Array<Piece>> entry:regions) {
            int x=(int)(entry.key>>32), y=(int)entry.key;
            if(x<minX||x>maxX||y<minY||y>maxY)expired.add(entry.key);
        }
        for(Long key:expired)regions.remove(key);
    }
    public void drawStairs(Batch batch) {
        for(Piece piece:visible)if(piece.floor)piece.draw(batch);
    }
    public void addWalls(Array<GameObject> objects) {
        for(Piece piece:visible)if(!piece.floor)objects.add(piece);
    }
    @Override public void dispose() {
        for(Texture sheet:sheets)sheet.dispose();
        regions.clear(); visible.clear();
    }
}

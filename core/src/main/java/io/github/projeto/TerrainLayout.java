package io.github.projeto;

/** Ground selection from TX Tileset Grass only; shared by rendering and placement. */
public final class TerrainLayout {
    public static final float TILE_SIZE = 32f / 48f;
    private TerrainLayout() {}

    private static long hash(int x, int y, long seed) {
        long value = seed ^ (x * 0x632be59bd9b4e019L) ^ (y * 0x9e3779b97f4a7c15L);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
    private static double value(int x, int y, long seed) {
        return (hash(x, y, seed) >>> 11) * 0x1.0p-53;
    }
    public static int grassVariant(int x, int y) {
        boolean flowers = value(Math.floorDiv(x,7), Math.floorDiv(y,7),891L) > .7
            && value(x,y,912L) > .5;
        return (int)(value(x,y,816L)*4)*8 + (flowers ? 4 : 0) + (int)(value(x,y,415L)*4);
    }

    public static boolean isPath(int x, int y) { return RuinLayout.isPath(x,y); }

    /** One of the intact or worn 16px pavers already embedded in the grass sheet. */
    public static int pathQuarter(int x, int y, int quarter) {
        int px = x * 2 + quarter % 2, py = y * 2 + quarter / 2;
        int variant = (int)(value(px,py,914L)*12);
        int[][] sources = {{0,128},{16,128},{32,128},{0,144},{16,144},{32,144},
            {0,176},{16,176},{32,176},{0,192},{16,192},{32,192}};
        int[] source = sources[variant];
        return source[0] | (source[1] << 8);
    }

    /** Sparse broken pavers alongside paths, also protected from plant generation. */
    public static int scatteredPaver(int x, int y) {
        if (isPath(x,y) || RuinLayout.structure(x,y) != RuinLayout.NONE
            || value(x,y,651L) > .25) return -1;
        return isPath(x-1,y) || isPath(x+1,y) || isPath(x,y-1) || isPath(x,y+1)
            ? (int)(value(x,y,652L)*4) : -1;
    }

    public static boolean isGrassArea(float x, float y, float width, float height) {
        return canPlace(x,y,width,height,true);
    }
    private static double gridCoordinate(float position) {
        double coordinate=position/(double)TILE_SIZE;
        double boundary=Math.rint(coordinate);
        return Math.abs(coordinate-boundary)<.0001?boundary:coordinate;
    }
    public static boolean canPlace(float x, float y, float width, float height, boolean plant) {
        int minX=(int)Math.floor(gridCoordinate(x)), minY=(int)Math.floor(gridCoordinate(y));
        int maxX=(int)Math.ceil(gridCoordinate(x+width))-1;
        int maxY=(int)Math.ceil(gridCoordinate(y+height))-1;
        for(int ty=minY;ty<=maxY;ty++) for(int tx=minX;tx<=maxX;tx++) {
            if(RuinLayout.reserved(tx,ty) || (plant && (isPath(tx,ty)||scatteredPaver(tx,ty)>=0))) return false;
        }
        return true;
    }
}

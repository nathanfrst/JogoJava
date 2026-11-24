package io.github.projeto;

import com.badlogic.gdx.math.Rectangle;

/** Visibility follows solid terrain, independent of the render depth and sprite height. */
public final class CombatGeometry {
    private CombatGeometry() {}
    public static boolean canReach(Rectangle source,Rectangle target) {
        return canReach(source.x+source.width/2f,source.y+Math.min(.05f,source.height/2f),target);
    }
    public static boolean canReach(float startX,float startY,Rectangle target) {
        float endX=target.x+target.width/2f,endY=target.y+Math.min(.05f,target.height/2f);
        float tile=TerrainLayout.TILE_SIZE;
        float sourceLevel=RuinLayout.elevation(startX/tile,startY/tile);
        float targetLevel=RuinLayout.elevation(endX/tile,endY/tile);
        if(Math.abs(sourceLevel-targetLevel)>.15f
            && !RuinLayout.isStair((int)Math.floor(startX/tile),(int)Math.floor(startY/tile))
            && !RuinLayout.isStair((int)Math.floor(endX/tile),(int)Math.floor(endY/tile)))return false;
        float regionSize=tile*RuinLayout.REGION_SIZE;
        int minX=(int)Math.floor(Math.min(startX,endX)/regionSize),maxX=(int)Math.floor(Math.max(startX,endX)/regionSize);
        int minY=(int)Math.floor(Math.min(startY,endY)/regionSize),maxY=(int)Math.floor(Math.max(startY,endY)/regionSize);
        for(int ry=minY;ry<=maxY;ry++)for(int rx=minX;rx<=maxX;rx++) {
            for(RuinLayout.Bounds solid:RuinLayout.region(rx,ry).colliders()) {
                if(intersects(startX,startY,endX,endY,(rx*64+solid.x())*tile,
                    (ry*64+solid.y())*tile,solid.width()*tile,solid.height()*tile))return false;
            }
        }
        return true;
    }
    /** Exact segment/AABB slab test: thin rails and corner crossings cannot be skipped. */
    private static boolean intersects(float sx,float sy,float ex,float ey,float x,float y,float w,float h) {
        float dx=ex-sx,dy=ey-sy,near=0f,far=1f;
        if(Math.abs(dx)<1e-7f) {
            if(sx<x||sx>x+w)return false;
        } else {
            float a=(x-sx)/dx,b=(x+w-sx)/dx;
            if(a>b){float swap=a;a=b;b=swap;}
            near=Math.max(near,a);far=Math.min(far,b);
            if(near>far)return false;
        }
        if(Math.abs(dy)<1e-7f) {
            if(sy<y||sy>y+h)return false;
        } else {
            float a=(y-sy)/dy,b=(y+h-sy)/dy;
            if(a>b){float swap=a;a=b;b=swap;}
            near=Math.max(near,a);far=Math.min(far,b);
            if(near>far)return false;
        }
        return true;
    }
}

package io.github.projeto;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.Arrays;

/** A shared reverse breadth-first search: one field guides every slime to the player. */
public final class SlimeNavigation {
    private static final int SIZE=144, SNAP=16, MARGIN=64;
    private final ObstacleWorld world;
    private final float bodyWidth,bodyHeight;
    private final boolean[] walkable=new boolean[SIZE*SIZE];
    private final int[] distance=new int[SIZE*SIZE],queue=new int[SIZE*SIZE];
    private final int[] offsets={-1,1,-SIZE,SIZE};
    private int originX=Integer.MIN_VALUE,originY=Integer.MIN_VALUE;
    private int goal=-1,goalTileX=Integer.MIN_VALUE,goalTileY=Integer.MIN_VALUE;
    private float refreshTimer,targetX,targetY;
    private boolean ready;
    private long worldRevision=Long.MIN_VALUE;

    public SlimeNavigation(ObstacleWorld world,float bodyWidth,float bodyHeight) {
        this.world=world;this.bodyWidth=bodyWidth;this.bodyHeight=bodyHeight;
    }
    public void update(Rectangle target,float deltaTime) {
        targetX=target.x+target.width/2f;targetY=target.y+target.height/2f;
        int tx=(int)Math.floor(targetX/TerrainLayout.TILE_SIZE);
        int ty=(int)Math.floor(targetY/TerrainLayout.TILE_SIZE);
        refreshTimer-=deltaTime;
        int ox=Math.floorDiv(tx,SNAP)*SNAP-MARGIN,oy=Math.floorDiv(ty,SNAP)*SNAP-MARGIN;
        boolean movedWindow=ox!=originX||oy!=originY;
        boolean changed=worldRevision!=world.getRevision();
        if(ready&&!movedWindow&&!changed&&(refreshTimer>0||tx==goalTileX&&ty==goalTileY))return;
        if(movedWindow||changed) {
            worldRevision=world.getRevision();
            originX=ox;originY=oy;
            Rectangle probe=new Rectangle(0,0,bodyWidth,bodyHeight);
            for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++) {
                probe.setPosition(centerX(x)-bodyWidth/2f,centerY(y)-bodyHeight/2f);
                walkable[y*SIZE+x]=world.canStand(probe);
            }
        }
        refreshTimer=.25f;goalTileX=tx;goalTileY=ty;ready=true;
        Arrays.fill(distance,-1);
        goal=findGoal(tx,ty);
        if(goal<0)return;
        int head=0,tail=0;queue[tail++]=goal;distance[goal]=0;
        while(head<tail) {
            int current=queue[head++];
            for(int offset:offsets) {
                int next=neighbor(current,offset);
                if(next<0||!walkable[next]||distance[next]>=0)continue;
                distance[next]=distance[current]+1;queue[tail++]=next;
            }
        }
    }
    private int findGoal(int tx,int ty) {
        int best=-1;float bestScore=Float.POSITIVE_INFINITY;
        Rectangle sight=new Rectangle(targetX-.05f,targetY-.05f,.1f,.1f);
        float targetLevel=RuinLayout.elevation(targetX/TerrainLayout.TILE_SIZE,targetY/TerrainLayout.TILE_SIZE);
        for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++) {
            int x=tx+dx-originX,y=ty+dy-originY;
            if(x<0||x>=SIZE||y<0||y>=SIZE||!walkable[y*SIZE+x])continue;
            float cx=centerX(x),cy=centerY(y);
            float level=RuinLayout.elevation(cx/TerrainLayout.TILE_SIZE,cy/TerrainLayout.TILE_SIZE);
            float score=Vector2.dst2(cx,cy,targetX,targetY)+Math.abs(level-targetLevel)*4f;
            if(score<bestScore&&world.canTravel(sight,cx,cy)) { bestScore=score;best=y*SIZE+x; }
        }
        return best;
    }
    private int neighbor(int index,int offset) {
        int x=index%SIZE;
        if(offset==-1&&x==0||offset==1&&x==SIZE-1)return -1;
        int next=index+offset;return next>=0&&next<distance.length?next:-1;
    }
    private float centerX(int x) { return (originX+x+.5f)*TerrainLayout.TILE_SIZE; }
    private float centerY(int y) { return (originY+y+.5f)*TerrainLayout.TILE_SIZE; }
    private void point(int index,Vector2 out) { out.set(centerX(index%SIZE),centerY(index/SIZE)); }
    private int descending(int index) {
        int best=index;
        for(int offset:offsets) {
            int next=neighbor(index,offset);
            if(next>=0&&distance[next]>=0&&distance[next]<distance[best])best=next;
        }
        return best;
    }
    public boolean nextWaypoint(Rectangle body,Vector2 out) {
        if(!ready||goal<0)return false;
        float cx=body.x+body.width/2f,cy=body.y+body.height/2f;
        int x=(int)Math.floor(cx/TerrainLayout.TILE_SIZE)-originX;
        int y=(int)Math.floor(cy/TerrainLayout.TILE_SIZE)-originY;
        if(x<0||x>=SIZE||y<0||y>=SIZE)return false;
        int current=y*SIZE+x;
        // Spawn positions need not coincide with cell centers. Find a reachable entry cell.
        if(distance[current]<0) {
            int best=-1;float bestScore=Float.POSITIVE_INFINITY;
            for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++) {
                int nx=x+dx,ny=y+dy;
                if(nx<0||nx>=SIZE||ny<0||ny>=SIZE)continue;
                int index=ny*SIZE+nx;if(distance[index]<0)continue;
                float px=centerX(nx),py=centerY(ny);
                float score=Vector2.dst2(cx,cy,px,py)*4+distance[index];
                if(score<bestScore&&world.canTravel(body,px,py)) { best=index;bestScore=score; }
            }
            if(best<0)return false;
            point(best,out);return true;
        }
        if(current==goal) {
            if(world.canTravel(body,targetX,targetY))out.set(targetX,targetY);
            else point(goal,out);
            return out.dst2(cx,cy)>1e-10f;
        }
        int next=descending(current);
        point(next,out);
        if(!world.canTravel(body,out.x,out.y)) {
            point(current,out);
            return out.dst2(cx,cy)>1e-10f&&world.canTravel(body,out.x,out.y);
        }
        // Smooth only swept, collision-free segments; never cut through a corner.
        int furthest=next;
        for(int i=0;i<6;i++) {
            int candidate=descending(furthest);
            if(candidate==furthest)break;
            float px=centerX(candidate%SIZE),py=centerY(candidate/SIZE);
            if(!world.canTravel(body,px,py))break;
            furthest=candidate;
        }
        point(furthest,out);return true;
    }
}

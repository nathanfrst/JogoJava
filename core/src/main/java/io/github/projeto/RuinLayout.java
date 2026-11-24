package io.github.projeto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Connected orthogonal footprints shared by terrain, walls, collision and navigation. */
public final class RuinLayout {
    public static final int REGION_SIZE=64, NONE=0, WALL=1, STAIRS=2, RAIL=3;
    private static final int SIZE=64, CELLS=SIZE*SIZE;
    public record Terrace(int left,int front,int right,int top,int stairLeft,int stairRight,
                          int level,int style,boolean[] surface) {
        public Terrace(int l,int f,int r,int t,int sl,int sr,int level,int style) {
            this(l,f,r,t,sl,sr,level,style,null);
        }
        public boolean contains(int x,int y) {
            return x>=0&&x<SIZE&&y>=0&&y<SIZE && (surface==null
                ? x>=left&&x<right&&y>=front+2&&y<top : surface[y*SIZE+x]);
        }
    }
    public record Bounds(float x,float y,float width,float height) {}
    public record Edge(Bounds bounds,int kind,int style) {}
    public record Landmark(Obstacle.Kind kind,int x,int y) {}
    public record Template(Terrace[] terraces,Landmark[] landmarks,Bounds[] colliders,
                           Edge[] edges,boolean[] roads,int[] structure) {}
    private record Spec(Terrace[] terraces,Landmark[] landmarks) {}
    private static final Spec[] SPECS=createSpecs();
    private static final Map<Long,Template> CACHE=new LinkedHashMap<>(32,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long,Template> entry) { return size()>256; }
    };
    private RuinLayout() {}
    private static Spec spec(Terrace[] terraces,Landmark[] landmarks) { return new Spec(terraces,landmarks); }
    // Authored themes provide landmarks; individual regions vary the geometry around them.
    private static Spec[] createSpecs() {
        return new Spec[] {
            spec(new Terrace[]{new Terrace(3,11,39,40,11,14,1,0), new Terrace(17,27,34,38,24,27,2,4)},
                new Landmark[]{new Landmark(Obstacle.Kind.STATUE,21,31),new Landmark(Obstacle.Kind.BENCH,7,30),
                    new Landmark(Obstacle.Kind.CHEST,30,32),new Landmark(Obstacle.Kind.WELL,32,21),new Landmark(Obstacle.Kind.BARREL,35,24),
                    new Landmark(Obstacle.Kind.SIGN,15,5)}),
            spec(new Terrace[]{new Terrace(8,13,34,36,19,23,1,2),new Terrace(39,18,53,33,44,48,1,1)},
                new Landmark[]{new Landmark(Obstacle.Kind.FOUNTAIN,14,29),new Landmark(Obstacle.Kind.STATUE,29,29),
                    new Landmark(Obstacle.Kind.BENCH,12,19),new Landmark(Obstacle.Kind.CHEST,42,27),new Landmark(Obstacle.Kind.BARREL,29,23),
                    new Landmark(Obstacle.Kind.SIGN,24,5)}),
            spec(new Terrace[]{new Terrace(4,13,41,40,29,33,1,3),new Terrace(8,25,24,38,13,17,2,5)},
                new Landmark[]{new Landmark(Obstacle.Kind.TOMB,20,19),new Landmark(Obstacle.Kind.CROSS,25,25),
                    new Landmark(Obstacle.Kind.GRAVE,35,23),new Landmark(Obstacle.Kind.GRAVE,35,29),
                    new Landmark(Obstacle.Kind.GRAVE,26,33),new Landmark(Obstacle.Kind.OBELISK,19,32),new Landmark(Obstacle.Kind.CHEST,19,30),
                    new Landmark(Obstacle.Kind.BARREL,36,33),new Landmark(Obstacle.Kind.SIGN,34,5)}),
            spec(new Terrace[]{new Terrace(4,15,24,37,11,15,1,1),new Terrace(30,12,52,33,39,43,1,4)},
                new Landmark[]{new Landmark(Obstacle.Kind.CHEST,19,29),new Landmark(Obstacle.Kind.WELL,8,24),
                    new Landmark(Obstacle.Kind.STATUE,34,27),new Landmark(Obstacle.Kind.BENCH,46,20),
                    new Landmark(Obstacle.Kind.CRATE_STACK,34,18),new Landmark(Obstacle.Kind.BARREL,48,26),
                    new Landmark(Obstacle.Kind.SIGN,16,5)}),
            spec(new Terrace[]{new Terrace(6,13,32,31,17,21,1,5)},
                new Landmark[]{new Landmark(Obstacle.Kind.BENCH,11,23),new Landmark(Obstacle.Kind.CHEST,26,25),
                    new Landmark(Obstacle.Kind.CRATE_STACK,35,22),new Landmark(Obstacle.Kind.CRATE_STACK,48,23),
                    new Landmark(Obstacle.Kind.WELL,42,29),new Landmark(Obstacle.Kind.SIGN,22,5),new Landmark(Obstacle.Kind.BARREL,26,19)})
        };
    }

    private static long hash(int x,int y,long seed) {
        long n=seed^x*0x632be59bd9b4e019L^y*0x9e3779b97f4a7c15L;
        n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;
        return n^(n>>>27);
    }
    private static int choice(int x,int y,long seed,int range) { return (int)Math.floorMod(hash(x,y,seed),range); }
    public static int variant(int rx,int ry) { return rx==0&&ry==0?0:choice(rx,ry,0,5); }
    public static Template region(int rx,int ry) {
        long key=((long)rx<<32)|(ry&0xffffffffL);
        Template result=CACHE.get(key);
        if(result==null) { result=generate(rx,ry);CACHE.put(key,result); }
        return result;
    }
    private static int clamp(int n,int low,int high) { return Math.max(low,Math.min(high,n)); }
    private static Template generate(int rx,int ry) {
        Spec source=SPECS[variant(rx,ry)];
        Terrace[] terraces=new Terrace[source.terraces.length];
        boolean origin=rx==0&&ry==0;
        for(int i=0;i<terraces.length;i++) {
            Terrace base=source.terraces[i];
            int dx=origin?0:choice(rx,ry,31+i,5)-2;
            int left=clamp(base.left+dx,3,50),right=clamp(base.right+dx+(origin?0:choice(rx,ry,41+i,7)-3),left+12,53);
            int front=clamp(base.front+(origin?0:choice(rx,ry,51+i,5)-1),11,30);
            int top=clamp(base.top+(origin?0:choice(rx,ry,61+i,9)-4),front+10,46);
            int stairWidth=origin?base.stairRight-base.stairLeft:3+choice(rx,ry,71+i,2);
            int stairLeft=clamp(base.stairLeft+dx+(origin?0:choice(rx,ry,81+i,5)-2),left+3,right-stairWidth-3);
            if(base.level>1) {
                Terrace parent=terraces[0];
                left=clamp(left,parent.left+3,parent.right-15);
                right=clamp(right,left+10,parent.right-2);
                front=clamp(front,parent.front+11,parent.top-9);
                top=clamp(top,front+7,parent.top-2);
                stairLeft=clamp(stairLeft,left+2,right-stairWidth-2);
            }
            boolean[] surface=new boolean[CELLS];
            int profile=origin?(i==0?0:4):choice(rx,ry,101+i,5);
            int midY=front+2+(top-front-2)/2;
            int cut=Math.max(3,(right-left)/3);
            int stemLeft=clamp(stairLeft-4,left,right-10),stemRight=clamp(stairLeft+stairWidth+4,stemLeft+9,right);
            for(int y=front+2;y<top;y++)for(int x=left;x<right;x++) {
                boolean keep=switch(profile) {
                    case 0 -> !(x<left+cut && y>=midY);
                    case 1 -> !(x>=right-cut && y>=midY);
                    case 2 -> y>=midY || x>=stemLeft&&x<stemRight;
                    case 3 -> !(x>=right-cut && y>=midY-2&&y<top-3);
                    default -> y<top-5 || x>=left+1&&x<right-1;
                };
                surface[y*SIZE+x]=keep;
            }
            terraces[i]=new Terrace(left,front,right,top,stairLeft,stairLeft+stairWidth,
                base.level,origin?base.style:choice(rx,ry,91+i,6),surface);
        }
        // The upper terrace and its approach must have supporting ground on the lower one.
        for(Terrace upper:terraces)if(upper.level>1) {
            Terrace parent=terraces[0];
            for(int y=Math.max(parent.front+2,upper.front-4);y<Math.min(parent.top,upper.top+1);y++)
                for(int x=Math.max(parent.left,upper.left-2);x<Math.min(parent.right,upper.right+2);x++)parent.surface[y*SIZE+x]=true;
        }
        ArrayList<Edge> edges=new ArrayList<>();ArrayList<Bounds> colliders=new ArrayList<>();
        int[] structure=new int[CELLS];
        for(Terrace terrace:terraces) {
            for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++)if(terrace.contains(x,y)) {
                if(!terrace.contains(x,y-1) && !(y==terrace.front+2&&x>=terrace.stairLeft&&x<terrace.stairRight))
                    edge(edges,colliders,new Bounds(x,y-2,1,2),WALL,terrace.style);
                if(!terrace.contains(x-1,y))edge(edges,colliders,new Bounds(x,y,.3f,1),4,terrace.style);
                if(!terrace.contains(x+1,y))edge(edges,colliders,new Bounds(x+.7f,y,.3f,1),5,terrace.style);
                if(!terrace.contains(x,y+1))edge(edges,colliders,new Bounds(x,y+.7f,1,.3f),RAIL,terrace.style);
            }
        }
        for(Edge edge:edges) {
            Bounds b=edge.bounds;
            for(int y=(int)b.y;y<Math.ceil(b.y+b.height-.0001f);y++)for(int x=(int)b.x;x<Math.ceil(b.x+b.width-.0001f);x++) {
                if(x>=0&&x<SIZE&&y>=0&&y<SIZE)structure[y*SIZE+x]=edge.kind==WALL?WALL:RAIL;
            }
        }
        for(Terrace t:terraces)for(int y=t.front-1;y<t.front+3;y++)for(int x=t.stairLeft;x<t.stairRight;x++)structure[y*SIZE+x]=STAIRS;
        Builder builder=new Builder(terraces,colliders.toArray(Bounds[]::new),structure);
        int west=4+choice(rx,ry,121,4),east=4+choice(rx+1,ry,121,4);
        int south=56+choice(rx,ry,122,3),north=56+choice(rx,ry+1,122,3);
        builder.route(0,west,18,6);
        builder.route(18,6,45,east);
        builder.route(45,east,63,east);
        builder.route(south,0,57,16);
        builder.route(57,16,56,44);
        builder.route(56,44,north,63);
        for(Terrace t:terraces) {
            int sx=(t.stairLeft+t.stairRight)/2;
            builder.connect(sx,t.front-2);
            builder.route(sx,t.front-2,sx,Math.min(t.top-4,t.front+7));
        }
        ArrayList<Landmark> marks=new ArrayList<>();
        for(Landmark desired:source.landmarks) {
            Landmark placed=builder.place(desired,marks);
            if(placed!=null)marks.add(placed);
        }
        // Short branches lead from the walkways to the paved landmark pads.
        builder.landmarks=marks.toArray(Landmark[]::new);
        for(Landmark mark:marks)if(mark.kind!=Obstacle.Kind.SIGN)builder.connect(mark.x+2,mark.y+2);
        // A landmark itself is not a road obstacle; its surrounding pad remains paved.
        for(Landmark mark:marks)if(mark.kind!=Obstacle.Kind.SIGN) {
            int maxX=(int)Math.ceil(mark.x+mark.kind.bodyWidth/TerrainLayout.TILE_SIZE);
            int maxY=(int)Math.ceil(mark.y+mark.kind.bodyHeight/TerrainLayout.TILE_SIZE);
            for(int y=mark.y;y<maxY;y++)for(int x=mark.x;x<maxX;x++)builder.roads[y*SIZE+x]=false;
        }
        return new Template(terraces,marks.toArray(Landmark[]::new),builder.solids,
            edges.toArray(Edge[]::new),builder.roads,structure);
    }
    private static void edge(ArrayList<Edge> edges,ArrayList<Bounds> solids,Bounds b,int kind,int style) {
        edges.add(new Edge(b,kind,style));solids.add(b);
    }
    private static boolean overlaps(float x,float y,float w,float h,Bounds b) {
        return x<b.x+b.width&&x+w>b.x&&y<b.y+b.height&&y+h>b.y;
    }
    private static float level(Terrace[] terraces,float x,float y) {
        float result=0;
        for(Terrace t:terraces) {
            if(x>=t.stairLeft&&x<t.stairRight&&y>=t.front-1&&y<t.front+2)
                result=Math.max(result,t.level-1+(y-t.front+1)/3f);
            else if(t.contains((int)Math.floor(x),(int)Math.floor(y)))result=Math.max(result,t.level);
        }
        return result;
    }
    private static final class Builder {
        final Terrace[] terraces;final Bounds[] solids;final int[] structure;
        final boolean[] walkable=new boolean[CELLS],roads=new boolean[CELLS];
        final int[] queue=new int[CELLS],parent=new int[CELLS];
        Landmark[] landmarks=new Landmark[0];
        Builder(Terrace[] terraces,Bounds[] solids,int[] structure) {
            this.terraces=terraces;this.solids=solids;this.structure=structure;
            for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++) {
                boolean free=true;
                for(Bounds b:solids)if(overlaps(x-.25f,y-.25f,1.5f,1.5f,b)){free=false;break;}
                walkable[y*SIZE+x]=free;
            }
        }
        boolean avoidsLandmarks(int x,int y) {
            for(Landmark mark:landmarks) {
                Bounds b=new Bounds(mark.x,mark.y,mark.kind.bodyWidth/TerrainLayout.TILE_SIZE,mark.kind.bodyHeight/TerrainLayout.TILE_SIZE);
                if(overlaps(x-.25f,y-.25f,1.5f,1.5f,b))return false;
            }
            return true;
        }
        int nearest(int x,int y) {
            int best=-1;float distance=Float.POSITIVE_INFINITY;
            for(int dy=-4;dy<=4;dy++)for(int dx=-4;dx<=4;dx++) {
                int nx=x+dx,ny=y+dy;
                if(nx<0||nx>=SIZE||ny<0||ny>=SIZE||!walkable[ny*SIZE+nx]||!avoidsLandmarks(nx,ny))continue;
                float score=dx*dx+dy*dy;
                if(score<distance){distance=score;best=ny*SIZE+nx;}
            }
            return best;
        }
        void route(int sx,int sy,int gx,int gy) { search(nearest(sx,sy),nearest(gx,gy),false); }
        void connect(int x,int y) { search(nearest(x,y),-1,true); }
        void search(int start,int goal,boolean connect) {
            if(start<0||!connect&&goal<0)return;
            Arrays.fill(parent,-2);int head=0,tail=0;queue[tail++]=start;parent[start]=-1;int end=-1;
            while(head<tail) {
                int cell=queue[head++];
                if(connect?roads[cell]:cell==goal){end=cell;break;}
                int x=cell%SIZE,y=cell/SIZE;
                for(int dir=0;dir<4;dir++) {
                    int nx=x+(dir==0?1:dir==1?-1:0),ny=y+(dir==2?1:dir==3?-1:0);
                    if(nx<0||nx>=SIZE||ny<0||ny>=SIZE)continue;
                    int next=ny*SIZE+nx;
                    if(parent[next]!=-2||!walkable[next]||!avoidsLandmarks(nx,ny))continue;
                    parent[next]=cell;queue[tail++]=next;
                }
            }
            while(end>=0) {
                int x=end%SIZE,y=end/SIZE;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx,ny=y+dy;
                    if(nx>=0&&nx<SIZE&&ny>=0&&ny<SIZE&&(structure[ny*SIZE+nx]==NONE||structure[ny*SIZE+nx]==STAIRS))roads[ny*SIZE+nx]=true;
                }
                end=parent[end];
            }
        }
        Landmark place(Landmark desired,ArrayList<Landmark> placed) {
            Landmark best=null;float bestScore=Float.POSITIVE_INFINITY;
            float desiredLevel=Math.max(desired.kind==Obstacle.Kind.SIGN?0:1,level(terraces,desired.x+.2f,desired.y+.2f));
            float w=desired.kind.bodyWidth/TerrainLayout.TILE_SIZE,h=desired.kind.bodyHeight/TerrainLayout.TILE_SIZE;
            for(int y=3;y<51;y++)for(int x=3;x<53;x++) {
                float ground=level(terraces,x+.2f,y+.2f);
                if(desired.kind!=Obstacle.Kind.SIGN&&ground<1 || desired.kind==Obstacle.Kind.SIGN&&ground>0)continue;
                boolean valid=true,nearRoad=false;
                for(int ty=y-1;ty<=Math.ceil(y+h);ty++)for(int tx=x-1;tx<=Math.ceil(x+w);tx++) {
                    if(structure[ty*SIZE+tx]!=NONE)valid=false;
                    if(roads[ty*SIZE+tx])nearRoad=true;
                }
                for(int ty=y;ty<Math.ceil(y+h);ty++)for(int tx=x;tx<Math.ceil(x+w);tx++)if(roads[ty*SIZE+tx])valid=false;
                if(!valid||desired.kind==Obstacle.Kind.SIGN&&!nearRoad)continue;
                for(Landmark mark:placed)if(overlaps(x-1,y-1,w+2,h+2,new Bounds(mark.x,mark.y,
                    mark.kind.bodyWidth/TerrainLayout.TILE_SIZE,mark.kind.bodyHeight/TerrainLayout.TILE_SIZE)))valid=false;
                if(!valid)continue;
                float score=(x-desired.x)*(x-desired.x)+(y-desired.y)*(y-desired.y)+Math.abs(ground-desiredLevel)*100;
                if(score<bestScore){bestScore=score;best=new Landmark(desired.kind,x,y);}
            }
            return best;
        }
    }
    public static int structure(int x,int y) {
        return region(Math.floorDiv(x,SIZE),Math.floorDiv(y,SIZE)).structure[Math.floorMod(y,SIZE)*SIZE+Math.floorMod(x,SIZE)];
    }
    public static boolean isBlocked(int x,int y) { int kind=structure(x,y);return kind==WALL||kind==RAIL; }
    public static boolean isStair(int x,int y) { return structure(x,y)==STAIRS; }
    public static float clipAxis(float x,float y,float width,float height,float amount,boolean horizontal) {
        float t=TerrainLayout.TILE_SIZE;
        int minX=(int)Math.floor(x/t/SIZE),minY=(int)Math.floor(y/t/SIZE);
        int maxX=(int)Math.floor((x+width)/t/SIZE),maxY=(int)Math.floor((y+height)/t/SIZE);
        for(int ry=minY;ry<=maxY;ry++)for(int rx=minX;rx<=maxX;rx++)for(Bounds b:region(rx,ry).colliders) {
            float left=(rx*SIZE+b.x)*t,bottom=(ry*SIZE+b.y)*t,w=b.width*t,h=b.height*t;
            if(x>=left+w||x+width<=left||y>=bottom+h||y+height<=bottom)continue;
            if(horizontal)x=amount>0?left-width:left+w;else y=amount>0?bottom-height:bottom+h;
        }
        return horizontal?x:y;
    }
    public static float elevation(float x,float y) {
        int rx=(int)Math.floor(x/SIZE),ry=(int)Math.floor(y/SIZE);
        return level(region(rx,ry).terraces,x-rx*SIZE,y-ry*SIZE);
    }
    public static boolean isPropPad(int x,int y) {
        Template template=region(Math.floorDiv(x,SIZE),Math.floorDiv(y,SIZE));
        int lx=Math.floorMod(x,SIZE),ly=Math.floorMod(y,SIZE);
        for(Landmark mark:template.landmarks)if(mark.kind!=Obstacle.Kind.SIGN) {
            if(lx>=mark.x-1&&lx<Math.ceil(mark.x+mark.kind.bodyWidth/TerrainLayout.TILE_SIZE)+1
                &&ly>=mark.y-1&&ly<Math.ceil(mark.y+mark.kind.bodyHeight/TerrainLayout.TILE_SIZE)+1)return true;
        }
        return false;
    }
    public static boolean isPath(int x,int y) { return !isBlocked(x,y)&&(isRoad(x,y)||isPropPad(x,y)); }
    public static boolean isRoad(int x,int y) {
        return region(Math.floorDiv(x,SIZE),Math.floorDiv(y,SIZE)).roads[Math.floorMod(y,SIZE)*SIZE+Math.floorMod(x,SIZE)];
    }
    public static boolean reserved(int x,int y) {
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)if(structure(x+dx,y+dy)!=NONE)return true;
        return isRoad(x,y);
    }
}

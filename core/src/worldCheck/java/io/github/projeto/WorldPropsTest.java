package io.github.projeto;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.HashSet;
import java.util.Set;

public class WorldPropsTest {
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    private static Viewport view(float x,float y,float width,float height) {
        Viewport view=new Viewport(){};view.setCamera(new OrthographicCamera());
        view.setWorldSize(width,height);view.getCamera().position.set(x,y,0);return view;
    }
    private static Obstacle chest(ObstacleWorld world,float x,float y) {
        for(Obstacle obstacle:world.getObstacles()) {
            if(obstacle.isBreakable()&&Math.abs(obstacle.rect.x-x)<.01&&Math.abs(obstacle.rect.y-y)<.01)return obstacle;
        }
        return null;
    }
    public static void main(String[] args) {
        float t=TerrainLayout.TILE_SIZE;
        ObstacleWorld world=new ObstacleWorld(8,4.5f);
        Set<Integer> seen=new HashSet<>();
        for(int ry=-2;ry<=2;ry++)for(int rx=-2;rx<=2;rx++) {
            if(!seen.add(RuinLayout.variant(rx,ry)))continue;
            Viewport view=view((rx*64+32)*t,(ry*64+32)*t,64*t,64*t);world.refresh(view);
            int chests=0,props=0,signs=0,barrels=0;
            for(Obstacle obstacle:world.getObstacles()) {
                int regionX=(int)Math.floor(obstacle.rect.x/(64*t)),regionY=(int)Math.floor(obstacle.rect.y/(64*t));
                if(regionX!=rx||regionY!=ry)continue;
                if(obstacle.getKind().plantSheet) {
                    require(TerrainLayout.isGrassArea(obstacle.rect.x,obstacle.rect.y,obstacle.rect.width,obstacle.rect.height),"Plant on paving");
                    continue;
                }
                props++;
                boolean authored=false;
                for(RuinLayout.Landmark mark:RuinLayout.region(rx,ry).landmarks()) {
                    if(mark.kind()==obstacle.getKind()&&Math.abs(obstacle.rect.x-(rx*64+mark.x())*t)<.01
                        &&Math.abs(obstacle.rect.y-(ry*64+mark.y())*t)<.01)authored=true;
                }
                require(authored,"Random wilderness prop: "+obstacle.getKind());
                if(obstacle.isBreakable()) {
                    chests++;
                    require(RuinLayout.elevation((obstacle.rect.x+.1f)/t,(obstacle.rect.y+.1f)/t)>=1,"Chest outside a courtyard");
                    require(RuinLayout.isPropPad((int)Math.floor((obstacle.rect.x+.1f)/t),(int)Math.floor((obstacle.rect.y+.1f)/t)),"Chest lacks stone pad");
                }
                if(obstacle.getKind()==Obstacle.Kind.SIGN)signs++;
                if(obstacle.getKind()==Obstacle.Kind.BARREL)barrels++;
            }
            require(chests==1,"Expected one authored chest in variant "+RuinLayout.variant(rx,ry)+", got "+chests);
            require(signs==1,"Missing junction sign in variant "+RuinLayout.variant(rx,ry));
            require(barrels==1,"Missing contextual barrel in variant "+RuinLayout.variant(rx,ry));
            require(props<=10,"Props remain too dense");
            // The structures sheet now supplies stairs only, with no freestanding arches.
            RuinArt.pieces(rx,ry,piece -> require(piece.sheet()!=1||piece.floor(),"Unexpected arch rendering"));
        }
        Viewport origin=view(30*t,32*t,10,10);world.refresh(origin);
        Obstacle chest=chest(world,30*t,32*t);require(chest!=null,"Canonical chest missing");
        Rectangle hitbox=new Rectangle(chest.rect);
        require(!world.canStand(hitbox),"Chest should block movement before breaking");
        long revision=world.getRevision();
        require(world.breakChests(hitbox)==1,"Chest must award one destruction");
        require(world.getRevision()>revision,"Collision revision not changed");
        require(world.breakChests(hitbox)==0,"Repeated attack awards duplicate score");
        require(world.canStand(hitbox),"Broken chest remains solid");
        require(chest(world,30*t,32*t)==null,"Broken chest remains visible");
        world.refresh(view(-300,300,10,10));world.refresh(origin);
        require(chest(world,30*t,32*t)==null,"Unloading respawned broken chest");
        world.reset();world.refresh(origin);
        require(chest(world,30*t,32*t)!=null,"Restart did not restore chest");
        world.dispose();System.out.println("Contextual props and chest persistence checks passed.");
    }
}

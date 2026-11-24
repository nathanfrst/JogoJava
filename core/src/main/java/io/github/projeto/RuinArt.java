package io.github.projeto;

import java.util.function.Consumer;

/** Only exposed footprint edges become walls; connected sections never get internal seams. */
public final class RuinArt {
    public record Piece(int sheet,int sx,int sy,int sw,int sh,
                        float x,float y,float width,float height,boolean floor) {}
    private RuinArt() {}
    public static void pieces(int rx,int ry,Consumer<Piece> output) {
        int ox=rx*64,oy=ry*64;
        RuinLayout.Template template=RuinLayout.region(rx,ry);
        for(RuinLayout.Edge edge:template.edges()) {
            RuinLayout.Bounds b=edge.bounds();
            int sx,sy,sw,sh;
            switch(edge.kind()) {
                case RuinLayout.WALL -> {sx=((int)b.x()+edge.style())%9==0?192:64;sy=192;sw=32;sh=64;}
                case 4 -> {sx=288;sy=32;sw=8;sh=32;}
                case 5 -> {sx=344;sy=32;sw=8;sh=32;}
                default -> {sx=384;sy=32;sw=32;sh=8;}
            }
            output.accept(new Piece(0,sx,sy,sw,sh,ox+b.x(),oy+b.y(),b.width(),b.height(),false));
        }
        for(RuinLayout.Terrace t:template.terraces()) {
            output.accept(new Piece(1,32+t.style()%3*96,32+t.style()/3*128,64,96,
                ox+t.stairLeft(),oy+t.front()-1,t.stairRight()-t.stairLeft(),3,true));
            for(int x:new int[]{t.stairLeft()-1,t.stairRight()}) {
                output.accept(new Piece(2,352,176,32,80,ox+x,oy+t.front(),1,2.5f,false));
            }
        }
    }
}

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

final class CinderQueenView {
    private static final BufferedImage FLY = GameAssets.load(ForestArt.FOREST + "Mob/Small Bee/Fly/Fly-Sheet.png");
    private static final BufferedImage ATTACK = GameAssets.load(ForestArt.FOREST + "Mob/Small Bee/Attack/Attack-Sheet.png");
    private static final BufferedImage FIRE = GameAssets.load(ForestArt.SWORDS + "Particle FX/Fire_02.png");
    private static final BufferedImage BLAST = GameAssets.load(ForestArt.SWORDS + "Particle FX/Explosion_01.png");
    private static final Color HOT = new Color(255, 239, 147);
    private static final Color EMBER = new Color(253, 99, 27);
    private static final Color HALO = new Color(255, 147, 38, 58);

    private CinderQueenView() {}

    static void draw(Graphics2D g, CinderQueen boss) {
        if (boss.aiming()) {
            g.setColor(new Color(255, 221, 154, 185));
            for (int i=-1; i<=1; i++) {
                double angle=boss.aim()+i*(boss.phase()==1 ? .38 : .57);
                for (int d=80; d<=170; d+=18) {
                    int x=(int)(boss.x()+d*Math.cos(angle)), y=(int)(boss.y()+d*Math.sin(angle));
                    g.fillOval(x-2,y-2,4,4);
                }
            }
        }
        Graphics2D body = (Graphics2D) g.create();
        body.translate(boss.x(), boss.y());
        double scale = 1 - boss.shutdownProgress();
        body.scale(-boss.facing() * scale, scale);
        int pulse = (int)(5 * Math.sin(boss.clock() * 12));
        body.setColor(HALO);
        body.fillOval(-62-pulse, -54-pulse, 124+2*pulse, 110+2*pulse);
        // A wreath of burning motes outlines the queen even against the darker trees.
        int motes = 4 + 2 * boss.phase();
        for (int i=0; i<motes; i++) {
            double a = i*2*Math.PI/motes + boss.clock()*1.7;
            int x=(int)(Math.cos(a)*55), y=(int)(Math.sin(a)*44);
            body.setColor(EMBER);
            body.fillOval(x-5,y-5,10,10);
            body.setColor(HOT);
            body.fillOval(x-2,y-2,4,4);
        }
        boolean attacking = boss.wave() != CinderQueen.Wave.COOLING;
        int frame = (int)(boss.clock() * (attacking ? 11 : 7)) % 4;
        BufferedImage bee = attacking && boss.wave() == CinderQueen.Wave.STINGER ? ATTACK : FLY;
        // The abdomen is centered at (30, 32) in the 64 px animation cells.
        ForestArt.tile(body, bee, frame*64, 0, 64, 64, -174,-186,372,372);
        if (boss.exposed() || boss.flashing()) {
            body.setColor(boss.flashing() ? Color.WHITE : HOT);
            body.setStroke(new BasicStroke(3));
            body.drawArc(-52,-50,104,100,25,120);
            body.drawArc(-52,-50,104,100,205,120);
        }
        body.dispose();

        for (CinderQueen.Flame f : boss.flames()) drawFlame(g, f);
    }

    private static void drawFlame(Graphics2D g, CinderQueen.Flame f) {
        int x=(int) f.x, y=(int) f.y;
        if (f.burst) {
            int frame = Math.min(7, (int)(f.age/.38*8));
            ForestArt.tile(g, BLAST, frame*192+48,42,96,96,x-53,y-64,106,106);
            return;
        }
        if (f.age < f.warning) {
            int floor = Arena.TOP + Arena.FOREST_HEIGHT - 28;
            g.setColor(new Color(255, 169, 64, 35));
            g.fillRect(x-12, y, 24, floor-y);
            g.setColor(HOT);
            g.drawLine(x, y, x, y+15+(int)(22*f.age/f.warning));
            g.drawLine(x-7, floor-8, x, floor-2);
            g.drawLine(x+7, floor-8, x, floor-2);
            return;
        }
        int r = f.radius();
        double speed=Math.hypot(f.vx,f.vy);
        double ux=f.vx/speed, uy=f.vy/speed;
        for (int i=3; i>0; i--) {
            int drift=(int)(Math.sin(18*f.age+i*1.7)*i*1.6);
            int tx=(int)(x-ux*r*i*1.15-uy*drift), ty=(int)(y-uy*r*i*1.15+ux*drift);
            int size=r*(4-i)/3;
            g.setColor(i==3 ? HALO : EMBER);
            g.fillOval(tx-size/2,ty-size/2,size,size);
        }
        g.setColor(HALO);
        g.fillOval(x-r-6,y-r-6,2*r+12,2*r+12);
        Graphics2D tail=(Graphics2D)g.create();
        tail.translate(x,y);
        tail.rotate(Math.atan2(uy,ux));
        double flicker=Math.sin(f.age*26 + x*.08);
        Path2D plume=new Path2D.Double();
        plume.moveTo(0,-r);
        plume.quadTo(-r*1.6,-r*1.3,-r*(2.4+.3*flicker),-r*.7);
        plume.quadTo(-r*1.7,-r*.6,-r*1.6,-r*.2);
        plume.quadTo(-r*2.5,r*.35,-r*(3.3+.5*flicker),r*.25);
        plume.quadTo(-r*2.2,r*1.1,-r*1.3,r*.7);
        plume.quadTo(-r*1.8,r*1.3,0,r);
        plume.closePath();
        tail.setColor(f.kind == CinderQueen.Kind.CINDER ? new Color(218, 63, 122) : EMBER);
        tail.fill(plume);
        tail.fillOval(-r,-r,r*2,r*2);
        tail.setColor(new Color(255,181,47));
        tail.fillOval(-2*r,-r/2,2*r,r);
        tail.dispose();
        g.setColor(HOT);
        g.fillOval(x-r/2,y-r/2,r,r);
        int frame=2+(int)(f.age*15)%3;
        ForestArt.tile(g,FIRE,frame*64+12,26,40,38,x-r,y-r,r*2,r*2);
    }
}

package io.github.projeto;

public class BenchRecoveryTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        ObstacleWorld world=new ObstacleWorld(8,4.5f);
        Player player=new Player(8,4.5f,null,null,null,world);
        BenchRecovery rest=new BenchRecovery();
        check(rest.tryHeal(player,20)==0,"Full life charged score");
        player.subLife(2);
        check(rest.tryHeal(player,9)==0&&player.getHealth()==3,"Insufficient score healed");
        check(rest.getRemaining()==0,"Failed attempt started cooldown");
        int score=25;score-=rest.tryHeal(player,score);
        check(score==15&&player.getHealth()==5,"Successful healing did not deduct exactly10");
        check(rest.getRemaining()==20&&rest.prompt(player,score).contains("20s"),"Cooldown not shown immediately");
        player.subLife(2);
        check(rest.tryHeal(player,score)==0&&player.getHealth()==3,"Repeated E bypassed cooldown");
        rest.update(7);check(rest.getRemaining()==13&&rest.prompt(player,score).contains("13s"),"Countdown incorrect");
        rest.update(13);score-=rest.tryHeal(player,score);
        check(score==5&&player.getHealth()==5,"Cooldown did not permit later paid heal");
        rest.update(100);player.subLife(1);
        check(rest.tryHeal(player,score)==0&&player.getHealth()==4,"Score could become negative");
        rest.reset();check(rest.getRemaining()==0,"New run kept cooldown");
        player.subLife(100);check(rest.tryHeal(player,100)==0&&player.isDead(),"Healing revived player");
        world.dispose();System.out.println("Paid healing checks passed: cost, cooldown, countdown, failed attempts, death and restart.");
    }
}

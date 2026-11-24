package io.github.projeto;

public final class SurvivalProgressTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void close(double actual,double expected,String message) {
        check(Math.abs(actual-expected)<.00001,message+": "+actual);
    }
    public static void main(String[] args) {
        SurvivalProgress run=new SurvivalProgress();
        check(run.getDisplayTime().equals("00:00"),"Initial timer");
        close(run.getSpawnInterval(),.75,"Initial spawn interval");
        run.update(59);check(run.getDisplayTime().equals("00:59"),"Seconds formatting");
        run.update(1);check(run.getDisplayTime().equals("01:00"),"Minute rollover");
        close(run.getSpawnInterval(),.65,"Difficulty after one minute");
        float previous=run.getSpawnInterval();
        for(int i=0;i<600;i++) {
            run.update(.5f);float interval=run.getSpawnInterval();
            check(interval<=previous&&interval>=SurvivalProgress.MIN_SPAWN_INTERVAL,"Nonmonotonic or excessive difficulty");
            previous=interval;
        }
        close(run.getSpawnInterval(),.25,"Minimum interval not reached/capped");
        check(run.getDisplayTime().equals("06:00"),"Survival elapsed time drift");
        run.update(3240);check(run.getDisplayTime().equals("60:00"),"Long runs lost elapsed minutes");
        close(run.getSpawnInterval(),.25,"Long run exceeded minimum interval");
        double time=run.getElapsedSeconds();run.update(-1);run.update(Float.NaN);run.update(Float.POSITIVE_INFINITY);
        close(run.getElapsedSeconds(),time,"Invalid delta altered time");
        run.reset();check(run.getDisplayTime().equals("00:00"),"Restart retained timer");
        close(run.getSpawnInterval(),.75,"Restart retained difficulty");
        SurvivalProgress a=new SurvivalProgress(),b=new SurvivalProgress();
        for(int i=0;i<600;i++)a.update(.1f);b.update(60);
        close(a.getElapsedSeconds(),b.getElapsedSeconds(),"Frame partition changed timer");
        close(a.getSpawnInterval(),b.getSpawnInterval(),"Frame partition changed difficulty");
        System.out.println("Survival checks passed: clock, minute rollover, gradual difficulty, minimum, frame partition and restart.");
    }
}

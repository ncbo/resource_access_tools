package org.ncbo.resource_access_tools.util;

/**
 * Simply used for to measure execution time.
 */

public class ExecutionTimer {
    private long start;
    private long end;

    public ExecutionTimer() {
        reset();
    }

    public void start() {
        start = System.currentTimeMillis();
    }

    public void end() {
        end = System.currentTimeMillis();
    }

    public long duration() {
        return (end - start);
    }

    private void reset() {
        start = 0;
        end = 0;
    }

    public String millisecondsToTimeString(long duration) {
        long time = duration / 1000;
        String seconds = Integer.toString((int) (time % 60));
        String minutes = Integer.toString((int) ((time % 3600) / 60));
        String hours = Integer.toString((int) (time / 3600));
        for (int i = 0; i < 2; i++) {
            if (seconds.length() < 2) {
                seconds = "0" + seconds;
            }
            if (minutes.length() < 2) {
                minutes = "0" + minutes;
            }
            if (hours.length() < 2) {
                hours = "0" + hours;
            }
        }
        return hours + " hours, " + minutes + " minutes, " + seconds + " seconds.";
    }

    public String display() {
        return "\t\tExecution done in: " + this.millisecondsToTimeString(this.duration()) + "(" + this.duration() + "ms).";
    }

    private String millisecondsToTimeString2(long duration) {
        long time = duration / 1000;
        String seconds = Integer.toString((int) (time % 60));
        String minutes = Integer.toString((int) ((time % 3600) / 60));
        String hours = Integer.toString((int) (time / 3600));
        for (int i = 0; i < 2; i++) {
            if (seconds.length() < 2) {
                seconds = "0" + seconds;
            }
            if (minutes.length() < 2) {
                minutes = "0" + minutes;
            }
            if (hours.length() < 2) {
                hours = "0" + hours;
            }
        }
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    public String display2() {
        return "[" + this.millisecondsToTimeString2(this.duration()) + "]";
    }

	  /*
      public static void main(String s[]) {
	    // simple example
	    ExecutionTimer t = new ExecutionTimer();
	    t.start();
	    for (int i=0; i < 80; i++){ System.out.print(".");}
	    t.end();
	    System.out.println("\n" + t.duration() + " ms");
	  }
	  */
}

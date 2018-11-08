/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

public class Mutex {
    private int threadCount;
    private boolean x;

    public Mutex() {
    }

    public Mutex(int side) {
        if (side == 0) {
            threadCount = 0;
            x = true;
        }
    }

    public int getThreadCount() {
        return threadCount;
    }

    public boolean isX() {
        return x;
    }

    public void increaseThreadCount() {
        this.threadCount++;
    }

    public void setX() {
        this.x = !x;
    }

    public void restartThreadCount() {
        this.threadCount = 0;
    }
}


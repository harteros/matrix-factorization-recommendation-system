/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

public class WorkerProfile {

    private int cores;
    private double ram;

    private int x_start;
    private int x_end;
    private int y_start;
    private int y_end;

    public WorkerProfile(int cores, double ram) {
        this.cores = cores;
        this.ram = (ram / 1024.0) / 1024.0;
    }

    public int getCores() {
        return cores;
    }

    public double getRam() {
        return ram;
    }

    public int getXStart() {
        return x_start;
    }

    public int getXEnd() {
        return x_end;
    }

    public int getYStart() {
        return y_start;
    }

    public int getYEnd() {
        return y_end;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public void setRam(double ram) {
        this.ram = ram;
    }

    public void setXStart(int start) {
        this.x_start = start;
    }

    public void setXEnd(int end) {
        this.x_end = end;
    }

    public void setYStart(int start) {
        this.y_start = start;
    }

    public void setYEnd(int end) {
        this.y_end = end;
    }
}

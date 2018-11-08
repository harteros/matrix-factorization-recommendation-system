/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

import org.apache.commons.math3.linear.RealMatrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ActionForWorkers extends Thread {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private MasterNode master;
    private Socket connection;
    private int workerID;
    private static Mutex lock;

    public ActionForWorkers(Socket connection, MasterNode master, int workerID) {
        //initialize worker thread
        this.master = master;
        this.connection = connection;
        this.workerID = workerID;
        lock = new Mutex(0);
        try {

            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //get the workers cores availability
    public int getCores() {
        try {
            return in.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    //get the workers ram availability
    public double getRam() {
        try {
            return in.readDouble();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    //send the range to calculate either X or Y matrix and the matrix that is static each time
    public void sendData(int start, int end, RealMatrix matrix) {
        try {
            out.reset();
            out.writeInt(start);
            out.flush();
            out.writeInt(end);
            out.flush();
            out.writeObject(matrix);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //send variable K to workers
    public void sendK(int k) {
        try {
            out.writeInt(k);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //send variable L to workers
    public void sendL(double l) {
        try {
            out.writeDouble(l);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //send number of iterations (seasons or epoxes) to workers
    public void sendSeasons(int k) {
        try {
            out.writeInt(k);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //send matrix to initialize P and C matrix at workers
    public void sendMatrix(RealMatrix matrix) {
        try {
            out.writeObject(matrix);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void run() {
        try {

            for (int j = 0; j < master.getNumberOfSeasons(); j++) {//for the number of seasons
                for (int i = 0; i < 2; i++) { //for X and Y (i=0 : X , i=1 : Y)

                    int start = in.readInt();//get from which point the worker calculated the matrix
                    RealMatrix sub = ((RealMatrix) in.readObject());//get the sub matrix that the worker calculated

                    if (lock.isX()) {//if X was calculated
                        synchronized (master.getXMatrix()) {
                            master.getXMatrix().setSubMatrix(sub.getData(), start, 0); //replace the server's X matrix sub matrix with the new X sub matrix
                        }
                    } else {//if Y was calculated
                        synchronized (master.getYMatrix()) {
                            master.getYMatrix().setSubMatrix(sub.getData(), start, 0);//replace the server's Y matrix sub matrix with the new Y sub matrix
                        }
                    }
                    synchronized (lock) {//wait for all workers to finish calculating their corresponding matrix
                        lock.increaseThreadCount();
                        if (lock.getThreadCount() != master.getWorkers().size()) {//if not all workers finished
                            try {
                                lock.wait();//worker thread wait for others
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        } else {//when the last worker finishes
                            lock.notifyAll();//notify all other threads
                            lock.restartThreadCount();//restart workers that finished
                            lock.setX();//change the matrix being calculated
                            synchronized (master.getServerLock()) {
                                master.getServerLock().notify();//notify server to send the other matrix
                            }
                        }
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                //close communication and connection with the workers
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}

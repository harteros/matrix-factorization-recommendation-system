/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class WorkerNode extends Thread implements Worker {

    private RealMatrix c_matrix;
    private RealMatrix p_matrix;
    private RealMatrix cu_matrix;
    private RealMatrix ci_matrix;
    private RealMatrix xu_matrix;
    private RealMatrix yi_matrix;

    private Socket requestSocket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private int start;
    private int end;

    private RealMatrix xx_matrix;
    private RealMatrix yy_matrix;

    private static boolean x;
    private static int K;
    private static double L;
    private static int Seasons;
    private static int port = 4244;
    private static String ip = "localhost";

    public static void main(String args[]) throws IOException {
        WorkerNode worker = new WorkerNode();

        worker.initialize();
        for (int i = 0; i < worker.Seasons(); i++) {
            worker.calculateX();
            worker.sendResultsToMaster();
            worker.calculateY();
            worker.sendResultsToMaster();
        }
        worker.close();
    }

    public WorkerNode() {
        x = true;//initialize at the worker that the starting matrix for computation is X
    }

    //calculate the part of X that is sent from the server
    public void calculateX() {

        try {

            while (in.available() == 0) ; //while no input wait

            start = in.readInt();//read the sub matrix size the worker have to calculate
            end = in.readInt();
            yi_matrix = ((RealMatrix) in.readObject());//get Y matrix
            System.out.println("Calculating X matrix ...");

            yy_matrix = preCalculateYY(yi_matrix);
            xu_matrix = MatrixUtils.createRealMatrix(end - start, K);//create sub matrix X which we will calculate

            int row = 0;
            for (int i = start; i < end; i++) {//for each user of the sub matrix X
                RealMatrix x = calculate_x_u(i, yi_matrix, c_matrix);//calculate xu
                xu_matrix.setRowMatrix(row, x.transpose());//set XuT to X

                row++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //calculate the part of Y that is sent from the server
    public void calculateY() {

        try {

            while (in.available() == 0) ;//while no input wait

            start = in.readInt();//read the sub matrix size the worker have to calculate
            end = in.readInt();
            xu_matrix = ((RealMatrix) in.readObject());//get X matrix
            System.out.println("Calculating Y matrix ...");

            xx_matrix = preCalculateXX(xu_matrix);
            yi_matrix = MatrixUtils.createRealMatrix(end - start, K);//create sub matrix Y which we will calculate

            int row = 0;
            for (int i = start; i < end; i++) {//for each user of the sub matrix Y
                RealMatrix y = calculate_y_i(i, xu_matrix, c_matrix);//calculate yi
                yi_matrix.setRowMatrix(row, y.transpose());//set YiT to Y
                row++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        try {
            /* Create socket for contacting the server on port 4321*/
            requestSocket = new Socket(ip, port);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        try {
            //send worker profile to server and receive the tables with the user poi data and the variables of X,Y sizes, iterations and L
            sendCores();
            sendRam();
            p_matrix = getMatrix();
            c_matrix = getMatrix();
            K = getK();
            L = getL();
            Seasons = getSeasons();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void close() {
        try {
            in.close();//close input output stream and connection with server
            out.close();
            requestSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void calculateCuMatrix(int user, RealMatrix matrix_cui) {
        cu_matrix = MatrixUtils.createRealDiagonalMatrix(matrix_cui.getRow(user));
    }

    public void calculateCiMatrix(int item, RealMatrix matrix_cui) {
        ci_matrix = MatrixUtils.createRealDiagonalMatrix(matrix_cui.getColumn(item));
    }

    public RealMatrix preCalculateYY(RealMatrix matrix_y) {
        return matrix_y.transpose().multiply(matrix_y);
    }

    public RealMatrix preCalculateXX(RealMatrix matrix_x) {
        return matrix_x.transpose().multiply(matrix_x);
    }

    //calculates xu matrix as described in the paper
    public RealMatrix calculate_x_u(int user, RealMatrix matrix_y, RealMatrix matrix_c) {

        calculateCuMatrix(user, matrix_c);

        RealMatrix cu_matrix_minus_i = cu_matrix.subtract(MatrixUtils.createRealIdentityMatrix(cu_matrix.getColumnDimension()));

        RealMatrix expression = matrix_y.transpose().multiply(cu_matrix_minus_i);

        expression = expression.multiply(matrix_y);

        expression = expression.add(yy_matrix);

        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(expression.getColumnDimension());

        identity = identity.scalarMultiply(L);

        expression = expression.add(identity);

        RealMatrix xu = new LUDecomposition(expression).getSolver().getInverse();//oti pio konta ston antistrofo (i kaluteri prosegkisi)

        xu = xu.multiply(matrix_y.transpose());

        xu = xu.multiply(cu_matrix);

        RealMatrix pu = MatrixUtils.createColumnRealMatrix(p_matrix.getRow(user));

        xu = xu.multiply(pu);
        return xu;
    }

    //calculates yi matrix as described in the paper
    public RealMatrix calculate_y_i(int item, RealMatrix matrix_x, RealMatrix matrix_c) {

        calculateCiMatrix(item, matrix_c);

        RealMatrix ci_matrix_minus_i = ci_matrix.subtract(MatrixUtils.createRealIdentityMatrix(ci_matrix.getColumnDimension()));

        RealMatrix expression = matrix_x.transpose().multiply(ci_matrix_minus_i);

        expression = expression.multiply(matrix_x);

        expression = expression.add(xx_matrix);

        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(expression.getColumnDimension());

        identity = identity.scalarMultiply(L);

        expression = expression.add(identity);

        RealMatrix yi = new LUDecomposition(expression).getSolver().getInverse();//oti pio konta ston antistrofo (i kaluteri prosegkisi)

        yi = yi.multiply(matrix_x.transpose());

        yi = yi.multiply(ci_matrix);

        RealMatrix pi = MatrixUtils.createColumnRealMatrix(p_matrix.getColumn(item));

        yi = yi.multiply(pi);

        return yi;
    }

    //sends the calculated sub matrixes to the server
    public void sendResultsToMaster() {
        try {
            System.out.println("Sending results to master ...");

            out.writeInt(start);
            out.flush();

            if (x) {
                out.writeObject(xu_matrix);
                out.flush();
                x = !x;
            } else {
                out.writeObject(yi_matrix);
                out.flush();
                x = !x;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendCores() throws IOException {
        out.writeInt(Runtime.getRuntime().availableProcessors());
        out.flush();
    }

    private void sendRam() throws IOException {
        out.writeDouble(Runtime.getRuntime().maxMemory());
        out.flush();
    }

    private RealMatrix getMatrix() {
        try {
            return ((RealMatrix) in.readObject());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getK() {
        try {
            return in.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private double getL() {
        try {
            return in.readDouble();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int getSeasons() {
        try {
            return in.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int Seasons() {
        return Seasons;
    }
}


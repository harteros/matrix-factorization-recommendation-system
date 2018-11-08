/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MasterNode extends Thread implements Master {

    private static final int A = 40;
    private static final int K = 20;
    private static final double L = 0.1;
    private static int numberOfSeasons = 5;
    private static int port = 4244;

    private OpenMapRealMatrix r_matrix;
    private RealMatrix c_matrix;
    private RealMatrix p_matrix;
    private RealMatrix x_matrix;
    private RealMatrix y_matrix;
    private ArrayList<Poi> pois;
    private ArrayList<ActionForWorkers> workers;
    private ArrayList<WorkerProfile> profiles;
    private Mutex server_lock;
    private int numberOfWorkers;
    private RandomGenerator random;


    public static void main(String args[]) {

        MasterNode server = new MasterNode(9);
        server.readFile("input_matrix_non_zeros.csv", 835, 1692);//765,1964,835,1692
        server.readPois("POIs.json");
        server.initialize();
        for (int k = 0; k < getNumberOfSeasons(); k++) {
            server.distributeXMatrixToWorkers();
            server.waitForWorker();
            server.distributeYMatrixToWorkers();
            server.waitForWorker();
            System.out.println("Season : " + k);
            System.out.println("Error : " + server.calculateError());
            System.out.println("************************************");
        }
        server.acceptClient();
    }


    public MasterNode(int workers) {
        this.numberOfWorkers = workers;
        server_lock = new Mutex();
    }

    public static void setNumberOfSeasons(int seasons) {
        numberOfSeasons = seasons;
    }

    public static int getNumberOfSeasons() {
        return numberOfSeasons;
    }

    public Mutex getServerLock() {
        return server_lock;
    }

    public RealMatrix getXMatrix() {
        return x_matrix;
    }

    public RealMatrix getYMatrix() {
        return y_matrix;
    }

    public ArrayList<ActionForWorkers> getWorkers() {
        return workers;
    }


    public void readFile(String csv_file, int rows, int columns) {
        random = new JDKRandomGenerator();
        random.setSeed(1);
        BufferedReader br = null;
        String line;
        r_matrix = new OpenMapRealMatrix(rows, columns);//initialize the matrix containing the data about users and pois

        try {

            br = new BufferedReader(new FileReader(csv_file));

            while ((line = br.readLine()) != null) {

                String[] values = line.split(",");
                //write the data from the file to the sparse matrix
                r_matrix.setEntry(Integer.parseInt(values[0].trim()), Integer.parseInt(values[1].trim()), Integer.parseInt(values[2].trim()));
            }
            //initialize P matrix and C matrix
            p_matrix = MatrixUtils.createRealMatrix(r_matrix.getRowDimension(), r_matrix.getColumnDimension());
            c_matrix = MatrixUtils.createRealMatrix(r_matrix.getRowDimension(), r_matrix.getColumnDimension());

            //calculate values of P and C matrix based on paper
            calculatePMatrix(r_matrix);
            calculateCMatrix(A, r_matrix);

            //initialize X and Y matrix with row dimensions based on static variable K
            x_matrix = MatrixUtils.createRealMatrix(r_matrix.getRowDimension(), K);
            y_matrix = MatrixUtils.createRealMatrix(r_matrix.getColumnDimension(), K);

            //give random values to X,Y matrix
            initializeXMatrix();
            initializeYMatrix();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();//close buffered reader
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void readPois(String csv_file) {


        pois = new ArrayList<Poi>();//initialize the matrix containing the data about users and pois
        JSONParser parser = new JSONParser();
        FileReader f = null;
        try {
            f = new FileReader(csv_file);
        } catch (FileNotFoundException ex) {
            System.out.println("File " + csv_file + " does not exist");
            System.out.println("Program will now exit");
            System.exit(1);
        }
        try {

            Object pois_file = parser.parse(f); //parses the file and puts it into an object
            JSONObject pois_object = (JSONObject) pois_file;
            for (int i = 0; i < r_matrix.getColumnDimension(); i++) {
                JSONObject poi_item = (JSONObject) pois_object.get(Integer.toString(i));
                Poi poi = new Poi(i, (String) poi_item.get("POI"), (Double) poi_item.get("latidude"), (Double) poi_item.get("longitude"), (String) poi_item.get("photos"), (String) poi_item.get("POI_category_id"), (String) poi_item.get("POI_name"));
                pois.add(poi);
            }

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //give random values from 0 to 1 to X matrix
    private void initializeXMatrix() {
        for (int i = 0; i < x_matrix.getRowDimension(); i++) {
            for (int j = 0; j < x_matrix.getColumnDimension(); j++) {
                x_matrix.setEntry(i, j, random.nextDouble());
            }
        }
    }

    //give random values from 0 to 1 to Y matrix
    private void initializeYMatrix() {

        for (int i = 0; i < y_matrix.getRowDimension(); i++) {
            for (int j = 0; j < y_matrix.getColumnDimension(); j++) {
                y_matrix.setEntry(i, j, random.nextDouble());
            }
        }
    }

    //calculated P matrix based on each user's preference
    public void calculatePMatrix(RealMatrix matrix) {
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                if (matrix.getEntry(i, j) > 0) p_matrix.setEntry(i, j, 1);
                else p_matrix.setEntry(i, j, 0);
            }
        }
    }

    //calculated C matrix based on each user's preference
    public void calculateCMatrix(int a, RealMatrix matrix) {

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                c_matrix.setEntry(i, j, 1 + a * matrix.getEntry(i, j));
            }
        }

    }

    //send a part of X matrix to a worker for calculation
    public void distributeXMatrixToWorkers(int worker, int start, int end, RealMatrix y_matrix) {
        workers.get(worker).sendData(start, end, y_matrix);
    }

    //send a part of Y matrix to a worker for calculation
    public void distributeYMatrixToWorkers(int worker, int start, int end, RealMatrix x_matrix) {
        workers.get(worker).sendData(start, end, x_matrix);

    }

    //distributes X matrix to workers based on their ram and cpu that was calculated
    public void distributeXMatrixToWorkers() {
        System.out.println("Sending X matrix to workers...");

        for (int i = 0; i < workers.size(); i++) {
            distributeXMatrixToWorkers(i, profiles.get(i).getXStart(), profiles.get(i).getXEnd(), y_matrix);
        }
    }

    //distributes Y matrix to workers based on their ram and cpu that was calculated
    public void distributeYMatrixToWorkers() {
        System.out.println("Sending Y matrix to workers...");

        for (int i = 0; i < workers.size(); i++) {
            distributeYMatrixToWorkers(i, profiles.get(i).getYStart(), profiles.get(i).getYEnd(), x_matrix);
        }
    }

    //calculates error after a computation of X and Y matrix
    public double calculateError() {
        System.out.println("Calculating error..");
        double sum = 0;

        for (int i = 0; i < r_matrix.getRowDimension(); i++) {
            for (int j = 0; j < r_matrix.getColumnDimension(); j++) {
                double dif = p_matrix.getEntry(i, j) - calculateScore(i, j);
                sum += c_matrix.getEntry(i, j) * Math.pow(dif, 2);
            }
        }

        double sumNorms = L * getNorms();

        return sum + sumNorms;
    }

    //calculates the Norms for X and Y matrix
    private double getNorms() {
        double sumX = 0;
        double sumY = 0;

        for (int i = 0; i < x_matrix.getRowDimension(); i++) {
            RealMatrix xu = x_matrix.getRowMatrix(i);
            double x_norm = Math.pow(xu.getFrobeniusNorm(), 2);
            sumX += x_norm;

        }
        for (int i = 0; i < y_matrix.getRowDimension(); i++) {
            RealMatrix yi = y_matrix.getRowMatrix(i);
            double y_norm = Math.pow(yi.getFrobeniusNorm(), 2);
            sumY += y_norm;
        }
        return sumX + sumY;
    }

    //calculates the preference of a user for a poi
    public double calculateScore(int user, int poi) {
        RealMatrix xu = x_matrix.getRowMatrix(user);//we get the row of the matrix X but we dont transpose it as it was already put in there transposed
        RealMatrix yi = y_matrix.getRowMatrix(poi).transpose();//we get the of the matrix Y but we transpose it as it was put in there transpose and thus we got to get back its initial form
        return xu.multiply(yi).getEntry(0, 0);

    }

    //calculates best pois for user based on poi he is located on radius of pois he wants as recommended and on category of pois he wants
    public List<Poi> calculateBestLocalPoisForUser(int userID, int location, int numberOfPois, double radius, String category) {
        Poi local = pois.get(location);
        double latitude = local.getLatitude();
        double longitude = local.getLongitude();
        if (numberOfPois < 1) return null;
        List<Poi> bestPois = new ArrayList<Poi>();
        List<Double> scores = new ArrayList<Double>();
        for (int poi = 0; poi < r_matrix.getColumnDimension(); poi++) {
            if (!category.equals("All")) {
                if (category.equals(pois.get(poi).getCategory())) {
                    if (p_matrix.getEntry(userID, poi) != 1) {
                        if (poi != location) {
                            double score = calculateScore(userID, poi);
                            double distance = Math.sqrt(Math.pow(latitude - pois.get(poi).getLatitude(), 2) + Math.pow(longitude - pois.get(poi).getLongitude(), 2));
                            if (bestPois.size() < numberOfPois) {
                                if ((bestPois.isEmpty()) && (distance < radius)) {

                                    bestPois.add(pois.get(poi));
                                    scores.add(score);
                                } else {
                                    for (int i = 0; i < scores.size(); i++) {
                                        if ((score > scores.get(i)) && (distance < radius)) {
                                            scores.add(i, score);
                                            bestPois.add(i, pois.get(poi));
                                            break;
                                        } else if ((i == scores.size() - 1) && (distance < radius)) {
                                            scores.add(scores.size(), score);
                                            bestPois.add(bestPois.size(), pois.get(poi));
                                            break;
                                        }
                                    }
                                }
                            } else {

                                for (int i = 0; i < scores.size(); i++) {
                                    if ((score > scores.get(i)) && (distance < radius)) {
                                        scores.remove(scores.size() - 1);
                                        bestPois.remove(bestPois.size() - 1);
                                        scores.add(i, score);
                                        bestPois.add(i, pois.get(poi));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (p_matrix.getEntry(userID, poi) != 1) {
                    if (poi != location) {
                        double score = calculateScore(userID, poi);
                        double distance = Math.sqrt(Math.pow(latitude - pois.get(poi).getLatitude(), 2) + Math.pow(longitude - pois.get(poi).getLongitude(), 2));
                        if (bestPois.size() < numberOfPois) {
                            if ((bestPois.isEmpty()) && (distance < radius)) {

                                bestPois.add(pois.get(poi));
                                scores.add(score);
                            } else {
                                for (int i = 0; i < scores.size(); i++) {
                                    if ((score > scores.get(i)) && (distance < radius)) {
                                        scores.add(i, score);
                                        bestPois.add(i, pois.get(poi));
                                        break;
                                    } else if ((i == scores.size() - 1) && (distance < radius)) {
                                        scores.add(scores.size(), score);
                                        bestPois.add(bestPois.size(), pois.get(poi));
                                        break;
                                    }
                                }
                            }
                        } else {

                            for (int i = 0; i < scores.size(); i++) {
                                if ((score > scores.get(i)) && (distance < radius)) {
                                    scores.remove(scores.size() - 1);
                                    bestPois.remove(bestPois.size() - 1);
                                    scores.add(i, score);
                                    bestPois.add(i, pois.get(poi));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        bestPois.add(0, local);
        return bestPois;
    }

    //calculates best pois for user based on latitude,longitude he is located on radius of pois he wants as recommended and on category of pois he wants
    public List<Poi> calculateBestLocalPoisForUser(int userID, double latitude, double longitude, int numberOfPois, double radius, String category) {
        if (numberOfPois < 1) return null;
        List<Poi> bestPois = new ArrayList<Poi>();
        List<Double> scores = new ArrayList<Double>();
        Poi location = null;
        for (int poi = 0; poi < r_matrix.getColumnDimension(); poi++) {

            if (!category.equals("All")) {
                if (category.equals(pois.get(poi).getCategory())) {
                    if (p_matrix.getEntry(userID, poi) != 1) {
                        if (pois.get(poi).getLatitude() != latitude && pois.get(poi).getLongitude() != longitude) {

                            double score = calculateScore(userID, poi);
                            double distance = Math.sqrt(Math.pow(latitude - pois.get(poi).getLatitude(), 2) + Math.pow(longitude - pois.get(poi).getLongitude(), 2));
                            if (bestPois.size() < numberOfPois) {
                                if ((bestPois.isEmpty()) && (distance < radius)) {

                                    bestPois.add(pois.get(poi));
                                    scores.add(score);
                                } else {
                                    for (int i = 0; i < scores.size(); i++) {
                                        if ((score > scores.get(i)) && (distance < radius)) {
                                            scores.add(i, score);
                                            bestPois.add(i, pois.get(poi));
                                            break;
                                        } else if ((i == scores.size() - 1) && (distance < radius)) {
                                            scores.add(scores.size(), score);
                                            bestPois.add(bestPois.size(), pois.get(poi));
                                            break;
                                        }
                                    }
                                }
                            } else {

                                for (int i = 0; i < scores.size(); i++) {
                                    if ((score > scores.get(i)) && (distance < radius)) {
                                        scores.remove(scores.size() - 1);
                                        bestPois.remove(bestPois.size() - 1);
                                        scores.add(i, score);
                                        bestPois.add(i, pois.get(poi));
                                        break;
                                    }
                                }
                            }
                        } else {
                            location = pois.get(poi);
                        }
                    }
                }
            } else {
                if (p_matrix.getEntry(userID, poi) != 1) {
                    if (pois.get(poi).getLatitude() != latitude && pois.get(poi).getLongitude() != longitude) {

                        double score = calculateScore(userID, poi);
                        double distance = Math.sqrt(Math.pow(latitude - pois.get(poi).getLatitude(), 2) + Math.pow(longitude - pois.get(poi).getLongitude(), 2));
                        if (bestPois.size() < numberOfPois) {
                            if ((bestPois.isEmpty()) && (distance < radius)) {

                                bestPois.add(pois.get(poi));
                                scores.add(score);
                            } else {
                                for (int i = 0; i < scores.size(); i++) {
                                    if ((score > scores.get(i)) && (distance < radius)) {
                                        scores.add(i, score);
                                        bestPois.add(i, pois.get(poi));
                                        break;
                                    } else if ((i == scores.size() - 1) && (distance < radius)) {
                                        scores.add(scores.size(), score);
                                        bestPois.add(bestPois.size(), pois.get(poi));
                                        break;
                                    }
                                }
                            }
                        } else {

                            for (int i = 0; i < scores.size(); i++) {
                                if ((score > scores.get(i)) && (distance < radius)) {
                                    scores.remove(scores.size() - 1);
                                    bestPois.remove(bestPois.size() - 1);
                                    scores.add(i, score);
                                    bestPois.add(i, pois.get(poi));
                                    break;
                                }
                            }
                        }
                    } else {
                        location = pois.get(poi);
                    }
                }
            }
        }
        if (location == null) {
            location = new Poi(-1, "Unknown", latitude, longitude, "Not exists", "Unknown", "Unknown");
        }
        bestPois.add(0, location);
        return bestPois;
    }

    //returns a list with the top K recommended poi for the user
    public List<Integer> calculateBestLocalPoisForUser(int userID, int numberOfPois) {
        if (numberOfPois < 1) return null;
        List<Integer> bestPois = new ArrayList<Integer>();
        List<Double> scores = new ArrayList<Double>();
        for (int poi = 0; poi < r_matrix.getColumnDimension(); poi++) {
            double score = calculateScore(userID, poi);

            if (bestPois.size() < numberOfPois) {

                if (poi == 0) {

                    bestPois.add(poi);
                    scores.add(score);
                } else {
                    for (int i = 0; i < scores.size(); i++) {
                        if (score > scores.get(i)) {
                            scores.add(i, score);
                            bestPois.add(i, poi);
                            break;
                        } else if (i == scores.size() - 1) {
                            scores.add(scores.size(), score);
                            bestPois.add(bestPois.size(), poi);
                            break;
                        }
                    }
                }
            } else {

                for (int i = 0; i < scores.size(); i++) {
                    if (score > scores.get(i)) {
                        scores.remove(scores.size() - 1);
                        bestPois.remove(bestPois.size() - 1);
                        scores.add(i, score);
                        bestPois.add(i, poi);
                        break;
                    }
                }
            }
        }
        return bestPois;

    }

    //waits for all workers to finish their matrix calculation
    public void waitForWorker() {
        synchronized (server_lock) {
            try {
                System.out.println("Waiting for workers to finish matrix calculation...");
                server_lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void initialize() {
        ServerSocket providerSocket;
        Socket connection;
        workers = new ArrayList<ActionForWorkers>();
        profiles = new ArrayList<WorkerProfile>();
        try {
            int id = 0;
            /* Create Server Socket */
            providerSocket = new ServerSocket(port, 10);
            while (id != numberOfWorkers) {
                /* Accept the connection */
                connection = providerSocket.accept();
                System.out.println("Got an new contection...");

                ActionForWorkers worker = new ActionForWorkers(connection, this, id);//create a new thread for handling the worker
                workers.add(worker);//add worker to the array with the workers
                profiles.add(new WorkerProfile(worker.getCores(), worker.getRam()));//save worker profile(ram,cpu) to the corresponding array

                worker.start();//start thread
                //send matrixes and variables to initialize the matrix at the worker's side
                worker.sendMatrix(p_matrix);
                worker.sendMatrix(c_matrix);
                worker.sendK(K);
                worker.sendL(L);
                worker.sendSeasons(numberOfSeasons);
                id++;

            }
            //after the desired workers have connected calculate the part of matrix that they will get
            calculateParts();


        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    public void acceptClient() {
        ServerSocket providerSocket;
        Socket connection;
        try {
            /* Create Server Socket */
            providerSocket = new ServerSocket(port, 10);
            while (true) {
                /* Accept the connection */
                connection = providerSocket.accept();
                System.out.println("Got an new client contection...");
                ActionForClients clients = new ActionForClients(connection, this);

                clients.start();

            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    //calculates the X and Y matrix parts for each worker based on thei ram and cpu
    private void calculateParts() {

        double[] values = new double[workers.size()];
        double profile_0 = profiles.get(0).getCores() + profiles.get(0).getRam();
        for (int i = 0; i < workers.size(); i++) {
            if (i == 0) {
                values[i] = 1;
            } else {
                double profile_i = profiles.get(i).getCores() + profiles.get(i).getRam();
                values[i] = profile_i / profile_0;
            }
        }
        double sum = 0;
        double min = 100;
        double max = -1;
        int min_spot = -1;
        int max_spot = -1;
        for (int i = 0; i < workers.size(); i++) {
            sum += values[i];
            if (values[i] < min) {
                min = values[i];
                min_spot = i;
            }
            if (values[i] > max) {
                max = values[i];
                max_spot = i;
            }
        }
        int[] spots_x = new int[workers.size()];
        int[] spots_y = new int[workers.size()];

        for (int i = 0; i < workers.size(); i++) {
            if (i == 0) {
                spots_x[i] = (int) Math.round(r_matrix.getRowDimension() / sum);
                spots_y[i] = (int) Math.round(r_matrix.getColumnDimension() / sum);
            } else {
                spots_x[i] = (int) Math.round(spots_x[0] * values[i]);
                spots_y[i] = (int) Math.round(spots_y[0] * values[i]);
            }
        }
        int sumx = 0;
        int sumy = 0;
        for (int i = 0; i < workers.size(); i++) {
            sumx += spots_x[i];
            sumy += spots_y[i];
        }

        int dif_x = 0;
        int dif_y = 0;
        if (sumx != r_matrix.getRowDimension()) dif_x = r_matrix.getRowDimension() - sumx;
        if (sumy != r_matrix.getColumnDimension()) dif_y = r_matrix.getColumnDimension() - sumy;
        if (dif_x > 0) {
            spots_x[max_spot] += dif_x;
        } else {
            spots_x[min_spot] += dif_x;
        }
        if (dif_y > 0) {
            spots_y[max_spot] += dif_y;
        } else {
            spots_y[min_spot] += dif_y;
        }

        for (int i = 0; i < workers.size(); i++) {
            if (i == 0) {
                profiles.get(i).setXStart(0);
                profiles.get(i).setYStart(0);
                profiles.get(i).setXEnd(spots_x[i]);
                profiles.get(i).setYEnd(spots_y[i]);
            } else {
                int previous_x = profiles.get(i - 1).getXEnd();
                int previous_y = profiles.get(i - 1).getYEnd();
                profiles.get(i).setXStart(previous_x);
                profiles.get(i).setYStart(previous_y);
                profiles.get(i).setXEnd(previous_x + spots_x[i]);
                profiles.get(i).setYEnd(previous_y + spots_y[i]);
            }
        }
    }


}
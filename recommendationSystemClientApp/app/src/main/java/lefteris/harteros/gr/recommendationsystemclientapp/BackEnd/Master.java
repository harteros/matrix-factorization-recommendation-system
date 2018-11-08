/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

import org.apache.commons.math3.linear.RealMatrix;

import java.util.List;

public interface Master {

    public void initialize();

    public void calculateCMatrix(int a, RealMatrix r_matrix);

    public void calculatePMatrix(RealMatrix r_matrix);

    public void distributeXMatrixToWorkers(int worker, int start, int end, RealMatrix y_matrix);

    public void distributeYMatrixToWorkers(int worker, int start, int end, RealMatrix x_matrix);

    public double calculateError();

    public double calculateScore(int user, int poi);

    public List<Poi> calculateBestLocalPoisForUser(int userID, double latitude, double longitude, int pois, double radius, String category);

    public List<Poi> calculateBestLocalPoisForUser(int userID, int location, int numberOfPois, double radius, String category);

}


/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

import org.apache.commons.math3.linear.RealMatrix;

public interface Worker {

    public void initialize();

    public void calculateCuMatrix(int i, RealMatrix matrix);

    public void calculateCiMatrix(int i, RealMatrix matrix);

    public RealMatrix preCalculateYY(RealMatrix matrix);

    public RealMatrix preCalculateXX(RealMatrix matrix);

    public RealMatrix calculate_x_u(int i, RealMatrix matrix_x, RealMatrix matrix_u);

    public RealMatrix calculate_y_i(int i, RealMatrix matrix_y, RealMatrix matrix_i);

    public void sendResultsToMaster();
}

/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

public interface CreateQuery {

    public void getLocation(double lat, double lon);

    public void createQuery(double lat, double lon, int pois, double radius, String category);

    public void sendQueryToServer();
}

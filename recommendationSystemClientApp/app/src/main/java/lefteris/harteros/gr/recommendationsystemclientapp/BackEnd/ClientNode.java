/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientNode implements AndroidClient, CreateQuery, SendNewQuery, ShowResults {

    private int userID;
    private int numberOfRecommendedPoi = 10;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private List<Poi> recommendedPoi;
    private double latitude;
    private double longitude;
    private double radius;
    private String category;
    private static int port = 4244;
    private static String ip = "192.168.1.6";

    public static void main(String[] args) {
        ClientNode client = new ClientNode(764);
        client.initializeAndroidClient();
        //client.setNumberOfRecommendedPoi(15);
        client.sendQuery();
        client.getResults();
        client.showResults();

    }

    public ClientNode(int userID) {
        this.userID = userID;
    }

    public void initializeAndroidClient() {
        try {
            /* Create socket for contacting the server on port 4321*/
            Socket requestSocket = new Socket(ip, port);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public List<Poi> getRecommendedPoi() {
        return recommendedPoi;
    }


    @Override
    public void sendQuery() {

        sendQueryToServer();
    }

    public void getResults() {
        try {
            recommendedPoi = ((List<Poi>) in.readObject());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void showResults() {
        System.out.println("Recommended Pois in descending order : ");
        for (Poi RecommendedPoiID : recommendedPoi) {
            System.out.println("Poi ID : " + RecommendedPoiID.getID());
        }
    }


    public void getLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

    }

    public void createQuery(double latitude, double longitude, int pois, double radius, String category) {
        getLocation(latitude, longitude);
        this.numberOfRecommendedPoi = pois;
        this.radius = radius;
        this.category = category;
    }

    public void sendQueryToServer() {
        try {
            //send the userID and the number of Poi that the user want to get recommendation
            out.writeInt(userID);
            out.flush();

            out.writeInt(numberOfRecommendedPoi);
            out.flush();

            out.writeDouble(latitude);
            out.flush();

            out.writeDouble(longitude);
            out.flush();

            out.writeDouble(radius);
            out.flush();

            out.writeUTF(category);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


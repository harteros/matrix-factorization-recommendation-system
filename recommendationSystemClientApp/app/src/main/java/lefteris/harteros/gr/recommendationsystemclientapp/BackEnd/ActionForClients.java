/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.BackEnd;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ActionForClients extends Thread {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket connection;
    private MasterNode master;

    public ActionForClients(Socket connection, MasterNode master) {
        //initialize client thread
        this.connection = connection;
        this.master = master;

        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {

            int id = in.readInt(); //read userID
            int numberOfPoi = in.readInt();//read number of poi the user want to get
            double latitude = in.readDouble();
            double longitude = in.readDouble();
            double radius = in.readDouble();
            String category = in.readUTF();
            if (longitude == -1) {
                out.writeObject(master.calculateBestLocalPoisForUser(id, (int) latitude, numberOfPoi, radius, category));//send back a list of poi that server recommended
                out.flush();
            } else {
                out.writeObject(master.calculateBestLocalPoisForUser(id, latitude, longitude, numberOfPoi, radius, category));//send back a list of poi that server recommended
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //close communication and connection with the client
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}

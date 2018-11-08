# matrix-factorization-recommendation-system
A recommendation system using matrix factorization algorithm with client application.

The currect application has 2 parts , the one is the backEnd and the other one is the frontEnd.

The backEnd directory inlcudes the matrix factorization algorithm which is broken down to multiple workers in order to complete the task faster.

In order to initiate the master - worker connection the user must specify the following things :

<b>MasterNode class</b>

<b>port number</b> to which the workers will connect </br>
<b>number of loops (seasons)</b> for which the algorithm will run

<b>WorkerNode class</b>

<b>port number</b> at which the master has opened the connection </br>
<b>ip address</b> of the masterNode (use localhost if running master and worker on the same pc)

Example files are given in order to test the algorithm (input_matrix_non_zeros.csv , POIs.json) 

The frontEnd dictory contains the UI of the android application and it implements the training results from the MasterNode.</br>
After the training is complete the user can ask for recommendations from the server through the ClientNode. 

In order to do so the user must specify the port number to which the request will be sent and the ip adress of the server (masterNode).

Note 1 : localhost will not work here as the emulator of the android app is seen as an external device to the pc. </br>
Note 2 : google maps api must be set in order to be able to view the maps (set api through file google_maps_api.xml)


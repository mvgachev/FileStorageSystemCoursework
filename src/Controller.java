import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Controller {

    private ServerSocket controllerSocket;
    private int r;
    private int timeout;
    private Index index = new Index();
    private Hashtable<Integer, Socket> allDStores = new Hashtable<>();

    public Controller(Integer cPort, int r, int timeout) throws IOException {

        controllerSocket = new ServerSocket(cPort);
        this.r = r;
        this.timeout = timeout;
        listen();
    }

    public void listen() throws IOException {
        int numberOfDStores = 0;
        //Sockets for the Dstores and clients
        while (true) {
            Socket socket = controllerSocket.accept();
            new SocketHandler(socket).start();
        }
    }


    class SocketHandler extends Thread {
        private Socket clientOrDstoreSocket;
        private PrintWriter out;
        private BufferedReader in;
        private List<Integer> dstorePortsUsed = new ArrayList<>();

        public SocketHandler(Socket socket) {
            this.clientOrDstoreSocket = socket;
        }

        @Override
        public void run() {

            try {
                out = new PrintWriter(clientOrDstoreSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientOrDstoreSocket.getInputStream()));

                String inputLine;
                while (true) {
                    if ((inputLine = in.readLine()) != null) {
                        parse(inputLine);
                    }
                    out.flush();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void parse(String inputLine) throws IOException, InterruptedException {
            String[] words = inputLine.split(" ");
            String firstWord = words[0];

            switch (firstWord) {

                case "JOIN":
                    Integer port = Integer.parseInt(words[1]);
                    allDStores.put(port, clientOrDstoreSocket);
                    System.out.println("The dstore joined.");
                    break;

                case "STORE":
                    String fileName = words[1];
                    Long fileSize = Long.parseLong(words[2]);
                    System.out.println("Client wants to store.");
                    //Checks if dstores are enough
                    if (allDStores.size() != r) {
                        out.println("ERROR_NOT_ENOUGH_DSTORES");
                    } else if (!index.reserveFileForStorage(fileName)) {
                        out.println("ERROR_FILE_ALREADY_EXISTS");
                    } else {
                        store(fileName, fileSize);
                        //Make the controller wait
                        TimeUnit.MILLISECONDS.sleep(timeout);
                        //Check if the file is saved in all the dstores
                        if (index.getLocations(fileName) != null) {
                            if (index.getLocations(fileName).size() == allDStores.size()) {
                                //Update status
                                index.updateStatus(fileName, Status.STORE_COMPLETE);
                                //Save fileSize
                                index.saveFileSize(fileName, fileSize);
                                //Send message to client
                                out.println("STORE_COMPLETE");
                            } else {
                                index.removeFile(fileName);
                            }
                        }
                    }
                    break;

                case "STORE_ACK":
                    fileName = words[1];
                    //If there is no location in the index create one
                    index.addLocation(fileName, clientOrDstoreSocket);
                    break;

                case "LOAD":
                    fileName = words[1];
                    System.out.println("Client wants to load file " + fileName);
                    if (allDStores.size() != r) {
                        out.println("ERROR_NOT_ENOUGH_DSTORES");
                    } else if (index.checkIfAvailable(fileName)) {
                        out.println("ERROR_FILE_DOES_NOT_EXIST");
                    } else {
                        load(fileName);
                    }
                    break;
                case "RELOAD":
                    fileName = words[1];
                    load(fileName);
                    break;

                case "REMOVE":
                    fileName = words[1];
                    System.out.println("Client wants to remove file " + fileName);
                    if(allDStores.size() != r) {
                        out.println("ERROR_NOT_ENOUGH_DSTORES");
                    } else if(!index.reserveFileForRemoval(fileName)) {
                        out.println("ERROR_FILE_DOES_NOT_EXIST");
                    } else {
                        remove(fileName);
                    }
                    break;

            }
        }

        private void store(String fileName, Long fileSize) throws IOException {
            //Create a status for the index
            //index.updateStatus(fileName, Status.STORE_IN_PROGRESS);
            //Select all endpoints
            StringBuilder allDstorePorts = new StringBuilder("STORE_TO");
            for (Integer port : allDStores.keySet()) {
                allDstorePorts.append(" ");
                allDstorePorts.append(port);
            }
            //Send message to client
            System.out.println("Sending this message to client: " + allDstorePorts);
            out.println(allDstorePorts);
        }

        private void load(String fileName) {
            //Controller selects one of the R Dstores that has the file
            for(Socket i : index.getLocations(fileName)) {
                if(!dstorePortsUsed.contains(i.getPort())) {
                    dstorePortsUsed.add(i.getPort());
                    out.println("LOAD_FROM " + i.getPort() + " " + index.getFileSize(fileName));
                    return;
                }
            }
            out.println("ERROR_LOAD");
            //Reset list fo used dstores
            dstorePortsUsed = new ArrayList<>();
        }

        private void remove(String fileName) throws IOException {
            int count = 0;
            for(Socket dSocket : index.getLocations(fileName)) {

                //Get the input and output to the dstore
                PrintWriter dstoreOut = new PrintWriter(dSocket.getOutputStream(),true);
                BufferedReader dstoreIn = new BufferedReader(new InputStreamReader(dSocket.getInputStream()));

                //Send remove message to all dstores
                dstoreOut.println("REMOVE " + fileName);
                String answer = dstoreIn.readLine();
                String[] words = answer.split(" ");
                //TODO log errors
                if(words[0].equals("REMOVE_ACK") || words[0].equals("ERROR_FILE_DOES_NOT_EXIST")) {
                    count++;
                }
            }
            if(count == index.getLocations(fileName).size()) {
                //TODO see if the file should be removed from the index or just have an updated status
                //index.removeFile(fileName);
                out.println("REMOVE_COMPLETE");
            }
        }
    }


    public static void main(String[] args) throws IOException {
        new Controller(8080, 1, 1000);
    }
}

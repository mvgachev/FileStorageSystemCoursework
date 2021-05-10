import java.awt.*;
import java.io.*;
import java.net.Socket;

import static java.lang.Integer.parseInt;

public class Client {

    private Socket controllerSocket;
    private PrintWriter out;
    private BufferedReader in;

    public Client(Integer cPort) throws IOException {
        controllerSocket = new Socket("localhost", cPort);

        out = new PrintWriter(controllerSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(controllerSocket.getInputStream()));
    }

    public void store(File file) throws IOException {
        out.println("STORE " + file.getName() + " " + file.length());

        String inputLine = in.readLine();
        System.out.println("Controller message: " + inputLine);
        String[] words = inputLine.split(" ");

        if (words[0].equals("STORE_TO")) {
            for (int i = 1; i < words.length; i++) {
                //Throws exception if port is not an integer
                int dstorePort = Integer.parseInt(words[i]);

                //Creating the socket to the dstore
                Socket dstoreSocket = new Socket("localhost", dstorePort);
                PrintWriter dstoreOut = new PrintWriter(dstoreSocket.getOutputStream(), true);
                BufferedReader dstoreIn = new BufferedReader(new InputStreamReader(dstoreSocket.getInputStream()));

                dstoreOut.println("STORE " + file.getName() + " " + file.length());
                //Check if Dstore is available
                String answer = dstoreIn.readLine();
                if (answer.equals("ACK")) {
                    //Create writer and reader for the files
                    FileTransfer.sendFile(dstoreSocket, file);
                    System.out.println("Transfering file");
                    out.flush();
                }
            }
        }

    }

    public void load(String fileName) throws IOException {
        //Send message to controller
        out.println("LOAD " + fileName);

        //Get the answer from the controller
        String inputLine = in.readLine();
        String[] words = inputLine.split(" ");
        if(words[0].equals("LOAD_FROM")) {
            int port = Integer.parseInt(words[1]);
            long fileSize = Long.parseLong(words[2]);
            //Create the socket to the dstore
            Socket dstoreSocket = new Socket("localhost", port);
            PrintWriter dstoreOut = new PrintWriter(dstoreSocket.getOutputStream(), true);
            BufferedReader dstoreIn = new BufferedReader(new InputStreamReader(dstoreSocket.getInputStream()));

            //Send message to the dstore
            dstoreOut.println("LOAD_DATA " + fileName);
            System.out.println("Received file");
            File newFile = FileTransfer.receiveFile(dstoreSocket,new File("test_files"),fileName,fileSize);
            Desktop.getDesktop().open(newFile);
            out.flush();
        }
    }

    public void remove(String fileName) throws IOException {
        out.println("REMOVE " + fileName);
        String inputLine = in.readLine();
        if(inputLine.equals("REMOVE_COMPLETE")) {
            System.out.println("Remove completed");
        }
    }


    public static void main(String[] args) throws IOException {

        File newFile = new File("test_files/download");

//        for(int i=0;i<2;i++) {
//            new Thread(() -> {
//                Client client = null;
//                try {
//                    client = new Client(8080);
//                    client.store(newFile);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }).start();
//        }
        new Thread(() -> {
            Client client = null;
            try {
                client = new Client(8080);
                client.store(newFile);
                client.load("download");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        //client.load("download");
        //client.load("dstore_files/dstore1/download");
    }
}

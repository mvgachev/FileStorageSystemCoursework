import javax.swing.plaf.FontUIResource;
import java.io.*;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class Dstore {

    private Socket controllerSocket;
    private ServerSocket dStoreSocket;
    private Integer port;
    private File fileFolder;
    private PrintWriter controllerOut;
    private BufferedReader controllerIn;
    private PrintWriter out;
    private BufferedReader in;

    public Dstore(Integer port, Integer cPort, String fileFolder) throws IOException {

        this.port = port;
        dStoreSocket = new ServerSocket(port);
        this.fileFolder = new File(fileFolder);
        this.fileFolder.mkdir();
        //Connection to the controller
        connectToController(cPort);
        while (true)
            listen();
    }

    public void listen() throws IOException {
        while (true) {
            Socket socket = dStoreSocket.accept();
            System.out.println("A client is connected.");
            new SocketHandler(socket).start();
        }
    }

    class SocketHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public SocketHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while (true) {
                    if ((inputLine = in.readLine()) != null) {
                        parse(inputLine);
                    }
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void parse(String inputLine) throws IOException {
            String[] words = inputLine.split(" ");
            System.out.println("Client's message: " + inputLine);

            switch (words[0]) {

                case "STORE":
                    String fileName = words[1];
                    long size = Long.parseLong(words[2]);
                    store(fileName, size);
                    break;
                case "LOAD_DATA":
                    fileName = words[1];
                    load(fileName);
                case "REMOVE":
                    fileName = words[1];
                    remove(fileName);
            }
        }

        private void store(String fileName, long fileSize) throws IOException {
            out.println("ACK");
            System.out.println("ACK");
            FileTransfer.receiveFile(clientSocket, fileFolder, fileName, fileSize);
            controllerOut.println("STORE_ACK " + fileName);
        }

        private void load(String fileName) throws IOException {
            File searchedFile = lookForFile(fileName);

            if (searchedFile != null) {
                System.out.println("Sending file " + searchedFile.getName());
                FileTransfer.sendFile(clientSocket, searchedFile);
            } else {
                //Closes the socket with the client since the dstore does not have the searched file
                clientSocket.close();
            }
        }

        private void remove(String fileName) {
            File searchedFile = lookForFile(fileName);

            if (searchedFile != null) {
                System.out.println("Removing file" + searchedFile.getName());
                searchedFile.delete();
            } else {
                //Sends a message to the controller that the file does not exist.
                out.println("ERROR_FILE_DOES_NOT_EXIST" + fileName);
            }
        }
    }

    private void connectToController(Integer cPort) throws IOException {
        controllerSocket = new Socket("localhost", cPort);

        controllerOut = new PrintWriter(controllerSocket.getOutputStream(), true);
        controllerIn = new BufferedReader(new InputStreamReader(controllerSocket.getInputStream()));

        //Send message to controller
        controllerOut.println("JOIN " + port);
    }

    private File lookForFile(String fileName) {
        File[] files = fileFolder.listFiles();
        File targetFile = new File(fileName);
        //TODO check size as well
        for (File f : files) {
            if (f.getName().equals(targetFile.getName())) {
                return f;
            }
        }
        return null;
    }


    public static void main(String[] args) throws IOException {
        new Dstore(9090, 8080, "dstore1");
    }
}


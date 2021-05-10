import java.io.*;
import java.net.Socket;

public class FileTransfer {

    public static void sendFile(Socket socket, File file) throws IOException {
        byte[] bytesOfFile = new byte[(int) file.length()];
        //Take the content of the file in an input stream
        FileInputStream fileInputStream = new FileInputStream(file);
        //Put it in a buffered input stream
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        //Read the file and put the data in the bytes array
        bufferedInputStream.read(bytesOfFile, 0, bytesOfFile.length);
        OutputStream outputStream = socket.getOutputStream();
        System.out.println("Sending " + file.getName() + " (" + file.length() + " bytes");
        outputStream.write(bytesOfFile, 0, bytesOfFile.length);
        outputStream.flush();
        System.out.println("Done");
    }

    public static File receiveFile(Socket socket, File fileFolder, String fileName, long fileSize) throws IOException {
        byte[] bytesOfFile = new byte[(int) fileSize];
        //Get the input stream from the client
        InputStream inputStream = socket.getInputStream();
        //Create a file output stream to the storage folder of the dstore
        String[] dir = fileName.split("/");
        fileName = dir[dir.length-1];
        FileOutputStream fileOutputStream = new FileOutputStream(new File(fileFolder.getName() + "/" + fileName));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

        //Reading the file
        int bytesRead = inputStream.read(bytesOfFile, 0, bytesOfFile.length);
        int current = bytesRead;

        do {
            bytesRead = inputStream.read(bytesOfFile, current, (bytesOfFile.length - current));
            if (bytesRead >= 0) current += bytesRead;
        } while(bytesRead > 0);

        bufferedOutputStream.write(bytesOfFile,0,current);
        bufferedOutputStream.flush();
        System.out.println("File " + fileName + " downloaded (" + current + "bytes read)");

        return new File(fileFolder + "/" + fileName);
    }
}

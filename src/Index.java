import java.net.Socket;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

public class Index {

    private Hashtable<String, Vector<Socket>> indexLocation = new Hashtable<>();
    private Hashtable<String, Status> indexStatus = new Hashtable<>();
    private Hashtable<String, Long> indexSize = new Hashtable<>();

    public Index() {

    }


    public synchronized void add(String fileName, Controller.SocketHandler handler, Status status) {
        indexStatus.put(fileName,status);
    }

    public synchronized void updateStatus(String fileName, Status newStatus) {
        indexStatus.put(fileName,newStatus);
    }

    public synchronized void addLocation(String fileName, Socket socket) {
        //Check if the file has any locations at all
        if(indexLocation.get(fileName) != null) {
            Vector<Socket> v = indexLocation.get(fileName);
            v.add(socket);
            indexLocation.put(fileName, v);
        } else {
            indexLocation.put(fileName, new Vector<>(Collections.singleton(socket)));
        }
    }

    public synchronized boolean checkIfAvailable(String fileName) {
        if(indexStatus.get(fileName) == null) return false;
        return indexStatus.get(fileName).equals(Status.STORE_COMPLETE);
    }

    public synchronized void saveFileSize(String fileName, Long fileSize) {
        indexSize.put(fileName,fileSize);
    }

    public synchronized Status getStatus(String fileName) {
        return indexStatus.get(fileName);
    }
    public synchronized Long getFileSize(String fileName) {
        return indexSize.get(fileName);
    }

    public synchronized Vector<Socket> getLocations(String fileName) {
        return indexLocation.get(fileName);
    }

    public synchronized boolean reserveFileForStorage(String fileName) {

        if (indexStatus.get(fileName) == null && indexLocation.get(fileName) == null) {
            //File is reserved
            updateStatus(fileName, Status.STORE_IN_PROGRESS);
            return true;
        }
        return false;
    }

    public synchronized boolean reserveFileForRemoval(String fileName) {
        if(indexStatus.get(fileName) != null && indexLocation.get(fileName) != null) {
            //File is reserved
            updateStatus(fileName, Status.REMOVE_IN_PROGRESS);
            return true;
        }
        return false;
    }

    public synchronized void removeFile(String fileName) {
        if(indexLocation.get(fileName) != null) {
            indexLocation.remove(fileName);
        }
        if(indexStatus.get(fileName) != null) {
            indexStatus.remove(fileName);
        }
    }
}

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Database {

    public static int serverPort;
    public static InetAddress serverAddress = null;
    public static HashMap<Integer, Integer> collector = new HashMap<>();
    public static HashMap<Integer, TcpNodeConnection> connectedNodes = new HashMap<>();
    public static HashMap<Integer, ServerWorkerThread> servers = new HashMap<>();
    public static HashMap<Integer, ServerWorkerThread> servingNodes = new HashMap<>();
    public static ServerSocket serverSocket;
    public static List<String> receivedTasks = new ArrayList<>();

}

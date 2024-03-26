import java.io.*;
import java.net.InetAddress;

public class DatabaseNode{


    public static void main(String[] args) throws IOException {

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-tcpport":
                    String portTmp = args[++i];
                    Database.serverPort = Integer.parseInt(portTmp);
                    ServerListener clientServer = new ServerListener();
                    clientServer.start();
                    break;
                case "-record":
                    String[] recordArray = args[++i].split(":");
                    Database.collector.put(Integer.parseInt(recordArray[0]), Integer.parseInt(recordArray[1]));
                    break;
                case "-connect":
                    String[] connectArray = args[++i].split(":");
                    int nodePort = Integer.parseInt(connectArray[1]);
                    if(Database.serverAddress == null) {
                        Database.serverAddress = InetAddress.getByName(connectArray[0]);
                    }
                    Database.connectedNodes.put(nodePort, new TcpNodeConnection(nodePort));
                    Database.connectedNodes.get(nodePort).connect(nodePort);
                    Database.connectedNodes.get(nodePort).start();
                    break;
            }
        }
    }
}





















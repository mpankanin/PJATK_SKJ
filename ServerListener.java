import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerListener extends Thread{


    public ServerListener() throws IOException {
        Database.serverSocket = new ServerSocket(Database.serverPort);
        log("Server created");
    }


    public void log(String str){
        System.out.println("[S" + Database.serverPort +"]: " + str);
        System.out.flush();
    }

    @Override
    public void run() {
        try {
            while(Database.serverSocket.isBound() && !Database.serverSocket.isClosed()){
                log("Listening...");
                Socket socket = Database.serverSocket.accept();
                log("Connection accepted: " + socket.getInetAddress());

                log("Connection diverting to thread");
                Database.servers.put(socket.getPort(),new ServerWorkerThread(socket));
                Database.servers.get(socket.getPort()).start();
                log("Connection diverted to thread");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

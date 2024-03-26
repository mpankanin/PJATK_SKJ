import java.io.*;
import java.net.Socket;
import java.util.Map;


public class TcpNodeConnection extends Thread{

    private final int nodePort;
    public Socket clientSocket;
    public BufferedReader br;
    public BufferedWriter bw;
    public PrintWriter pw;

    public TcpNodeConnection(int nodePort){
        this.nodePort = nodePort;
    }

    public void communicateWithPeer() throws IOException {
        String command = br.readLine();
        log(command);
        String[] commandArr = command.split(" ");
        switch (commandArr[0]) {
            case "customerTerminate":
                log("Deleting " + commandArr[1] + " from list");
                Database.connectedNodes.remove(Integer.parseInt(commandArr[1]));
                log("Deleted");
                log("Closing socket");
                bw.close();
                br.close();
                clientSocket.close();
                log("Socket closed");
                break;
            case "get-value":
                int peerPort = Integer.parseInt(commandArr[3]);
                command = commandArr[0] + " " + commandArr[1] + " " + commandArr[2];
                if(!checkTaskList(command)){
                    String toSend;
                    log("adding to list");
                    Database.receivedTasks.add(command);
                    if(Database.collector.containsKey(Integer.parseInt(commandArr[1]))){
                        log("found");
                        toSend = Database.collector.get(Integer.parseInt(commandArr[1])).toString();
                        toSend = "Found " + toSend;
                    }else{
                        if(!Database.servingNodes.isEmpty()) {
                            for (Map.Entry<Integer, ServerWorkerThread> set : Database.servingNodes.entrySet()) {
                                if (set.getKey() != peerPort && set.getKey() != nodePort) {
                                    log("SERVING sending to: " + set.getKey() + " command: " + command);
                                    set.getValue().pw.println(command);
                                }
                            }
                        }

                        if (!Database.connectedNodes.isEmpty()) {
                            log("Checking in serving nodes");
                            for (Map.Entry<Integer, TcpNodeConnection> set : Database.connectedNodes.entrySet()) {
                                log("Sending command: " + command + " " + Database.serverPort);
                                set.getValue().pw.println(command + " " + Database.serverPort);
                                toSend = set.getValue().br.readLine();
                                log("Received flag: " + toSend);
                                if (!toSend.equals("ERROR")) {
                                    break;
                                }
                            }
                        }
                        log("not found");
                        toSend = "response error " + Database.serverPort;
                    }
                    log("sending: " + toSend);
                    response(toSend);
                    communicateWithPeer();
                }else {
                    response("Ignored" + Database.serverPort);
                }


            case "response":
                    switch (commandArr[1]){
                        case "checkmyconnected":
                            pw.println("checkmyconnected");
                            break;
                        case "ignored":
                            communicateWithPeer();
                        case "end":

                        case "error":
                            String clientPort = Database.servers.keySet().toString();
                            clientPort = clientPort.replace("[", "").replace("]", "");
                            Database.servers.get(Integer.parseInt(clientPort)).pw.println("ERROR");
                    }
                break;
        }



    }

    public void response(String str){
        pw.println(str);
    }



    public void connect(int nodePort) throws IOException {
        log("Connecting to server");
        clientSocket = new Socket(Database.serverAddress, nodePort);
        log("Connected to server");
        createStreams();
        bw.write("peerMode " + Database.serverPort);
        bw.newLine();
        bw.flush();
    }

    public void sendTerminate() throws IOException {
            log("Sending terminate to: " + nodePort);
            bw.write("nodeTerminate");
            bw.newLine();
            bw.flush();
            log("Terminate sent to: " + nodePort);
            bw.close();
            br.close();
            clientSocket.close();
    }

    public void createStreams() throws IOException{
        log("Creating streams");
        InputStream is = clientSocket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        br = new BufferedReader(isr);
        OutputStream os = clientSocket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        bw = new BufferedWriter(osw);
        pw = new PrintWriter(clientSocket.getOutputStream(), true);
        log("Streams created");
    }

    public boolean checkTaskList(String str){
        boolean found = false;
        for(String tmp : Database.receivedTasks){
            if (str.equals(tmp)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public void log(String str){
        System.out.println("[C" + nodePort +"]: " + str);
        System.out.flush();
    }

    @Override
    public void run() {
        try {
            communicateWithPeer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Map;


public class ServerWorkerThread extends Thread {

    public final Socket socket;
    private BufferedWriter bw;
    private BufferedReader br;
    PrintWriter pw;
    private int peerLookingFor;


    public ServerWorkerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            createStreams();
            serveClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serveClient() throws IOException {
        String command = br.readLine();
        System.out.println(command);
        String[] commandArr = command.split(" ");
        switch (commandArr[0]) {
            case "peerMode":
                changeToPeerMode(Integer.parseInt(commandArr[1]));
                break;
            case "nodeTerminate":
                nodeTerminate();
                break;
            case "terminate":
                terminate();
                break;
            case "get-value":
                getValue(Integer.parseInt(commandArr[1]));
                break;
            case "set-value":
                break;
            case "find-key":
                break;
            case "get-max":
                break;
            case "get-min":
                break;
            case "new-record":
                newRecord(commandArr[1]);
                break;
        }
    }

    public void servePeer() throws IOException {
        log("Listening for peer commands...");
        String command = br.readLine();
        log("Peer received task: " + command);
        String[] commandArr = command.split(" ");
        switch (commandArr[0]) {
            case "nodeTerminate":
                nodeTerminate();
                break;
            case "get-value":
                boolean served = false;
                int peerPort = Integer.parseInt(commandArr[3]);
                peerLookingFor =peerPort;
                command = commandArr[0] + " " + commandArr[1] + " " + commandArr[2];
                log("Peerport: " + peerPort + " command: " + command);
                log("Checking on list");
                if (!checkTaskList(command)) {
                    String toSend;
                    Database.receivedTasks.add(command);
                    log("Added to list, checking in my collection");
                    if (Database.collector.containsKey(Integer.parseInt(commandArr[1]))) {
                        toSend = Database.collector.get(Integer.parseInt(commandArr[1])).toString();
                        Database.servingNodes.get(peerPort).pw.println("response end " + toSend);
                        log("Sent to: " + peerPort);
                        served = true;
                    } else {
                        if (!Database.servingNodes.isEmpty()) {
                            for (Map.Entry<Integer, ServerWorkerThread> set : Database.servingNodes.entrySet()) {
                                if (set.getKey() != peerPort) {
                                    log("SERVING sending to: " + set.getKey() + " command: " + command + " " + Database.serverPort);
                                    set.getValue().pw.println(command + " " + Database.serverPort);
                                    served = true;
                                }
                            }
                        }
                    }
                    if(!served) {
                        if(!Database.connectedNodes.isEmpty())
                        Database.servingNodes.get(peerPort).pw.println("response checkmyconnected " + peerPort);
                        log("Sent to: " + peerPort);
                    }else {
                        Database.servingNodes.get(peerPort).pw.println("response error " + Database.serverPort);
                    }

                } else {
                    Database.servingNodes.get(peerPort).pw.println("response ignored " + peerPort);
                }
            case "checkconnected":
                break;
            case "found":
                //Database.servingNodes.get(peerPort).pw.println("response end " + toSend);
                //log("Sent to: " + peerPort);
            case "response":
                switch (commandArr[1]){
                    case "checkmyconnected":
                        pw.println("checkmyconnected");
                        break;
                    case "ignored":
                        servePeer();
                    case "end":
                        String clientPort = Database.servers.keySet().toString();
                        clientPort = clientPort.replace("[", "").replace("]", "");
                        Database.servingNodes.get(peerLookingFor).pw.println("response checkmyconnected " + peerLookingFor);
                        log("Sent to: " + peerLookingFor);
                        log("sent");
                    case "error":
                        Database.connectedNodes.get(peerLookingFor).pw.println("response error");
                }
                break;



        }
    }


    public void newRecord(String str) throws IOException {
        log("Deleting record");
        String[] arr = str.split(":");
        Database.collector.clear();
        log("Adding new record");
        Database.collector.put(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
        respond("OK");
        socket.close();
        currentThread().interrupt();
    }

    public void changeToPeerMode(int port) throws IOException {
        ServerWorkerThread swt = Database.servers.get(socket.getPort());
        Database.servers.remove(socket.getPort());
        Database.servingNodes.put(port, swt);
        servePeer();
    }

    public boolean checkTaskList(String str) {
        boolean found = false;
        for (String tmp : Database.receivedTasks) {
            if (str.equals(tmp)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public void getValue(int value) throws IOException {
        String id = LocalDateTime.now().toString();
        String writeLine = null;
        String command = "get-value " + value + " " + id + " " + Database.serverPort;
        if (Database.collector.containsKey(value)) {
            writeLine = Database.collector.get(value).toString();
        } else {
            log("Checking in connected nodes");
            if (!Database.servingNodes.isEmpty()) {
                for (Map.Entry<Integer, ServerWorkerThread> set : Database.servingNodes.entrySet()) {
                    log("SERVING sending to: " + set.getKey() + " command: " + command);
                    set.getValue().pw.println(command);
                }
            }
        }

        if (!Database.connectedNodes.isEmpty()) {
            log("Checking in serving nodes");
            for (Map.Entry<Integer, TcpNodeConnection> set : Database.connectedNodes.entrySet()) {
                log("Sending command: " + command);
                set.getValue().pw.println(command);
                writeLine = set.getValue().br.readLine();
                log("Received flag: " + writeLine);
                if (!writeLine.equals("ERROR")) {
                    break;
                }
            }
        }




        respond(writeLine);
        closeStreams();
        socket.close();
        currentThread().interrupt();
        }

    public void terminate() throws IOException {

        sendTerminateMyConnections();
        sendTerminateConnectedWithMe();


        log("Sending response to client");
        bw.write("OK");
        bw.newLine();
        bw.flush();
        Database.serverSocket.close();
        log("Terminated");
        System.exit(0);

      }

    public void nodeTerminate(){
        log("Removing node from list");
        Database.connectedNodes.remove(socket.getLocalPort()); //TODO CZY ABY NA PEWNO TEN PORT JEST DOBRY
        log("Node removed");
    }

    public void sendTerminateMyConnections() throws IOException {
        if(!Database.connectedNodes.isEmpty()) {
            log("Sending terminate info to peers");
            for (Map.Entry<Integer, TcpNodeConnection> set : Database.connectedNodes.entrySet()) {
                set.getValue().sendTerminate();
            }
            log("Terminating info sent");
        }
    }

    public void sendTerminateConnectedWithMe() throws IOException {
        if(!Database.servingNodes.isEmpty()) {
            log("Sending terminate info to customer nodes");
            for (Map.Entry<Integer, ServerWorkerThread> set : Database.servingNodes.entrySet()) {
                set.getValue().bw.write("customerTerminate " + Database.serverPort);
                set.getValue().bw.newLine();
                set.getValue().bw.flush();
            }
            log("Termination info sent to nodes");
        }
    }

    public void createStreams() throws IOException {
        log("Creating streams");
        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        br = new BufferedReader(isr);
        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        bw = new BufferedWriter(osw);
        pw = new PrintWriter(socket.getOutputStream(), true);
        log("Streams created");
    }

    public void closeStreams() throws IOException {
        log("Closing streams");
        bw.close();
        br.close();
        log("Streams closed");
    }

    public void respond(String str) throws IOException {
        bw.write(str);
        bw.newLine();
        bw.flush();
    }

    public void log(String str){
        System.out.println("[TS" + socket.getLocalPort() +"]: " + str);
        System.out.flush();
    }

}
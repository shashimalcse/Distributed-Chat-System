package com.heartbeat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.heartbeat.pb.heartbeatGrpc;
import com.heartbeat.pb.Heartbeat.HeartBeatRequest;
import com.heartbeat.pb.Heartbeat.HeartBeatResponse;
import com.heartbeat.pb.Heartbeat.ServerClient;
import com.heartbeat.pb.Heartbeat.acknowledge;
import com.heartbeat.pb.Heartbeat.clientExistRequest;
import com.heartbeat.pb.Heartbeat.clientExistResponse;
import com.heartbeat.pb.Heartbeat.clientQuitRequest;
import com.heartbeat.pb.Heartbeat.deleteRoomRequest;
import com.heartbeat.pb.Heartbeat.deleteRoomResponse;
import com.heartbeat.pb.Heartbeat.joinRoomRequest;
import com.heartbeat.pb.Heartbeat.joinRoomResponse;
import com.heartbeat.pb.Heartbeat.moveJoinRequest;
import com.heartbeat.pb.Heartbeat.moveJoinResponse;
import com.heartbeat.pb.Heartbeat.roomExistRequest;
import com.heartbeat.pb.Heartbeat.roomExistResponse;
import com.heartbeat.pb.Heartbeat.serverConnectionRequest;
import com.heartbeat.pb.Heartbeat.serverConnectionResponse;
import com.heartbeat.pb.Heartbeat.serverDownDetail;
import com.heartbeat.pb.heartbeatGrpc.heartbeatBlockingStub;
import com.heartbeat.pb.heartbeatGrpc.heartbeatStub;
import com.google.rpc.context.AttributeContext.Response;
import com.heartbeat.LeaderElectionService;
import com.leader_election.pb.coordinatorGrpc;
import com.leader_election.pb.LeaderElect.elect;
import com.leader_election.pb.LeaderElect.ok;
import com.leader_election.pb.coordinatorGrpc.coordinatorBlockingStub;
import com.leader_election.pb.coordinatorGrpc.coordinatorStub;
import com.leader_election.pb.LeaderElect.coordinatorMsg;
import com.leader_election.pb.LeaderElect.coordinatorMsgAck;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.Collections;

public class Server implements Runnable {

    private static ArrayList<Chatroom> chatrooms = new ArrayList<Chatroom>();
    String conf_file;
    private Chatroom chatroom = new Chatroom("", new Owner(""), this);
    static String leader;
    static int leaderPort;
    static String leaderAddress;
    private ServerSocket serverSocket;
    io.grpc.Server grpcServer;
    io.grpc.Server grpcServerCoord;
    static int client_port;
    static int coordination_port;
    static CopyOnWriteArrayList<String> server_ids;
    static CopyOnWriteArrayList<String> server_addresses;
    static CopyOnWriteArrayList<Integer> clients_ports;
    static CopyOnWriteArrayList<Integer> coordination_ports;
    static String serverid;
    static String server_address;
    static CopyOnWriteArrayList<String> servers_islive;
    private static HeartBeatHandler heartbeat_thread;
    static String leader_alive;
    static LeaderElectionHandler leader_election;
    static Map<String, String> users;

    static Map<String, String> rooms;

    public Server(Chatroom chatroom, String serverid, String server_address, int client_port, int coordination_port,
            CopyOnWriteArrayList<String> server_ids, CopyOnWriteArrayList<String> server_addresses,
            CopyOnWriteArrayList<Integer> clients_ports,
            CopyOnWriteArrayList<Integer> coordination_ports, String servers_conf) {
        this.client_port = client_port;
        this.coordination_port = coordination_port;
        this.server_address = server_address;
        this.server_ids = new CopyOnWriteArrayList<String>();
        this.server_addresses = server_addresses;
        this.clients_ports = clients_ports;
        this.coordination_ports = new CopyOnWriteArrayList<Integer>();
        this.chatroom = chatroom;
        this.serverid = serverid;
        this.chatrooms.add(chatroom);
        this.servers_islive = new CopyOnWriteArrayList<String>();
        //this.leader = "s1";
        //this.leaderPort = 5555;
        //this.leaderAddress = "127.0.0.1";
        this.leader_alive = "live";
        this.users = new HashMap<String, String>();
        this.rooms = new HashMap<String, String>();

        this.conf_file = servers_conf;

        this.rooms.put(chatroom.name, this.serverid);

    }

    @Override
    public void run() {

        try {
            FileInputStream fis = new FileInputStream(conf_file);
            Scanner sc = new Scanner(fis);
            sc.nextLine();
            boolean otherServersFound=false;
            while (sc.hasNextLine()) {
                String[] info = sc.nextLine().split("\\s+");

                if (serverid.equals(info[0])) {
                    continue;
                } else {

                    /*
                     * server_ids.add(info[0]);
                     * server_addresses.add(info[1]);
                     * int cp = Integer.valueOf(info[2]);
                     * clients_ports.add(cp);
                     * int cop = Integer.valueOf(info[3]);
                     * coordination_ports.add(cop);
                     */
                    int cop = Integer.valueOf(info[3]);

                    try {
                        ManagedChannel channel = ManagedChannelBuilder
                                .forAddress(info[1], cop)
                                .usePlaintext().build();
                        heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                        serverConnectionRequest request = serverConnectionRequest.newBuilder().setServerid(serverid)
                                .setPort(coordination_port).setAddress(server_address).setClientPort(client_port)
                                .build();
                        serverConnectionResponse response = stub.serverConnected(request);

                        if (response.getLeader().equals(info[0])) {
                            leader = response.getLeader();
                            leaderAddress = response.getLeaderAddress();
                            leaderPort = response.getLeaderPort();
                            System.out.println("Connected with leader");
                            for (int servercnt = 0; servercnt < response.getCoPortsCount(); servercnt++) {
                                this.server_ids.add(response.getServerids(servercnt));
                                this.coordination_ports.add(response.getCoPorts(servercnt));
                                this.server_addresses.add(response.getAddress(servercnt));
                                this.clients_ports.add(response.getClientPorts(servercnt));
                                this.servers_islive.add("live");
                                // rooms.put("MainHall-" + response.getServerids(servercnt),
                                // response.getServerids(servercnt));
                            }
                            for (int roomcnt = 0; roomcnt < response.getRoomidCount(); roomcnt++) {
                                rooms.put(response.getRoomid(roomcnt), response.getServer(roomcnt));
                            }
                            // rooms.put("MainHall-"+leader, leader);
                        } else {
                            System.out.println("Leader has been changed, new leader is:" + response.getLeader());
                            System.out
                                    .println("New leader address:" + response.getLeaderAddress() + "; new leader port:"
                                            + response.getLeaderPort());
                            leader = response.getLeader();
                            leaderAddress = response.getLeaderAddress();
                            leaderPort = response.getLeaderPort();
                            ManagedChannel channel1 = ManagedChannelBuilder
                                    .forAddress(this.leaderAddress, this.leaderPort)
                                    .usePlaintext().build();
                            heartbeatBlockingStub stub1 = heartbeatGrpc.newBlockingStub(channel1);
                            serverConnectionRequest request1 = serverConnectionRequest.newBuilder()
                                    .setServerid(serverid)
                                    .setPort(coordination_port).setAddress(server_address).setClientPort(client_port)
                                    .build();
                            serverConnectionResponse response1 = stub1.serverConnected(request1);
                            System.out.println(response1.toString());

                            for (int servercnt1 = 0; servercnt1 < response1.getCoPortsCount(); servercnt1++) {
                                this.server_ids.add(response1.getServerids(servercnt1));
                                this.coordination_ports.add(response1.getCoPorts(servercnt1));
                                this.server_addresses.add(response1.getAddress(servercnt1));
                                this.clients_ports.add(response1.getClientPorts(servercnt1));
                                this.servers_islive.add("live");
                                // rooms.put("MainHall-" + response.getServerids(servercnt),
                                // response.getServerids(servercnt));
                            }
                            for (int roomcnt1 = 0; roomcnt1 < response1.getRoomidCount(); roomcnt1++) {
                                rooms.put(response1.getRoomid(roomcnt1), response1.getServer(roomcnt1));
                            }
                            channel1.shutdown();
                        }
                        channel.shutdown();
                        otherServersFound=true;
                        break;
                    } catch (StatusRuntimeException e) {
                        continue;
                    }
                }
            }
            if(!otherServersFound){
                leader=serverid;
                leaderAddress=server_address;
                leaderPort=coordination_port;
            }

            /*
            if (this.serverid.equals(leader)) {

            } else {

                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(this.leaderAddress, this.leaderPort)
                        .usePlaintext().build();
                heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                serverConnectionRequest request = serverConnectionRequest.newBuilder().setServerid(serverid)
                        .setPort(coordination_port).setAddress(server_address).setClientPort(
                                client_port)
                        .build();
                serverConnectionResponse response = stub.serverConnected(request);
                System.out.println(response.toString());
                if (response.getLeader().equals(leader)) {
                    leader = response.getLeader();
                    leaderAddress = response.getLeaderAddress();
                    leaderPort = response.getLeaderPort();
                    System.out.println("Connected with leader");
                    for (int servercnt = 0; servercnt < response.getCoPortsCount(); servercnt++) {
                        this.server_ids.add(response.getServerids(servercnt));
                        this.coordination_ports.add(response.getCoPorts(servercnt));
                        this.server_addresses.add(response.getAddress(servercnt));
                        this.clients_ports.add(response.getClientPorts(servercnt));
                        this.servers_islive.add("live");
                        // rooms.put("MainHall-" + response.getServerids(servercnt),
                        // response.getServerids(servercnt));
                    }
                    for (int roomcnt = 0; roomcnt < response.getRoomidCount(); roomcnt++) {
                        rooms.put(response.getRoomid(roomcnt), response.getServer(roomcnt));
                    }
                    // rooms.put("MainHall-"+leader, leader);
                } else {
                    System.out.println("Leader has been changed, new leader is:" +
                            response.getLeader());
                    System.out.println("New leader address:" + response.getLeaderAddress() +
                            "; new leader port:"
                            + response.getLeaderPort());
                    leader = response.getLeader();
                    leaderAddress = response.getLeaderAddress();
                    leaderPort = response.getLeaderPort();
                    ManagedChannel channel1 = ManagedChannelBuilder
                            .forAddress(this.leaderAddress, this.leaderPort)
                            .usePlaintext().build();
                    heartbeatBlockingStub stub1 = heartbeatGrpc.newBlockingStub(channel1);
                    serverConnectionRequest request1 = serverConnectionRequest.newBuilder().setServerid(serverid)
                            .setPort(coordination_port).setAddress(server_address).setClientPort(
                                    client_port)
                            .build();
                    serverConnectionResponse response1 = stub1.serverConnected(request1);
                    System.out.println(response1.toString());

                    for (int servercnt1 = 0; servercnt1 < response1.getCoPortsCount(); servercnt1++) {
                        this.server_ids.add(response1.getServerids(servercnt1));
                        this.coordination_ports.add(response1.getCoPorts(servercnt1));
                        this.server_addresses.add(response1.getAddress(servercnt1));
                        this.clients_ports.add(response1.getClientPorts(servercnt1));
                        this.servers_islive.add("live");
                        // rooms.put("MainHall-" + response.getServerids(servercnt),
                        // response.getServerids(servercnt));
                    }
                    for (int roomcnt1 = 0; roomcnt1 < response1.getRoomidCount(); roomcnt1++) {
                        rooms.put(response1.getRoomid(roomcnt1), response1.getServer(roomcnt1));
                    }
                    channel1.shutdown();
                }
                channel.shutdown();

            }
            */
            
            serverSocket = new ServerSocket(this.client_port);
            grpcServer = ServerBuilder.forPort(this.coordination_port).addService(new HeartBeatService(this))
                    .addService(
                            new LeaderElectionService(this))
                    .build();

            grpcServer.start();
            leader_election = new LeaderElectionHandler(this, this.serverid, this.coordination_port,
                    this.coordination_ports);
            leader_elect();
            run_heartbeat();
            while (true)
                new ClientHandler(this.client_port, serverSocket.accept(), this, this.chatroom, this.serverid,
                        this.server_ids, this.server_addresses, this.clients_ports, this.coordination_ports).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void leader_elect() {
        leader_election.leaderElect();
    }

    public static void setLeader(String newLeader) {
        System.out.println(newLeader + "  elected as new leader");
        leader = newLeader;

        if (newLeader.equals(serverid)) {
            leaderPort = coordination_port;
            leaderAddress = server_address;
        } else {
            System.out.println("Index of new leader:" + server_ids.indexOf(newLeader));
            leaderAddress = server_addresses.get(server_ids.indexOf(newLeader));
            leaderPort = coordination_ports.get(server_ids.indexOf(newLeader));
        }

        // leaderPort =
        // Server.coordination_ports.get(Server.server_ids.indexOf(leader));
        // leaderAddress =
        // Server.server_addresses.get(Server.server_ids.indexOf(leader));
    }

    public void run_heartbeat() {
        heartbeat_thread = new HeartBeatHandler();
        Thread thread = new Thread(heartbeat_thread);
        thread.start();
    }

    public void stop_heartbeat() {
        heartbeat_thread.stop_heartbeat();
    }

    public static class HeartBeatHandler implements Runnable {
        private final static AtomicBoolean heartbeat_running = new AtomicBoolean(false);

        public void stop_heartbeat() {
            heartbeat_running.set(false);
        }

        public boolean is_running() {
            return heartbeat_running.get();
        }

        @Override
        public void run() {
            heartbeat_running.set(true);
            while (heartbeat_running.get()) {
                try {
                    if (Server.coordination_port == Server.leaderPort) {
                        for (int coo : Server.coordination_ports) {
                            try {
                                ManagedChannel channel = ManagedChannelBuilder
                                        .forAddress(Server.server_addresses.get(Server.coordination_ports.indexOf(coo)),
                                                coo)
                                        .usePlaintext().build();
                                heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);

                                HeartBeatRequest request = HeartBeatRequest.newBuilder().setIsLive("isLive").build();
                                HeartBeatResponse response = stub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS)
                                        .isLive(request);
                                if (response.getIsLive().equals("isLive")) {
                                    Server.servers_islive.set(Server.coordination_ports.indexOf(coo), "live");
                                    System.out.println("Server "
                                            + Server.server_ids.get(Server.coordination_ports.indexOf(coo))
                                            + " is live");
                                } else {
                                    if (Server.servers_islive.get(Server.coordination_ports.indexOf(coo))
                                            .equals("live")) {
                                        System.out.println("Server "
                                                + Server.server_ids.get(Server.coordination_ports.indexOf(coo))
                                                + " is critical");
                                        Server.servers_islive.set(Server.coordination_ports.indexOf(coo), "critical");
                                    } else {
                                        System.out.println("Server "
                                                + Server.server_ids.get(Server.coordination_ports.indexOf(coo))
                                                + " is dead");
                                        Server.servers_islive.set(Server.coordination_ports.indexOf(coo), "dead");
                                        String s_id = Server.server_ids.get(Server.coordination_ports.indexOf(coo));
                                        Server.serverDown(s_id);
                                    }

                                }
                                channel.shutdown();
                            } catch (Exception e) {
                                if (Server.servers_islive.get(Server.coordination_ports.indexOf(coo)).equals("live")) {
                                    System.out.println(
                                            "Server " + Server.server_ids.get(Server.coordination_ports.indexOf(coo))
                                                    + " is critical");
                                    Server.servers_islive.set(Server.coordination_ports.indexOf(coo), "critical");
                                } else {
                                    System.out.println(
                                            "Server " + Server.server_ids.get(Server.coordination_ports.indexOf(coo))
                                                    + " is dead");
                                    Server.servers_islive.set(Server.coordination_ports.indexOf(coo), "dead");
                                    String s_id = Server.server_ids.get(Server.coordination_ports.indexOf(coo));
                                    Server.serverDown(s_id);
                                }
                            }

                        }

                    } else {
                        try {
                            ManagedChannel channel = ManagedChannelBuilder
                                    .forAddress(Server.leaderAddress, Server.leaderPort)
                                    .usePlaintext().build();
                            heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);

                            HeartBeatRequest request = HeartBeatRequest.newBuilder().setIsLive("isLive").build();
                            HeartBeatResponse response = stub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS)
                                    .isLive(request);
                            if (response.getIsLive().equals("isLive")) {
                                Server.leader_alive = "live";
                            } else {
                                if (Server.leader_alive.equals("live")) {
                                    Server.leader_alive = "critical";
                                    System.out.println("Leader" + " is critical");
                                } else {
                                    Server.leader_alive = "dead";
                                    System.out.println("Leader" + " is dead");
                                    Server.leader_elect();
                                }

                            }
                            channel.shutdown();
                        } catch (Exception e) {
                            if (Server.leader_alive.equals("live")) {
                                Server.leader_alive = "critical";
                                System.out.println("Leader" + " is critical");
                            } else {
                                Server.leader_alive = "dead";
                                System.out.println("Leader" + " is dead");
                                Server.leader_elect();
                            }
                        }

                    }
                    Thread.sleep(4000);
                } catch (Exception e) {
                    System.out.println(e);
                }

            }
        }

    }

    public static class LeaderElectionHandler {
        private String serverId;
        private String leader;
        private int coordination_port;
        List<Integer> coordination_ports;
        private Server myServer;

        public LeaderElectionHandler(Server myServer, String serverId, int coordination_port,
                List<Integer> coordination_ports) {
            this.myServer = myServer;
            this.serverId = serverId;
            this.coordination_port = coordination_port;
            this.coordination_ports = coordination_ports;

        }

        public void leaderElect() {
            System.out.println("Leader " + myServer.leader);
            System.out.println("My server ID " + serverId);
            System.out.println("LEADER ELECTION HANDLER");
            System.out.println(coordination_ports.toString());
            ArrayList<Integer> respondedServerIds = new ArrayList<Integer>();
            for (int coo : this.coordination_ports) {
                // ELECT RPC
                try {
                    System.out.println("ports : " + coo);
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(Server.server_addresses.get(Server.coordination_ports.indexOf(coo)), coo)
                            .usePlaintext()
                            .build();
                    coordinatorBlockingStub stub = coordinatorGrpc.newBlockingStub(channel);
                    elect.Builder electBuilder = elect.newBuilder();
                    electBuilder.setType("ELECT");
                    electBuilder.setServerId(Integer.parseInt(serverId.substring(1)));
                    elect electRequest = electBuilder.build();
                    ok response = stub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS).leaderElect(electRequest);
                    System.out.println("OK response");
                    Integer respondedServerId = response.getServerId();
                    respondedServerIds.add(respondedServerId);
                    channel.shutdown();
                } catch (Exception e) {
                    System.out.println("No response");
                }

            }
            if (respondedServerIds.size() > 0) {
                Integer respondedServerIdMax = Collections.max(respondedServerIds);
                String newLeader = "s" + Integer.toString(respondedServerIdMax);
                Server.setLeader(newLeader);

                // SET COORDINATOR RPC
                for (int coo : this.coordination_ports) {
                    try {
                        ManagedChannel channelCoordSet = ManagedChannelBuilder
                                .forAddress(Server.server_addresses.get(Server.coordination_ports.indexOf(coo)), coo)
                                .usePlaintext()
                                .build();
                        coordinatorBlockingStub stub = coordinatorGrpc.newBlockingStub(channelCoordSet);
                        coordinatorMsg.Builder coordMsgBuilder = coordinatorMsg.newBuilder();
                        coordMsgBuilder.setType("COORDINATOR");
                        coordMsgBuilder.setServerId(Integer.parseInt(serverId.substring(1)));
                        coordMsgBuilder.setCoordinator(respondedServerIdMax);
                        coordinatorMsg coordMsgRequest = coordMsgBuilder.build();
                        stub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS).setCoordinator(coordMsgRequest);
                        System.out.println("COORDINATOR ACK response");
                        channelCoordSet.shutdown();
                    } catch (Exception e) {
                        System.out.println("No COORDINATOR ACK from Server on port number: " + coo);
                    }

                }
            } else {
                Server.setLeader(Server.serverid);
            }
        }
    }

    public static class ClientHandler extends Thread {

        private Server server;
        private Socket clientSocket;
        private Socket serverSocket;
        private PrintWriter out;
        private PrintWriter out2;
        private BufferedReader in;

        private Chatroom chatroom;
        private Client client;
        private int client_port;

        private String serverid;
        List<String> server_ids;
        List<String> server_addresses;
        List<Integer> clients_ports;
        static List<Integer> coordination_ports;

        private boolean existInOtherServers = true;

        private boolean serverChange = false;

        public ClientHandler(int client_port, Socket socket, Server server, Chatroom chatroom, String serverid,
                List<String> server_ids, List<String> server_addresses, List<Integer> clients_ports,
                List<Integer> coordination_ports) {
            this.client_port = client_port;
            this.server = server;
            this.clientSocket = socket;
            this.chatroom = chatroom;

            this.server_ids = server_ids;
            this.serverid = serverid;
            this.server_addresses = server_addresses;
            this.clients_ports = clients_ports;
            this.coordination_ports = coordination_ports;
        }

        // new id
        public boolean isAlphaNumeric(String s) {
            return s != null && s.matches("^[a-zA-Z0-9]*$");
        }

        public boolean checkConnectedClient(String clientId) {
            boolean clientExistance = false;
            if (server.users.containsKey(clientId)) {
                if (server.users.get(clientId).equals(server.serverid)) {
                    clientExistance = true;
                }
            }
            return clientExistance;
        }

        public boolean checkChatroomExistance(String chatRoomName) {
            boolean chatRoomExistance = false;
            for (int i = 0; i < chatrooms.size(); i++) {
                if (chatrooms.get(i).name.equals(chatRoomName)) {
                    chatRoomExistance = true;
                }
            }
            return chatRoomExistance;

        }

        public boolean checkChatroomOwnerExistance(String ownerName) {
            boolean chatRoomOwnerExistance = false;
            for (int i = 0; i < chatrooms.size(); i++) {
                if (chatrooms.get(i).owner.id.equals(ownerName)) {
                    chatRoomOwnerExistance = true;
                }
            }
            return chatRoomOwnerExistance;
        }

        public boolean checkRoomIdentity(String chatRoomName, String ownerName) {

            boolean roomPropertyExistance = true;
            // || checkChatroomExistance(chatRoomName)==true
            if (checkChatroomOwnerExistance(ownerName) == true || checkConnectedClient(ownerName) == false
                    || (isAlphaNumeric(chatRoomName) == false)
                    || 3 > chatRoomName.length() || chatRoomName.length() > 16) {

                roomPropertyExistance = false;

            } else if (checkOtherServersForChatRoom(chatRoomName) == true) {
                roomPropertyExistance = false;
            }
            return roomPropertyExistance;
        }

        // check alphanumeric
        // 3<length<16
        // check identity in other servers
        // broadcast roomchange message to chat room members
        public JSONObject newIdentify(String identity) {
            JSONObject newIdentity = new JSONObject();
            newIdentity.put("type", "newidentity");
            newIdentity.put("approved", "true");

            checkOtherServers(identity);
            if (existInOtherServers || (isAlphaNumeric(identity) == false) || 3 > identity.length()
                    || identity.length() > 16) {
                newIdentity.replace("approved", "false");
            }

            return newIdentity;
        }

        public boolean checkOtherServersForChatRoom(String chatRoomName) {
            boolean roomExistance = false;

            if (server.serverid.equals(server.leader)) {
                if (server.rooms.containsKey(chatRoomName)) {
                    roomExistance = true;
                } else {
                    for (int coo : coordination_ports) {
                        try {
                            ManagedChannel channel = ManagedChannelBuilder
                                    .forAddress(this.server_addresses.get(this.coordination_ports.indexOf(coo)), coo)
                                    .usePlaintext().build();
                            heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                            roomExistResponse response = roomExistResponse.newBuilder()
                                    .setType("createroom")
                                    .setRoomid(chatRoomName)
                                    .setServerid(serverid)
                                    .setApproved("true")
                                    .build();

                            acknowledge ack = stub.addNewRoom(response);
                            System.out.println(ack.toString());
                            channel.shutdown();
                        } catch (Exception e) {

                        }

                    }
                    roomExistance = false;
                }
            }

            else {
                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(server.server_addresses.get(server.server_ids.indexOf(server.leader)),
                                server.coordination_ports.get(server.server_ids.indexOf(server.leader)))
                        .usePlaintext().build();
                heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);

                roomExistRequest request = roomExistRequest.newBuilder().setType("createroom").setRoomid(chatRoomName)
                        .setServerid(this.server.serverid).build();

                roomExistResponse response = stub.roomExist(request);

                System.out.println("here!!");
                System.out.println(response);

                if (response.getApproved().equals("true")) {
                    // System.out.println("Approved");
                    roomExistance = false;
                    // System.out.println(roomExistance);
                } else {
                    // System.out.println("Rejected");
                    roomExistance = true;
                }

            }

            return roomExistance;
        }

        public void checkOtherServers(String clientId) {
            if (server.serverid.equals(server.leader)) {
                if (server.users.containsKey(clientId)) {
                    existInOtherServers = true;
                } else {
                    for (int coo : server.coordination_ports) {
                        try {
                            ManagedChannel channel = ManagedChannelBuilder
                                    .forAddress(this.server_addresses.get(this.coordination_ports.indexOf(coo)), coo)
                                    .usePlaintext().build();
                            heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                            clientExistResponse req = clientExistResponse.newBuilder().setTyp("newidentity")
                                    .setIdentity(clientId)
                                    .setServerid(server.serverid).setApproved("true").build();

                            acknowledge ack = stub.addNewClient(req);
                            System.out.println(ack.toString());

                        } catch (Exception e) {

                        }

                    }
                    existInOtherServers = false;
                }
            } else {
                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(server.server_addresses.get(server.server_ids.indexOf(server.leader)),
                                server.coordination_ports.get(server.server_ids.indexOf(server.leader)))
                        .usePlaintext().build();
                heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                clientExistRequest request = clientExistRequest.newBuilder().setTyp("newidentity").setIdentity(clientId)
                        .setServerid(this.server.serverid).build();
                clientExistResponse response = stub.clientExist(request);
                System.out.println(response.getApproved());
                if (response.getApproved().equals("true")) {
                    // System.out.println("Approved");
                    existInOtherServers = false;
                    System.out.println(existInOtherServers);
                } else {
                    // System.out.println("Rejected");
                    existInOtherServers = true;
                }

            }
        }

        public void deleteroom(Client client, String roomid, Chatroom chatroom) {

            if (server.serverid.equals(server.leader)) {
                int index = getChatroomByProperty(roomid);
                Chatroom current = chatrooms.get(index);
                if (current.owner.id.equals(client.id)) {
                    JSONObject response = new JSONObject();
                    response.put("type", "deleteroom");
                    response.put("roomid", roomid);
                    response.put("approved", "true");
                    client.clientHandler.out.println(response);

                    for (int i = 1; i < current.clients.size(); i++) {
                        joinRoomLocal(current.clients.get(i), chatroom.name);
                    }
                    joinRoomLocal(client, chatroom.name);

                    server.rooms.remove(roomid);
                    chatrooms.remove(index);
                    System.out.println(server.rooms);

                    for (int coo : server.coordination_ports) {
                        try {
                            ManagedChannel channel = ManagedChannelBuilder
                                    .forAddress(this.server_addresses.get(this.coordination_ports.indexOf(coo)), coo)
                                    .usePlaintext().build();
                            heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);

                            deleteRoomResponse res = deleteRoomResponse.newBuilder()
                                    .setType("deleteroom")
                                    .setServerid(server.serverid)
                                    .setRoomid(roomid)
                                    .setApproved("true")
                                    .build();

                            acknowledge ack = stub.deleteRequestedRoom(res);
                            System.out.println(ack.toString());

                        } catch (Exception e) {

                        }

                    }
                } else {
                    JSONObject response = new JSONObject();
                    response.put("type", "deleteroom");
                    response.put("roomid", roomid);
                    response.put("approved", "false");
                    client.clientHandler.out.println(response);
                }
            } else {

                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(server.server_addresses.get(server.server_ids.indexOf(server.leader)),
                                server.coordination_ports.get(server.server_ids.indexOf(server.leader)))
                        .usePlaintext().build();
                heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);

                deleteRoomRequest request = deleteRoomRequest.newBuilder().setType("deleteroom")
                        .setServerid(this.server.serverid)
                        .setRoomid(roomid)
                        .build();
                deleteRoomResponse res = stub.deleteRoom(request);

                String approved = res.getApproved();

                System.out.println(approved);

                if (approved.equals("true")) {
                    // System.out.println("Approved");
                    int index = getChatroomByProperty(roomid);
                    System.out.println(index);
                    Chatroom current = chatrooms.get(index);

                    if (current.owner.id.equals(client.id)) {
                        JSONObject response = new JSONObject();
                        response.put("type", "deleteroom");
                        response.put("roomid", roomid);
                        response.put("approved", "true");
                        client.clientHandler.out.println(response);
                        // joinRoom(client, chatroom.name);

                        System.out.println("Newly joining chatroom:" + chatroom.name);
                        for (int i = 1; i < current.clients.size(); i++) {
                            joinRoomLocal(current.clients.get(i), chatroom.name);
                        }

                        joinRoomLocal(client, chatroom.name);
                        chatrooms.remove(index);
                        server.rooms.remove(roomid);
                        // System.out.println(chatrooms.size());
                    } else {
                        JSONObject response = new JSONObject();
                        response.put("type", "deleteroom");
                        response.put("roomid", roomid);
                        response.put("approved", "false");
                        client.clientHandler.out.println(response);
                    }

                    // for (int i = 0; i < current.clients.size(); i++) {
                    // //joinRoom(current.clients.get(i), chatroom.name);
                    // joinRoom(current.clients.get(i), chatroom.name);
                    // }

                } else {
                    JSONObject response = new JSONObject();
                    response.put("type", "deleteroom");
                    response.put("roomid", roomid);
                    response.put("approved", "false");
                    client.clientHandler.out.println(response);

                }

            }

        }

        public int getChatroomByProperty(String roomid) {
            for (int i = 0; i < chatrooms.size(); i++) {
                if (chatrooms.get(i) != null && chatrooms.get(i).name.equals(roomid)) {
                    return i;
                }
            }
            return -1;// not there is list
        }

        public boolean checkDestinationServerAlive(String serverid) {
            return true;
        }

        public void joinRoomLocal(Client client, String roomid) {
            String former = client.chatroom.name;

            client.chatroom.removeClient(client);
            broadcastRoomMessage(client.id, former, roomid, client.chatroom);

            int roomExist = getChatroomByProperty(roomid);
            if (roomExist == -1) {
                Chatroom chtroom = new Chatroom(roomid, new Owner(client.id), server);
                client.chatroom = chtroom;
                chatrooms.add(chtroom);
                client.chatroom.addClient(client);
                broadcastRoomMessage(client.id, former, roomid, client.chatroom);
            } else {
                client.chatroom = chatrooms.get(getChatroomByProperty(roomid));
                client.chatroom.addClient(client);
                System.out.println("Former:" + former);
                System.out.println("New:" + roomid);
                broadcastRoomMessage(client.id, former, roomid, client.chatroom);
            }
        }

        public void broadcastRoomMessage(String identity, String former, String roomid, Chatroom room) {
            // System.out.println(room.clients.size());
            for (int i = 0; i < room.clients.size(); i++) {
                JSONObject res = new JSONObject();
                res.put("type", "roomchange");
                res.put("identity", identity);
                res.put("former", former);
                res.put("roomid", roomid);

                chatrooms.get(chatrooms.indexOf(room)).clients.get(i).clientHandler.sendResponse(res);
            }

        }

        @Override
        public void run() {
            try {

                JSONParser parser = new JSONParser();
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);

                    try {
                        JSONObject json = (JSONObject) parser.parse(inputLine);

                        if (json.get("type").equals("newidentity")) {
                            String identity = (String) json.get("identity");
                            JSONObject newIdentify = newIdentify(identity);
                            out.println(newIdentify);
                            System.out.println(newIdentify.toString());

                            if (newIdentify.get("approved").equals("true")) {
                                // System.out.println(chatroom.name);
                                client = new Client(identity, server, chatroom, this);
                                chatroom.addClient(client);
                                server.users.put(client.id, server.serverid);
                                broadcastRoomMessage(identity, "", chatroom.name, chatroom);
                            }
                        } else if (json.get("type").equals("deleteroom")) {
                            String roomid = (String) json.get("roomid");
                            deleteroom(client, roomid, chatroom);
                            // System.out.println(chatroom.name);
                        } else if (json.get("type").equals("message")) {
                            String identity = client.id;
                            Chatroom room = client.chatroom;

                            JSONObject message = new JSONObject();
                            message.put("type", "message");
                            message.put("identity", identity);
                            message.put("content", json.get("content"));

                            for (int i = 0; i < room.clients.size(); i++) {
                                Client receiver = chatrooms.get(chatrooms.indexOf(room)).clients.get(i);
                                if (client.id.equals(receiver.id)) {
                                    continue;
                                }
                                receiver.clientHandler
                                        .sendResponse(message);
                            }
                        } else if (json.get("type").equals("list")) {
                            JSONObject response = new JSONObject();
                            response.put("type", "roomlist");
                            JSONArray jsonarray = new JSONArray();

                            ArrayList<String> roomids = new ArrayList<>(server.rooms.keySet());
                            for (int i = 0; i < server.rooms.size(); i++) {
                                jsonarray.add(roomids.get(i));
                            }
                            response.put("rooms", jsonarray);
                            out.println(response);
                        } else if (json.get("type").equals("who")) {
                            JSONObject response = new JSONObject();
                            response.put("type", "roomcontents");
                            response.put("roomid", client.chatroom.name);

                            // roomid, identities, owner
                            JSONArray jsonarray = new JSONArray();
                            for (int i = 0; i < client.chatroom.clients.size(); i++) {
                                jsonarray.add(client.chatroom.clients.get(i).id);
                            }
                            response.put("identities", jsonarray);
                            response.put("owner", client.chatroom.owner.id);
                            out.println(response);
                        } else if (json.get("type").equals("createroom")) {
                            String roomid = (String) json.get("roomid");
                            String owner = client.id;
                            if (checkRoomIdentity(roomid, owner)) {
                                JSONObject response = new JSONObject();
                                response.put("type", "createroom");
                                response.put("roomid", roomid);
                                response.put("approved", "true");
                                out.println(response);

                                server.rooms.put(roomid, server.serverid);
                                // chatrooms.add(new Chatroom(roomid, new Owner(client.id), server));
                                joinRoomLocal(client, roomid);
                                // broadcastRoomMessage(client.id, client.chatroom.name, roomid, client);

                            } else {
                                JSONObject response = new JSONObject();
                                response.put("type", "createroom");
                                response.put("roomid", roomid);
                                response.put("approved", "false");
                                out.println(response);
                            }
                        } else if (json.get("type").equals("joinroom")) {
                            System.out.println("This is a joinroom request");
                            if (rooms.containsKey((String) json.get("roomid"))) {
                                if (rooms.get((String) json.get("roomid")).equals(server.serverid)) {

                                    if (client.chatroom.owner.id.equals(client.id)) {
                                        System.out.println("Join failed because the client owns a chatroom");

                                        JSONObject response = new JSONObject();
                                        response.put("type", "roomchange");
                                        response.put("identity", client.id);
                                        response.put("former", client.chatroom.name);
                                        response.put("roomid", client.chatroom.name);
                                        out.println(response);
                                    } else {
                                        System.out.println("Localjoin");
                                        joinRoomLocal(client, (String) json.get("roomid"));
                                    }

                                } else {
                                    System.out.println("Externaljoin");
                                    joinRoomExternal((String) json.get("roomid"));
                                }
                            } else {
                                System.out.println("Externaljoin");
                                joinRoomExternal((String) json.get("roomid"));
                            }

                        } else if (json.get("type").equals("quit")) {
                            quit();
                        } else if (json.get("type").equals("movejoin")) {
                            move((String) json.get("former"), (String) json.get("roomid"),
                                    (String) json.get("identity"));
                        }

                    } catch (ParseException e) {
                        // e.printStackTrace();
                        // System.out.println(e);
                    }

                }

                System.out.println("Client disconnected abruptly");
                quit();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        public void sendResponse(JSONObject res) {
            out.println(res);

        }

        void quit() {
            Chatroom current = chatrooms.get(chatrooms.indexOf(client.chatroom));
            if (current.owner.id.equals(client.id)) {
                deleteroom(client, client.chatroom.name, chatroom);
            }

            broadcastRoomMessage(client.id, client.chatroom.name, "", client.chatroom);

            client.chatroom.removeClient(client);
            server.users.remove(client.id);

            if (serverChange) {

            } else {
                if (server.serverid.equals(server.leader)) {

                    for (int coo : server.coordination_ports) {
                        try {
                            ManagedChannel channel = ManagedChannelBuilder
                                    .forAddress(this.server_addresses.get(this.coordination_ports.indexOf(coo)), coo)
                                    .usePlaintext().build();
                            heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                            clientQuitRequest request = clientQuitRequest.newBuilder().setId(client.id).build();
                            acknowledge response = stub.clientQuit(request);
                            System.out.println(response.toString());
                        } catch (Exception e) {

                        }

                    }
                    existInOtherServers = false;

                } else {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(server.server_addresses.get(server.server_ids.indexOf(server.leader)),
                                    server.coordination_ports.get(server.server_ids.indexOf(server.leader)))
                            .usePlaintext().build();
                    heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                    clientQuitRequest request = clientQuitRequest.newBuilder().setId(client.id).build();
                    acknowledge response = stub.clientQuit(request);
                    System.out.println(response.toString());
                }
            }

            try {
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        public void joinRoomExternal(String newRoom) {
            if (server.serverid.equals(server.leader)) {
                if (server.rooms.containsKey(newRoom)) {
                    JSONObject res = new JSONObject();
                    res.put("type", "route");
                    res.put("roomid", newRoom);
                    res.put("host", server_addresses.get(server_ids.indexOf(server.rooms.get(newRoom))));
                    res.put("port", clients_ports.get(server_ids.indexOf(server.rooms.get(newRoom))).toString());
                    System.out.println(res.toString());
                    out.println(res);

                    serverChange = true;
                } else {
                    JSONObject res = new JSONObject();
                    res.put("type", "roomchange");
                    res.put("identity", client.id);
                    res.put("former", client.chatroom.name);
                    res.put("roomid", client.chatroom.name);
                    out.println(res);
                }

            } else {
                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(server.server_addresses.get(server.server_ids.indexOf(server.leader)),
                                server.coordination_ports.get(server.server_ids.indexOf(server.leader)))
                        .usePlaintext().build();
                heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                joinRoomRequest request = joinRoomRequest.newBuilder().setType("joinroom").setIdentity(client.id)
                        .setFormer(client.chatroom.name).setRoomid(newRoom).build();
                joinRoomResponse response = stub.joinRoom(request);
                System.out.println(response.getApproved());
                if (response.getApproved().equals("true")) {

                    System.out.println("Approved for joinroom across servers");
                    System.out.println(response.getServerid());
                    System.out.println("Host:" + server_addresses.get(server_ids.indexOf(response.getServerid())));
                    System.out.println("Port:" + clients_ports.get(server_ids.indexOf(response.getServerid())));

                    JSONObject res = new JSONObject();
                    res.put("type", "route");
                    res.put("roomid", newRoom);
                    res.put("host", server_addresses.get(server_ids.indexOf(response.getServerid())));
                    res.put("port", clients_ports.get(server_ids.indexOf(response.getServerid())).toString());
                    System.out.println(res.toString());
                    out.println(res);

                    serverChange = true;
                } else {
                    System.out.println("Not approved for joinroom across servers");
                    JSONObject res = new JSONObject();
                    res.put("type", "roomchange");
                    res.put("identity", client.id);
                    res.put("former", client.chatroom.name);
                    res.put("roomid", client.chatroom.name);
                    out.println(res);

                }

            }
        }

        public void move(String former, String roomid, String identity) {

            System.out.println("Request came from existing client of another server to join a chatroom");

            if (server.serverid.equals(server.leader)) {

                for (int coo : server.coordination_ports) {
                    try {
                        ManagedChannel channel = ManagedChannelBuilder
                                .forAddress(server.server_addresses.get(server.coordination_ports.indexOf(coo)), coo)
                                .usePlaintext().build();
                        heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                        moveJoinRequest req = moveJoinRequest.newBuilder()
                                .setFormer(former)
                                .setRoomid(roomid)
                                .setIdentity(identity)
                                .setNewServer(server.serverid)
                                .build();

                        acknowledge ack = stub.moveJoin(req);
                        System.out.println(ack.toString());

                        if (server.rooms.containsKey(roomid)) {
                            Chatroom room = chatrooms.get(getChatroomByProperty(roomid));
                            client = new Client(identity, server, room, this);

                            room.addClient(client);
                            server.users.put(client.id, server.serverid);

                        } else {
                            Chatroom room = chatrooms.get(0);
                            client = new Client(identity, server, room, this);

                            room.addClient(client);
                            server.users.put(client.id, server.serverid);

                        }

                        JSONObject res = new JSONObject();
                        res.put("type", "serverchange");
                        res.put("approved", "true");
                        res.put("serverid", server.serverid);
                        out.println(res);

                        broadcastRoomMessage(identity, "", roomid, client.chatroom);
                    } catch (Exception e) {

                    }

                }

                server.rooms.put(roomid, server.serverid);

            } else {
                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(server.server_addresses.get(server.server_ids.indexOf(server.leader)),
                                server.coordination_ports.get(server.server_ids.indexOf(server.leader)))
                        .usePlaintext().build();
                heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                moveJoinRequest request = moveJoinRequest.newBuilder().setFormer(former).setRoomid(roomid)
                        .setIdentity(identity).setNewServer(server.serverid).build();
                acknowledge response = stub.moveJoin(request);

                System.out.println("Response:" + response.getAck());
                if (response.getAck().equals("Ack")) {
                    if (server.rooms.containsKey(roomid)) {
                        Chatroom room = chatrooms.get(getChatroomByProperty(roomid));
                        client = new Client(identity, server, room, this);
                        room.addClient(client);
                        server.users.put(client.id, server.serverid);

                    } else {
                        Chatroom room = chatrooms.get(0);
                        client = new Client(identity, server, room, this);
                        room.addClient(client);
                        server.users.put(client.id, server.serverid);
                        // broadcastRoomMessage(identity, "", roomid, room);
                    }

                    JSONObject res = new JSONObject();
                    res.put("type", "serverchange");
                    res.put("approved", "true");
                    res.put("serverid", server.serverid);
                    out.println(res);

                    broadcastRoomMessage(identity, "", roomid, client.chatroom);

                    System.out.println("Respone sent to the newly coming client");
                } else {

                }
            }

        }
    }

    public static synchronized void serverDown(String server_id) {
        System.out.println("Server Down : " + serverid);
        if (serverid.equals(leader)) {
            coordination_ports.remove(server_ids.indexOf(server_id));
            // clients_ports.remove(server_ids.indexOf(server_id));
            // server_addresses.remove(server_ids.indexOf(server_id));
            // servers_islive.remove(server_ids.indexOf(server_id));
            // server_ids.remove(server_id);

            for (String usr : users.keySet()) {
                if (users.get(usr).equals(server_id)) {
                    users.remove(usr);
                }
            }

            for (String room : rooms.keySet()) {
                if (rooms.get(room).equals(server_id)) {
                    rooms.remove(room);
                }
            }

            for (int coo : coordination_ports) {
                try {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(server_addresses.get(coordination_ports.indexOf(coo)), coo)
                            .usePlaintext().build();
                    heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);

                    serverDownDetail res = serverDownDetail.newBuilder()
                            .setServerid(server_id)
                            .build();

                    acknowledge ack = stub.serverDown(res);
                    System.out.println(ack.toString());

                } catch (Exception e) {

                }

            }
            Server.leader_elect();
        } else {
            System.out.println("Error: only leader should call serverDown method to inform other servers");
        }
    }

}

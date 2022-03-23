package com.heartbeat;

import java.util.ArrayList;

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
import com.heartbeat.pb.Heartbeat.serverConnectionResponse.Builder;
import com.heartbeat.pb.Heartbeat.serverDownDetail;
import com.heartbeat.pb.heartbeatGrpc.heartbeatBlockingStub;
import com.heartbeat.pb.heartbeatGrpc.heartbeatStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

//import com.heartbeat.pb.Heartbeat.Client;

public class HeartBeatService extends heartbeatGrpc.heartbeatImplBase {

    ArrayList<String> user_names = new ArrayList<>();

    private Server server;

    public HeartBeatService(Server server) {
        this.server = server;
    }

    @Override
    public void isLive(HeartBeatRequest request, io.grpc.stub.StreamObserver<HeartBeatResponse> responseObserver) {
        // System.out.println("isLive");
        responseObserver.onNext(HeartBeatResponse.newBuilder().setIsLive("isLive").build());
        responseObserver.onCompleted();
    }

    // public boolean check_user(String user){

    // boolean exist = false;
    // for (int i=0;i<user_names.size();i++ ){
    // if(user_names.get(i).equals(user)){
    // exist = true;
    // }
    // }
    // return exist;
    // }

    // @Override
    // public void sendNewIdentify(ServerClient request,
    // io.grpc.stub.StreamObserver<ServerClient> responseObserver){
    // System.out.println("New Identity");
    // String user = request.getIdentity();

    // if (user_names.size()==0){
    // user_names.add(user);
    // //System.out.println("here size 0");
    // responseObserver.onNext(ServerClient.newBuilder().setType("new_identify_by_leader").setIdentity(user).setServerid(request.getServerid()).build());
    // }
    // else{
    // if(check_user(user)==false){
    // user_names.add(user);
    // //System.out.println("here false");
    // responseObserver.onNext(ServerClient.newBuilder().setType("new_identify_by_leader").setIdentity(user).setServerid(request.getServerid()).build());
    // }
    // else{
    // //System.out.println("here true");
    // responseObserver.onNext(ServerClient.newBuilder().setType("new_identify_r_by_leader").setIdentity(user).setServerid(request.getServerid()).build());
    // }
    // }
    // //System.out.println(user_names.toString());
    // responseObserver.onCompleted();
    // //user_names.add(user);

    // }

    @Override
    public void clientExist(clientExistRequest request, StreamObserver<clientExistResponse> responseObserver) {

        System.out.println("userid:" + request.getIdentity() + ";server:" + request.getServerid());
        System.out.println(server.users.toString());
        if (server.users.containsKey(request.getIdentity())) {
            System.out.println("User exists");
            responseObserver.onNext(
                    clientExistResponse.newBuilder().setTyp("newidentity").setIdentity(request.getIdentity())
                            .setServerid(request.getServerid()).setApproved("false").build());

            responseObserver.onCompleted();
        } else {
            System.out.println("User does not exist");
            server.users.put(request.getIdentity(), request.getServerid());
            responseObserver.onNext(
                    clientExistResponse.newBuilder().setTyp("newidentity").setIdentity(request.getIdentity())
                            .setServerid(request.getServerid()).setApproved("true").build());
            responseObserver.onCompleted();

            System.out.println("Cordination ports:" + server.coordination_ports.toString());
            for (int coo : server.coordination_ports) {
                try {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(server.server_addresses.get(server.coordination_ports.indexOf(coo)), coo)
                            .usePlaintext().build();
                    heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                    clientExistResponse req = clientExistResponse.newBuilder().setTyp("newidentity")
                            .setIdentity(request.getIdentity())
                            .setServerid(request.getServerid()).setApproved("true").build();

                    acknowledge ack = stub.addNewClient(req);
                    System.out.println(ack.toString());

                } catch (Exception e) {

                }

            }

        }

    }

    @Override
    public void addNewClient(clientExistResponse request, StreamObserver<acknowledge> responseObserver) {
        System.out.println(request.toString());
        server.users.put(request.getIdentity(), request.getServerid());
        System.out.println(server.users.toString());
        responseObserver.onNext(acknowledge.newBuilder().setAck("Ack").build());
        responseObserver.onCompleted();
    }

    // check room existance

    @Override
    public void roomExist(roomExistRequest request, StreamObserver<roomExistResponse> responseObserver) {
        System.out.println("roomid: " + request.getRoomid() + ";serverid: " + request.getServerid());
        System.out.println("beginning: " + server.rooms.toString());

        if (server.rooms.containsKey(request.getRoomid())) {
            System.out.println("chat room exist");
            responseObserver.onNext(roomExistResponse.newBuilder().setType("createroom").setRoomid(request.getRoomid())
                    .setServerid(request.getServerid())
                    .setApproved("false").build());
            responseObserver.onCompleted();
        } else {
            System.out.println("the room is not exist");
            server.rooms.put(request.getRoomid(), request.getServerid());
            responseObserver.onNext(roomExistResponse.newBuilder().setType("createroom").setRoomid(request.getRoomid())
                    .setServerid(request.getServerid())
                    .setApproved("true").build());
            responseObserver.onCompleted();

            System.out.println("Cordination ports:" + server.coordination_ports.toString());
            for (int coo : server.coordination_ports) {
                try {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(server.server_addresses.get(server.coordination_ports.indexOf(coo)), coo)
                            .usePlaintext().build();
                    heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                    roomExistResponse response = roomExistResponse.newBuilder()
                            .setType("createroom")
                            .setRoomid(request.getRoomid())
                            .setServerid(request.getServerid())
                            .setApproved("true")
                            .build();

                    acknowledge ack = stub.addNewRoom(response);
                    System.out.println(ack.toString());

                } catch (Exception e) {

                }

            }

        }
        System.out.println("end: " + server.rooms.toString());

    }

    @Override
    public void addNewRoom(roomExistResponse request, StreamObserver<acknowledge> responseObserver) {
        // System.out.println(request.toString());
        server.rooms.put(request.getRoomid(), request.getServerid());
        System.out.println(server.rooms.toString());
        // deleteRoom();
        responseObserver.onNext(acknowledge.newBuilder().setAck("Ack").build());
        responseObserver.onCompleted();

    }

    @Override
    public void deleteRoom(deleteRoomRequest request, StreamObserver<deleteRoomResponse> responseObserver) {

        System.out.println("Delete room !");

        if (server.rooms.containsKey(request.getRoomid())) {

            System.out.println("chat room exist for delete");
            server.rooms.remove(request.getRoomid());

            responseObserver.onNext(deleteRoomResponse.newBuilder().setType("deleteroom")
                    .setServerid(request.getServerid()).setRoomid(request.getRoomid())
                    .setApproved("true").build());
            responseObserver.onCompleted();

            System.out.println("Cordination ports:" + server.coordination_ports.toString());
            for (int coo : server.coordination_ports) {
                try {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(server.server_addresses.get(server.coordination_ports.indexOf(coo)), coo)
                            .usePlaintext().build();
                    heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);

                    deleteRoomResponse response = deleteRoomResponse.newBuilder()
                            .setType("deleteroom")
                            .setServerid(request.getServerid())
                            .setRoomid(request.getRoomid())
                            .setApproved("true")
                            .build();

                    acknowledge ack = stub.deleteRequestedRoom(response);
                    System.out.println(ack.toString());

                    // acknowledge ack=stub.addNewRoom(response);
                    // System.out.println(ack.toString());

                } catch (Exception e) {

                }

            }

        } else {

            System.out.println("chat room does not exist for delete");
            responseObserver.onNext(deleteRoomResponse.newBuilder().setType("deleteroom")
                    .setServerid(request.getServerid()).setRoomid(request.getRoomid())
                    .setApproved("false").build());
            responseObserver.onCompleted();

        }
    }

    @Override
    public void deleteRequestedRoom(deleteRoomResponse request, StreamObserver<acknowledge> responseObserver) {
        server.rooms.remove(request.getRoomid());
        System.out.println(server.rooms.toString());
        // deleteRoom();
        responseObserver.onNext(acknowledge.newBuilder().setAck("Ack").build());
        responseObserver.onCompleted();

    }

    @Override
    public void joinRoom(joinRoomRequest request, StreamObserver<joinRoomResponse> responseObserver) {

        System.out.println("join room request to room:" + request.getRoomid());

        if (server.serverid.equals(server.leader)) {
            if (server.rooms.containsKey(request.getRoomid())) {
                responseObserver.onNext(joinRoomResponse.newBuilder().setType("joinroom").setRoomid(request.getRoomid())
                        .setServerid(server.rooms.get(request.getRoomid())).setApproved("true").build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onNext(joinRoomResponse.newBuilder().setType("joinroom").setRoomid(request.getRoomid())
                        .setApproved("false").build());
                responseObserver.onCompleted();
            }
        }

    }

    @Override
    public void serverConnected(serverConnectionRequest request,
            StreamObserver<serverConnectionResponse> responseObserver) {

        if (server.serverid.equals(server.leader)) {

            for (int coo : server.coordination_ports) {
                try {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(server.server_addresses.get(server.coordination_ports.indexOf(coo)), coo)
                            .usePlaintext().build();
                    heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                    serverConnectionRequest req = serverConnectionRequest.newBuilder()
                            .setServerid(request.getServerid())
                            .setPort(request.getPort())
                            .setAddress(request.getAddress())
                            .setClientPort(request.getClientPort())
                            .build();

                    serverConnectionResponse ack = stub.serverConnected(req);
                    System.out.println(ack.toString());

                } catch (Exception e) {

                }

            }
            Builder builder = serverConnectionResponse.newBuilder();
            builder.setAck("Ack");
            builder.addServerids(server.serverid);
            builder.addCoPorts(server.coordination_port);
            builder.addAddress(server.server_address);
            builder.addClientPorts(server.client_port);

            ArrayList<String> roomids = new ArrayList<>(server.rooms.keySet());
            // ArrayList<String> servers=new ArrayList<>(server.rooms.va);
            if (server.server_ids.size() == 0) {

            } else {
                for (int i = 0; i < server.server_ids.size(); i++) {
                    builder.addCoPorts(server.coordination_ports.get(i));
                    builder.addServerids(server.server_ids.get(i));
                    builder.addAddress(server.server_addresses.get(i));
                    builder.addClientPorts(server.clients_ports.get(i));
                }
            }
            for (int j = 0; j < roomids.size(); j++) {
                builder.addRoomid(roomids.get(j));
                builder.addServer(server.rooms.get(roomids.get(j)));
            }

            builder.setLeader(server.leader);
            builder.setLeaderAddress(server.leaderAddress);
            builder.setLeaderPort(server.leaderPort);

            server.rooms.put("MainHall-" + request.getServerid(), request.getServerid());
            serverConnectionResponse res = builder.build();
            System.out.println(res.toString());
            server.server_ids.add(request.getServerid());
            server.coordination_ports.add(request.getPort());
            server.server_addresses.add(request.getAddress());
            server.clients_ports.add(request.getClientPort());
            server.servers_islive.add("live");
            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } else {
            server.rooms.put("MainHall-" + request.getServerid(), request.getServerid());
            server.server_ids.add(request.getServerid());
            server.coordination_ports.add(request.getPort());
            server.server_addresses.add(request.getAddress());
            server.clients_ports.add(request.getClientPort());
            server.servers_islive.add("live");
            responseObserver
                    .onNext(serverConnectionResponse.newBuilder().setAck("Ack").setLeader(server.leader)
                            .setLeaderAddress(server.leaderAddress).setLeaderPort(server.leaderPort).build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void clientQuit(clientQuitRequest request, StreamObserver<acknowledge> responseObserver) {
        if (server.serverid.equals(server.leader)) {
            for (int coo : server.coordination_ports) {
                try {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(server.server_addresses.get(server.coordination_ports.indexOf(coo)), coo)
                            .usePlaintext().build();
                    heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                    clientQuitRequest req = clientQuitRequest.newBuilder()
                            .setId(request.getId())
                            .build();

                    acknowledge ack = stub.clientQuit(req);
                    System.out.println(ack.toString());

                } catch (Exception e) {

                }

            }

            server.users.remove(request.getId());
            responseObserver.onNext(acknowledge.newBuilder().setAck("Ack").build());
            responseObserver.onCompleted();
        } else {
            server.users.remove(request.getId());
            responseObserver.onNext(acknowledge.newBuilder().setAck("Ack").build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void moveJoin(moveJoinRequest request, StreamObserver<acknowledge> responseObserver) {

        if (server.serverid.equals(server.leader)) {
            System.out.println("Movejoin request came to the leader");
            for (int coo : server.coordination_ports) {
                try {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(server.server_addresses.get(server.coordination_ports.indexOf(coo)), coo)
                            .usePlaintext().build();
                    heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
                    moveJoinRequest req = moveJoinRequest.newBuilder()
                            .setFormer(request.getFormer())
                            .setRoomid(request.getRoomid())
                            .setIdentity(request.getIdentity())
                            .setNewServer(request.getNewServer())
                            .build();

                    acknowledge ack = stub.moveJoin(req);
                    System.out.println(ack.toString());

                } catch (Exception e) {

                }

            }

            server.rooms.put(request.getRoomid(), request.getNewServer());

            responseObserver.onNext(acknowledge.newBuilder().setAck("Ack").build());
            responseObserver.onCompleted();
        } else {
            server.rooms.put(request.getRoomid(), request.getNewServer());
            responseObserver.onNext(acknowledge.newBuilder().setAck("Ack").build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void serverDown(serverDownDetail request, StreamObserver<acknowledge> responseObserver) {
        server.coordination_ports.remove(server.server_ids.indexOf(request.getServerid()));
        server.clients_ports.remove(server.server_ids.indexOf(request.getServerid()));
        server.server_addresses.remove(server.server_ids.indexOf(request.getServerid()));
        server.servers_islive.remove(server.server_ids.indexOf(request.getServerid()));
        server.server_ids.remove(server.serverid);
        

        for (String usr : server.users.keySet()) {
            if (server.users.get(usr).equals(request.getServerid())) {
                server.users.remove(usr);
            }
        }

        for (String room : server.rooms.keySet()) {
            if (server.rooms.get(room).equals(request.getServerid())) {
                server.rooms.remove(room);
            }
        }

        responseObserver.onNext(acknowledge.newBuilder().setAck("Ack").build());
        responseObserver.onCompleted();
    }

}

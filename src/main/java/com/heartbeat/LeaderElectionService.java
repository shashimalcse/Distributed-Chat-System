package com.heartbeat;

import com.leader_election.pb.coordinatorGrpc;
import com.leader_election.pb.LeaderElect.elect;
import com.leader_election.pb.LeaderElect.ok;
import com.leader_election.pb.LeaderElect.coordinatorMsg;
import com.leader_election.pb.LeaderElect.coordinatorMsgAck;

import io.grpc.stub.StreamObserver;

public class LeaderElectionService extends coordinatorGrpc.coordinatorImplBase {

    private int myServerId;
    private Server myServer;

    public LeaderElectionService(Server myServer) {
        this.myServer = myServer;
        String tempServerId = myServer.serverid;
        this.myServerId = Integer.parseInt(tempServerId.substring(1));
        // this.myServerId = myServerId;
    }

    @Override
    public void leaderElect(elect request, StreamObserver<ok> responseObserver) {
        System.out.println("leaderElect RPC");
        int serverId = request.getServerId();

        // serverId check
        if (myServerId > serverId) {
            ok.Builder response = ok.newBuilder();
            response.setServerId(myServerId);
            response.setType("OK");
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
        else{
            this.myServer.leader_elect();
        }
    }

    @Override
    public void setCoordinator(coordinatorMsg request, StreamObserver<coordinatorMsgAck> responseObserver) {
        System.out.println("setCoordinator RPC");
        int newLeaderId = request.getCoordinator();
        String newLeader = "s" + Integer.toString(newLeaderId);
        System.out.println(newLeader);
        myServer.setLeader(newLeader);

        coordinatorMsgAck.Builder response = coordinatorMsgAck.newBuilder();
        response.setType("COORDINATOR ACK");
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();   
    }
}
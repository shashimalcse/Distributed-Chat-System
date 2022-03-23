package com.heartbeat;

import java.util.ArrayList;

import com.heartbeat.pb.heartbeatGrpc;

import com.heartbeat.pb.Heartbeat.ServerClient;
import com.heartbeat.pb.heartbeatGrpc.heartbeatBlockingStub;
import com.heartbeat.pb.heartbeatGrpc.heartbeatStub;


import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class ChatSystem {



    // public static void test(String payload){
    //     ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 5555).usePlaintext().build();
    //     heartbeatStub stub = heartbeatGrpc.newStub(channel);
    //     ServerClient client_request = ServerClient.newBuilder().setIdentity(payload).build();
    //     //String client_response = ServerClient.newBuilder().getIdentity();
    //     //final String y="";
    //     stub.sendNewIdentify(client_request, new StreamObserver<ServerClient>(){
    //         @Override
    //         public void onNext(ServerClient value) {
    //             System.out.println(value.toString()); 
    //             //return value.toString();  
    //             //y = value.toString();
    //         }

    //         @Override
    //         public void onError(Throwable t) {
    //             System.out.println("No Data!");
    //         }

    //         @Override
    //         public void onCompleted() {

    //         }
    //     });
        
    // }



    // public static boolean send_to_other_servers(ArrayList<Integer> coordination_ports , String payload){
    //         ArrayList<ServerClient> other_servers_res = new ArrayList<>();
    //         boolean exist = false;
    //         for (int coo : coordination_ports){
    //             ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", coo).usePlaintext().build();
    //             heartbeatBlockingStub stub = heartbeatGrpc.newBlockingStub(channel);
    //             ServerClient client_request = ServerClient.newBuilder().setType("identity").setIdentity(payload).setServerid("s1").build();

    //             ServerClient res = stub.sendNewIdentify(client_request);
    //             System.out.println(res);
    //             other_servers_res.add(res);
    //             String type = res.getType();
    //             if(type.equals("new_identify_r_by_leader")){
    //                 exist = true;
    //             }
                
    //             // stub.sendNewIdentify(client_request, new StreamObserver<ServerClient>(){
    //             //     @Override
    //             //     public void onNext(ServerClient value) {
    //             //         System.out.println(value.toString()+coo);
                        
                        
    //             //     }

    //             //     @Override
    //             //     public void onError(Throwable t) {
    //             //         //t.printStackTrace();
    //             //         System.out.println("No Data!");
    //             //     }

    //             //     @Override
    //             //     public void onCompleted() {

    //             //     }
    //             // });

    //         }
    //         //System.out.println(exist);
    //         return exist;
        
    // }

    
}

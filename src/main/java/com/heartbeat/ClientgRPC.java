package com.heartbeat;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import com.heartbeat.pb.heartbeatGrpc;
import com.heartbeat.pb.Heartbeat.HeartBeatRequest;
import com.heartbeat.pb.Heartbeat.HeartBeatResponse;
import com.heartbeat.pb.heartbeatGrpc.heartbeatBlockingStub;
import com.heartbeat.pb.heartbeatGrpc.heartbeatStub;

public class ClientgRPC {

    private heartbeatBlockingStub blockingStub;
    private heartbeatStub asyncStub;
    private String target;

    

    public ClientgRPC(String target){
        this.target = target;
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        blockingStub =heartbeatGrpc.newBlockingStub(channel);
        asyncStub = heartbeatGrpc.newStub(channel);
    }

    public void getStatus(){
        HeartBeatRequest request = HeartBeatRequest.newBuilder().setIsLive("isLive").build();

        HeartBeatResponse response;

        response = blockingStub.isLive(request);

        System.out.println(response);

    }

    // void test(String target){
    //     //System.out.print("Hi!");

    //     ClientgRPC client = new ClientgRPC(target);
        
    //     client.getStatus();

    // }

    // private static void log(String string) {
	// 	System.out.println(string);
	// }
    // public static boolean isSocketAliveUitlitybyCrunchify(String hostName, int port) {
	// 	boolean isAlive = false;
 
	// 	// Creates a socket address from a hostname and a port number
	// 	SocketAddress socketAddress = new InetSocketAddress(hostName, port);
	// 	Socket socket = new Socket();
 
	// 	// Timeout required - it's in milliseconds
	// 	int timeout = 2000;
 
	// 	log("hostName: " + hostName + ", port: " + port);
	// 	try {
	// 		socket.connect(socketAddress, timeout);
	// 		socket.close();
	// 		isAlive = true;
 
	// 	} catch (SocketTimeoutException exception) {
	// 		System.out.println("SocketTimeoutException " + hostName + ":" + port + ". " + exception.getMessage());
	// 	} catch (IOException exception) {
	// 		System.out.println(
	// 				"IOException - Unable to connect to " + hostName + ":" + port + ". " + exception.getMessage());
	// 	}
	// 	return isAlive;
	// }


    
}

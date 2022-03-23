package com.heartbeat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hello world!
 */
public final class App {
    // private App() {
    // }

    // /**
    //  * Says hello to the world.
    //  * @param args The arguments of the program.
    //  */
    private static String serverid;
    private static String server_address;
    private static int clients_port;
    private static int coordination_port;
    
    public static void main(String[] args) {
        String server_id = args[0];
        String servers_conf = args[1];
        CopyOnWriteArrayList<String> server_ids = new CopyOnWriteArrayList<String>();
        CopyOnWriteArrayList<String> server_addresses = new CopyOnWriteArrayList<String>();
        CopyOnWriteArrayList<Integer> clients_ports = new CopyOnWriteArrayList<Integer>();
        CopyOnWriteArrayList<Integer> coordination_ports =new CopyOnWriteArrayList<Integer>();
        CopyOnWriteArrayList<Chatroom> chatrooms = new CopyOnWriteArrayList<Chatroom>();
        try {
            FileInputStream fis = new FileInputStream(servers_conf);
            Scanner sc = new Scanner(fis);
            sc.nextLine();
            while (sc.hasNextLine()) {
                String[] info = sc.nextLine().split("\\s+");
                
                if(server_id.equals(info[0])) {
                    server_address = info[1];
                    serverid = info[0];
                    clients_port = Integer.valueOf(info[2]);
                    coordination_port = Integer.valueOf(info[3]);
                }
                else{
                    /*
                    server_ids.add(info[0]);
                    server_addresses.add(info[1]);
                    int cp = Integer.valueOf(info[2]);
                    clients_ports.add(cp);
                    int cop = Integer.valueOf(info[3]);
                    coordination_ports.add(cop);
                    */
                }
            }
            
            Chatroom chatroom = new Chatroom("MainHall-"+server_id,new Owner(""),null);
            chatrooms.add(chatroom);
            // Runnable server = new Server(chatroom,server_id,server_address,clients_port,coordination_port,server_ids,server_addresses,clients_ports,coordination_ports);
            // new Thread(server).start();
            sc.close();
            Runnable server = new Server(chatroom,serverid,server_address,clients_port,coordination_port,server_ids,server_addresses,clients_ports,coordination_ports, servers_conf);
            new Thread(server).start();
            // System.out.println(server_id);
            // System.out.println(clients_port);
            // System.out.println(coordination_port);
            // ClientgRPC clientgrpc = new ClientgRPC("localhost"+coordination_port);
            // clientgrpc.getStatus();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
    
}

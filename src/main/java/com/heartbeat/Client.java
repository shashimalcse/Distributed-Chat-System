package com.heartbeat;

import com.heartbeat.Server.ClientHandler;

public class Client {
    String id = "";
    Server server;
    Chatroom chatroom;
    ClientHandler clientHandler;
    
    public Client(String id, Server server, Chatroom chatroom, ClientHandler connection) {
        this.id = id;
        this.server = server;
        this.chatroom = chatroom;
        this.clientHandler=connection;
    }

    void setChatRoom(Chatroom room){
        this.chatroom=room;
    }
}

package com.src.server;

public class ServerProgram {

    public static void main(String args[]) {
        Server server = new Server(args);
        server.start();
    }
}
package com.src.server;

public class ServerProgram {

    public static void main(String args[]) {
        // args = new String[]{"-p=10333"};
        Server server = new Server(args);
        server.start();
    }
}
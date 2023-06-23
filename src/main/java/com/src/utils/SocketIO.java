package com.src.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class SocketIO {
    public Socket socket = null;
    public BufferedReader is = null;
    public BufferedWriter os = null;

    public SocketIO(Socket socket) throws IOException{
        this.socket = socket;
        this.os = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        this.is = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }
}
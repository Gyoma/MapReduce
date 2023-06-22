package com.src;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.cli.*;

public class SimpleServerProgram {

    static private int n = 0;

    public static void main(String args[]) {

        Options options = new Options();

        Option state = new Option("s", "state", false, "A status of the node");
        //input.setRequired(true);
        options.addOption(state);

        Option count = new Option("n", "number", true, "Total number of workers");
        options.addOption(count);

        //Option output = new Option("r", "role", true, "A role of the node");
        ////output.setRequired(true);
        //options.addOption(output);
        
        CommandLineParser parser = new DefaultParser();

        
        
        
        // try {
        //     cmd = parser.parse(options, args, false);
        // } catch (ParseException e) {
        //     System.out.println(e.getMessage());
        //     formatter.printHelp("utility-name", options);

        //     System.exit(1);
        // }


        // CommandLineParser parser = new DefaultParser();
        // HelpFormatter formatter = new HelpFormatter();
        // CommandLine cmd = null;//not a good practice, it serves it purpose 


        ServerSocket listener = null;
        String line;
        BufferedReader is;
        BufferedWriter os;
        Socket socketOfServer = null;

        // Try to open a server socket on port 10325
        // Note that we can't choose a port less than 1023 if we are not
        // privileged users (root)

        try {
            listener = new ServerSocket(10325);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }

        try {
            System.out.println("Server is waiting to accept user...");

            // Accept client connection request
            // Get new Socket at Server.
            socketOfServer = listener.accept();
            System.out.println("Accept a client!");

            // Open input and output streams
            is = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
            os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));

            while (true) {
                line = is.readLine();

                os.write(">> " + line);
                os.newLine();
                os.flush();

                // If users send QUIT (To end conversation).
                if (line.equals("QUIT")) {
                    os.write(">> OK");
                    os.newLine();
                    os.flush();
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
        System.out.println("Sever stopped!");
    }
}
package com.src.client;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.src.utils.LogFormatter;

public class ClientProgram {

    private static MasterClient master = null;
    static Logger logger = Logger.getLogger("Client");

    public static void main(String[] args) {

        // args = new String[]{"-p=10325",
        //         "-path=CC-MAIN-20230320211022-20230321001022-00511.warc.wet",
        //         "-s=127.0.0.1:10333;127.0.0.1:10334"};

        LogManager.getLogManager().reset();
        logger.setLevel(Level.INFO);

        logger.addHandler(new ConsoleHandler());

        try {
            Handler fileHandler = new FileHandler("./client.log", 2000, 5);
            logger.addHandler(fileHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Handler handler : logger.getHandlers())
            handler.setFormatter(new LogFormatter());

        Options options = new Options();
        Option serversOpt = new Option("s", "servers", true, "");
        options.addOption(serversOpt);
        Option portOpt = new Option("p", "port", true, "");
        options.addOption(portOpt);
        Option pathOpt = new Option("path", "path", true, "");
        options.addOption(pathOpt);

        logger.log(Level.INFO, String.join(" ", args));

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args, false);
            //Integer port = Integer.parseInt(cmd.getOptionValue("port", "10325"));

            logger.log(Level.INFO, "Starting client program...");
            logger.log(Level.INFO, "File path: " + cmd.getOptionValue("path", ""));

            master = new MasterClient(cmd.getOptionValue("path", ""));

            if (cmd.hasOption('s')) {
                String[] addresses = cmd.getOptionValue("servers", "").split(";");

                logger.log(Level.INFO, "Processing the following addresses:\n" + String.join("\n", addresses));

                for (String data : addresses) {
                    String[] pair = data.split(":");
                    String address = pair[0];
                    int port = Integer.parseInt(pair[1]);

                    logger.log(Level.INFO, "Starting a handler for " + address + " on port " + port);
                    master.addConnection(address, port);
                }

                logger.log(Level.INFO, "Starting the master");
                master.start();
            } else {
                logger.log(Level.INFO, "Cannot find any addresses to process");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        logger.log(Level.INFO, "Stopping client program...");
        master.stop();
        logger.log(Level.INFO, "Client program stopped.");
    }

}
package com.src.client;

import java.io.IOException;
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

        // Server Host
        // final String serverHost = "tp-1a201-26.enst.fr";
        
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
        Option so = new Option("s", "servers", true, "");
        options.addOption(so);
        Option po = new Option("p", "path", true, "");
        options.addOption(po);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        logger.log(Level.INFO, String.join(" ", args));

        logger.log(Level.INFO, "Starting client program...");

        try {
            cmd = parser.parse(options, args, false);

            logger.log(Level.INFO, "File path: " + cmd.getOptionValue('p', ""));
            master = new MasterClient(cmd.getOptionValue('p', ""));

            if (cmd.hasOption('s')) {
                String[] addresses = cmd.getOptionValue('s', "").split(";");

                logger.log(Level.INFO, "Processing the following addresses:\n" + String.join("\n", addresses));

                for (String address : addresses) {
                    logger.log(Level.INFO, "Starting a handler for " + address);
                    master.addConnection(address, 10325);
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
    }

}
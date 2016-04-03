package com.dummyc0m.pylon.pylonmc;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by Dummyc0m on 3/23/16.
 */
public class McConsoleHandler extends Thread {
    private final Logger logger = LogManager.getLogger();
    private final PylonMcServer server;
    private PipedOutputStream outputStream;
    private final Queue<String> queue = new ConcurrentLinkedDeque<>();

    public McConsoleHandler(PylonMcServer server) {
        setName("Pylon Minecraft console thread");
        this.server = server;
        outputStream = new PipedOutputStream();
        try {
            System.setIn(new PipedInputStream(outputStream));
        } catch (Exception e) {
            logger.log(Level.ERROR, "Error substituting inputstream", e);
        }
    }

    @Override
    public void run() {
        try {
            String line;
            while(server.isMcAlive()) {
                if((line = queue.poll()) != null) {
                    outputStream.write((line).getBytes(Charset.forName("UTF-8")));
                    outputStream.flush();
                }
                Thread.sleep(100L);
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, "Error passing command to Mc", e);
        }
    }

    public void sendCommand(String cmd) {
        queue.offer(cmd + "\n");
    }
}

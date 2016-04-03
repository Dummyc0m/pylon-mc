package com.dummyc0m.pylon.pylonmc;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;

/**
 * Created by Dummyc0m on 3/22/16.
 */
public class PylonMcServer {
    public static void main(String[] args) throws IOException {
        new PylonMcServer().launch(args);
    }

    private Logger log = LogManager.getLogger();
    private McConsoleHandler mcConsole;
    private volatile boolean mcAlive;

    private void launch(String[] args) throws IOException {
        Thread.currentThread().setName("Pylon thread");
        PrintStream out = System.out;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Runtime runtime = Runtime.getRuntime();
        launchMc(args);
        Charset utf8 = Charset.forName("UTF-8");
        Thread ioThread = new Thread("Pylon IO Thread") {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                String line;
                try {
                    out.write(("pylon.logFile-" + new File(System.getProperty("user.dir") + File.separator + "logs", "latest.log").getAbsolutePath() + "\n").getBytes(utf8));
                    int counter = 0;
                    while (mcAlive) {
                        if(reader.ready()) {
                            line = reader.readLine();
                            switch (line) {
                                case "pylon.kill":
                                    mcConsole.sendCommand("stop");
                                    Thread.sleep(10000L);
                                    Thread[] threads = getThreads();
                                    try {
                                        for (Thread thread : threads) {
                                            if(thread != null && thread.isAlive() && !thread.equals(Thread.currentThread())) {
                                                thread.interrupt();
                                                thread.stop();
                                            }
                                        }
                                    } catch (SecurityException e) {
                                        //empty
                                    }
                                    System.exit(0);
                                    break;
                                default:
                                    mcConsole.sendCommand(line);
                                    break;
                            }
                        }
                        if(counter < 1) {
                            out.write(("pylon.maxMemory-" + String.valueOf(runtime.maxMemory()) + "\n").getBytes(utf8));
                            out.write(("pylon.freeMemory-" + String.valueOf(runtime.freeMemory()) + "\n").getBytes(utf8));
                            counter = 5;
                        }
                        counter--;
                        out.flush();
                        Thread.sleep(100L);
                    }
                } catch (IOException e) {
                    log.log(Level.ERROR, "Error passing command", e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        ioThread.setDaemon(true);
        ioThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread("Pylon Shutdown Thread") {
            @Override
            public void run() {
                try {
                    mcAlive = false;
                    out.write(("pylon.stop\n").getBytes(utf8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out.flush();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void launchMc(String[] args) {
        mcAlive = true;
        mcConsole = new McConsoleHandler(this);
        mcConsole.setDaemon(true);
        mcConsole.start();

        try {
            log.log(Level.INFO, "Loading Minecraft jar");
            File file = new File(System.getProperty("user.dir"), args[0]);
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{file.toURI().toURL()}, getClass().getClassLoader());
            Class clazz = classLoader.loadClass("net.minecraft.server.MinecraftServer");
            Method method = clazz.getMethod("main", String[].class);

            String[] launchArgs = new String[args.length];
            System.arraycopy(args, 1, launchArgs, 0, args.length - 1);
            launchArgs[launchArgs.length - 1] = "nogui";

            log.log(Level.INFO, "Launching Minecraft server");
            method.invoke(null, new Object[]{launchArgs});
        } catch (Exception e) {
            mcAlive = false;
            log.log(Level.ERROR, "Missing Minecraft jar", e);
        }
    }

    boolean isMcAlive() {
        return mcAlive;
    }

    private Thread[] getThreads() {
        ThreadGroup rootGroup = Thread.currentThread( ).getThreadGroup( );
        ThreadGroup parentGroup;
        while ( ( parentGroup = rootGroup.getParent() ) != null ) {
            rootGroup = parentGroup;
        }
        Thread[] threads = new Thread[ rootGroup.activeCount() ];
        while ( rootGroup.enumerate( threads, true ) == threads.length ) {
            threads = new Thread[ threads.length * 2 ];
        }
        return threads;
    }
}

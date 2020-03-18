package com.viettel.lb.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

//import com.viettel.lb.alert.EmailSender;
//import com.viettel.lb.alert.SendAlertThread;
import com.viettel.lb.config.Config;
import com.viettel.lb.monitor.*;
import com.viettel.lb.zoo.CheckSession;
import com.viettel.lb.zoo.SetDataZNodeThread;
import com.viettel.lb.zoo.ZKClient;

public class LoadBalanceApp {
    public static void main(String[] args) {
        if (args.length > 3) {
            throw new IllegalArgumentException("\n" +
                    "This app need max 3 arguments: \n" +
                    "\t- 1st is mode [rr|lc|lc_strict_timeout]\n" +
                    "\t- 2nd is application name\n " +
                    "\t- 3rd is path to file config.properties\n" +
                    "\t(Can skip! System auto generate by default)");
        }
        String mode, rpName, pathConfig;

        if (args.length == 0) {
            mode = "lc";
            rpName = "Reverse_Proxy";
            pathConfig = "../config/config.properties";
        } else if (args.length == 1) {
            if (!(args[0].equals("rr") || args[0].equals("lc") || args[0].equals("lc_strict_timeout"))) {
                throw new IllegalArgumentException("First parameter must is 'rr' or 'lc' or 'lc_strict_timeout'");
            }
            mode = args[0];
            rpName = "Reverse_Proxy";
            pathConfig = "../config/config.properties";
        } else if (args.length == 2) {
            if (!(args[0].equals("rr") || args[0].equals("lc") || args[0].equals("lc_strict_timeout"))) {
                throw new IllegalArgumentException("First parameter must is 'rr' or 'lc' or ''lc_strict_timeout");
            }
            mode = args[0];
            rpName = args[1];
            pathConfig = "../config/config.properties";
        } else {
            mode = args[0];
            rpName = args[1];
            pathConfig = args[2];
        }
        Config.loadProperties(pathConfig);

        ZKClient zoo = new ZKClient();
        String dataZNode;
        String pidActiveRP;
        do {
            final CountDownLatch latch = new CountDownLatch(1);

            System.out.println("Waiting for check session... ");
            Thread checkSession = new Thread(new CheckSession(latch));
            checkSession.start();

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            dataZNode = zoo.getDataZNode();
            pidActiveRP = dataZNode.split(" ")[0];

        } while (!checkPort(Config.LB_PORT, pidActiveRP));

        System.out.println("\n[START] Starting reverse proxy... \n");
        startApp(mode, rpName);
    }

    private static void startApp(String mode, String rpName) {
//        Thread sendAlert = new Thread(new SendAlertThread(new EmailSender(rpName)));
//        sendAlert.start();

        long pid = getProcessID();
        Thread writeToZNode = new Thread(new SetDataZNodeThread(new ZKClient(pid, rpName)));
        writeToZNode.start();

        if (mode.equals("rr")) {
            modeRoundRobin();
        } else if (mode.equals("lc")) {
            modeLeastConnection(false);
        } else {
            modeLeastConnection(true);
        }
    }

    // (Round Robin) Find and select node server sequentially.
    private static void modeRoundRobin() {
        try {
            Monitor monitor = new Monitor();
            CountDownLatch latch = new CountDownLatch(1);

            // Open thread update running worker on cluster each {Config.TIME_CALL_RM_API} times.
            Thread callApiRM = new Thread(new CallApiRM(monitor, latch));
            callApiRM.start();

            latch.await();

            // Create thread continuous update all running worker on cluster
            Thread callApiSpark = new Thread(new CallApiSpark(monitor));
            callApiSpark.start();

            // Open Load Balance Socket for handle request from Clients.
            ServerSocket lbSocket = new ServerSocket(Config.LB_PORT);

            int indexWorker = 0;
            while (!Thread.interrupted()) {
                try {
                    // Accept a new client connection.
                    Socket clientSocket = lbSocket.accept();

                    indexWorker = (indexWorker + 1) % monitor.getListAvailableServers().size();
                    Worker targetWorker = monitor.getListAvailableServers().get(indexWorker);

                    System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date(System
                            .currentTimeMillis())) + "] Client: " + clientSocket.getInetAddress() + ":" +
                            clientSocket.getPort() + " connect to" + " => [" + targetWorker.getName() +
                            ", currentActiveJobs: " + targetWorker.getActiveJobs() + "]\n");

                    // Open connection to selected worker.
                    Socket workerSocket = new Socket(targetWorker.getHost(), targetWorker.getPort());

                    Responser responser = new Responser(clientSocket, workerSocket, targetWorker);
                    responser.start();
                    ResponserManagement.getInstance().add(responser);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // (Least Connections) Find and select Worker with least active connections, and increment its load.
    private static void modeLeastConnection(boolean modeTimeOutStrict) {
        try {
            final Monitor monitor = new Monitor();
            CountDownLatch latch = new CountDownLatch(1);

            // Open thread update running worker on cluster each {Config.TIME_CALL_RM_API} times.
            Thread callApiRM = new Thread(new CallApiRM(monitor, latch));
            callApiRM.start();

            latch.await();

            // Create thread continuous update all running worker on cluster.
            Thread callApiSpark = new Thread(new CallApiSpark(monitor));
            callApiSpark.start();

            // Open Load Balance Socket for handle request from Clients.
            ServerSocket lbSocket = new ServerSocket(Config.LB_PORT);

            // Log connection
            Logger logger = Logger.getLogger("ConnectionLog");
            FileHandler logFile;
            try {
                logFile = new FileHandler(Config.PATH_CONNECTION_LOGS, Config.LIMIT_LOGS_FILE_SIZE_MB * 1024 * 1024,
                        Config.LIMIT_LOGS_FILE_NUMBERS, true);
                logger.addHandler(logFile);
                logFile.setFormatter(new SimpleFormatter() {
                    private static final String format = "[%2$-7s] %3$s %n";

                    @Override
                    public synchronized String format(LogRecord lr) {
                        return String.format(format,
                                new Date(lr.getMillis()),
                                lr.getLevel().getLocalizedName(),
                                lr.getMessage()
                        );
                    }
                });
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            while (!Thread.interrupted()) {
                // Accept a new client connection.
                Socket clientSocket = lbSocket.accept();

                Worker targetWorker = monitor.getMinActiveJobsWorker();

                // Open connection to selected worker.
                Socket workerSocket = new Socket(targetWorker.getHost(), targetWorker.getPort());

                String connectLog = "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new Date(System.currentTimeMillis())) + "] Client: " +
                        clientSocket.getInetAddress() + ":" + clientSocket.getPort() +
                        " connect to" + " => [" + targetWorker.getName() +
                        ", currentActiveJobs: " + targetWorker.getActiveJobs() + "]\n";

                System.out.println(connectLog);

                try {
                    // Logs all connection pass through LB
                    logger.info(connectLog);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }

                monitor.incrementActiveJobs(targetWorker);

                if (modeTimeOutStrict) {
                    workerSocket.setSoTimeout(10000);
                }

                // Start 2 thread to take and forward request/response between Client & Server,.
                Thread requester = new Thread(new Requester(clientSocket, workerSocket));
                requester.start();

                Responser responser = new Responser(clientSocket, workerSocket, targetWorker);
                responser.start();

                ResponserManagement.getInstance().add(responser);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int getProcessID() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();

        // JVM returns something like 6969@hostname. The value before the @ symbol is the PID.
        String jvmName = bean.getName();

        return Integer.parseInt(jvmName.split("@")[0]);
    }

    private static boolean checkPort(int port, String pidActiveRP) {
        System.out.println("Check port...");

        try {
            String path = new BufferedReader(new InputStreamReader(Runtime.
                    getRuntime().exec("pwd").getInputStream())).readLine();

            if (path != null) {
                String command = "python " + path + "/../config/check_port.py";

                String checkPortOutput = new BufferedReader(new InputStreamReader(Runtime.getRuntime()
                        .exec(command).getInputStream())).readLine();

                if (checkPortOutput != null) {
                    System.out.println("Already has other process binding port [" + port + "], trying kill process...");

                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(checkPortOutput);

                    if (matcher.find()) {
                        String pid = matcher.group();
                        System.out.println("PID of process binding port [" + port + "]: " + pid);

                        if (pid.equals(pidActiveRP)) {
                            return false;
                        } else {
                            String killCmd = "kill -9 " + pid;

                            Process kill;
                            int count = 0;
                            do {
                                count++;
                                System.out.println("Try kill process... " + "(" + count + ")");
                                kill = Runtime.getRuntime().exec(killCmd);

                                Thread.sleep(Config.RETRY_INTERVAL_MILISECOND);
                            } while (kill.exitValue() != 0 && Config.RETRY_TIMES < 3);

                            if (kill.exitValue() != 0) {
                                System.out.println("Can't kill process. Try this manually by super user!\n" +
                                        "Shutting down...");
                                System.exit(0);
                            }

                            if (count < Config.RETRY_TIMES) {
                                System.out.println("[Done] Port " + port + " ready to open...");
                            }
                            return true;
                        }
                    } else {
                        System.out.println("Can't kill process. Try this manually by super user!\n" +
                                "Shutting down...");
                        System.exit(0);
                    }
                } else {
                    System.out.println("Port [" + port + "] free, ready to open...");
                }
            } else {
                System.out.println("Can't read config file!!!\nShutting down...");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}

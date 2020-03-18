package com.viettel.lb.monitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.viettel.lb.config.Config;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class RecallApiSpark implements Runnable {
    private final ArrayList<Worker> listAvailableServers;
    private ArrayList<Worker> toRemove;
    private Monitor monitor;

    RecallApiSpark(ArrayList<Worker> listAvailableServers, ArrayList<Worker> toRemove, Monitor monitor) {
        this.listAvailableServers = listAvailableServers;
        this.toRemove = toRemove;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        synchronized (listAvailableServers) {
            for (Worker worker : toRemove) {
                JsonObject json = readJsonFromUrl(Config.BASE_URL + worker.getWorkerId() + "/metrics/json");

                byte count = 0;
                while (json == null && count < Config.RETRY_TIMES) {
                    try {
                        Thread.sleep(Config.RETRY_INTERVAL_MILISECOND);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count++;
                    System.out.println("[Recall] Server "+ worker.getName() +
                            " does'nt response, trying recall... (" + count + ")\n");

                    json = readJsonFromUrl(Config.BASE_URL + worker.getWorkerId() + "/metrics/json");
                }

                if (json != null && count < Config.RETRY_TIMES) {
                    int activeJobs = json.getAsJsonObject("gauges").getAsJsonObject(worker.getWorkerId() +
                            ".driver.DAGScheduler.job.activeJobs").get("value").getAsInt();
                    worker.setActiveJobs(activeJobs);

                    listAvailableServers.add(worker);
                } else {
                    System.out.println("Already recall " + Config.RETRY_TIMES +
                            " times, but some servers still don't response.\nTrying recall RM_API...\n");
                    monitor.getAllAvailableServers();
                }
            }
        }
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private JsonObject readJsonFromUrl(String url) {
        try {
            InputStream input = new URL(url).openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            String jsonText = readAll(reader);

            return new JsonParser().parse(jsonText.trim()).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

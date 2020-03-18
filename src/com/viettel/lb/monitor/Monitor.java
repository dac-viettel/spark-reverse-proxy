package com.viettel.lb.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.viettel.lb.config.Config;
import com.viettel.lb.main.ResponserManagement;

import static com.viettel.lb.config.Config.RM_API;

public class Monitor {
    private ArrayList<Worker> listAvailableServers;
    private ArrayList<String> listServersInConfig = new ArrayList<>(Arrays.asList(Config.LIST_THRIFT.split(",")));

    public Monitor() { }

    public ArrayList<Worker> getListAvailableServers() {
        return this.listAvailableServers;
    }

    // Call API to ResourceManager => get available servers (NOW RUNNING).
    // Return false if has something wrong when call api
    // (ex: Json response is'nt standard format, Resource Manager does'nt response...)
    void getAllAvailableServers() {
        ArrayList<Worker> listServers = new ArrayList<>();

        System.out.println("Call RM_API ...");
        JsonObject jsonFromRM = readJsonFromUrl(RM_API);

        byte count = 0;
        while (jsonFromRM == null && count < Config.RETRY_TIMES) {
            try {
                Thread.sleep(Config.RETRY_INTERVAL_MILISECOND);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
            System.out.println("[Recall] ResourceManager does'nt response, trying recall... (" + count + ")\n");
            jsonFromRM = readJsonFromUrl(RM_API);
        }

        if (jsonFromRM != null && count < Config.RETRY_TIMES) {
            JsonArray allAppRunning = jsonFromRM.getAsJsonObject("apps").getAsJsonArray("app");

            for (int i = 0; i < allAppRunning.size(); i++) {

                JsonElement element = allAppRunning.get(i);
                String workerName = element.getAsJsonObject().get("name").getAsString();

                for (String item : listServersInConfig) {
                    if (item.contains(workerName)) {
                        Worker worker = new Worker(
                                element.getAsJsonObject().get("id").getAsString(),
                                workerName,
                                Config.getHostByInfo(item),
                                Config.getPortByInfo(item));
                        listServers.add(worker);
                    }
                }
            }
            listAvailableServers = listServers;
        } else {
            System.out.println("Recall RM_API failed.");
        }
    }

    // Call API Spark-Thrift Server
    // Return false if has something wrong when call api
    void getInfoServers() {
        ArrayList<Worker> toRemove = new ArrayList<>();

        for (Worker worker : listAvailableServers) {
            JsonObject json = readJsonFromUrl(Config.BASE_URL + worker.getWorkerId() + "/metrics/json");

            if (json != null) {
                int activeJobs = json.getAsJsonObject("gauges")
                        .getAsJsonObject(worker.getWorkerId() + ".driver.DAGScheduler.job.activeJobs")
                        .get("value").getAsInt();
                worker.setActiveJobs(activeJobs);
            } else {
                toRemove.add(worker);
            }
        }
        if (toRemove.size() != 0) {
            listAvailableServers.removeAll(toRemove);

            Thread recallApi = new Thread(new RecallApiSpark(listAvailableServers, toRemove, this));
            recallApi.start();
        }
    }

    void updateServersStatus() {
        System.out.println(ResponserManagement.getInstance().getListResponserString());

        if (listAvailableServers.size() != 0) {
            System.out.println("[UPDATED - " + new SimpleDateFormat("HH:mm:ss dd-MM-yyyy")
                    .format(new Date(System.currentTimeMillis())) + "] CLUSTER STATUS");
            for (Worker worker : listAvailableServers) {
                System.out.println("[ " + worker.getName().split("-")[0] + " | Active Jobs: "
                        + worker.getActiveJobs() + " ]");
            }
            System.out.println("----------------------------------------------\n");
        } else {
            System.out.println("------ [WARN] NOTHING WORKER ON CLUSTER ------\n");
        }
    }

    public synchronized Worker getMinActiveJobsWorker() {
        Worker targetWorker = listAvailableServers.get(0);
        for (int i = 1; i < listAvailableServers.size(); i++) {
            if (listAvailableServers.get(i).getActiveJobs() < targetWorker.getActiveJobs()) {
                targetWorker = listAvailableServers.get(i);
            }
        }
        return targetWorker;
    }

    public synchronized void incrementActiveJobs(Worker worker) {
        worker.setActiveJobs(worker.getActiveJobs() + 1);
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

            return new JsonParser().parse(jsonText).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
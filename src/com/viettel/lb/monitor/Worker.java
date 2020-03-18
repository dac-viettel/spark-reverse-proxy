package com.viettel.lb.monitor;

public class Worker {
    private String workerId;
    private String name;
    private int activeJobs;

    private String host;
    private int port;

    public Worker(String workerId, String name, String host, int port) {
        this.workerId = workerId;
        this.name= name;
        this.host=host;
        this.port=port;
    }

    @Override
    public String toString() {
        return "Worker{" + "name='" + name + '\'' + ", activeJobs=" + activeJobs + '}';
    }

    public String getWorkerId() {
        return this.workerId;
    }

    public String getName() {
        return name;
    }

    public int getActiveJobs() {
        return this.activeJobs;
    }

    public void setActiveJobs(int activeJobs) {
        this.activeJobs = activeJobs;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}

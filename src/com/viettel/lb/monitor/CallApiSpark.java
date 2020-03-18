package com.viettel.lb.monitor;

import com.viettel.lb.config.Config;

public class CallApiSpark implements Runnable {
    private Monitor monitor;

    public CallApiSpark(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                monitor.getInfoServers();
                monitor.updateServersStatus();

                Thread.sleep(Config.TIME_CALL_SPARK_API_MILISECOND);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

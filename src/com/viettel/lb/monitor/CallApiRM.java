package com.viettel.lb.monitor;

import com.viettel.lb.config.Config;

import java.util.concurrent.CountDownLatch;

public class CallApiRM implements Runnable {
    private Monitor monitor;
    private CountDownLatch latch;

    public CallApiRM(Monitor monitor, CountDownLatch latch) {
        this.monitor = monitor;
        this.latch = latch;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                monitor.getAllAvailableServers();

                latch.countDown();

                Thread.sleep(Config.TIME_CALL_RM_API_MILISECOND);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

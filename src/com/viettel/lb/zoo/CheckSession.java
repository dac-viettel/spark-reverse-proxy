package com.viettel.lb.zoo;

import com.viettel.lb.config.Config;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.text.SimpleDateFormat;

public class CheckSession implements Runnable {
    private final CountDownLatch latch;

    public CheckSession(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void run() {
        ZKClient zoo = new ZKClient();

        while (!Thread.interrupted()) {
            String nowRunningLB = zoo.getDataZNode();
            long lastTimeModify = zoo.getMtime();

            long difTime = (System.currentTimeMillis() - lastTimeModify);
            if (difTime > Config.MAX_TIME_WAIT_HEARTBEAT_FROM_LB_MILISECOND) {
                System.out.println("[TRIGGER] After " + difTime/1000 + " seconds system not update ZNode!\n");
                break;
            }

            if (nowRunningLB == null) {
                System.out.println("[Nothing data in ZNode. Wait for update...]");
            } else {
                System.out.println("[SLEEP] - [Now active LB: " + nowRunningLB + ", last time update: "
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastTimeModify)) + "]\n");
            }

            try {
                Thread.sleep(Config.TIME_CALL_ZOOKEEPER_API_TO_GET_DATA_ZNODE_MILISECOND);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        latch.countDown();
    }
}

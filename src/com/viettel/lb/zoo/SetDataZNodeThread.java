package com.viettel.lb.zoo;

import com.viettel.lb.config.Config;

public class SetDataZNodeThread implements Runnable {
    private ZKClient zoo;

    public SetDataZNodeThread(ZKClient zoo) {
        this.zoo = zoo;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                zoo.setDataZNode();

                Thread.sleep(Config.TIME_CALL_ZOOKEEPER_API_TO_SET_DATA_ZNODE_MILISECOND);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

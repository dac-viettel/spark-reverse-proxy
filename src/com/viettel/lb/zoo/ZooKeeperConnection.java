package com.viettel.lb.zoo;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

public class ZooKeeperConnection {
    private ZooKeeper zoo;
    private final CountDownLatch connectedSignal = new CountDownLatch(1);

    // Method to connect zookeeper ensemble.
    ZooKeeper connect(String host) throws IOException,InterruptedException {
        zoo = new ZooKeeper(host,4000, new Watcher() {
            public void process(WatchedEvent we) {
                if (we.getState() == KeeperState.SyncConnected) {
                    connectedSignal.countDown();
                }
            }
        });

        connectedSignal.await();
        return zoo;
    }
}
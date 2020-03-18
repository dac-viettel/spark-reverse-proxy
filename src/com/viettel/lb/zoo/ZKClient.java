package com.viettel.lb.zoo;

import com.viettel.lb.config.Config;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ZKClient {
    private static ZooKeeper zoo;
    private static ZooKeeperConnection con;
    private String path = Config.ZNODE_PATH;
    private String rpName;
    private long pid;

    public ZKClient() { }

    public ZKClient(long pid, String rpName) {
        this.pid = pid;
        this.rpName = rpName;
    }

    // Get last time modify ZNode
    public long getMtime() {
        long mTime = 0;
        try {
            con = new ZooKeeperConnection();
            zoo = con.connect(Config.ZOO_SERVER);

            Stat stat = znodeExists(path);
            mTime = stat.getMtime();
        } catch (InterruptedException | KeeperException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                zoo.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return mTime;
    }

    public String getDataZNode() {
        try {
            con = new ZooKeeperConnection();
            zoo = con.connect(Config.ZOO_SERVER);
            Stat stat = znodeExists(path);

            if (stat != null) {
                byte[] b = zoo.getData(path, false, null);

                System.out.println("Get data ZNode done on " +
                        new SimpleDateFormat("HH:mm:ss dd-MM-yyyy").format(new Date(System.currentTimeMillis())) +
                        " - " + new String(b, StandardCharsets.UTF_8));
                return new String(b, StandardCharsets.UTF_8);
            } else {
                System.out.println("ZNode does not exists!");
            }
        } catch(IOException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        } finally {
            try {
                zoo.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void setDataZNode() {
        byte[] data = (pid + " " + rpName).getBytes();

        try {
            con = new ZooKeeperConnection();
            zoo = con.connect(Config.ZOO_SERVER);

            System.out.println("Set \"" + pid + " " + rpName + "\" to ZNode on " +
                    new SimpleDateFormat("HH:mm:ss dd-MM-yyyy").format(new Date(System.currentTimeMillis())));
            update(path, data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                zoo.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Stat znodeExists(String path) throws KeeperException,InterruptedException {
        return zoo.exists(path,true);
    }

    private void update(String path, byte[] data) throws KeeperException,InterruptedException {
        zoo.setData(path, data, zoo.exists(path,false).getVersion());
    }
}

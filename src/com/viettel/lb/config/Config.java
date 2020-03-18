package com.viettel.lb.config;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.*;

public class Config {
    public static String RM_API;
    public static String BASE_URL;

    public static int LB_PORT;

    public static int RETRY_TIMES;
    public static int RETRY_INTERVAL_MILISECOND;

    public static long TIME_CALL_RM_API_MILISECOND;

    public static int TIME_CALL_SPARK_API_MILISECOND;

    public static int MAX_TIME_WAIT_HEARTBEAT_FROM_LB_MILISECOND;

    public static int TIME_CALL_ZOOKEEPER_API_TO_GET_DATA_ZNODE_MILISECOND;
    public static int TIME_CALL_ZOOKEEPER_API_TO_SET_DATA_ZNODE_MILISECOND;

    public static String PATH_CONNECTION_LOGS;
    public static int LIMIT_LOGS_FILE_SIZE_MB;
    public static int LIMIT_LOGS_FILE_NUMBERS;

    public static String ZOO_SERVER;
    public static String ZNODE_PATH;

    public static String LIST_THRIFT;

    public static String SMTP_HOST;
    public static String SSL_PORT;

    public static String FROM_EMAIL;
    public static String PASSWORD;
    public static String TO_EMAIL;
    public static String CC_EMAIL;
    public static String MAIL_SUBJECT;
    public static String MAIL_CONTENT;

     // Load properties from file if it name exactly match with properties of class
    public static void loadProperties(String path) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(path));
            String key;
            Class c = Config.class;
            Field f;

            for (Object objectKey : prop.keySet()) {
                key = objectKey.toString();
                f = c.getDeclaredField(key);
                String typeName = f.getType().getName();
                if (typeName.equals(String.class.getName())) {
                    f.set(Config.class, prop.getProperty(key));
                } else if (typeName.equals(int.class.getName()) || typeName.equals(Integer.class.getName())) {
                    f.set(Config.class, Integer.valueOf(prop.getProperty(key)));
                } else if (typeName.equals(long.class.getName()) || typeName.equals(Long.class.getName())) {
                    f.set(Config.class, Long.valueOf(prop.getProperty(key)));
                } else {
                    f.set(Config.class, prop.getProperty(key));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getHostByInfo(String info) {
        return info.split("-")[1];
    }

    public static int getPortByInfo(String info) {
        return Integer.parseInt(info.split("-")[2]);
    }
}

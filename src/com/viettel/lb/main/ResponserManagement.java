package com.viettel.lb.main;

import java.util.ArrayList;
import java.util.List;

public class ResponserManagement {
    private static ResponserManagement instance;

    private List<Responser> responsers;

    public ResponserManagement() {
        responsers = new ArrayList<>();

    }

    public static ResponserManagement getInstance() {
        if (instance == null) {
            instance = new ResponserManagement();
        }
        return instance;
    }

    public void add(Responser responser) {
        removeTerminatedOne();
        responsers.add(responser);
    }

    public String getListResponserString() {
        removeTerminatedOne();
        StringBuilder stringBuilder = new StringBuilder();
        for (Responser r : responsers) {
            stringBuilder.append(r.toString() + "\n");
        }
        return stringBuilder.toString();

    }

    private void removeTerminatedOne() {
        List<Responser> aliveResponser = new ArrayList<>();
        for (Responser r : responsers) {
            if (r.isAlive()) {
                aliveResponser.add(r);
            }
        }
        responsers.clear();
        responsers.addAll(aliveResponser);
        aliveResponser.clear();
    }

}

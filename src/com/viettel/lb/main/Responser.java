package com.viettel.lb.main;

import com.viettel.lb.monitor.Worker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Responser extends Thread {
    private Socket clientSocket;
    private Socket workerSocket;
    private Worker targetWorker;

    public Responser(Socket clientSocket, Socket workerSocket, Worker targetWorker) {
        this.clientSocket = clientSocket;
        this.workerSocket = workerSocket;
        this.targetWorker = targetWorker;
    }

    @Override
    public void run() {
        try {
            final byte[] reply = new byte[512];

            final BufferedInputStream workerReader = new BufferedInputStream(workerSocket.getInputStream());
            final BufferedOutputStream clientWriter = new BufferedOutputStream(clientSocket.getOutputStream());

            try {
                int bytes_read;
                while ((bytes_read = workerReader.read(reply)) != -1) {
                    clientWriter.write(reply, 0, bytes_read);
                    clientWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            workerReader.close();
            clientWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                workerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "Connection from "+
                clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort() +
                " to "+ targetWorker.getName() + " - " +
                workerSocket.getInetAddress().toString() + ":" + workerSocket.getPort();
    }
}

package com.viettel.lb.main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Requester implements Runnable {
    private Socket clientSocket;
    private Socket workerSocket;
    
    public Requester(Socket clientSocket, Socket workerSocket) {
        this.clientSocket = clientSocket;
        this.workerSocket = workerSocket;
    }

    @Override
    public void run() {
        try {
            final byte[] request = new byte[512];

            final BufferedInputStream clientReader = new BufferedInputStream(clientSocket.getInputStream());
            final BufferedOutputStream workerWriter = new BufferedOutputStream(workerSocket.getOutputStream());

            try {
                int bytes_read;
                while ((bytes_read = clientReader.read(request)) != -1) {
                    workerWriter.write(request, 0, bytes_read);
                    workerWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            clientReader.close();
            workerWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

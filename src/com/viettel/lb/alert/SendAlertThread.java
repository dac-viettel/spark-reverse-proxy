package com.viettel.lb.alert;

public class SendAlertThread implements Runnable {
    EmailSender emailSender;

    public SendAlertThread(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void run() {
        emailSender.sendEmail();
    }
}

package com.viettel.lb.alert;

import com.viettel.lb.config.Config;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class EmailSender {
    String rpName;

    public EmailSender(String rpName) {
        this.rpName = rpName;
    }

    public void sendEmail() {
        Properties props = new Properties();
        props.put("mail.smtp.host", Config.SMTP_HOST); //SMTP Host
        props.put("mail.smtp.socketFactory.port", Config.SSL_PORT); //SSL Port
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); //SSL Factory Class
        props.put("mail.smtp.auth", "true"); //Enabling SMTP Authentication
        props.put("mail.smtp.port", "465"); //SMTP Port

        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Config.FROM_EMAIL, Config.PASSWORD);
            }
        };

        Session session = Session.getDefaultInstance(props, auth);
//        System.out.println("[Mail-Service] SSL Mail-Session created...\n");

        try
        {
            MimeMessage msg = new MimeMessage(session);
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress(Config.FROM_EMAIL, "Bot"));

            msg.setReplyTo(InternetAddress.parse(Config.TO_EMAIL, false));

            msg.setSubject(Config.MAIL_SUBJECT, "UTF-8");

            msg.setText(rpName + " starting up on " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(new Date(System.currentTimeMillis())) + "\n" + Config.MAIL_CONTENT, "UTF-8");

            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(Config.TO_EMAIL, false));
            msg.addRecipients(Message.RecipientType.CC, InternetAddress.parse(Config.CC_EMAIL));

            Transport.send(msg);

            System.out.println("E-Mail sent successfully!!!\n");
        }
        catch (Exception e) {
            System.out.println("[WARN] Mail-Service not available!\n");
        }
    }
}

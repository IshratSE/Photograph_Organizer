package com.mycompany.photographytagger;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class Mailer {

    public static void sendEmail(String to, String subject, String messageText) {

        
        final String fromEmail = "ishratishty09@gmail.com";  
        final String appPassword = "rwqx qsga tnpk wdeq";
        

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props,new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(messageText);

            Transport.send(message);
            System.out.println("OTP sent successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

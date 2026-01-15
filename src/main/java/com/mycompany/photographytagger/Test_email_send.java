
package com.mycompany.photographytagger;

import java.util.Scanner;

public class Test_email_send {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter recipient email: ");
        String recipient = scanner.nextLine().trim();

        
        String otp = OTPGenerator.generateOTP(6);

        
        String subject = "Your OTP Code";
        String body = "Your verification OTP is: " + otp + "\nThis OTP will expire in 5 minutes.";

        
        System.out.println("Sending OTP to " + recipient + " ...");
        Mailer.sendEmail(recipient, subject, body);
        System.out.println("OTP sent (check inbox/spam).");

        
        System.out.println("(For testing) Generated OTP: " + otp);

        
        System.out.print("Enter the OTP you received: ");
        String userInput = scanner.nextLine().trim();

        if (userInput.equals(otp)) {
            System.out.println("OTP Verified Successfully!");
        } else {
            System.out.println("Invalid OTP. Verification failed.");
        }

        scanner.close();
    }
}


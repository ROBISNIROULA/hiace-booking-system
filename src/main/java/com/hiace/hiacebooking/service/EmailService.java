package com.hiace.hiacebooking.service;

public interface EmailService {

    // Sent after successful eSewa payment
    void sendPaymentConfirmation(
        String toEmail,
        String userName,
        String source,
        String destination,
        int seatNumber,
        double fare,
        String transactionId,
        String bookingDate
    );

    // Sent when booking is cancelled
    void sendCancellationNotification(
        String toEmail,
        String userName,
        String source,
        String destination,
        int seatNumber
    );
}
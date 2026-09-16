package com.hiace.hiacebooking.serviceimpl;

import com.hiace.hiacebooking.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendPaymentConfirmation(
            String toEmail,
            String userName,
            String source,
            String destination,
            int seatNumber,
            double fare,
            String transactionId,
            String bookingDate) {

        System.out.println("=================================");
        System.out.println("sendPaymentConfirmation called");
        System.out.println("From: "    + fromEmail);
        System.out.println("To: "      + toEmail);
        System.out.println("Subject: Booking Confirmed — "
                           + source + " → " + destination);
        System.out.println("=================================");

        if (toEmail == null || toEmail.trim().isEmpty()) {
            System.out.println("❌ toEmail is null/empty!");
            return;
        }

        try {
            MimeMessage message =
                mailSender.createMimeMessage();
            MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("✅ Booking Confirmed — "
                + source + " → " + destination);
            helper.setText(
                buildConfirmationHtml(
                    userName, source, destination,
                    seatNumber, fare,
                    transactionId, bookingDate),
                true);

            mailSender.send(message);
            System.out.println("✅ EMAIL SENT SUCCESSFULLY to: "
                               + toEmail);

        } catch (MessagingException e) {
            System.out.println("❌ MessagingException: "
                               + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("❌ Unexpected email error: "
                               + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void sendCancellationNotification(
            String toEmail,
            String userName,
            String source,
            String destination,
            int seatNumber) {

        System.out.println("sendCancellationNotification to: "
                           + toEmail);

        if (toEmail == null || toEmail.trim().isEmpty()) {
            System.out.println("❌ toEmail is null/empty!");
            return;
        }

        try {
            MimeMessage message =
                mailSender.createMimeMessage();
            MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("❌ Booking Cancelled — "
                + source + " → " + destination);
            helper.setText(
                buildCancellationHtml(
                    userName, source,
                    destination, seatNumber),
                true);

            mailSender.send(message);
            System.out.println("✅ Cancellation email sent to: "
                               + toEmail);

        } catch (Exception e) {
            System.out.println("❌ Cancellation email error: "
                               + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── HTML TEMPLATES ────────────────────────────────────────────────────────

    private String buildConfirmationHtml(
            String userName,
            String source,
            String destination,
            int seatNumber,
            double fare,
            String transactionId,
            String bookingDate) {

        return "<!DOCTYPE html><html><head>" +
            "<meta charset='UTF-8'><style>" +
            "body{margin:0;padding:20px;background:#f5f7fa;" +
            "font-family:Arial,sans-serif;}" +
            ".wrap{max-width:560px;margin:0 auto;" +
            "border-radius:16px;overflow:hidden;" +
            "box-shadow:0 4px 20px rgba(0,0,0,.1);}" +
            ".hdr{background:linear-gradient(" +
            "135deg,#007bff,#0056b3);" +
            "padding:32px;text-align:center;}" +
            ".hdr h1{color:#fff;margin:0;font-size:24px;}" +
            ".hdr p{color:rgba(255,255,255,.85);" +
            "margin:6px 0 0;font-size:14px;}" +
            ".body{background:#fff;padding:32px;}" +
            ".status{background:#d4edda;color:#155724;" +
            "padding:6px 18px;border-radius:20px;" +
            "font-size:13px;font-weight:700;" +
            "display:inline-block;margin-bottom:20px;}" +
            ".route{background:linear-gradient(" +
            "135deg,#007bff,#0056b3);" +
            "border-radius:12px;padding:18px;" +
            "text-align:center;margin-bottom:22px;}" +
            ".route span{color:#fff;font-size:22px;" +
            "font-weight:800;}" +
            ".route .arr{color:#93c5fd;margin:0 10px;}" +
            "table{width:100%;border-collapse:collapse;" +
            "margin-bottom:20px;}" +
            "td{padding:12px 14px;" +
            "border-bottom:1px solid #f1f5f9;" +
            "font-size:14px;}" +
            ".lbl{color:#64748b;width:45%;}" +
            ".val{color:#1e293b;font-weight:700;}" +
            ".amt{background:#f0fdf4;" +
            "border:2px solid #86efac;" +
            "border-radius:12px;padding:18px;" +
            "text-align:center;margin-bottom:20px;}" +
            ".amt-n{font-size:30px;font-weight:900;" +
            "color:#15803d;}" +
            ".btn{display:block;background:#007bff;" +
            "color:#fff;text-decoration:none;" +
            "padding:14px;border-radius:10px;" +
            "text-align:center;font-size:15px;" +
            "font-weight:700;margin-bottom:16px;}" +
            ".ftr{background:#1e293b;padding:20px;" +
            "text-align:center;}" +
            ".ftr p{color:#64748b;font-size:12px;margin:3px 0;}" +
            ".brand{color:#38bdf8!important;" +
            "font-size:15px!important;font-weight:700!important;}" +
            "</style></head><body><div class='wrap'>" +

            // Header
            "<div class='hdr'>" +
            "<h1>&#x1F6A0; Booking Confirmed!</h1>" +
            "<p>Your seat has been successfully reserved</p>" +
            "</div>" +

            // Body
            "<div class='body'>" +
            "<p style='font-size:16px;color:#1e293b;" +
            "margin-bottom:16px;'>Dear <strong>" +
            userName + "</strong>,</p>" +
            "<p style='color:#64748b;font-size:14px;" +
            "margin-bottom:20px;'>" +
            "Your eSewa payment was successful and your " +
            "booking is now confirmed.</p>" +

            "<div style='text-align:center'>" +
            "<span class='status'>&#x2713; PAYMENT CONFIRMED" +
            "</span></div>" +

            "<div class='route'>" +
            "<span>" + source + "</span>" +
            "<span class='arr'>&#8594;</span>" +
            "<span>" + destination + "</span>" +
            "</div>" +

            "<table>" +
            "<tr><td class='lbl'>&#x1FA91; Seat Number</td>" +
            "<td class='val'>Seat " + seatNumber +
            "</td></tr>" +
            "<tr><td class='lbl'>&#x1F4C5; Booking Date</td>" +
            "<td class='val'>" + bookingDate +
            "</td></tr>" +
            "<tr><td class='lbl'>&#x1F516; Transaction ID</td>" +
            "<td class='val'>" + transactionId +
            "</td></tr>" +
            "<tr><td class='lbl'>&#x1F4B3; Payment Method</td>" +
            "<td class='val'>&#x1F49A; eSewa</td></tr>" +
            "</table>" +

            "<div class='amt'>" +
            "<p style='font-size:12px;color:#166534;" +
            "margin:0 0 6px;'>Amount Paid</p>" +
            "<div class='amt-n'>Rs. " + (long) fare +
            "</div></div>" +

            "<a href='http://localhost:8080/" +
            "booking/my-bookings' class='btn'>" +
            "View My Bookings</a>" +

            "<p style='font-size:12px;color:#94a3b8;" +
            "text-align:center;'>" +
            "Thank you for choosing Hiace Booking System!" +
            "</p></div>" +

            // Footer
            "<div class='ftr'>" +
            "<p class='brand'>&#x1F690; Hiace Booking</p>" +
            "<p>Nepal's trusted Hiace booking platform</p>" +
            "</div></div></body></html>";
    }

    private String buildCancellationHtml(
            String userName,
            String source,
            String destination,
            int seatNumber) {

        return "<!DOCTYPE html><html><head>" +
            "<meta charset='UTF-8'><style>" +
            "body{margin:0;padding:20px;background:#f5f7fa;" +
            "font-family:Arial,sans-serif;}" +
            ".wrap{max-width:560px;margin:0 auto;" +
            "border-radius:16px;overflow:hidden;}" +
            ".hdr{background:linear-gradient(" +
            "135deg,#dc3545,#c82333);" +
            "padding:28px;text-align:center;}" +
            ".hdr h1{color:#fff;margin:0;font-size:20px;}" +
            ".body{background:#fff;padding:28px;" +
            "border:1px solid #e2e8f0;}" +
            "table{width:100%;border-collapse:collapse;" +
            "margin:16px 0;}" +
            "td{padding:12px;border-bottom:1px solid #f1f5f9;" +
            "font-size:14px;}" +
            ".ftr{background:#1e293b;padding:16px;" +
            "text-align:center;}" +
            ".ftr p{color:#64748b;font-size:12px;margin:0;}" +
            "</style></head><body><div class='wrap'>" +
            "<div class='hdr'>" +
            "<h1>&#x274C; Booking Cancelled</h1></div>" +
            "<div class='body'>" +
            "<p>Dear <strong>" + userName + "</strong>,</p>" +
            "<p style='color:#64748b;margin:12px 0;'>" +
            "Your booking has been cancelled.</p>" +
            "<table>" +
            "<tr><td style='color:#64748b;'>Route</td>" +
            "<td style='font-weight:700;'>" +
            source + " &#8594; " + destination +
            "</td></tr>" +
            "<tr><td style='color:#64748b;'>Seat</td>" +
            "<td style='font-weight:700;'>Seat " +
            seatNumber + "</td></tr>" +
            "</table></div>" +
            "<div class='ftr'>" +
            "<p>&#x1F690; Hiace Booking System</p>" +
            "</div></div></body></html>";
    }
}
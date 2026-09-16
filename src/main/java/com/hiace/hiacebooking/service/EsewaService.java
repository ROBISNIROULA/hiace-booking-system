package com.hiace.hiacebooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class EsewaService {

    @Value("${esewa.secret-key}")
    private String secretKey;

    /**
     * Generates HMAC-SHA256 signature for eSewa v2
     * Message format: total_amount,transaction_uuid,product_code
     */
    public String generateSignature(String totalAmount,
                                     String transactionUuid,
                                     String productCode) {
        try {
            String message = "total_amount=" + totalAmount +
                             ",transaction_uuid=" + transactionUuid +
                             ",product_code=" + productCode;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes("UTF-8"), "HmacSHA256");
            mac.init(keySpec);

            byte[] rawHmac = mac.doFinal(
                message.getBytes("UTF-8"));

            return Base64.getEncoder()
                         .encodeToString(rawHmac);

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to generate eSewa signature: "
                + e.getMessage());
        }
    }
}
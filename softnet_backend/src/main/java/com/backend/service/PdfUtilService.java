package com.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class PdfUtilService {

    public String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);

            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Checksum calculation failed", e);
        }
    }

    public String generateFileName(String cui, LocalDateTime timestamp, String version, String checksum) {
        String timestampStr = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String shortChecksum = checksum.substring(0, 8);
        return String.format("%s_%s_v%s_%s.pdf", cui, timestampStr, version, shortChecksum);
    }
}

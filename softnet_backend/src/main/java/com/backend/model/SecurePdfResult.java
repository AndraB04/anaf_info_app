package com.backend.model;

import java.time.LocalDateTime;

public class SecurePdfResult {
    private final byte[] pdfData;
    private final String fileName;
    private final String checksum;
    private final long fileSize;
    private final LocalDateTime timestamp;
    private final String version;

    public SecurePdfResult(byte[] pdfData, String fileName, String checksum, long fileSize, LocalDateTime timestamp, String version) {
        this.pdfData = pdfData;
        this.fileName = fileName;
        this.checksum = checksum;
        this.fileSize = fileSize;
        this.timestamp = timestamp;
        this.version = version;
    }

    public byte[] getPdfData() {
        return pdfData;
    }

    public String getFileName() {
        return fileName;
    }

    public String getChecksum() {
        return checksum;
    }

    public long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getVersion() {
        return version;
    }
}

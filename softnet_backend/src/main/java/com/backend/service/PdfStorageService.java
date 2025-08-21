package com.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfStorageService {

    @Value("${app.pdf.storage.path:./pdf-storage}")
    private String storagePath;

    @Value("${app.pdf.storage.enabled:true}")
    private boolean storageEnabled;

    @Value("${app.pdf.signature.secret}")
    private String signatureSecret;

    private final PdfUtilService pdfUtilService;

    public String storePdf(byte[] pdfBytes, String cui, LocalDateTime timestamp,
                           String checksum, String version) throws IOException {

        if (!storageEnabled) {
            log.info("PDF storage is disabled, skipping file save.");
            return pdfUtilService.generateFileName(cui, timestamp, version, checksum);
        }

        String fileName = pdfUtilService.generateFileName(cui, timestamp, version, checksum);
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            log.info("Created PDF storage directory: {}", storageDir);
        }
        String yearMonth = timestamp.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path subDir = storageDir.resolve(yearMonth);
        if (!Files.exists(subDir)) {
            Files.createDirectories(subDir);
        }

        Path filePath = subDir.resolve(fileName);
        if (Files.exists(filePath)) {
            log.warn("PDF file already exists, skipping write operation: {}", filePath);
            return fileName;
        }

        Files.write(filePath, pdfBytes, StandardOpenOption.CREATE_NEW);
        writeMetadata(filePath, cui, timestamp, checksum, version, pdfBytes.length);
        log.info("PDF stored successfully: {} (size: {} bytes)", filePath, pdfBytes.length);

        return fileName;
    }


    public StorageResult retrieveAndVerifyPdf(String fileName) throws IOException {
        try {
            Path fullPath = findPdfPath(fileName);
            if (fullPath == null) {
                throw new FileNotFoundException("PDF not found in storage: " + fileName);
            }

            byte[] pdfData = Files.readAllBytes(fullPath);
            String actualChecksum = pdfUtilService.calculateChecksum(pdfData);
            String expectedShortChecksum = extractShortChecksumFromFileName(fileName);

            if (!actualChecksum.startsWith(expectedShortChecksum)) {
                log.error("INTEGRITY VIOLATION: File {} may have been tampered with! Expected checksum starting with: {}, but actual checksum is: {}",
                        fileName, expectedShortChecksum, actualChecksum);
                throw new SecurityException("PDF integrity verification failed. The file may be corrupt or modified.");
            }

            log.info("PDF integrity verified successfully for file: {} (checksum prefix: {})", fileName, expectedShortChecksum);
            return new StorageResult(fullPath.toString(), fileName, actualChecksum, pdfData.length, pdfData);

        } catch (IOException e) {
            log.error("I/O error retrieving and verifying PDF: {}", fileName, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving and verifying PDF: {}", fileName, e);
            throw new RuntimeException("PDF retrieval/verification failed", e);
        }
    }

    public boolean pdfExists(String fileName) {
        if (!storageEnabled) return false;
        try {
            Path filePath = findPdfPath(fileName);
            return filePath != null && Files.exists(filePath);
        } catch (Exception e) {
            log.error("Error checking PDF existence for file: {}", fileName, e);
            return false;
        }
    }

    public List<StoredPdfInfo> getStoredPdfsForCompany(String cui) {
        List<StoredPdfInfo> storedPdfs = new ArrayList<>();
        if (!storageEnabled) {
            return storedPdfs;
        }

        try {
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                return storedPdfs;
            }

            final String filePrefix = cui + "_";

            Files.walk(storageDir)
                    .filter(path -> path.toString().endsWith(".pdf"))
                    .filter(path -> path.getFileName().toString().startsWith(filePrefix))
                    .forEach(path -> {
                        try {
                            storedPdfs.add(new StoredPdfInfo(
                                    path.getFileName().toString(),
                                    Files.size(path),
                                    LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), java.time.ZoneId.systemDefault())
                            ));
                        } catch (IOException e) {
                            log.error("Could not read file info for: {}", path, e);
                        }
                    });

            return storedPdfs.stream()
                    .sorted((a, b) -> b.getLastModified().compareTo(a.getLastModified()))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Error getting stored PDFs for CUI: {}", cui, e);
            return storedPdfs;
        }
    }

    public StorageStats getStorageStats() {
        if (!storageEnabled) {
            return new StorageStats(0, 0, false);
        }
        try {
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                return new StorageStats(0, 0, true);
            }

            long[] stats = Files.walk(storageDir)
                    .filter(path -> path.toString().endsWith(".pdf"))
                    .mapToLong(path -> {
                        try { return Files.size(path); } catch (IOException e) { return 0; }
                    })
                    .collect(() -> new long[2],
                            (arr, size) -> { arr[0]++; arr[1] += size; },
                            (arr1, arr2) -> { arr1[0] += arr2[0]; arr1[1] += arr2[1]; });

            return new StorageStats(stats[0], stats[1], true);
        } catch (IOException e) {
            log.error("Error calculating storage stats", e);
            return new StorageStats(0, 0, true);
        }
    }

    private void writeMetadata(Path pdfPath, String cui, LocalDateTime timestamp,
                               String checksum, String version, int fileSize) throws IOException {
        String metadataFileName = pdfPath.getFileName().toString().replace(".pdf", "_metadata.json");
        Path metadataPath = pdfPath.getParent().resolve(metadataFileName);
        String metadata = String.format("""
            {
              "cui": "%s",
              "generated_at": "%s",
              "version": "%s",
              "checksum_sha256": "%s",
              "file_size_bytes": %d,
              "pdf_filename": "%s",
              "storage_path": "%s",
              "immutable": true
            }
            """,
                cui, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), version,
                checksum, fileSize, pdfPath.getFileName().toString(), pdfPath.toString());
        Files.writeString(metadataPath, metadata, StandardOpenOption.CREATE_NEW);
        log.debug("Metadata written for: {}", metadataPath);
    }

    private Path findPdfPath(String fileName) throws IOException {
        Path storageRoot = Paths.get(storagePath);
        if (!Files.exists(storageRoot)) {
            return null;
        }
        try (var stream = Files.walk(storageRoot)) {
            return stream
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElse(null);
        }
    }

    private String extractShortChecksumFromFileName(String fileName) {
        String[] parts = fileName.replace(".pdf", "").split("_");
        if (parts.length >= 4) {
            return parts[parts.length - 1];
        }
        throw new IllegalArgumentException("Invalid PDF filename format for checksum extraction: " + fileName);
    }

    public String generateSignature(byte[] pdfBytes) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(signatureSecret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(pdfBytes);
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    public boolean verifySignature(byte[] pdfBytes, String providedSignature) throws Exception {
        String calculatedSignature = generateSignature(pdfBytes);
        return calculatedSignature.equals(providedSignature);
    }

    public static class StorageResult {
        private final String fullPath;
        private final String fileName;
        private final String checksum;
        private final long fileSize;
        private final byte[] pdfData;
        public StorageResult(String fullPath, String fileName, String checksum, long fileSize, byte[] pdfData) {
            this.fullPath = fullPath; this.fileName = fileName; this.checksum = checksum;
            this.fileSize = fileSize; this.pdfData = pdfData;
        }
        public String getFullPath() { return fullPath; }
        public String getFileName() { return fileName; }
        public String getChecksum() { return checksum; }
        public long getFileSize() { return fileSize; }
        public byte[] getPdfData() { return pdfData; }
    }

    public static class StoredPdfInfo {
        private final String fileName;
        private final long fileSize;
        private final LocalDateTime lastModified;
        public StoredPdfInfo(String fileName, long fileSize, LocalDateTime lastModified) {
            this.fileName = fileName; this.fileSize = fileSize; this.lastModified = lastModified;
        }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public LocalDateTime getLastModified() { return lastModified; }
        public String getFileSizeFormatted() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        }
    }

    public static class StorageStats {
        private final long fileCount;
        private final long totalSize;
        private final boolean enabled;
        public StorageStats(long fileCount, long totalSize, boolean enabled) {
            this.fileCount = fileCount; this.totalSize = totalSize; this.enabled = enabled;
        }
        public long getFileCount() { return fileCount; }
        public long getTotalSize() { return totalSize; }
        public boolean isEnabled() { return enabled; }
        public String getTotalSizeFormatted() {
            if (totalSize < 1024) return totalSize + " B";
            if (totalSize < 1024 * 1024) return String.format("%.1f KB", totalSize / 1024.0);
            if (totalSize < 1024 * 1024 * 1024) return String.format("%.1f MB", totalSize / (1024.0 * 1024));
            return String.format("%.1f GB", totalSize / (1024.0 * 1024 * 1024));
        }
    }
}
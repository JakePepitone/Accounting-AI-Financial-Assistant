package com.accountingai.model;

import java.time.LocalDateTime;

/**
 * AI-style analysis generated from extracted document text.
 *
 * <p>The current implementation is deterministic and local: it summarizes the
 * parsed statement, classifies the document, and records semantic metadata
 * without sending financial data to an external service.</p>
 */
public class DocumentAiAnalysis {

    /** Coarse document classification, such as BANK_STATEMENT. */
    private String documentType;

    /** Key-value semantic metadata extracted from the document text. */
    private String extractedMetadata;

    /** Human-readable summary of the document. */
    private String summary;

    /** Time when the analysis was generated. */
    private LocalDateTime analyzedAt;

    /** No-arg constructor required for framework/database mapping. */
    public DocumentAiAnalysis() {
    }

    /**
     * All-args constructor.
     *
     * @param documentType      coarse document classification
     * @param extractedMetadata key-value semantic metadata
     * @param summary           generated document summary
     * @param analyzedAt        analysis timestamp
     */
    public DocumentAiAnalysis(String documentType, String extractedMetadata,
                              String summary, LocalDateTime analyzedAt) {
        this.documentType = documentType;
        this.extractedMetadata = extractedMetadata;
        this.summary = summary;
        this.analyzedAt = analyzedAt;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getExtractedMetadata() {
        return extractedMetadata;
    }

    public void setExtractedMetadata(String extractedMetadata) {
        this.extractedMetadata = extractedMetadata;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    @Override
    public String toString() {
        return "DocumentAiAnalysis{" +
                "documentType='" + documentType + '\'' +
                ", analyzedAt=" + analyzedAt +
                '}';
    }
}

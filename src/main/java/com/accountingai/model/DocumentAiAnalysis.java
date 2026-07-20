package com.accountingai.model;

import java.time.LocalDateTime;

/**
 * AI-style analysis generated from extracted document text.
 *
 * <p>The app can generate this locally or through a configured remote AI
 * provider. The provider/model fields record how the analysis was produced.</p>
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

    /** Provider that produced the analysis, such as LOCAL or OPENAI. */
    private String provider;

    /** Model or algorithm identifier used by the provider. */
    private String model;

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
        this(documentType, extractedMetadata, summary, analyzedAt, null, null);
    }

    /**
     * All-args constructor including provider metadata.
     *
     * @param documentType      coarse document classification
     * @param extractedMetadata key-value semantic metadata
     * @param summary           generated document summary
     * @param analyzedAt        analysis timestamp
     * @param provider          AI provider identifier
     * @param model             AI model or algorithm identifier
     */
    public DocumentAiAnalysis(String documentType, String extractedMetadata,
                              String summary, LocalDateTime analyzedAt,
                              String provider, String model) {
        this.documentType = documentType;
        this.extractedMetadata = extractedMetadata;
        this.summary = summary;
        this.analyzedAt = analyzedAt;
        this.provider = provider;
        this.model = model;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "DocumentAiAnalysis{" +
                "documentType='" + documentType + '\'' +
                ", provider='" + provider + '\'' +
                ", model='" + model + '\'' +
                ", analyzedAt=" + analyzedAt +
                '}';
    }
}

package com.accountingai.model;

import java.time.LocalDateTime;

/**
 * Metadata about an imported PDF document.
 * <p>
 * Maps to the {@code document_metadata} table: (document_id, file_name,
 * file_path, file_size_bytes, page_count, title, author, uploaded_at,
 * statement_id, status, ai_document_type, ai_extracted_metadata, ai_summary,
 * ai_analyzed_at, ai_provider, ai_model). The {@code statementId} field is
 * nullable because a document may be imported before it is linked to a parsed
 * statement.
 */
public class DocumentMetadata {

    /** Primary key (document_id). 0 means "not yet persisted". */
    private int id;

    /** The original file name (e.g. "june-statement.pdf"). */
    private String fileName;

    /** The absolute path where the file is stored. */
    private String filePath;

    /** The file size in bytes. */
    private long fileSizeBytes;

    /** The number of pages in the PDF. */
    private int pageCount;

    /** The document title from PDF metadata (may be null/empty). */
    private String title;

    /** The document author from PDF metadata (may be null/empty). */
    private String author;

    /** When the document was uploaded/imported. */
    private LocalDateTime uploadedAt;

    /** Foreign key to the linked statement (nullable until parsed & linked). */
    private Integer statementId;

    /** Processing status (e.g. "IMPORTED", "PARSED"). */
    private String status;

    /** AI-generated coarse document type (e.g. BANK_STATEMENT). */
    private String aiDocumentType;

    /** AI-extracted semantic metadata in key=value form. */
    private String aiExtractedMetadata;

    /** AI-generated summary of the imported document. */
    private String aiSummary;

    /** Timestamp for the AI analysis. */
    private LocalDateTime aiAnalyzedAt;

    /** AI provider that generated the analysis (LOCAL, OPENAI, etc.). */
    private String aiProvider;

    /** AI model or local algorithm identifier. */
    private String aiModel;

    /** No-arg constructor required for framework/database mapping. */
    public DocumentMetadata() {
    }

    /**
     * All-args constructor.
     *
     * @param id            the document id (0 if not persisted)
     * @param fileName      the original file name
     * @param filePath      the absolute stored path
     * @param fileSizeBytes the file size in bytes
     * @param pageCount     the number of pages
     * @param title         the PDF title (nullable)
     * @param author        the PDF author (nullable)
     * @param uploadedAt    the upload timestamp
     * @param statementId   the linked statement id (nullable)
     * @param status        the processing status
     */
    public DocumentMetadata(int id, String fileName, String filePath, long fileSizeBytes,
                            int pageCount, String title, String author,
                            LocalDateTime uploadedAt, Integer statementId, String status) {
        this(id, fileName, filePath, fileSizeBytes, pageCount, title, author,
                uploadedAt, statementId, status, null, null, null, null, null, null);
    }

    /**
     * All-args constructor including AI analysis fields.
     *
     * @param id                  the document id (0 if not persisted)
     * @param fileName            the original file name
     * @param filePath            the absolute stored path
     * @param fileSizeBytes       the file size in bytes
     * @param pageCount           the number of pages
     * @param title               the PDF title (nullable)
     * @param author              the PDF author (nullable)
     * @param uploadedAt          the upload timestamp
     * @param statementId         the linked statement id (nullable)
     * @param status              the processing status
     * @param aiDocumentType      AI document classification
     * @param aiExtractedMetadata AI semantic metadata
     * @param aiSummary           AI-generated summary
     * @param aiAnalyzedAt        AI analysis timestamp
     */
    public DocumentMetadata(int id, String fileName, String filePath, long fileSizeBytes,
                            int pageCount, String title, String author,
                            LocalDateTime uploadedAt, Integer statementId, String status,
                            String aiDocumentType, String aiExtractedMetadata,
                            String aiSummary, LocalDateTime aiAnalyzedAt) {
        this(id, fileName, filePath, fileSizeBytes, pageCount, title, author,
                uploadedAt, statementId, status, aiDocumentType, aiExtractedMetadata,
                aiSummary, aiAnalyzedAt, null, null);
    }

    /**
     * All-args constructor including AI analysis and provider fields.
     *
     * @param id                  the document id (0 if not persisted)
     * @param fileName            the original file name
     * @param filePath            the absolute stored path
     * @param fileSizeBytes       the file size in bytes
     * @param pageCount           the number of pages
     * @param title               the PDF title (nullable)
     * @param author              the PDF author (nullable)
     * @param uploadedAt          the upload timestamp
     * @param statementId         the linked statement id (nullable)
     * @param status              the processing status
     * @param aiDocumentType      AI document classification
     * @param aiExtractedMetadata AI semantic metadata
     * @param aiSummary           AI-generated summary
     * @param aiAnalyzedAt        AI analysis timestamp
     * @param aiProvider          AI provider identifier
     * @param aiModel             AI model or local algorithm identifier
     */
    public DocumentMetadata(int id, String fileName, String filePath, long fileSizeBytes,
                            int pageCount, String title, String author,
                            LocalDateTime uploadedAt, Integer statementId, String status,
                            String aiDocumentType, String aiExtractedMetadata,
                            String aiSummary, LocalDateTime aiAnalyzedAt,
                            String aiProvider, String aiModel) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.pageCount = pageCount;
        this.title = title;
        this.author = author;
        this.uploadedAt = uploadedAt;
        this.statementId = statementId;
        this.status = status;
        this.aiDocumentType = aiDocumentType;
        this.aiExtractedMetadata = aiExtractedMetadata;
        this.aiSummary = aiSummary;
        this.aiAnalyzedAt = aiAnalyzedAt;
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Integer getStatementId() {
        return statementId;
    }

    public void setStatementId(Integer statementId) {
        this.statementId = statementId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAiDocumentType() {
        return aiDocumentType;
    }

    public void setAiDocumentType(String aiDocumentType) {
        this.aiDocumentType = aiDocumentType;
    }

    public String getAiExtractedMetadata() {
        return aiExtractedMetadata;
    }

    public void setAiExtractedMetadata(String aiExtractedMetadata) {
        this.aiExtractedMetadata = aiExtractedMetadata;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public LocalDateTime getAiAnalyzedAt() {
        return aiAnalyzedAt;
    }

    public void setAiAnalyzedAt(LocalDateTime aiAnalyzedAt) {
        this.aiAnalyzedAt = aiAnalyzedAt;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    @Override
    public String toString() {
        return "DocumentMetadata{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSizeBytes=" + fileSizeBytes +
                ", pageCount=" + pageCount +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", statementId=" + statementId +
                ", status='" + status + '\'' +
                ", aiDocumentType='" + aiDocumentType + '\'' +
                ", aiProvider='" + aiProvider + '\'' +
                ", aiModel='" + aiModel + '\'' +
                ", aiAnalyzedAt=" + aiAnalyzedAt +
                '}';
    }
}

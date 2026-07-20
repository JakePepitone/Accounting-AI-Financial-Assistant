package com.accountingai.model;

import java.time.LocalDateTime;

/**
 * Metadata about an imported PDF document.
 * <p>
 * Maps to the {@code document_metadata} table: (document_id, file_name,
 * file_path, file_size_bytes, page_count, title, author, uploaded_at,
 * statement_id, status). The {@code statementId} field is nullable because a
 * document may be imported before it is linked to a parsed statement.
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
                '}';
    }
}

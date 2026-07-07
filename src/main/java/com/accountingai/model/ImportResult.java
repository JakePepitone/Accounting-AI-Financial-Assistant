package com.accountingai.model;

/**
 * The outcome of importing a single PDF document.
 * <p>
 * On success it carries the stored file path plus the parsed {@link Statement}
 * and extracted {@link DocumentMetadata}. On failure it carries a human-readable
 * message. Use the static factory methods {@link #ok} and {@link #fail} rather
 * than the constructors directly.
 */
public class ImportResult {

    /** True if the import succeeded. */
    private boolean success;

    /** The path where the imported file was stored (null on failure). */
    private String storedFilePath;

    /** The parsed statement (null on failure). */
    private Statement statement;

    /** The extracted document metadata (null on failure). */
    private DocumentMetadata metadata;

    /** A human-readable status or error message. */
    private String message;

    /** No-arg constructor required for framework/database mapping. */
    public ImportResult() {
    }

    /**
     * All-args constructor.
     *
     * @param success        whether the import succeeded
     * @param storedFilePath the stored file path (nullable)
     * @param statement      the parsed statement (nullable)
     * @param metadata       the extracted metadata (nullable)
     * @param message        the status/error message
     */
    public ImportResult(boolean success, String storedFilePath, Statement statement,
                        DocumentMetadata metadata, String message) {
        this.success = success;
        this.storedFilePath = storedFilePath;
        this.statement = statement;
        this.metadata = metadata;
        this.message = message;
    }

    /**
     * Builds a successful result.
     *
     * @param storedFilePath the path where the file was stored
     * @param s              the parsed statement
     * @param m              the extracted metadata
     * @return a success {@link ImportResult}
     */
    public static ImportResult ok(String storedFilePath, Statement s, DocumentMetadata m) {
        return new ImportResult(true, storedFilePath, s, m, "Imported");
    }

    /**
     * Builds a failed result.
     *
     * @param message the failure reason
     * @return a failure {@link ImportResult}
     */
    public static ImportResult fail(String message) {
        return new ImportResult(false, null, null, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getStoredFilePath() {
        return storedFilePath;
    }

    public void setStoredFilePath(String storedFilePath) {
        this.storedFilePath = storedFilePath;
    }

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public DocumentMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DocumentMetadata metadata) {
        this.metadata = metadata;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ImportResult{" +
                "success=" + success +
                ", storedFilePath='" + storedFilePath + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

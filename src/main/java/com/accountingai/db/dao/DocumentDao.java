package com.accountingai.db.dao;

import com.accountingai.db.DatabaseManager;
import com.accountingai.model.DocumentMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for {@link DocumentMetadata} rows in the
 * {@code document_metadata} table.
 *
 * <p>Timestamps are stored as ISO-8601 {@link LocalDateTime} strings. The
 * {@code statement_id} link is nullable (an imported document may not yet be
 * associated with a parsed statement).</p>
 */
public class DocumentDao {

    private final DatabaseManager db;

    /**
     * @param db the database manager used to obtain connections
     */
    public DocumentDao(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Inserts a document metadata record and returns its generated primary key.
     *
     * @param d the metadata to persist
     * @return the generated document id (or -1 if none was returned)
     */
    public int insert(DocumentMetadata d) {
        String sql = "INSERT INTO document_metadata("
                + "file_name, file_path, file_size_bytes, page_count, title, author, "
                + "uploaded_at, statement_id, status, ai_document_type, "
                + "ai_extracted_metadata, ai_summary, ai_analyzed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, d.getFileName());
            ps.setString(2, d.getFilePath());
            ps.setLong(3, d.getFileSizeBytes());
            ps.setInt(4, d.getPageCount());
            ps.setString(5, d.getTitle());
            ps.setString(6, d.getAuthor());
            ps.setString(7, timestampToString(d.getUploadedAt()));
            // statementId is a nullable Integer — bind NULL when absent.
            if (d.getStatementId() == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, d.getStatementId());
            }
            ps.setString(9, d.getStatus());
            ps.setString(10, d.getAiDocumentType());
            ps.setString(11, d.getAiExtractedMetadata());
            ps.setString(12, d.getAiSummary());
            ps.setString(13, timestampToString(d.getAiAnalyzedAt()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert document metadata", e);
        }
    }

    /**
     * Finds a document by its primary key.
     *
     * @param id the document id
     * @return an {@link Optional} document metadata record
     */
    public Optional<DocumentMetadata> findById(int id) {
        String sql = baseSelect() + " WHERE document_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find document by id", e);
        }
    }

    /**
     * @return every document, newest first (highest id first)
     */
    public List<DocumentMetadata> findAll() {
        String sql = baseSelect() + " ORDER BY document_id DESC";
        List<DocumentMetadata> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list documents", e);
        }
    }

    /**
     * Case-insensitive substring search over file names.
     *
     * @param q the text to search for
     * @return matching documents, newest first (empty for a {@code null}/blank query)
     */
    public List<DocumentMetadata> searchByFileName(String q) {
        if (q == null || q.isBlank()) {
            return new ArrayList<>();
        }
        String sql = baseSelect()
                + " WHERE file_name LIKE '%' || ? || '%' ORDER BY document_id DESC";
        List<DocumentMetadata> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search documents", e);
        }
    }

    /**
     * Updates the processing status of a document (e.g. "IMPORTED", "PARSED").
     *
     * @param id     the document id
     * @param status the new status value
     */
    public void updateStatus(int id, String status) {
        String sql = "UPDATE document_metadata SET status = ? WHERE document_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update document status", e);
        }
    }

    // ------------------------------------------------------------------
    // Row mapping & conversion helpers
    // ------------------------------------------------------------------

    /** Common SELECT clause shared by the finder methods. */
    private String baseSelect() {
        return "SELECT document_id, file_name, file_path, file_size_bytes, page_count, "
                + "title, author, uploaded_at, statement_id, status, "
                + "ai_document_type, ai_extracted_metadata, ai_summary, ai_analyzed_at "
                + "FROM document_metadata";
    }

    /** Maps the current row to a {@link DocumentMetadata}. */
    private DocumentMetadata mapRow(ResultSet rs) throws SQLException {
        DocumentMetadata d = new DocumentMetadata();
        d.setId(rs.getInt("document_id"));
        d.setFileName(rs.getString("file_name"));
        d.setFilePath(rs.getString("file_path"));
        d.setFileSizeBytes(rs.getLong("file_size_bytes"));
        d.setPageCount(rs.getInt("page_count"));
        d.setTitle(rs.getString("title"));
        d.setAuthor(rs.getString("author"));
        d.setUploadedAt(stringToTimestamp(rs.getString("uploaded_at")));

        // statement_id may be SQL NULL; preserve that as a null Integer.
        int statementId = rs.getInt("statement_id");
        d.setStatementId(rs.wasNull() ? null : statementId);

        d.setStatus(rs.getString("status"));
        d.setAiDocumentType(rs.getString("ai_document_type"));
        d.setAiExtractedMetadata(rs.getString("ai_extracted_metadata"));
        d.setAiSummary(rs.getString("ai_summary"));
        d.setAiAnalyzedAt(stringToTimestamp(rs.getString("ai_analyzed_at")));
        return d;
    }

    /** ISO string for a timestamp, or {@code null}. */
    private static String timestampToString(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    /** Parses an ISO timestamp string, tolerating {@code null}/blank. */
    private static LocalDateTime stringToTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value);
    }
}

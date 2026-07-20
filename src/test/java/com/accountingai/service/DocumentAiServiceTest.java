package com.accountingai.service;

import com.accountingai.model.DocumentAiAnalysis;
import com.accountingai.model.Statement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the local AI-style document analysis service.
 */
class DocumentAiServiceTest {

    private static final String SAMPLE_TEXT =
            "Bank Statement\n"
            + "Customer: John Smith\n"
            + "Account Number: 123456789\n"
            + "Period: 2026-06-01 to 2026-06-30\n"
            + "Beginning Balance: 5000.00\n"
            + "Total Deposits: 2500.00\n"
            + "Total Withdrawals: 1273.56\n"
            + "Ending Balance: 6226.44\n"
            + "Transactions:\n"
            + "2026-06-01 Payroll Deposit 2500.00\n"
            + "2026-06-03 Amazon Purchase -125.99\n"
            + "2026-06-05 Utility Bill -85.12\n"
            + "2026-06-10 Restaurant -62.45\n"
            + "2026-06-15 Gas Station -48.00\n"
            + "2026-06-20 Grocery Store -152.00\n"
            + "2026-06-25 Internet Service -75.00\n";

    @Test
    void analyzeExtractsSemanticMetadataAndSummary() {
        Statement statement = new StatementParser().parseStatement(SAMPLE_TEXT);

        DocumentAiAnalysis analysis = new DocumentAiService().analyze(SAMPLE_TEXT, statement);

        assertEquals("BANK_STATEMENT", analysis.getDocumentType());
        assertNotNull(analysis.getAnalyzedAt());
        assertTrue(analysis.getExtractedMetadata().contains("customer_name=John Smith"));
        assertTrue(analysis.getExtractedMetadata().contains("account_number=123456789"));
        assertTrue(analysis.getExtractedMetadata().contains("transaction_count=7"));
        assertTrue(analysis.getSummary().contains("7 transactions"));
        assertTrue(analysis.getSummary().contains("Ending balance is $6,226.44"));
        assertTrue(analysis.getSummary().contains("Largest inflow: Payroll Deposit"));
        assertTrue(analysis.getSummary().contains("Largest outflow: Grocery Store"));
    }

    @Test
    void analyzeHandlesBlankDocuments() {
        DocumentAiAnalysis analysis = new DocumentAiService().analyze("", null);

        assertEquals("DOCUMENT", analysis.getDocumentType());
        assertTrue(analysis.getSummary().contains("No readable document text"));
        assertTrue(analysis.getExtractedMetadata().contains("line_count=0"));
        assertTrue(analysis.getExtractedMetadata().contains("word_count=0"));
    }
}

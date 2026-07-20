package com.accountingai.service;

import com.accountingai.db.dao.AccountDao;
import com.accountingai.db.dao.DocumentDao;
import com.accountingai.db.dao.StatementDao;
import com.accountingai.db.dao.TransactionDao;
import com.accountingai.model.Account;
import com.accountingai.model.DocumentMetadata;
import com.accountingai.model.ImportResult;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;

import java.util.List;

/**
 * Persists a successful PDF import into accounts, statements, transactions, and
 * document metadata tables.
 *
 * <p>Keeping this logic in the service layer lets single-file and batch import
 * share the exact same backend persistence path.</p>
 */
public class ImportPersistenceService {

    private final AccountDao accountDao;
    private final StatementDao statementDao;
    private final TransactionDao transactionDao;
    private final DocumentDao documentDao;

    /**
     * @param accountDao     account persistence
     * @param statementDao   statement persistence
     * @param transactionDao transaction persistence
     * @param documentDao    document metadata persistence
     */
    public ImportPersistenceService(AccountDao accountDao,
                                    StatementDao statementDao,
                                    TransactionDao transactionDao,
                                    DocumentDao documentDao) {
        this.accountDao = accountDao;
        this.statementDao = statementDao;
        this.transactionDao = transactionDao;
        this.documentDao = documentDao;
    }

    /**
     * Persists one successful import and returns the inserted document id.
     *
     * @param result successful import result
     * @return inserted document id, or -1 if no metadata was available
     */
    public int persist(ImportResult result) {
        if (result == null || !result.isSuccess()) {
            throw new IllegalArgumentException("Only successful imports can be persisted.");
        }

        Statement statement = result.getStatement();
        DocumentMetadata metadata = result.getMetadata();
        int statementId = 0;

        if (statement != null) {
            Account account = normalizedAccount(result);
            int accountId = accountDao.findOrCreate(account);
            statement.setAccountId(accountId);
            statementId = statementDao.insert(statement);
            statement.setId(statementId);

            List<Transaction> txns = statement.getTransactions();
            if (txns != null && !txns.isEmpty()) {
                for (Transaction t : txns) {
                    if (t != null) {
                        t.setStatementId(statementId);
                    }
                }
                transactionDao.insertBatch(txns);
            }
        }

        if (metadata == null) {
            return -1;
        }
        if (statementId > 0) {
            metadata.setStatementId(statementId);
        }
        int documentId = documentDao.insert(metadata);
        metadata.setId(documentId);
        return documentId;
    }

    private Account normalizedAccount(ImportResult result) {
        Account account = result.getAccount();
        if (account == null) {
            account = new Account();
        }
        if (account.getCustomerName() == null || account.getCustomerName().isBlank()) {
            account.setCustomerName("Unknown Customer");
        }
        if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
            DocumentMetadata md = result.getMetadata();
            String base = md != null && md.getFileName() != null ? md.getFileName() : "unknown";
            account.setAccountNumber("AUTO-" + Integer.toHexString(base.hashCode()));
        }
        return account;
    }
}

package com.accountingai;

import com.accountingai.db.DatabaseManager;
import com.accountingai.db.dao.AccountDao;
import com.accountingai.db.dao.DocumentDao;
import com.accountingai.db.dao.StatementDao;
import com.accountingai.db.dao.TransactionDao;
import com.accountingai.db.dao.UserDao;
import com.accountingai.service.BatchProcessor;
import com.accountingai.service.PdfImportService;
import com.accountingai.service.PreviewService;
import com.accountingai.service.SearchService;
import com.accountingai.service.SettingsService;
import com.accountingai.service.export.DefaultExportService;
import com.accountingai.service.export.ExportService;

/**
 * Singleton service/DAO container for the whole application.
 *
 * <p>Building a {@link DatabaseManager}, the DAOs and the services can be
 * expensive (it creates the SQLite database and seeds it on first run), so we
 * create everything once and hand out the same instances to every controller
 * through the static {@link #get()} accessor. This keeps the JavaFX controllers
 * simple: they just ask {@code AppServices.get().something()} whenever they need
 * a collaborator.</p>
 */
public final class AppServices {

    /** The lazily-created single instance shared across the application. */
    private static AppServices INSTANCE;

    // --- Database + DAOs -------------------------------------------------
    private final DatabaseManager databaseManager;
    private final AccountDao accountDao;
    private final StatementDao statementDao;
    private final TransactionDao transactionDao;
    private final DocumentDao documentDao;
    private final UserDao userDao;

    // --- Services --------------------------------------------------------
    private final PdfImportService importService;
    private final SearchService searchService;
    private final PreviewService previewService;
    private final BatchProcessor batchProcessor;
    private final SettingsService settingsService;
    private final ExportService exportService;

    /**
     * Wires up all collaborators. Called once by {@link #get()}.
     * Initializes the database (creates + seeds it if this is the first run).
     */
    private AppServices() {
        // Database first: initialize() self-creates schema + seed data if empty.
        this.databaseManager = new DatabaseManager();
        this.databaseManager.initialize();

        // DAOs (each opens its own short-lived connection per method).
        this.accountDao = new AccountDao(databaseManager);
        this.statementDao = new StatementDao(databaseManager);
        this.transactionDao = new TransactionDao(databaseManager);
        this.documentDao = new DocumentDao(databaseManager);
        this.userDao = new UserDao(databaseManager);

        // Services.
        this.importService = new PdfImportService();
        this.searchService = new SearchService(transactionDao);
        this.previewService = new PreviewService();
        this.batchProcessor = new BatchProcessor(importService);
        this.settingsService = new SettingsService();
        this.exportService = new DefaultExportService();
    }

    /**
     * Returns the shared {@code AppServices} instance, creating it on first use.
     *
     * @return the singleton container
     */
    public static synchronized AppServices get() {
        if (INSTANCE == null) {
            INSTANCE = new AppServices();
        }
        return INSTANCE;
    }

    // --- Accessors -------------------------------------------------------

    public DatabaseManager databaseManager() {
        return databaseManager;
    }

    public AccountDao accountDao() {
        return accountDao;
    }

    public StatementDao statementDao() {
        return statementDao;
    }

    public TransactionDao transactionDao() {
        return transactionDao;
    }

    public DocumentDao documentDao() {
        return documentDao;
    }

    public UserDao userDao() {
        return userDao;
    }

    public PdfImportService importService() {
        return importService;
    }

    public SearchService searchService() {
        return searchService;
    }

    public PreviewService previewService() {
        return previewService;
    }

    public BatchProcessor batchProcessor() {
        return batchProcessor;
    }

    public SettingsService settingsService() {
        return settingsService;
    }

    public ExportService exportService() {
        return exportService;
    }
}

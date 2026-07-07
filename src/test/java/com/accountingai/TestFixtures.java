package com.accountingai;

import com.accountingai.model.Account;
import com.accountingai.model.ExportData;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Central factory of reusable, deterministic sample domain objects for tests.
 *
 * <p>Every fixture mirrors the "John Smith" sample statement described in the
 * project's schema mapping (account 123456789, June-2026 period, 7 transactions).
 * Keeping the sample data in one place means the DAO, service, and export tests
 * all assert against the same known-good numbers.</p>
 */
public final class TestFixtures {

    private TestFixtures() {
        // Static factory holder — not instantiable.
    }

    /**
     * The sample account: John Smith, account number 123456789, id 1.
     *
     * @return a fresh {@link Account} instance each call
     */
    public static Account sampleAccount() {
        return new Account(1, "John Smith", "123456789");
    }

    /**
     * The seven sample transactions belonging to statement 101.
     * Deposits are positive, withdrawals negative.
     *
     * @return a fresh mutable list of {@link Transaction} each call
     */
    public static List<Transaction> sampleTransactions() {
        List<Transaction> txns = new ArrayList<>();
        txns.add(new Transaction(1001, 101, LocalDate.parse("2026-06-01"), "Payroll Deposit", new BigDecimal("2500.00")));
        txns.add(new Transaction(1002, 101, LocalDate.parse("2026-06-03"), "Amazon Purchase", new BigDecimal("-125.99")));
        txns.add(new Transaction(1003, 101, LocalDate.parse("2026-06-05"), "Utility Bill", new BigDecimal("-85.12")));
        txns.add(new Transaction(1004, 101, LocalDate.parse("2026-06-10"), "Restaurant", new BigDecimal("-62.45")));
        txns.add(new Transaction(1005, 101, LocalDate.parse("2026-06-15"), "Gas Station", new BigDecimal("-48.00")));
        txns.add(new Transaction(1006, 101, LocalDate.parse("2026-06-20"), "Grocery Store", new BigDecimal("-152.00")));
        txns.add(new Transaction(1007, 101, LocalDate.parse("2026-06-25"), "Internet Service", new BigDecimal("-75.00")));
        return txns;
    }

    /**
     * The sample statement: id 101, June 2026, the four known balances, with the
     * seven sample transactions attached to its convenience list.
     *
     * @return a fresh {@link Statement} instance each call
     */
    public static Statement sampleStatement() {
        Statement s = new Statement(
                101,
                1,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("1273.56"),
                new BigDecimal("6226.44"),
                new ArrayList<>());
        s.getTransactions().addAll(sampleTransactions());
        return s;
    }

    /**
     * A fully-populated {@link ExportData} bundle (account + statement + transactions)
     * suitable for exercising the CSV/XLSX/PDF exporters.
     *
     * @return a fresh {@link ExportData} instance each call
     */
    public static ExportData sampleExportData() {
        return new ExportData(sampleAccount(), sampleStatement(), sampleTransactions());
    }
}

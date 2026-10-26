package fi.poltsi.vempain.site.service;

import java.io.EOFException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;

import javax.sql.DataSource;

import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.zaxxer.hikari.HikariDataSource;

import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs the site database part of a publish in one site transaction and retries it when the connection to the site database is lost. The site database may sit
 * behind a WireGuard tunnel that drops silently now and then: a pooled connection that died while idle fails on first use, and a connection can also die in the
 * middle of a publish. Such failures are recognised by their cause chain (connection exceptions, PostgreSQL SQL states 08xxx and 57Pxx, socket errors); the
 * dead connections are evicted from the site pool, and after a growing pause the whole unit of work runs again in a fresh site transaction, so the site never
 * keeps half of a publish. Any other failure is not retried.
 */
@Slf4j
@Component
public class SiteDatabaseRetry {

    /**
     * The site writes of one publish item; it may raise the publish's checked exception, which is passed through without a retry.
     */
    @FunctionalInterface
    public interface SiteWork<T> {

        T run() throws VempainEntityNotFoundException;
    }

    @FunctionalInterface
    public interface Sleeper {

        void sleep(long millis) throws InterruptedException;
    }

    static final long MAX_BACKOFF_MS = 30_000;
    private static final int MAX_BACKOFF_SHIFT = 10;

    private final TransactionTemplate siteTransaction;
    private final DataSource siteDataSource;
    private final int attempts;
    private final long backoffMs;
    private final Sleeper sleeper;

    @Autowired
    public SiteDatabaseRetry(@Qualifier("siteTransactionManager") PlatformTransactionManager siteTransactionManager,
            @Qualifier("siteDataSource") DataSource siteDataSource, @Value("${vempain.site.database.retry-attempts:5}") int attempts,
            @Value("${vempain.site.database.retry-backoff-ms:2000}") long backoffMs) {
        this(siteTransactionManager, siteDataSource, attempts, backoffMs, Thread::sleep);
    }

    /**
     * @param attempts  how many times the work is run at most, at least 1
     * @param backoffMs the pause before the second attempt; it doubles for every further attempt up to {@value #MAX_BACKOFF_MS} ms
     * @param sleeper   how the pause is waited (tests pass a no-op)
     */
    public SiteDatabaseRetry(PlatformTransactionManager siteTransactionManager, DataSource siteDataSource, int attempts, long backoffMs, Sleeper sleeper) {
        this.siteTransaction = new TransactionTemplate(siteTransactionManager);
        this.siteTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.siteDataSource = siteDataSource;
        this.attempts = Math.max(1, attempts);
        this.backoffMs = Math.max(0, backoffMs);
        this.sleeper = sleeper;
    }

    /**
     * Runs {@code work} in a site transaction of its own, again after a lost site database connection until it succeeds or the attempts are used up.
     *
     * @param what the item being published, for the log
     * @return the result of the work
     */
    public <T> T execute(String what, SiteWork<T> work) throws VempainEntityNotFoundException {
        for (int attempt = 1;; attempt++) {
            try {
                return inSiteTransaction(work);
            } catch (RuntimeException e) {
                if (!isConnectionFailure(e)
                    || attempt >= attempts) {
                    throw e;
                }
                var delay = backoffFor(attempt);
                log.warn("The site database connection failed while publishing {} (attempt {} of {}): {}. Retrying in {} ms", what, attempt, attempts,
                        rootMessage(e), delay);
                evictSiteConnections();
                try {
                    sleeper.sleep(delay);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread()
                        .interrupt();
                    throw e;
                }
            }
        }
    }

    private <T> T inSiteTransaction(SiteWork<T> work) throws VempainEntityNotFoundException {
        try {
            return siteTransaction.execute(status -> {
                try {
                    return work.run();
                } catch (VempainEntityNotFoundException e) {
                    // Rolls the site transaction back on the way out; unwrapped below
                    throw new NotFoundInSiteWork(e);
                }
            });
        } catch (NotFoundInSiteWork e) {
            throw e.notFound;
        }
    }

    /**
     * @return the pause before the next attempt: the backoff doubled for every attempt already made, at most {@value #MAX_BACKOFF_MS} ms
     */
    long backoffFor(int attempt) {
        if (backoffMs == 0) {
            return 0;
        }
        var shift = Math.min(MAX_BACKOFF_SHIFT, Math.max(0, attempt - 1));
        return Math.min(MAX_BACKOFF_MS, backoffMs << shift);
    }

    /**
     * Drops the idle connections of the site pool and marks the borrowed ones for eviction on return, so that the next attempt starts on fresh connections
     * instead of other connections that died with the tunnel.
     */
    void evictSiteConnections() {
        if (siteDataSource instanceof HikariDataSource hikari) {
            var pool = hikari.getHikariPoolMXBean();
            if (pool != null) {
                pool.softEvictConnections();
            }
        }
    }

    /**
     * Whether the failure is a lost or unreachable site database connection, somewhere in the cause chain: Spring's resource failures, Hibernate's connection
     * exception, the JDBC connection exceptions, PostgreSQL SQL states 08xxx (connection exception) and 57Pxx (server shutdown, crash, cannot connect now), and
     * the socket errors the driver wraps.
     */
    static boolean isConnectionFailure(Throwable throwable) {
        var seen = 0;
        for (var cause = throwable; cause != null
                                    && seen < 32; cause = cause.getCause() == cause ? null : cause.getCause(), seen++) {
            if (cause instanceof DataAccessResourceFailureException
                || cause instanceof TransientDataAccessResourceException
                || cause instanceof CannotCreateTransactionException
                || cause instanceof JDBCConnectionException
                || cause instanceof SQLTransientConnectionException
                || cause instanceof SQLNonTransientConnectionException
                || cause instanceof SQLRecoverableException
                || cause instanceof SocketException
                || cause instanceof SocketTimeoutException
                || cause instanceof EOFException) {
                return true;
            }
            if (cause instanceof SQLException sql
                && sql.getSQLState() != null
                && (sql.getSQLState()
                    .startsWith("08")
                    || sql.getSQLState()
                        .startsWith("57P"))) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable throwable) {
        var root = throwable;
        var seen = 0;
        while (root.getCause() != null
               && root.getCause() != root
               && seen++ < 32) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass()
            .getSimpleName() : root.getMessage();
    }

    private static final class NotFoundInSiteWork extends RuntimeException {

        private final transient VempainEntityNotFoundException notFound;

        private NotFoundInSiteWork(VempainEntityNotFoundException notFound) {
            super(notFound);
            this.notFound = notFound;
        }
    }
}

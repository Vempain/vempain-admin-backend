package fi.poltsi.vempain.site.service;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The site writes of a publish item run in a site transaction of their own and are run again after a lost site database connection: the dead connections are
 * evicted, the pause doubles, and anything that is not a connection failure is passed through at once.
 */
class SiteDatabaseRetryUTC {

    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus status = mock(TransactionStatus.class);
    private final HikariDataSource dataSource = mock(HikariDataSource.class);
    private final HikariPoolMXBean pool = mock(HikariPoolMXBean.class);
    private final List<Long> pauses = new ArrayList<>();

    private SiteDatabaseRetry retry(int attempts, long backoffMs) {
        when(transactionManager.getTransaction(any())).thenReturn(status);
        when(dataSource.getHikariPoolMXBean()).thenReturn(pool);
        return new SiteDatabaseRetry(transactionManager, dataSource, attempts, backoffMs, pauses::add);
    }

    @Test
    void runsTheWorkInANewSiteTransactionAndReturnsItsResult() throws Exception {
        var retry = retry(3, 2000);

        var result = retry.execute("gallery x", () -> 42L);

        assertThat(result).isEqualTo(42L);
        var definition = org.mockito.ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        assertThat(definition.getValue()
            .getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verify(transactionManager).commit(status);
        verify(pool, never()).softEvictConnections();
        assertThat(pauses).isEmpty();
    }

    @Test
    void aLostConnectionRollsBackEvictsThePoolPausesAndRunsTheWorkAgain() throws Exception {
        var retry = retry(5, 2000);
        var calls = new AtomicInteger();

        var result = retry.execute("gallery x", () -> {
            if (calls.incrementAndGet() < 3) {
                throw new DataAccessResourceFailureException("An I/O error occurred while sending to the backend",
                        new SQLException("I/O error", "08006", new SocketTimeoutException("Read timed out")));
            }
            return "published";
        });

        assertThat(result).isEqualTo("published");
        assertThat(calls.get()).isEqualTo(3);
        verify(transactionManager, times(2)).rollback(status);
        verify(transactionManager, times(1)).commit(status);
        verify(pool, times(2)).softEvictConnections();
        // The pause doubles for every attempt already made
        assertThat(pauses).containsExactly(2000L, 4000L);
    }

    @Test
    void givesUpAfterTheConfiguredAttemptsWithTheLastFailure() {
        var retry = retry(3, 1000);
        var calls = new AtomicInteger();

        assertThatThrownBy(() -> retry.execute("page y", () -> {
            throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction", new SQLTransientConnectionException("pool timeout"));
        })).isInstanceOf(CannotCreateTransactionException.class);

        assertThat(pauses).containsExactly(1000L, 2000L);
        verify(pool, times(2)).softEvictConnections();
        verify(transactionManager, never()).commit(status);
        assertThat(calls.get()).isZero();
    }

    @Test
    void otherFailuresAndTheNotFoundExceptionAreNotRetried() {
        var retry = retry(5, 2000);
        var calls = new AtomicInteger();

        assertThatThrownBy(() -> retry.execute("gallery x", () -> {
            calls.incrementAndGet();
            throw new DataIntegrityViolationException("duplicate key");
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> retry.execute("gallery x", () -> {
            calls.incrementAndGet();
            throw new VempainEntityNotFoundException();
        })).isInstanceOf(VempainEntityNotFoundException.class);

        assertThat(calls.get()).isEqualTo(2);
        assertThat(pauses).isEmpty();
        verify(pool, never()).softEvictConnections();
        // Both failures roll the site transaction back, the checked one as well
        verify(transactionManager, times(2)).rollback(status);
    }

    @Test
    void anInterruptedPauseStopsTheRetriesWithTheFailure() {
        when(transactionManager.getTransaction(any())).thenReturn(status);
        var retry = new SiteDatabaseRetry(transactionManager, null, 5, 2000, millis -> {
            throw new InterruptedException();
        });

        assertThatThrownBy(() -> retry.execute("gallery x", () -> {
            throw new DataAccessResourceFailureException("closed");
        })).isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(Thread.interrupted()).as("the interrupt is kept")
            .isTrue();
        // A data source that is not a Hikari pool is left alone
        retry.evictSiteConnections();
    }

    @Test
    void backoffDoublesUpToTheCap() {
        var retry = retry(10, 2000);
        assertThat(retry.backoffFor(1)).isEqualTo(2000);
        assertThat(retry.backoffFor(2)).isEqualTo(4000);
        assertThat(retry.backoffFor(4)).isEqualTo(16000);
        assertThat(retry.backoffFor(5)).isEqualTo(SiteDatabaseRetry.MAX_BACKOFF_MS);
        assertThat(retry.backoffFor(40)).isEqualTo(SiteDatabaseRetry.MAX_BACKOFF_MS);
        assertThat(retry(2, 0).backoffFor(3)).isZero();
    }

    @Test
    void connectionFailuresAreRecognisedAnywhereInTheCauseChain() {
        assertThat(SiteDatabaseRetry.isConnectionFailure(new DataAccessResourceFailureException("closed"))).isTrue();
        assertThat(SiteDatabaseRetry.isConnectionFailure(new JpaSystemException(new RuntimeException(new JDBCConnectionException("lost", new SQLException())))))
            .isTrue();
        assertThat(
                SiteDatabaseRetry.isConnectionFailure(new RuntimeException(new SQLException("terminating connection due to administrator command", "57P01"))))
            .isTrue();
        assertThat(SiteDatabaseRetry.isConnectionFailure(new RuntimeException(new SQLException("This connection has been closed.", "08003")))).isTrue();
        assertThat(SiteDatabaseRetry.isConnectionFailure(new RuntimeException(new IOException("stream", new EOFException())))).isTrue();
        assertThat(SiteDatabaseRetry.isConnectionFailure(new RuntimeException(new SQLException("duplicate key", "23505")))).isFalse();
        assertThat(SiteDatabaseRetry.isConnectionFailure(new DataIntegrityViolationException("duplicate"))).isFalse();
        assertThat(SiteDatabaseRetry.isConnectionFailure(new RuntimeException(new SQLException("no state")))).isFalse();
        assertThat(SiteDatabaseRetry.isConnectionFailure(null)).isFalse();
    }
}

package com.template.quarkus.scheduler;

import com.template.quarkus.repository.ProductRepository;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * FEATURE: Scheduled Tasks with @Scheduled.
 *
 * Quarkus Scheduler lets you run methods on a fixed schedule.
 * Two scheduling styles are supported:
 *
 *   1. every()  — interval-based: "5m", "1h", "30s"
 *   2. cron()   — Unix cron expression: "0 0 * * * ?"
 *                  (seconds minutes hours day-of-month month day-of-week)
 *
 * Config-driven schedules (preferred for production):
 *   Use property expressions so schedules can be changed without a rebuild:
 *     @Scheduled(every = "{app.scheduler.stats-interval}")
 *   Then set in application.properties:
 *     app.scheduler.stats-interval=1h
 *
 * Key annotations & options:
 *   @Scheduled(every = "1h")              — runs every hour
 *   @Scheduled(cron = "0 0 2 * * ?")      — runs at 2 AM daily
 *   @Scheduled(every = "30s", delay = 10) — first run after 10s delay
 *   @Scheduled(every = "5m", concurrentExecution = SKIP) — skip if previous run is still active
 *
 * The method parameter ScheduledExecution is optional — inject it to
 * get metadata about the current execution (trigger time, scheduled time, etc.)
 */
@ApplicationScoped
public class ProductMaintenanceScheduler {

    private static final Logger LOG = Logger.getLogger(ProductMaintenanceScheduler.class);

    @Inject
    ProductRepository productRepository;

    // -------------------------------------------------------------------------
    // Task 1: Log product statistics every hour
    // "every" uses a simple duration string: 1h, 30m, 5s, etc.
    // -------------------------------------------------------------------------
    @Scheduled(
        every = "1h",
        delay = 30,                // wait 30 seconds after startup before first run
        identity = "product-stats" // unique name — useful for programmatic control
    )
    void logProductStatistics(ScheduledExecution execution) {
        LOG.infof("[SCHEDULER] Running product statistics job (triggered at %s)",
                execution.getScheduledFireTime());

        long totalActive = productRepository.countActive();
        long totalAll    = productRepository.count();
        long totalInactive = totalAll - totalActive;

        LOG.infof("[SCHEDULER] Product stats — active=%d, inactive=%d, total=%d",
                totalActive, totalInactive, totalAll);
    }

    // -------------------------------------------------------------------------
    // Task 2: Purge old soft-deleted products using a cron expression
    // This runs at 2:00 AM every day (cron: sec min hour day month weekday)
    // "0 0 2 * * ?" → at second=0, minute=0, hour=2, every day
    // -------------------------------------------------------------------------
    @Scheduled(
        cron = "0 0 2 * * ?",
        identity = "product-purge"
    )
    @Transactional
    void purgeInactiveProducts() {
        LOG.info("[SCHEDULER] Running nightly inactive-product purge");

        // Example: delete products inactive for more than 90 days
        // In a real app you'd add a deactivatedAt timestamp and filter by that
        long deleted = productRepository.delete("active = false");
        LOG.infof("[SCHEDULER] Purged %d inactive products", deleted);
    }

    // -------------------------------------------------------------------------
    // Task 3: Low-stock alert every 15 minutes
    // Demonstrates a short-interval task with config-driven threshold
    // -------------------------------------------------------------------------
    @Scheduled(
        every = "15m",
        delay = 60,
        identity = "low-stock-check"
    )
    void checkLowStock() {
        LOG.debug("[SCHEDULER] Checking for low-stock products");

        // Find products with stock below threshold
        long lowStockCount = productRepository
                .find("active = true AND stockQuantity <= ?1", 5)
                .count();

        if (lowStockCount > 0) {
            LOG.warnf("[SCHEDULER] Low-stock alert: %d product(s) have <= 5 units remaining", lowStockCount);
            // In production: send email/Slack notification here
        } else {
            LOG.debug("[SCHEDULER] No low-stock products found");
        }
    }
}

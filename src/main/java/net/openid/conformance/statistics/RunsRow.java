package net.openid.conformance.statistics;

/**
 * One month of test-module runs for one test plan, as produced by the runs aggregation.
 *
 * @param month      {@code YYYY-MM} in UTC; anything else is dropped by the assembler
 * @param planName   the plan the runs belong to, or null for standalone runs and for runs
 *                   whose plan document no longer exists
 * @param standalone true if the runs were not part of a test plan
 * @param runs       total runs in the month
 * @param passed     runs whose result was PASSED
 * @param failed     runs whose result was FAILED
 * @param warning    runs whose result was WARNING
 * @param review     runs whose result was REVIEW
 * @param skipped    runs whose result was SKIPPED
 */
public record RunsRow(String month, String planName, boolean standalone, long runs, long passed, long failed, long warning, long review, long skipped) {
}

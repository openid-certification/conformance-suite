package net.openid.conformance.statistics;

/**
 * Runs of one test module, in one month, by one user: the fact table behind the modules
 * table of the overview.
 *
 * <p>The user is part of the key because the question the table answers - how many
 * <em>people</em> hit a failure on this module - can only be answered by counting distinct
 * users, and a user who ran the same module twenty times counts once however many of those
 * runs failed.
 *
 * <p>Unlike the run cells, module cells are kept for the trailing
 * {@value StatisticsCube#MODULE_MONTHS} months only: there is one of them per user, month
 * and module, so keeping the whole history would grow without bound.
 *
 * @param month    {@code YYYY-MM} in UTC
 * @param testName the test module name, as stored on {@code TEST_INFO.testName}
 * @param ownerId  an id standing in for the user's {@code iss} and {@code sub}, meaningful
 *                 only among module cells of the same cube - {@link UserTuple} ids are
 *                 handed out separately and are not the same numbers
 * @param runs     runs of that module by that user in that month
 * @param failed   how many of those runs ended in FAILED
 */
public record ModuleUserCell(String month, String testName, int ownerId, long runs, long failed) {
}

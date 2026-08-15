package net.openid.conformance.statistics;

/**
 * One month of test plans created for one plan name.
 *
 * @param month     {@code YYYY-MM} in UTC; anything else is dropped by the assembler
 * @param planName  the plan name, or null if the document has none
 * @param plans     plans created in the month
 * @param certified plans that were made immutable (certification submissions)
 */
public record PlanRow(String month, String planName, long plans, long certified) {
}

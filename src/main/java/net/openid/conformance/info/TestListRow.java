package net.openid.conformance.info;

/**
 * A row of the test-log listing, whichever projection it was read as. The listing carries the
 * name of the plan a test belongs to, which the test document itself does not hold: it is
 * looked up for a whole page at once and attached here.
 */
interface TestListRow {

	String getPlanId();

	void setPlanName(String planName);
}

package net.openid.conformance.plan;

/**
 * A collection of test modules intended to be run with a single
 * test configuration.
 */
public interface TestPlan {
	interface ProfileNames {
		String rptest = "Test a Relying Party / OAuth2 Client";
		String optest = "Test an OpenID Provider / Authorization Server";
	}
}

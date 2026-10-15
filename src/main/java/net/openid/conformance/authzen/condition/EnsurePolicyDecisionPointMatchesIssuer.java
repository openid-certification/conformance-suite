package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Section 9.2.3 — the `policy_decision_point` value returned in the metadata
 * document MUST be identical to the URL used to construct the discovery URL.
 * Mismatch means the metadata MUST NOT be used (9.2.3).
 *
 * <p>The caller runs this stop-on-failure, because every request the test makes after this
 * point goes to an endpoint named by the very response §9.2.3 has just ruled out, and there
 * is no other source of endpoints to fall back to. The messages below therefore say that the
 * run has stopped and what to change, rather than leaving the tester to infer it from a
 * truncated log.
 */
public class EnsurePolicyDecisionPointMatchesIssuer extends AbstractCondition {

	public static final String PUBLISHED_DOCUMENT = "the PDP's discovery document";

	public static final String SIGNED_METADATA = "the PDP's signed_metadata";

	private final String document;

	public EnsurePolicyDecisionPointMatchesIssuer() {
		this(PUBLISHED_DOCUMENT);
	}

	/**
	 * @param document names the metadata in {@code server} in the messages, such as
	 *                 {@link #PUBLISHED_DOCUMENT} or {@link #SIGNED_METADATA}
	 */
	public EnsurePolicyDecisionPointMatchesIssuer(String document) {
		this.document = document;
	}

	@Override
	@PreEnvironment(required = {"config", "server"})
	public Environment evaluate(Environment env) {
		String configured = env.getString("config", "pdp.policy_decision_point");
		if (configured == null) {
			throw error("'PDP Identifier' field is missing from the 'AuthZEN' section in the test configuration");
		}
		JsonElement returnedElement = env.getElementFromObject("server", "policy_decision_point");
		if (returnedElement != null && !OIDFJSON.isString(returnedElement)) {
			throw error("The `policy_decision_point` in " + document + " is not a string, so it cannot be identical to the "
				+ "'PDP Identifier' the metadata was fetched from. AuthZEN 9.2.3 says the data in the response MUST NOT be "
				+ "used otherwise, so the test stops here.",
				args("returned_policy_decision_point", returnedElement));
		}
		String returned = returnedElement == null ? null : OIDFJSON.getString(returnedElement);
		if (returned == null) {
			throw error(capitalize(document) + " contains no `policy_decision_point`, so it cannot be checked "
				+ "against the 'PDP Identifier' the metadata was fetched from. AuthZEN 9.2.3 requires the two to be "
				+ "identical and says the data in the response MUST NOT be used otherwise, so the test stops here.");
		}
		if (!configured.equals(returned)) {
			// The difference goes in the message, not only in args: this failure ends the run, so the
			// one line the tester reads first has to say what to change.
			throw error("The `policy_decision_point` in " + document + " is not identical to the "
					+ "'PDP Identifier' the metadata was fetched from - " + describeDifference(configured, returned)
					+ ". AuthZEN 9.2.3: \"The policy_decision_point value returned MUST be identical to the Policy "
					+ "Decision Point identifier value into which the well-known URI string was inserted to create "
					+ "the URL used to retrieve the metadata. If these values are not identical, the data contained "
					+ "in the response MUST NOT be used.\" The test stops here, because every endpoint it would use "
					+ "next comes from this response. Make the two values identical - either correct the PDP's "
					+ "metadata, or correct the 'PDP Identifier' in the test configuration.",
				args("configured_pdp_identifier", configured, "returned_policy_decision_point", returned));
		}
		logSuccess("The policy_decision_point in " + document + " matches the 'PDP Identifier' the metadata was fetched from",
			args("policy_decision_point", returned));
		return env;
	}

	private static String capitalize(String text) {
		return Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	/**
	 * Name the difference when it is one of the near-misses that are easy to stare past in two long
	 * URLs, so the tester does not have to diff them by eye.
	 */
	private String describeDifference(String configured, String returned) {
		if (configured.equalsIgnoreCase(returned)) {
			return "the two values differ only in case, and are compared case-sensitively";
		}
		if (stripTrailingSlashes(configured).equals(stripTrailingSlashes(returned))) {
			return "the two values differ only in a trailing '/'";
		}
		if (configured.trim().equals(returned.trim())) {
			return "the two values differ only in surrounding whitespace";
		}
		return "the two values differ in more than case, a trailing '/' or surrounding whitespace";
	}

	private String stripTrailingSlashes(String url) {
		int end = url.length();
		while (end > 0 && url.charAt(end - 1) == '/') {
			end--;
		}
		return url.substring(0, end);
	}
}

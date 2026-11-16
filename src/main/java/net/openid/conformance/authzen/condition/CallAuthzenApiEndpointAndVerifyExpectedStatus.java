package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Calls the AuthZEN API endpoint and asserts the actual HTTP status is in
 * the acceptable-codes set previously written to {@code authzen_expected_http_status_codes}.
 * Used by modules that accept an error status, alone or alongside 200 — the
 * test author overrides {@code getAcceptableHttpStatusCodes()} to declare
 * which codes are acceptable.
 *
 * <p>Note: this sequence uses {@link CallAuthzenApiEndpointAllowingJsonParseFailure}
 * because 4xx responses commonly carry non-JSON bodies (HTML error pages,
 * plaintext, empty), so {@code authzen_api_endpoint_response.body_json} is absent
 * when the body did not parse. The caller validates the body of a 200 response
 * only, and the {@code ExtractAuthzenApiEndpoint*Response} conditions fail when
 * {@code body_json} is absent.
 */
public class CallAuthzenApiEndpointAndVerifyExpectedStatus extends AbstractConditionSequence {

	private final String[] requirements;

	public CallAuthzenApiEndpointAndVerifyExpectedStatus(String... requirements) {
		this.requirements = requirements;
	}

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request"}, strings = "authzen_api_endpoint")
	public void evaluate() {
		callAndStopOnFailure(CallAuthzenApiEndpointAllowingJsonParseFailure.class, "AUTHZEN-10");

		call(exec().mapKey("endpoint_response", "authzen_api_endpoint_response"));

		callAndContinueOnFailure(EnsureHttpStatusCodeMatchesExpected.class, Condition.ConditionResult.FAILURE, requirements);
		call(exec().unmapKey("endpoint_response"));
	}
}

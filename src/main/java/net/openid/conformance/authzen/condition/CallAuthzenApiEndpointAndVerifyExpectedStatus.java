package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Calls the Authzen API endpoint and asserts the actual HTTP status is in
 * the acceptable-codes set previously written to {@code authzen_expected_http_status_codes}.
 * Intended for negative tests where the spec only mandates "some 4xx" — the
 * test author overrides {@code getAcceptableHttpStatusCodes()} to declare
 * which codes are acceptable.
 *
 * <p>Note: this sequence uses {@link CallAuthzenApiEndpointAllowingJsonParseFailure}
 * because 4xx responses commonly carry non-JSON bodies (HTML error pages,
 * plaintext, empty). Downstream conditions that read
 * {@code authzen_api_endpoint_response.body_json} will silently no-op when the
 * body did not parse; do not chain body-shape validators after this sequence
 * — use {@link CallAuthzenApiEndpointAndVerifySuccessfulResponse} when the
 * response must parse as JSON.
 */
public class CallAuthzenApiEndpointAndVerifyExpectedStatus extends AbstractConditionSequence {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request"}, strings = "authzen_api_endpoint")
	public void evaluate() {
		callAndStopOnFailure(CallAuthzenApiEndpointAllowingJsonParseFailure.class, "AUTHZEN-10");

		call(exec().mapKey("endpoint_response", "authzen_api_endpoint_response"));

		callAndContinueOnFailure(EnsureHttpStatusCodeMatchesExpected.class, Condition.ConditionResult.FAILURE, "AUTHZEN-10.1");
		call(exec().unmapKey("endpoint_response"));
	}
}

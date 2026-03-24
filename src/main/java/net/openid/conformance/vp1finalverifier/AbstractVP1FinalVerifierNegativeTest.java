package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;

/**
 * Base class for VP1 Final verifier negative tests that expect the verifier
 * to reject the presentation with a 4xx HTTP status code.
 */
public abstract class AbstractVP1FinalVerifierNegativeTest extends AbstractVP1FinalVerifierTest {

	@Override
	protected void validateDirectPostEndpointResponse() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
	}
}

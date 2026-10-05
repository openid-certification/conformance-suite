package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestOrInvalidRequestObjectError;
import net.openid.conformance.condition.client.EnsureNoVpTokenInAuthorizationEndpointResponse;
import net.openid.conformance.condition.common.ExpectInvalidRequestObjectTypErrorPage;
import net.openid.conformance.testmodule.TestFailureException;

/**
 * Negative tests that send a validly signed request object whose 'typ' header is wrong or absent.
 * Per OID4VP section 5 the wallet MUST NOT process such a request object: it was not intended for
 * the wallet, so the wallet must not act on it. What the wallet may send back depends on the flow:
 *
 * - in the redirect flow the response_uri is only known from inside the request object, so there
 *   is no trusted endpoint and any call to the response endpoint is a failure, including one
 *   carrying an error response (as for an invalid request object signature);
 * - over the DC API the response goes back to the calling origin, which the wallet learnt from
 *   the platform rather than from the request object, so a fulfilled response carrying a
 *   protocol error (OID4VP Appendix A.4) is accepted. A rejected promise is accepted too. A
 *   fulfilled response carrying a vp_token is a failure.
 *
 * For multi-signed requests (Appendix A.3.2.2) the typ is defective in the protected header of
 * every signature, so the outcome does not depend on which signature the wallet validates.
 */
public abstract class AbstractVP1FinalWalletInvalidRequestObjectTyp extends AbstractVP1FinalWalletNegativeTestExpectingError {

	/**
	 * The signing condition that produces a request object with the defective typ header for the
	 * given Client Identifier Prefix, otherwise identical to what the normal flow sends.
	 */
	protected abstract Class<? extends Condition> defectiveSigningCondition(VP1FinalWalletClientIdPrefix prefix);

	/**
	 * The condition that produces a multi-signed request object (JWS JSON Serialization) with the
	 * defective typ in the protected header of every signature.
	 */
	protected abstract Class<? extends Condition> multiSigningConditionWithDefectiveTyp();

	@Override
	protected Class<? extends Condition> getActiveSigningCondition() {
		if (requestMethod == VP1FinalWalletRequestMethod.REQUEST_URI_MULTISIGNED) {
			return multiSigningConditionWithDefectiveTyp();
		}
		return defectiveSigningCondition(clientIdPrefix);
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectInvalidRequestObjectTypErrorPage.class, "OID4VP-1FINAL-5");
		env.putString("error_callback_placeholder", env.getString("invalid_request_object_typ_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - the request object's typ header is not 'oauth-authz-req+jwt', so the wallet should display an error, a screenshot of which must be uploaded for the test to transition to 'FINISHED'.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post (response_uri) endpoint has been called but the wallet must not process a request object whose typ header is not 'oauth-authz-req+jwt'.");
	}

	@Override
	protected void validateErrorResponse() {
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OID4VP-1FINALA-A.4");
		callAndContinueOnFailure(EnsureNoVpTokenInAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5");
		// OID4VP names no error code for a request object with a bad typ; RFC 9101 uses
		// invalid_request_object for an invalid request object and RFC 6749's invalid_request also
		// fits, so only warn if the wallet chose something else
		callAndContinueOnFailure(EnsureInvalidRequestOrInvalidRequestObjectError.class, ConditionResult.WARNING, "OID4VP-1FINAL-8.5");
	}

}

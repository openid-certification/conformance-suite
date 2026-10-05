package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.ExpectInvalidRequestObjectTypErrorPage;
import net.openid.conformance.testmodule.TestFailureException;

/**
 * Negative tests that send a validly signed request object whose 'typ' header is wrong or absent.
 * Per OID4VP section 5 the wallet MUST NOT process such a request object: it was not intended for
 * the wallet, so the wallet must not act on it at all. Like the invalid request object signature
 * test, these are 'terminate request processing' cases, where no response must be sent:
 *
 * - in the redirect flow the response_uri is only known from inside the request object, so any
 *   call to the response endpoint is a failure, including one carrying an error response;
 * - over the DC API, where OID4VP Appendix A.3.2.1 defines the signed request as a section 5
 *   request object, a fulfilled response of any kind is a failure; the wallet is expected to
 *   reject the request.
 *
 * For multi-signed requests (Appendix A.3.2.2) the typ is defective in the protected header of
 * every signature, so the outcome does not depend on which signature the wallet validates.
 */
public abstract class AbstractVP1FinalWalletInvalidRequestObjectTyp extends AbstractVP1FinalWalletTest {

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
	protected void processBrowserApiResponse() {
		handleBrowserApiResponseAsNegativeTest(
			"Browser API returned a response but the wallet must not process a request object whose typ header is not 'oauth-authz-req+jwt'.");
	}

}

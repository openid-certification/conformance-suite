package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantSetup;

public abstract class AbstractFAPIRWID2AttemptReuseAuthorizationCode extends AbstractFAPIRWID2ServerTestModule {

	private Class<? extends ConditionSequence> generateNewClientAssertionSteps;

	@Override
	protected void onPostAuthorizationFlowComplete() {

		eventLog.startBlock("Attempting reuse of authorization code");

		waitForAmountOfTime();

		// We're testing that reuse of the _code_ is refused. Reusing the client assertion
		// (only present for private_key_jwt) is also an error, so generate a new one here.
		if (generateNewClientAssertionSteps != null) {
			call(sequence(generateNewClientAssertionSteps));
		}

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-13");

		verifyError();

		eventLog.startBlock("Testing if access token was revoked after authorization code reuse (the AS 'should' have revoked the access token)");
		callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "RFC6749-4.1.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));

		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.2", "RFC6750-3.1");

		call(exec().unmapKey("endpoint_response"));

		eventLog.endBlock();

		fireTestFinished();
	}

	protected abstract void waitForAmountOfTime();

	protected void verifyError() {
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	@Override
	public void setupMTLS() {
		super.setupMTLS();
		generateNewClientAssertionSteps = null;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	@Override
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
		generateNewClientAssertionSteps = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
	}
}

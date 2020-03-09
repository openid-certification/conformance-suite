package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenExpectingError;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.WaitFor30Seconds;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "oidcc-codereuse-30seconds",
	displayName = "OIDCC: Authorization code reuse with a 30 second delay",
	summary = "This test tries using an authorization code for a second time, 30 seconds after the first use. The server must return an invalid_grant error as the authorization code has already been used. The originally issued access token should be revoked (as per RFC6749-4.1.2) - a warning is issued if the access token still works.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl"
	}
)
// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_OAuth_2nd_30s
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token", "id_token token"})
public class OIDCCAuthCodeReuseAfter30Seconds extends AbstractOIDCCServerTest {

	private Class<? extends ConditionSequence> generateNewClientAssertionSteps;

	@Override
	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJwt() {
		super.setupClientSecretJwt();
		generateNewClientAssertionSteps = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
	}

	@Override
	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
		generateNewClientAssertionSteps = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		testReuseOfAuthorizationCode();
		super.onPostAuthorizationFlowComplete();
	}

	private void testReuseOfAuthorizationCode() {
		callAndStopOnFailure(WaitFor30Seconds.class);

		eventLog.startBlock("Attempting reuse of authorisation code & testing if access token is revoked");

		if (generateNewClientAssertionSteps != null) {
			call(sequence(generateNewClientAssertionSteps));
		}

		callAndContinueOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		// https://github.com/rohe/oidctest/blob/41ef7a64fd8a24d8150077781dac93a11a0c5023/test_tool/cp/test_op/flows/OP-OAuth-2nd.json#L52 allowed other error codes,
		// and https://github.com/rohe/oidctest/blob/41ef7a64fd8a24d8150077781dac93a11a0c5023/test_tool/cp/test_op/flows/OP-OAuth-2nd-Revokes.json#L51 different ones again -
		// we go with the "what the RFC actually says" case.
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");

		// The AS 'SHOULD' have revoked the access token; try it again".
		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenExpectingError.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.2");
		eventLog.endBlock();
	}
}

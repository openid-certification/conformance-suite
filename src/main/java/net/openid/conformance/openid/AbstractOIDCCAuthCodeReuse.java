package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

@VariantNotApplicable(parameter = ResponseType.class, values={"id_token", "id_token token"})
public abstract class AbstractOIDCCAuthCodeReuse extends AbstractOIDCCServerTest {
	private Class<? extends ConditionSequence> generateNewClientAssertionSteps;

	@Override
	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJwt() {
		super.setupClientSecretJwt();
		generateNewClientAssertionSteps = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
	}

	@Override
	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
		generateNewClientAssertionSteps = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		testReuseOfAuthorizationCode();
		super.onPostAuthorizationFlowComplete();
	}

	protected void testReuseOfAuthorizationCode() {
		eventLog.startBlock("Attempting reuse of authorization code");

		if (generateNewClientAssertionSteps != null) {
			call(sequence(generateNewClientAssertionSteps));
		}

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE);
		checkResponse();
		eventLog.endBlock();
	}

	protected void checkResponse() {
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
	}
}

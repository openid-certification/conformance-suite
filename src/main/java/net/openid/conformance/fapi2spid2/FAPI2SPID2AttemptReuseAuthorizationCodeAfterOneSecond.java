package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.ServerAllowedReusingAuthorizationCode;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantSetup;
import org.apache.hc.core5.http.HttpStatus;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-attempt-reuse-authorization-code-after-one-second",
	displayName = "FAPI2-Security-Profile-ID2: try to reuse authorization code after one second",
	summary = "This test tries reusing an authorization code after one second, as the authorization code has already been used this must fail with the AS returning an invalid_grant error.\n\nAny issued access token 'should' also be revoked as per RFC6749 section 4.1.2 (although this is only recommended behaviour and this warning won't prevent you certifying) - see https://bitbucket.org/openid/fapi/issues/397/query-over-certification-test-for-access",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPI2SPID2AttemptReuseAuthorizationCodeAfterOneSecond extends AbstractFAPI2SPID2ServerTestModule {

	private Class<? extends ConditionSequence> generateNewClientAssertionSteps;

	protected void waitForAmountOfTime() {
		callAndStopOnFailure(WaitForOneSecond.class);
	}

	protected void verifyError() {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");
		if (httpStatus == HttpStatus.SC_OK) {
			callAndContinueOnFailure(ServerAllowedReusingAuthorizationCode.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.1.2-9");
		} else {
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		}
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {

		eventLog.startBlock("Attempting reuse of authorization code");

		waitForAmountOfTime();

		// We're testing that reuse of the _code_ is refused. Reusing the client assertion
		// (only present for private_key_jwt) is also an error, so generate a new one here.
		if (generateNewClientAssertionSteps != null) {
			call(sequence(generateNewClientAssertionSteps));
		}

		callSenderConstrainedTokenEndpointAndStopOnFailure( "FAPI2-SP-ID2-5.3.1.2-9");

		verifyError();

		eventLog.startBlock("Testing if access token was revoked after authorization code reuse (the AS 'should' have revoked the access token)");
		if(isDpop()) {
			updateResourceRequestAndCallProtectedResourceUsingDpop("RFC6749-4.1.2");
		} else {
			updateResourceRequest();
			callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "RFC6749-4.1.2");
		}
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));

		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.2", "RFC6750-3.1");

		call(exec().unmapKey("endpoint_response"));

		eventLog.endBlock();

		fireTestFinished();
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
		if(getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.CBUAE){
			generateNewClientAssertionSteps = CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest.class;
		} else {
			generateNewClientAssertionSteps = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
		}
	}
}

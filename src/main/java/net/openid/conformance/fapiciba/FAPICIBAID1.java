package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddRequestedExp300SToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateLongRandomClientNotificationToken;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.FAPICIBAAddAcrValuesToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.FAPICIBAValidateIdTokenACRClaims;
import net.openid.conformance.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetUtf8JsonAcceptHeadersForResourceEndpointRequest;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.common.CheckForBCP195InsecureFAPICiphers;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddPrivateKeyJWTClientAuthenticationToBackchannelRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi-ciba-id1",
	displayName = "FAPI-CIBA-ID1: Two client test",
	summary = "This test requires two different clients registered under the FAPI-CIBA profile. The test authenticates the user twice (using different variations on the authorization request etc), tests that certificate bound access tokens are implemented correctly. Do not respond to the request until the test enters the 'WAITING' state.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.scope",
		"client2.jwks",
		"client2.acr_value",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)

public class FAPICIBAID1 extends AbstractFAPICIBAID1MultipleClient {
	private Class<? extends ConditionSequence> generateNewClientAssertionSteps = null;

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	@Override
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
		setAddBackchannelClientAuthentication(() -> new AddPrivateKeyJWTClientAuthenticationToBackchannelRequest(isSecondClient(), false));
		generateNewClientAssertionSteps = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
	}

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		super.performProfileAuthorizationEndpointSetup();

		if (isSecondClient()) {
			skipIfElementMissing("server", "acr_values_supported",
				Condition.ConditionResult.INFO, FAPICIBAAddAcrValuesToAuthorizationEndpointRequest.class,
				Condition.ConditionResult.FAILURE, "CIBA-7.1");
		}

	}

	protected void checkAccountResourceEndpointTLS() {
		eventLog.startBlock("Accounts resource endpoint TLS test");
		env.mapKey("tls", "accounts_resource_endpoint_tls");
		checkEndpointTLS();
		env.unmapKey("tls");
		eventLog.endBlock();
	}

	protected void checkAccountRequestEndpointTLS() {
		eventLog.startBlock("Accounts request endpoint TLS test");
		env.mapKey("tls", "accounts_request_endpoint_tls");
		checkEndpointTLS();
		eventLog.endBlock();
	}

	protected void checkEndpointTLS() {
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-1");
		callAndContinueOnFailure(CheckForBCP195InsecureFAPICiphers.class, Condition.ConditionResult.WARNING, "FAPI1-ADV-8.5", "RFC9325A-A", "RFC9325-4.2");
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		if (testType == CIBAMode.PING && isSecondClient()) {
			callAndStopOnFailure(CreateLongRandomClientNotificationToken.class, "CIBA-7.1", "RFC6750-2.1");
			callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequest.class, "CIBA-7.1");
		} else {
			super.modeSpecificAuthorizationEndpointRequest();
		}
	}

	protected void verifyAccessTokenWithResourceEndpointDifferentAcceptHeader() {
		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-4");
		callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI-R-6.2.2-4");
		callAndStopOnFailure(SetUtf8JsonAcceptHeadersForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		callAndStopOnFailure(ClearAcceptHeaderForResourceEndpointRequest.class);
	}

	@Override
	protected void performPostAuthorizationFlow(boolean finishTest) {

		if (!isSecondClient()) {

			checkAccountRequestEndpointTLS();

			checkAccountResourceEndpointTLS();

			super.performPostAuthorizationFlow(false);

			verifyAccessTokenWithResourceEndpointDifferentAcceptHeader();

			switchToSecondClient();

			performAuthorizationFlow();

		} else {

			// check access token works
			requestProtectedResource();

			// Switch back to client 1
			eventLog.startBlock("Use client1's TLS cert with client2's access token (which should fail)");
			unmapClient();

			// Try client 2's access token with client 1's keys
			callAndStopOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE, "FAPIRW-5.2.2-5", "RFC8705-3");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.FAILURE, "RFC6749-4.1.2", "RFC6750-3.1", "RFC8705-3");
			call(exec().unmapKey("endpoint_response"));

			eventLog.endBlock();

			eventLog.startBlock("Attempting reuse of client2's auth_req_id (which should fail)");
			switchToSecondClient();

			if (generateNewClientAssertionSteps != null) {
				call(sequence(generateNewClientAssertionSteps));
			}

			callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class,  "CIBA-11");
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "CIBA-11");

			eventLog.endBlock();

			unmapClient();

			fireTestFinished();
		}
	}

	@Override
	protected void performProfileIdTokenValidation() {
		super.performProfileIdTokenValidation();

		if (isSecondClient()) {
			skipIfElementMissing("server", "acr_values_supported",
				Condition.ConditionResult.INFO, FAPICIBAValidateIdTokenACRClaims.class,
				Condition.ConditionResult.FAILURE, "CIBA-7.1", "FAPI-CIBA-5.2.2-8");
		}

	}

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();

		if (isSecondClient()) {
			// set a fairly standard requested expiry to verify server doesn't reject it
			callAndStopOnFailure(AddRequestedExp300SToAuthorizationEndpointRequest.class, "CIBA-11");
		}
	}
}

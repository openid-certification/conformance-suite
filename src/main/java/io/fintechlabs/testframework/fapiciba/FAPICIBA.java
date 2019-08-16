package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddRequestedExp300SToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import io.fintechlabs.testframework.condition.client.CallProtectedResourceWithBearerTokenExpectingError;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus400;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.ClearAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateLongRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.DisallowAccessTokenInQuery;
import io.fintechlabs.testframework.condition.client.FAPICIBAAddAcrValuesToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.FAPICIBAValidateIdTokenACRClaims;
import io.fintechlabs.testframework.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SetPlainJsonAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.sequence.client.AddPrivateKeyJWTClientAuthenticationToBackchannelRequest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba",
	displayName = "FAPI-CIBA: Two client test",
	summary = "This test requires two different clients registered under the FAPI-CIBA profile. The test authenticates the user twice (using different variations on the authorisation request etc), tests that certificate bound access tokens are implemented correctly. Do not respond to the request until the test enters the 'WAITING' state.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"client2.acr_value",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)

public class FAPICIBA extends AbstractFAPICIBA {

	@Variant(name = variant_ping_mtls)
	public void setupPingMTLS() {
		super.setupPingMTLS();
	}

	@Variant(name = variant_ping_privatekeyjwt)
	public void setupPingPrivateKeyJwt() {
		super.setupPingPrivateKeyJwt();
		setAddBackchannelClientAuthentication(() -> new AddPrivateKeyJWTClientAuthenticationToBackchannelRequest(isSecondClient(), false));
	}

	@Variant(name = variant_poll_mtls)
	public void setupPollMTLS() {
		super.setupPollMTLS();
	}

	@Variant(name = variant_poll_privatekeyjwt)
	public void setupPollPrivateKeyJwt() {
		super.setupPollPrivateKeyJwt();
		setAddBackchannelClientAuthentication(() -> new AddPrivateKeyJWTClientAuthenticationToBackchannelRequest(isSecondClient(), false));
	}

	@Variant(name = variant_openbankinguk_ping_mtls)
	public void setupOpenBankingUkPingMTLS() {
		super.setupOpenBankingUkPingMTLS();
	}

	@Variant(name = variant_openbankinguk_ping_privatekeyjwt)
	public void setupOpenBankingUkPingPrivateKeyJwt() {
		super.setupOpenBankingUkPingPrivateKeyJwt();
	}

	@Variant(name = variant_openbankinguk_poll_mtls)
	public void setupOpenBankingUkPollMTLS() {
		super.setupOpenBankingUkPollMTLS();
	}

	@Variant(name = variant_openbankinguk_poll_privatekeyjwt)
	public void setupOpenBankingUkPollPrivateKeyJwt() {
		super.setupOpenBankingUkPollPrivateKeyJwt();
	}

	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("client_public_jwks", "client_public_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
	}

	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("client_public_jwks");
		env.unmapKey("mutual_tls_authentication");
	}

	protected String currentClientString() {
		if (isSecondClient()) {
			return "Second client: ";
		}
		return "";
	}

	protected void performProfileAuthorizationEndpointSetup() {
		super.performProfileAuthorizationEndpointSetup();

		if (isSecondClient()) {
			skipIfElementMissing("server", "acr_values_supported",
				Condition.ConditionResult.INFO, FAPICIBAAddAcrValuesToAuthorizationEndpointRequest.class,
				Condition.ConditionResult.FAILURE, "CIBA-7.1");
		}

	}

	protected void verifyAccessTokenWithResourceEndpointDifferentAcceptHeader() {
		callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-4");

		callAndStopOnFailure(SetPlainJsonAcceptHeaderForResourceEndpointRequest.class);

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "RFC7231-5.3.2");

		callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);

		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");

		callAndStopOnFailure(ClearAcceptHeaderForResourceEndpointRequest.class);
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
		callAndContinueOnFailure(EnsureTLS12.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-1");
	}

	protected void modeSpecificAuthorizationEndpointRequest() {
		if (testType == TestType.PING && isSecondClient()) {
			callAndStopOnFailure(CreateLongRandomClientNotificationToken.class, "CIBA-7.1", "RFC6750-2.1");
			callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequest.class, "CIBA-7.1");
		} else {
			super.modeSpecificAuthorizationEndpointRequest();
		}
	}

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
			callAndContinueOnFailure(CallProtectedResourceWithBearerTokenExpectingError.class, Condition.ConditionResult.FAILURE, "OB-6.2.1-2");

			eventLog.endBlock();

			eventLog.startBlock("Attempting reuse of client2's auth_req_id (which should fail)");
			switchToSecondClient();

			callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class,  "CIBA-11");
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "CIBA-11");

			eventLog.endBlock();

			unmapClient();

			cleanUpPingTestResources();
			fireTestFinished();
		}
	}

	protected void performProfileIdTokenValidation() {
		super.performProfileIdTokenValidation();

		if (isSecondClient()) {
			skipIfElementMissing("server", "acr_values_supported",
				Condition.ConditionResult.INFO, FAPICIBAValidateIdTokenACRClaims.class,
				Condition.ConditionResult.FAILURE, "CIBA-7.1", "FAPI-CIBA-5.2.2-8");
		}

	}

	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();

		if (isSecondClient()) {
			// set a fairly standard requested expiry to verify server doesn't reject it
			callAndStopOnFailure(AddRequestedExp300SToAuthorizationEndpointRequest.class, "CIBA-11");
		}
	}

	@Override
	protected void configClient() {
		setupClient1();

		setupClient2();
	}

	@Override
	protected void cleanUpPingTestResources() {
		unregisterClient1();

		unregisterClient2();
	}
}

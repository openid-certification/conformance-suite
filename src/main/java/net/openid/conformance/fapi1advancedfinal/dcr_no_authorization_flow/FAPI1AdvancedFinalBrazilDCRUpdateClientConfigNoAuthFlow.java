package net.openid.conformance.fapi1advancedfinal.dcr_no_authorization_flow;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilDCRUpdateClientConfig;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-update-client-config-no-authorization-flow",
	displayName = "FAPI1-Advanced-Final: Brazil DCR update client config without authentication flow",
	summary = "\u2022 Obtains a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration)\n" +
		"\u2022 Registers a new client on the target authorization server.\n" +
		"\u2022 After the registration, a PUT will be made to the RFC7592 client to change the redirect uri (both redirect uris must be present in the software on the Brazil directory), which must succeed.\n" +
		"\u2022 The contents of the 'PUT' is the dynamic registration response the server supplied, so any problems with the PUT request are due to errors in the DCR response. PUTs to the client config url to change the redirect_uri with various bad authentication will then be tried, which should all be rejected. The test will then verify the redirect uri wasn't changed.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase"
	}
)
public class FAPI1AdvancedFinalBrazilDCRUpdateClientConfigNoAuthFlow extends FAPI1AdvancedFinalBrazilDCRUpdateClientConfig {

	private String originalRedirectUri;

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// get a new SSA (there is already one, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);

		// try negative tests changing redirect uri back to original
		String currentRedirectUri = env.getString("redirect_uri");
		env.putString("redirect_uri", this.originalRedirectUri);
		callAndStopOnFailure(AddRedirectUriToClientConfigurationRequest.class);

		eventLog.startBlock("Try to change redirect uri using bad MTLS certificate");
		callAndStopOnFailure(GenerateFakeMTLSCertificate.class);
		env.mapKey("mutual_tls_authentication", "fake_mutual_tls_authentication");
		updateClientConfigWithTlsIssue();
		env.unmapKey("mutual_tls_authentication");

		eventLog.startBlock("Try to change redirect uri using no MTLS certificate");
		env.mapKey("mutual_tls_authentication", "none_existent_key");
		updateClientConfigWithTlsIssue();
		env.unmapKey("mutual_tls_authentication");

		updateClientConfigWithBadAccessToken();

		updateClientConfigWithNoSsa();

		updateClientConfigWithInvalidSsa();

		// verify redirect uri and end test
		env.putString("redirect_uri", currentRedirectUri);

		eventLog.startBlock("Retrieve client configuration (twice)");

		getClientDetails();

		// a second call; if the client registration token was rotated this checks the new token works
		// (and also matches the requirements of the RP DCR test so we can run RP tests against OP tests)
		getClientDetails();

		eventLog.startBlock("Delete client");

		deleteClient();

		fireTestFinished();
	}

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

	@Override
	protected void callRegistrationEndpoint() {
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		eventLog.endBlock();

		eventLog.startBlock("Make PUT request to client configuration endpoint to change redirect uri");
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		// get a new SSA (technically there should be one in the DCR response, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);
		this.originalRedirectUri = env.getString("redirect_uri");
		callAndStopOnFailure(AddRedirectUriQuerySuffix.class, "RFC6749-3.1.2");
		callAndStopOnFailure(CreateRedirectUri.class, "RFC6749-3.1.2");
		callAndContinueOnFailure(FapiBrazilVerifyRedirectUriContainedInSoftwareStatement.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(AddRedirectUriToClientConfigurationRequest.class);

		callAndStopOnFailure(CallClientConfigurationEndpoint.class);
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
	}

}
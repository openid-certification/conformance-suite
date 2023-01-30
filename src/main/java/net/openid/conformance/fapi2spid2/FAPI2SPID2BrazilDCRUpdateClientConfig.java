package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRedirectUriQuerySuffix;
import net.openid.conformance.condition.client.AddRedirectUriToClientConfigurationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToClientConfigurationRequest;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CallClientConfigurationEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientConfigurationUriFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckNoClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRedirectUrisFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.CreateClientConfigurationRequestFromDynamicClientRegistrationResponse;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.FapiBrazilVerifyRedirectUriContainedInSoftwareStatement;
import net.openid.conformance.condition.client.GenerateFakeMTLSCertificate;
import net.openid.conformance.condition.client.InvalidateSoftwareStatementSignature;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-brazildcr-update-client-config",
	displayName = "FAPI2-Security-Profile-ID2: Brazil DCR update client config",
	summary = "Obtain a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration), register a new client on the target authorization server and perform an authorization flow. After the registration, a PUT will be made to the RFC7592 client to change the redirect uri (both redirect uris must be present in the software on the Brazil directory), which must succeed and an authorization flow will then be run using the new redirect uri. The contents of the 'PUT' is the dynamic registration response the server supplied, so any problems with the PUT request are due to errors in the DCR response. PUTs to the client config url to change the redirect_uri with various bad authentication will then be tried, which should all be rejected. The test will then verify the redirect uri wasn't changed.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI2SPID2BrazilDCRUpdateClientConfig extends AbstractFAPI2SPID2BrazilDCR {
	String originalRedirectUri;

	@Override
	protected void callRegistrationEndpoint() {
		super.callRegistrationEndpoint();

		eventLog.startBlock("Make PUT request to client configuration endpoint to change redirect uri");
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		// get a new SSA (technically there should be one in the DCR response, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);
		originalRedirectUri = env.getString("redirect_uri");
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

	@Override
	protected void onPostAuthorizationFlowComplete() {
		// get a new SSA (there is already one, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);

		// try negative tests changing redirect uri back to original
		String currentRedirectUri = env.getString("redirect_uri");
		env.putString("redirect_uri", originalRedirectUri);
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
		super.onPostAuthorizationFlowComplete();
	}

	protected void updateClientConfigWithTlsIssue() {
		callAndStopOnFailure(CallClientConfigurationEndpointAllowingTLSFailure.class);
		boolean sslError = env.getBoolean(CallClientConfigurationEndpointAllowingTLSFailure.RESPONSE_SSL_ERROR_KEY);
		if (sslError) {
			// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
			// is required, so there's no further checks to do
		} else {
			env.mapKey("endpoint_response", "registration_client_endpoint_response");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "RFC7592-2.1");
			callAndContinueOnFailure(CheckNoClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE);
		}
	}

	protected void updateClientConfigWithBadAccessToken() {
		String accessToken = env.getString("client", "registration_access_token");
		env.putString("client", "registration_access_token", "ivegotthisterriblepaininallthediodesdownmyleftside");

		eventLog.startBlock("Calling PUT on configuration endpoint with invalid access token");

		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		env.mapKey("endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "RFC7592-2.1");
		callAndContinueOnFailure(CheckNoClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		env.putString("client", "registration_access_token", accessToken);
	}

	protected void updateClientConfigWithNoSsa() {
		eventLog.startBlock("Calling PUT on configuration endpoint with no software statement assertion expecting failure");

		JsonObject originalRequestBody = env.getObject("registration_client_endpoint_request_body").deepCopy();

		JsonObject requestBody = env.getObject("registration_client_endpoint_request_body");
		requestBody.remove("software_statement");
		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		env.mapKey("endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2", "RFC7592-2.2");
		call(exec().unmapKey("endpoint_response"));

		env.putObject("registration_client_endpoint_request_body", originalRequestBody);
	}

	protected void updateClientConfigWithInvalidSsa() {
		eventLog.startBlock("Calling PUT on configuration endpoint with software statement assertion with bad signature expecting failure");

		JsonObject originalRequestBody = env.getObject("registration_client_endpoint_request_body").deepCopy();

		callAndStopOnFailure(InvalidateSoftwareStatementSignature.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);
		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		env.mapKey("endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2", "RFC7592-2.2");
		call(exec().unmapKey("endpoint_response"));

		env.putObject("registration_client_endpoint_request_body", originalRequestBody);
	}
}

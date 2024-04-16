package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAuthorizationCodeGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientCredentialsGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddJwksUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddMTLSEndpointAliasesToEnvironment;
import net.openid.conformance.condition.client.AddRedirectUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddRefreshTokenGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTlsClientAuthSubjectDnToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientConfigurationUriFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckRedirectUrisFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.CheckScopesFromDynamicRegistrationEndpointContainRequiredScopes;
import net.openid.conformance.condition.client.ClientManagementEndpointAndAccessTokenRequired;
import net.openid.conformance.condition.client.CopyOrgJwksFromDynamicRegistrationTemplateToClientConfiguration;
import net.openid.conformance.condition.client.CopyScopeFromDynamicRegistrationTemplateToClientConfiguration;
import net.openid.conformance.condition.client.CreateEmptyDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractClientNameFromStoredConfig;
import net.openid.conformance.condition.client.ExtractDirectoryConfiguration;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilExtractClientMTLSCertificateSubject;
import net.openid.conformance.condition.client.FAPIBrazilExtractJwksUriFromSoftwareStatement;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCheckDirectoryApiBase;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCheckDirectoryDiscoveryUrl;
import net.openid.conformance.condition.client.FapiBrazilVerifyRedirectUriContainedInSoftwareStatement;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.SetDirectorySoftwareScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetResponseTypeCodeInDynamicRegistrationRequest;
import net.openid.conformance.condition.client.StoreOriginalClientConfiguration;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.variant.ClientAuthType;

public abstract class AbstractFAPI2SPID2BrazilDCR extends AbstractFAPI2SPID2ServerTestModule {
	protected void getSsa() {

		eventLog.startBlock("Obtain access token for directory and retrieve a software statement");

		callAndStopOnFailure(ExtractDirectoryConfiguration.class);

		callAndContinueOnFailure(FAPIBrazilOpenBankingCheckDirectoryDiscoveryUrl.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-6.1");

		callAndContinueOnFailure(FAPIBrazilOpenBankingCheckDirectoryApiBase.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-6.1");

		env.mapKey("config", "directory_config");
		env.mapKey("server", "directory_server");
		env.mapKey("client", "directory_client");
		env.mapKey("access_token", "directory_access_token");
		env.mapKey("discovery_endpoint_response", "directory_discovery_endpoint_response");
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		// this overwrites the non-directory values; we will have to replace them below
		callAndContinueOnFailure(AddMTLSEndpointAliasesToEnvironment.class, Condition.ConditionResult.FAILURE, "RFC8705-5");

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

		callAndStopOnFailure(SetDirectorySoftwareScopeOnTokenEndpointRequest.class);

		// MTLS client auth
		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

		callSenderConstrainedTokenEndpointAndStopOnFailure(false);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		env.unmapKey("server");
		env.unmapKey("client");
		env.unmapKey("discovery_endpoint_response");
		env.unmapKey("config");
		env.unmapKey("access_token");

		// restore MTLS aliases to the values for the server being tested
		callAndContinueOnFailure(AddMTLSEndpointAliasesToEnvironment.class, Condition.ConditionResult.FAILURE, "RFC8705-5");

		callAndStopOnFailure(FAPIBrazilExtractClientMTLSCertificateSubject.class);

		// use access token to get ssa
		// https://matls-api.sandbox.directory.openbankingbrasil.org.br/organisations/${ORGID}/softwarestatements/${SSID}/assertion
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);

		eventLog.endBlock();
	}

	@Override
	protected void configureClient() {
		ClientAuthType clientAuthType = getVariant(ClientAuthType.class);
		String clientAuth = clientAuthType.toString();
		if (clientAuthType == ClientAuthType.MTLS) {
			// the FAPI auth type variant doesn't use the name required for the registration request as per
			// https://datatracker.ietf.org/doc/html/rfc8705#section-2.1.1
			clientAuth = "tls_client_auth";
		}
		env.putString("client_auth_type", clientAuth);

		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);

		// normally our DCR tests create a key on the fly to use, but in this case the key has to be registered
		// manually with the central directory so we must use user supplied keys
		callAndStopOnFailure(ExtractJWKSDirectFromClientConfiguration.class);

		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");

		getSsa();

		eventLog.startBlock("Perform Dynamic Client Registration");

		callAndStopOnFailure(StoreOriginalClientConfiguration.class);
		callAndStopOnFailure(ExtractClientNameFromStoredConfig.class);

		setupJwksUri();

		// create basic dynamic registration request
		callAndStopOnFailure(CreateEmptyDynamicRegistrationRequest.class);

		callAndStopOnFailure(AddAuthorizationCodeGrantTypeToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddRefreshTokenGrantTypeToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddClientCredentialsGrantTypeToDynamicRegistrationRequest.class);

		if (clientAuthType == ClientAuthType.MTLS) {
			callAndStopOnFailure(AddTlsClientAuthSubjectDnToDynamicRegistrationRequest.class);
		}

		addJwksToRequest();
		callAndStopOnFailure(AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment.class);
		callAndStopOnFailure(SetResponseTypeCodeInDynamicRegistrationRequest.class);
		validateSsa();
		callAndStopOnFailure(AddRedirectUriToDynamicRegistrationRequest.class);

		addSoftwareStatementToRegistrationRequest();

		callRegistrationEndpoint();
	}

	protected void addJwksToRequest() {
		callAndStopOnFailure(AddJwksUriToDynamicRegistrationRequest.class, "RFC7591-2", "BrazilOBDCR-7.1-5");
	}

	protected void setupJwksUri() {
		// the jwks is hosted on the directory, we must use the url in the software statement
		callAndStopOnFailure(FAPIBrazilExtractJwksUriFromSoftwareStatement.class, "BrazilOBDCR-7.1-5");
	}

	protected void validateSsa() {
		callAndContinueOnFailure(FapiBrazilVerifyRedirectUriContainedInSoftwareStatement.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-6");
	}

	protected void addSoftwareStatementToRegistrationRequest() {
		callAndStopOnFailure(AddSoftwareStatementToDynamicRegistrationRequest.class);
	}

	protected void callRegistrationEndpoint() {
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));

		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");

		callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointContainRequiredScopes.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1.1", "RFC7591-2", "RFC7591-3.2.1");

		// The tests expect scope to be part of the 'client' object, but it may not be in the dcr response so copy across
		callAndStopOnFailure(CopyScopeFromDynamicRegistrationTemplateToClientConfiguration.class);
		callAndStopOnFailure(CopyOrgJwksFromDynamicRegistrationTemplateToClientConfiguration.class);

		eventLog.endBlock();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Retrieve client configuration (twice)");

		getClientDetails();

		// a second call; if the client registration token was rotated this checks the new token works
		// (and also matches the requirements of the RP DCR test so we can run RP tests against OP tests)
		getClientDetails();

		eventLog.startBlock("Delete client");

		deleteClient();

		super.onPostAuthorizationFlowComplete();
	}

	protected void getClientDetails() {
		env.removeObject("registration_client_endpoint_request_body"); // so a 'GET' is made
		callAndStopOnFailure(CallClientConfigurationEndpoint.class, "OIDCD-4.2");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
	}

	protected void deleteClient() {
		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClient.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-9.3.2-4.", "RFC7592-2.3");
		// when we just deregistered the client, so prevent cleanup from trying to do so again
		JsonObject client = env.getObject("client");
		client.remove("registration_client_uri");
		client.remove("registration_access_token");
	}

	public void unregisterClient1() {
		eventLog.startBlock("Unregister dynamically registered client");

		call(condition(UnregisterDynamicallyRegisteredClient.class)
			.skipIfObjectsMissing(new String[] {"client"})
			.onSkip(Condition.ConditionResult.INFO)
			.onFail(Condition.ConditionResult.WARNING)
			.dontStopOnFailure());

		eventLog.endBlock();
	}

	@Override
	public void cleanup() {
		unregisterClient1();
	}
}

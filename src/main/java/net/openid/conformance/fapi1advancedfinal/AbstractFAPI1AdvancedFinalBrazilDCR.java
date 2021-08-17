package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAuthorizationCodeGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientCredentialsGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddImplicitGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddJwksUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddMTLSEndpointAliasesToEnvironment;
import net.openid.conformance.condition.client.AddRedirectUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddRefreshTokenGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTlsClientAuthSubjectDnToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CopyScopeFromDynamicRegistrationTemplateToClientConfiguration;
import net.openid.conformance.condition.client.CreateEmptyDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractClientNameFromStoredConfig;
import net.openid.conformance.condition.client.ExtractDirectoryConfiguration;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilCheckDirectoryApiBase;
import net.openid.conformance.condition.client.FAPIBrazilCheckDirectoryDiscoveryUrl;
import net.openid.conformance.condition.client.FAPIBrazilExtractClientMTLSCertificateSubject;
import net.openid.conformance.condition.client.FAPIBrazilExtractJwksUriFromSoftwareStatement;
import net.openid.conformance.condition.client.FapiBrazilVerifyRedirectUriContainedInSoftwareStatement;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.SetDirectorySoftwareScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetResponseTypeCodeIdTokenInDynamicRegistrationRequest;
import net.openid.conformance.condition.client.SetResponseTypeCodeInDynamicRegistrationRequest;
import net.openid.conformance.condition.client.StoreOriginalClientConfiguration;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.variant.ClientAuthType;

public abstract class AbstractFAPI1AdvancedFinalBrazilDCR extends AbstractFAPI1AdvancedFinalServerTestModule {
	protected void getSsa() {

		eventLog.startBlock("Obtain access token for directory and retrieve a software statement");

		callAndStopOnFailure(ExtractDirectoryConfiguration.class);

		callAndContinueOnFailure(FAPIBrazilCheckDirectoryDiscoveryUrl.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-1");

		callAndContinueOnFailure(FAPIBrazilCheckDirectoryApiBase.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-1");

		env.mapKey("config", "directory_config");
		env.mapKey("server", "directory_server");
		env.mapKey("client", "directory_client");
		env.mapKey("discovery_endpoint_response", "directory_discovery_endpoint_response");
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		// this overwrites the non-directory values; we will have to replace them below
		callAndContinueOnFailure(AddMTLSEndpointAliasesToEnvironment.class, Condition.ConditionResult.FAILURE, "RFC8705-5");

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

		callAndStopOnFailure(SetDirectorySoftwareScopeOnTokenEndpointRequest.class);

		// MTLS client auth
		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		// map access token too?
		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		env.unmapKey("server");
		env.unmapKey("client");
		env.unmapKey("discovery_endpoint_response");
		env.unmapKey("config");

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

		// the jwks is hosted on the directory, we must use the url in the software statement
		callAndStopOnFailure(FAPIBrazilExtractJwksUriFromSoftwareStatement.class, "BrazilOBDCR-7.1-5");

		// create basic dynamic registration request
		callAndStopOnFailure(CreateEmptyDynamicRegistrationRequest.class);

		callAndStopOnFailure(AddAuthorizationCodeGrantTypeToDynamicRegistrationRequest.class);
		if (!jarm) {
			// implicit is only required when id_token is returned in frontchannel
			callAndStopOnFailure(AddImplicitGrantTypeToDynamicRegistrationRequest.class);
		}
		callAndStopOnFailure(AddRefreshTokenGrantTypeToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddClientCredentialsGrantTypeToDynamicRegistrationRequest.class);

		if (clientAuthType == ClientAuthType.MTLS) {
			callAndStopOnFailure(AddTlsClientAuthSubjectDnToDynamicRegistrationRequest.class);
		}

		callAndStopOnFailure(AddJwksUriToDynamicRegistrationRequest.class, "RFC7591-2", "BrazilOBDCR-7.1-5");
		callAndStopOnFailure(AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment.class);
		if (jarm) {
			callAndStopOnFailure(SetResponseTypeCodeInDynamicRegistrationRequest.class);
		} else {
			callAndStopOnFailure(SetResponseTypeCodeIdTokenInDynamicRegistrationRequest.class);
		}
		validateSsa();
		callAndStopOnFailure(AddRedirectUriToDynamicRegistrationRequest.class);

		callAndStopOnFailure(AddSoftwareStatementToDynamicRegistrationRequest.class);

		callRegistrationEndpoint();
	}

	protected void validateSsa() {
		callAndContinueOnFailure(FapiBrazilVerifyRedirectUriContainedInSoftwareStatement.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-6");
	}

	protected void callRegistrationEndpoint() {
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));

		// The tests expect scope to be part of the 'client' object, but it may not be in the dcr response so copy across
		callAndStopOnFailure(CopyScopeFromDynamicRegistrationTemplateToClientConfiguration.class);

		eventLog.endBlock();
	}

	public void unregisterClient1() {
		eventLog.startBlock("Unregister dynamically registered client");

		// IF management interface, delete the client to clean up
		skipIfMissing(new String[]{"client"},
			new String[]{"registration_client_uri", "registration_access_token"},
			Condition.ConditionResult.INFO,
			UnregisterDynamicallyRegisteredClient.class);

		eventLog.endBlock();
	}

	@Override
	public void cleanup() {
		unregisterClient1();
	}
}

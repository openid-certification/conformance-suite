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
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CopyScopeFromDynamicRegistrationTemplateToClientConfiguration;
import net.openid.conformance.condition.client.CreateEmptyDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractClientMTLSCertificateSubject;
import net.openid.conformance.condition.client.ExtractClientNameFromStoredConfig;
import net.openid.conformance.condition.client.ExtractDirectoryConfiguration;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilExtractJwksUriFromSoftwareStatement;
import net.openid.conformance.condition.client.FapiBrazilVerifyRedirectUriContainedInSoftwareStatement;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.SetDirectorySoftwareScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetResponseTypeCodeIdTokenInDynamicRegistrationRequest;
import net.openid.conformance.condition.client.StoreOriginalClientConfiguration;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-happy-flow",
	displayName = "FAPI1-Advanced-Final: Brazil DCR happy flow",
	summary = "Obtain a software statement from the directory, register a new client and perform an authorization flow.",
	profile = "FAPI1-Advanced-Final",
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
public class FAPI1AdvancedFinalBrazilDCRHappyFlow extends AbstractFAPI1AdvancedFinalServerTestModule {

	protected void getSsa() {

		eventLog.startBlock("Obtain access token for directory and retrieve a software statement");

		callAndStopOnFailure(ExtractDirectoryConfiguration.class);

		env.mapKey("config", "directory_config");
		env.mapKey("server", "directory_server");
		env.mapKey("client", "directory_client");
		env.mapKey("discovery_endpoint_response", "directory_discovery_endpoint_response");
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		// this overwrites the non-directory values; we will have to replace them below
		callAndStopOnFailure(AddMTLSEndpointAliasesToEnvironment.class);

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
		callAndStopOnFailure(AddMTLSEndpointAliasesToEnvironment.class);

		callAndStopOnFailure(ExtractClientMTLSCertificateSubject.class);

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
		callAndStopOnFailure(AddImplicitGrantTypeToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddRefreshTokenGrantTypeToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddClientCredentialsGrantTypeToDynamicRegistrationRequest.class);

		if (clientAuthType == ClientAuthType.MTLS) {
			callAndStopOnFailure(AddTlsClientAuthSubjectDnToDynamicRegistrationRequest.class);
		}

		callAndStopOnFailure(AddJwksUriToDynamicRegistrationRequest.class, "RFC7591-2", "BrazilOBDCR-7.1-5");
		callAndStopOnFailure(AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment.class);
		callAndStopOnFailure(SetResponseTypeCodeIdTokenInDynamicRegistrationRequest.class);
		callAndContinueOnFailure(FapiBrazilVerifyRedirectUriContainedInSoftwareStatement.class,"BrazilOBDCR-7.1-6");
		callAndStopOnFailure(AddRedirectUriToDynamicRegistrationRequest.class);

		callAndStopOnFailure(AddSoftwareStatementToDynamicRegistrationRequest.class);

		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		// The tests expect scope to be part of the 'client' object, but it may not be in the dcr response so copy across
		callAndStopOnFailure(CopyScopeFromDynamicRegistrationTemplateToClientConfiguration.class);

		eventLog.endBlock();
	}
}

package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientIdToRequest;
import net.openid.conformance.condition.client.AddMTLSEndpointAliasesToEnvironment;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractDirectoryConfiguration;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClient2Configuration;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilExtractClientMTLSCertificateSubject;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCheckDirectoryApiBase;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCheckDirectoryDiscoveryUrl;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.SetDirectorySoftwareScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.ValidateMTLSCertificates2Header;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OpenBankingBrazilDynamicClientRegistrationCredentialSetup extends AbstractConditionSequence {

	private final boolean secondClient;

	public OpenBankingBrazilDynamicClientRegistrationCredentialSetup(boolean secondClient) {
		this.secondClient = secondClient;
	}

	@Override
	public void evaluate() {
		if (secondClient) {
			callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(ExtractMTLSCertificates2FromConfiguration.class);
			callAndStopOnFailure(ExtractJWKSDirectFromClient2Configuration.class);
		} else {
			callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);
			callAndStopOnFailure(ExtractJWKSDirectFromClientConfiguration.class);
		}
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class,
			Condition.ConditionResult.FAILURE, "RFC7517-4.5");

		callAndStopOnFailure(ExtractDirectoryConfiguration.class);
		callAndContinueOnFailure(FAPIBrazilOpenBankingCheckDirectoryDiscoveryUrl.class,
			Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-1");
		callAndContinueOnFailure(FAPIBrazilOpenBankingCheckDirectoryApiBase.class,
			Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-1");

		call(exec()
			.mapKey("config", "directory_config")
			.mapKey("server", "directory_server")
			.mapKey("client", "directory_client")
			.mapKey("access_token", "directory_access_token")
			.mapKey("discovery_endpoint_response", "directory_discovery_endpoint_response"));
		callAndStopOnFailure(GetDynamicServerConfiguration.class);
		callAndContinueOnFailure(AddMTLSEndpointAliasesToEnvironment.class,
			Condition.ConditionResult.FAILURE, "RFC8705-5");
		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetDirectorySoftwareScopeOnTokenEndpointRequest.class);
		call(exec()
			.mapKey("request_form_parameters", "token_endpoint_request_form_parameters")
			.mapKey("request_headers", "token_endpoint_request_headers"));
		callAndStopOnFailure(AddClientIdToRequest.class);
		call(exec()
			.unmapKey("request_form_parameters")
			.unmapKey("request_headers"));
		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndStopOnFailure(CheckTokenEndpointHttpStatus200.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckForAccessTokenValue.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
		call(exec()
			.unmapKey("server")
			.unmapKey("client")
			.unmapKey("discovery_endpoint_response")
			.unmapKey("config")
			.unmapKey("access_token"));

		callAndContinueOnFailure(AddMTLSEndpointAliasesToEnvironment.class,
			Condition.ConditionResult.FAILURE, "RFC8705-5");
		callAndStopOnFailure(FAPIBrazilExtractClientMTLSCertificateSubject.class);
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
	}
}

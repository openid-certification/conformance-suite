package net.openid.conformance.openbanking_brasil;

import net.openid.conformance.condition.client.*;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;

public class ObtainAccessTokenWithClientCredentials extends AbstractConditionSequence {
	@Override
	public void evaluate() {
		callAndStopOnFailure(GetDynamicServerConfiguration.class);
		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);
		callAndStopOnFailure(AddMTLSEndpointAliasesToEnvironment.class);
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
		call(sequence(CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class));
		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
	}
}

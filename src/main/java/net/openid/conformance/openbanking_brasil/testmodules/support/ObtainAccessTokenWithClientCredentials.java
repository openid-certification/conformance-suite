package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.client.*;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

public class ObtainAccessTokenWithClientCredentials extends AbstractConditionSequence {

	private Class<? extends ConditionSequence> clientAuthSequence;

	public ObtainAccessTokenWithClientCredentials(Class<? extends ConditionSequence> clientAuthSequence) {
		this.clientAuthSequence = clientAuthSequence;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(GetDynamicServerConfiguration.class);
		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);
		callAndStopOnFailure(AddMTLSEndpointAliasesToEnvironment.class);
		callAndStopOnFailure(GetStaticClientConfiguration.class);
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
		call(sequence(clientAuthSequence));
		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
	}

}

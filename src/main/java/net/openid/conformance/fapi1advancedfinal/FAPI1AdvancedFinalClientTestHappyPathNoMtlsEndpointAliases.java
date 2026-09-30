package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.RemoveMtlsEndpointAliasesFromServerConfiguration;
import net.openid.conformance.condition.as.SetParEndpointToMtlsParEndpoint;
import net.openid.conformance.condition.as.SetTokenEndpointToMtlsTokenEndpoint;
import net.openid.conformance.condition.as.SetUserinfoEndpointToMtlsUserinfoEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPIAuthRequestMethod;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-happy-path-no-mtls-endpoint-aliases",
	displayName = "FAPI1-Advanced-Final: client test for happy path without mtls_endpoint_aliases",
	summary = "Tests a 'happy path' flow where the server does not publish mtls_endpoint_aliases; the client is expected to determine from its own configuration (client authentication type, profile) which literal endpoint URL to call for each of token/PAR/userinfo, since no alias is available to consult.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)
public class FAPI1AdvancedFinalClientTestHappyPathNoMtlsEndpointAliases extends FAPI1AdvancedFinalClientTest {

	@Override
	protected void adjustServerConfigurationForMtlsEndpointAliasesVariant() {
		// The token endpoint is always mTLS-bound in FAPI1 Advanced regardless of client
		// authentication type - see the unconditional checkMtlsCertificate() call in tokenEndpoint().
		callAndStopOnFailure(SetTokenEndpointToMtlsTokenEndpoint.class, "RFC8705-5");

		// PAR is only mTLS-bound for MTLS client authentication, or in the always-mTLS Brazil/KSA
		// ecosystems - see the rejections in handleClientRequestForPath's "par" case.
		if (authRequestMethod == FAPIAuthRequestMethod.PUSHED
			&& (clientAuthType == ClientAuthType.MTLS || isBrazil() || isKSA())) {
			callAndStopOnFailure(SetParEndpointToMtlsParEndpoint.class, "RFC8705-5");
		}

		// The userinfo endpoint is always mTLS-bound, like the token endpoint above - see the
		// unconditional rejection in handleClientRequestForPath's "userinfo" case.
		callAndStopOnFailure(SetUserinfoEndpointToMtlsUserinfoEndpoint.class, "RFC8705-5");

		callAndStopOnFailure(RemoveMtlsEndpointAliasesFromServerConfiguration.class, "RFC8705-5");
	}
}

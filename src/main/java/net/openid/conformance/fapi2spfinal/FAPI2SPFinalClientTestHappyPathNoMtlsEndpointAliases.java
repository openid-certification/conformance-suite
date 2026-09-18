package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.RemoveMtlsEndpointAliasesFromServerConfiguration;
import net.openid.conformance.condition.as.SetParEndpointToMtlsParEndpoint;
import net.openid.conformance.condition.as.SetTokenEndpointToMtlsTokenEndpoint;
import net.openid.conformance.condition.as.SetUserinfoEndpointToMtlsUserinfoEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-happy-path-no-mtls-endpoint-aliases",
	displayName = "FAPI2-Security-Profile-Final: client test for happy path without mtls_endpoint_aliases",
	summary = "Tests a 'happy path' flow where the server does not publish mtls_endpoint_aliases; the client is expected to determine from its own configuration (client authentication type, sender-constraining method) which literal endpoint URL to call for each of token/userinfo/PAR, since no alias is available to consult.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)
public class FAPI2SPFinalClientTestHappyPathNoMtlsEndpointAliases extends FAPI2SPFinalClientTestHappyPath {

	@Override
	protected void adjustServerConfigurationForMtlsEndpointAliasesVariant() {
		if (tokenEndpointRequiresMtls()) {
			callAndStopOnFailure(SetTokenEndpointToMtlsTokenEndpoint.class, "RFC8705-5");
		}
		if (userinfoEndpointRequiresMtls()) {
			callAndStopOnFailure(SetUserinfoEndpointToMtlsUserinfoEndpoint.class, "RFC8705-5");
		}
		if (profileBehavior.shouldRegisterPAREndpoint() && parEndpointRequiresMtls()) {
			callAndStopOnFailure(SetParEndpointToMtlsParEndpoint.class, "RFC8705-5");
		}
		callAndStopOnFailure(RemoveMtlsEndpointAliasesFromServerConfiguration.class, "RFC8705-5");
	}
}

package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.RemoveMtlsEndpointAliasesFromServerConfiguration;
import net.openid.conformance.condition.as.SetBackchannelAuthenticationEndpointToMtlsBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.as.SetTokenEndpointToMtlsTokenEndpoint;
import net.openid.conformance.condition.as.SetUserinfoEndpointToMtlsUserinfoEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-test-happy-path-no-mtls-endpoint-aliases",
	displayName = "FAPI-CIBA-ID1: client test for happy path without mtls_endpoint_aliases",
	summary = "Tests a 'happy path' flow where the server does not publish mtls_endpoint_aliases; the client is expected to determine from its own configuration (client authentication type) which literal endpoint URL to call for each of token/backchannel/userinfo, since no alias is available to consult.",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAClientTestHappyPathNoMtlsEndpointAliases extends FAPICIBAClientTest {

	@Override
	protected void adjustServerConfigurationForMtlsEndpointAliasesVariant() {
		// The token endpoint is always fetched over mTLS in CIBA, regardless of client
		// authentication type - see the unconditional rejection in handleHttp's "token" case.
		callAndStopOnFailure(SetTokenEndpointToMtlsTokenEndpoint.class, "RFC8705-5");
		if (clientAuthType == ClientAuthType.MTLS || profileBehavior.requiresMtlsForBackchannelEndpoint()) {
			callAndStopOnFailure(SetBackchannelAuthenticationEndpointToMtlsBackchannelAuthenticationEndpoint.class, "RFC8705-5");
		}
		if (profileBehavior.userInfoEndpointRequiresMTLS()) {
			callAndStopOnFailure(SetUserinfoEndpointToMtlsUserinfoEndpoint.class, "RFC8705-5");
		}
		callAndStopOnFailure(RemoveMtlsEndpointAliasesFromServerConfiguration.class, "RFC8705-5");
	}
}

package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-test",
	displayName = "FAPI-CIBA-ID1: Client test",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, " +
		"call the backchannel endpoint and either poll the token endpoint or wait to get pinged " +
		"and then use the access token from the token endpoint response in request to the resource endpoint " +
		"(typically accounts or userinfo) displayed below",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAClientTest extends AbstractFAPICIBAClientTest {

}

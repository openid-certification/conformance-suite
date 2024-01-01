package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-test",
	displayName = "FAPI-CIBA-ID1: Client test",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, " +
		"call the backchannel endpoint and either poll the token endpoint or wait to get pinged " +
		"and then use the access token from the token endpoint response in a resource endpoint request.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.backchannel_client_notification_endpoint",
		"client.certificate",
		"client.jwks"
	}
)
public class FAPICIBAClientTest extends AbstractFAPICIBAClientTest {

}

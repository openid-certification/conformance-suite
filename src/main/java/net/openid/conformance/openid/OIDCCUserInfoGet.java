package net.openid.conformance.openid;

import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-UserInfo-Endpoint
@PublishTestModule(
	testName = "oidcc-userinfo-get",
	displayName = "OIDCC: make GET request to UserInfo endpoint",
	summary = "This tests makes an authenticated GET request to the UserInfo endpoint and validates the response",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client2.scope",
		"resource.resourceUrl"
	}
)
public class OIDCCUserInfoGet extends AbstractOIDCCUserInfoTest {

}

package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.PublishTestModule;

/**
 * the default happy path test
 */
@PublishTestModule(
	testName = "oidcc-client-test",
	displayName = "OIDCC: Relying party test, success case",
	summary = "The client is expected to make an authentication request " +
		"(also a token request and a userinfo request where applicable)" +
		"using the selected response_type and other configuration options. ",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTest extends AbstractOIDCCClientTest {
	//do nothing
}

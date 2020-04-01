package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * the default happy path test for refresh tokens
 */
@PublishTestModule(
	testName = "oidcc-client-test-refresh-token",
	displayName = "OIDCC: Relying party refresh token test",
	summary = "For this test, the client is expected to obtain a refresh_token, then use the refresh_token to" +
		" obtain a new access_token and call the userinfo endpoint using the newly obtained access_token." +
		" This is a new test with no corresponding test in the old conformance suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token", "id_token token"})
public class OIDCCClientTestRefreshToken extends AbstractOIDCCClientTestRefreshToken {
	//same as super class
}

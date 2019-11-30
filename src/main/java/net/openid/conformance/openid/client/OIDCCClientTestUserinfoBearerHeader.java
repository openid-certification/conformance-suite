package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-userinfo-bearer-header",
	displayName = "OIDCC: Relying party test, pass the access token using Bearer authentication scheme",
	summary = "The client is expected to Pass the access token using the 'Bearer' authentication scheme while doing the UserInfo Request." +
		" Corresponds to rp-userinfo-bearer-header test in the old suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token"})
public class OIDCCClientTestUserinfoBearerHeader extends AbstractOIDCCClientTest {
	/*
	This test might seem unnecessary but there is a matching test in the old suite
	and we need to have a matching test in the new one
	until Mike and/or others agree that this can be removed
	*/
}

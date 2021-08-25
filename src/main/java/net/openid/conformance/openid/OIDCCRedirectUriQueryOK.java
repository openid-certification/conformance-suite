package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddQueryToRedirectUri;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_redirect_uri_Query_OK
@PublishTestModule(
	testName = "oidcc-redirect-uri-query-OK",
	displayName = "OIDCC: request with a redirect_uri with a query component when a redirect_uri with the same query component is registered",
	summary = "This test uses a redirect uri with a query component. Authorization should complete successfully.",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = {"static_client"})
public class OIDCCRedirectUriQueryOK extends AbstractOIDCCServerTest {

	@Override
	protected void configureDynamicClient() {

		callAndStopOnFailure(AddQueryToRedirectUri.class);
		exposeEnvString("redirect_uri");

		super.configureDynamicClient();
	}
}

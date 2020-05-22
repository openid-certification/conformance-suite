package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.AddMtlsEndpointAliasesToServerConfiguration;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.OIDCCClientAuthType;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-mtls-endpoint-aliases",
	displayName = "OIDCC: Relying party test, test mtls_endpoint_aliases support",
	summary = "The client is expected to make an authentication request " +
		"and a token request using the correct mtls enabled token endpoint obtained " +
		"from mtls_endpoint_aliases. A userinfo request is also required for the test to end.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = OIDCCClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt", "private_key_jwt"
})
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token", "id_token token"})
public class OIDCCClientTestMtlsEndpointAliases extends AbstractOIDCCClientTest {

	@Override
	protected void adjustTokenEndpointInServerConfigurationIfUsingMtls() {
		//we don't want to change the token endpoint but we just want to add mtls_endpoint_aliases
		callAndStopOnFailure(AddMtlsEndpointAliasesToServerConfiguration.class, "RFC8705-5");
	}
}

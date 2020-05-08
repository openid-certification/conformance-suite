package net.openid.conformance.openid;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantConfigurationFields;

import static net.openid.conformance.variant.ClientRegistration.STATIC_CLIENT;

@PublishTestModule(
	testName = "oidcc-server-client-secret-post",
	displayName = "OIDCC",
	summary = "Tests 'happy flow' using client_secret_post client authentication",
	profile = "OIDCC"
)
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	// as the basic etc certification profiles run tests with different client authentication types, we need to
	// allow the user to provide multiple clients if using static clients (as many/most servers restrict each
	// clients to using only one authentication method)
	"client_secret_post.client_id",
	"client_secret_post.client_secret"
})
public class OIDCCServerTestClientSecretPost extends AbstractOIDCCServerTest {

	@Override
	protected void configureClient() {
		if (getVariant(ClientRegistration.class) == STATIC_CLIENT) {
			// copy the client_secret_post supporting client into the place the normal conditions expect to find it
			var config = env.getObject("config");
			config.add("client", config.get("client_secret_post"));
		}

		super.configureClient();
	}
}

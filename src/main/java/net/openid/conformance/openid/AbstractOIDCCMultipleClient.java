package net.openid.conformance.openid;

import net.openid.conformance.condition.client.ExtractClientNameFromStoredConfig;
import net.openid.conformance.condition.client.ExtractInitialAccessTokenFromStoredConfig;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.StoreOriginalClient2Configuration;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_basic", configurationFields = {
	"client2.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_post", configurationFields = {
	"client2.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_jwt", configurationFields = {
	"client2.client_secret",
	"client2.client_secret_jwt_alg"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "private_key_jwt", configurationFields = {
	"client2.jwks"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "mtls", configurationFields = {
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	"client2.client_id"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client2.client_name",
	"client2.initial_access_token"
})
public abstract class AbstractOIDCCMultipleClient extends AbstractOIDCCServerTest {

	@Override
	protected void configureClient() {
		super.configureClient();

		switchToSecondClient();
		switch (getVariant(ClientRegistration.class)) {
		case STATIC_CLIENT:
			callAndStopOnFailure(GetStaticClient2Configuration.class);
			configureStaticClient();
			break;
		case DYNAMIC_CLIENT:
			callAndStopOnFailure(StoreOriginalClient2Configuration.class);
			callAndStopOnFailure(ExtractClientNameFromStoredConfig.class);
			callAndStopOnFailure(ExtractInitialAccessTokenFromStoredConfig.class);
			configureDynamicClient();
			break;
		}

		completeClientConfiguration();
		unmapClient();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!isSecondClient()) {
			switchToSecondClient();
			performAuthorizationFlow();
		} else {
			performSecondClientTests();
			super.onPostAuthorizationFlowComplete();
		}
	}

	/**
	 * These are additional tests involving the second client,
	 * performed after the usual post-authorization flow.
	 */
	protected abstract void performSecondClientTests();

	@Override
	public void cleanup() {
		unmapClient();
		super.cleanup();
		switchToSecondClient();
		unregisterClient();
	}

	@Override
	protected String currentClientString() {
		if (isSecondClient()) {
			return "Second client: ";
		} else {
			return "";
		}
	}

	@Override
	protected boolean isSecondClient() {
		return env.isKeyMapped("client");
	}

	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("client_public_jwks", "client_public_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
	}

	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("client_public_jwks");
		env.unmapKey("mutual_tls_authentication");
	}
}

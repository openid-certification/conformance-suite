package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

/**
 * applicable only when response type includes code
 */
@PublishTestModule(
	testName = "oidcc-client-test-client-secret-basic",
	displayName = "OIDCC: Relying party test using client_secret_basic",
	summary = "The client MUST use client_secret_basic authentication method " +
		"regardless of selected client authentication type in test configuration." +
		"Corresponds to rp-token_endpoint-client_secret_basic in the old suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values = {"id_token token", "id_token"})
public class OIDCCClientTestClientSecretBasic extends AbstractOIDCCClientTest {

	@Override
	@VariantSetup(parameter = ClientAuthType.class, value = "none")
	public void setupClientAuthNone() {
		setupClientSecretBasic();
	}

	@Override
	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		setupClientSecretBasic();
	}

	@Override
	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJWT() {
		setupClientSecretBasic();
	}

	@Override
	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_post")
	public void setupClientSecretPost() {
		setupClientSecretBasic();
	}

	@Override
	protected ClientAuthType getEffectiveClientAuthTypeVariant() {
		return ClientAuthType.CLIENT_SECRET_BASIC;
	}
}

package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.OIDCCClientAuthType;
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
	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "none")
	public void setupClientAuthNone() {
		setupClientSecretBasic();
	}

	@Override
	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		setupClientSecretBasic();
	}

	@Override
	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJWT() {
		setupClientSecretBasic();
	}

	@Override
	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "client_secret_post")
	public void setupClientSecretPost() {
		setupClientSecretBasic();
	}

	@Override
	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "tls_client_auth")
	public void setupTlsClientAuth() {
		setupClientSecretBasic();
	}

	@Override
	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "self_signed_tls_client_auth")
	public void setupSelfSignedTlsClientAuth() {
		setupClientSecretBasic();
	}

	@Override
	protected OIDCCClientAuthType getEffectiveClientAuthTypeVariant() {
		return OIDCCClientAuthType.CLIENT_SECRET_BASIC;
	}
}

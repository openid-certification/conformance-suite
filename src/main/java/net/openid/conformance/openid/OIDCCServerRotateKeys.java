package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.client.CheckServerKeysIsValid;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetStaticServerConfiguration;
import net.openid.conformance.condition.client.TellUserToRotateOpKeys;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.client.VerifyNewJwksHasNewSigningKey;
import net.openid.conformance.condition.client.VerifyNewJwksStillHasOldSigningKey;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "oidcc-server-rotate-keys",
	displayName = "OIDCC",
	summary = "Test that the authorization server is able to rotate signing keys held in it's jwks_uri, by comparing the contents of the jwks_uri before and after rotation; it must have a new key and should still contain the old key as well. Before pressing the 'Start' button, please trigger a key rotation in your OP. If you are not able to cause the server to rotate the keys while running the test, then you will have to self-assert that your deployment can do OP signing key rotation as part of your certification application, see the section about 'Attestation Statement' on https://openid.net/certification/submission/",
	profile = "OIDCC"
)
@VariantParameters({
	ServerMetadata.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"server.issuer",
	"server.token_endpoint"
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "discovery", configurationFields = {
	"server.discoveryUrl"
})
public class OIDCCServerRotateKeys extends AbstractTestModule {

	@Override
	public boolean autoStart() {
		// we want the user to manually start the test once they've rotated the keys
		return false;
	}

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		switch (getVariant(ServerMetadata.class)) {
			case DISCOVERY:
				callAndStopOnFailure(GetDynamicServerConfiguration.class);
				break;
			case STATIC:
				callAndStopOnFailure(GetStaticServerConfiguration.class);
				break;
		}

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		eventLog.startBlock("Fetch & validate current server keys");
		env.mapKey("server_jwks", "original_jwks");
		fetchAndValidateJwks();
		eventLog.endBlock();

		callAndStopOnFailure(TellUserToRotateOpKeys.class);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	private void fetchAndValidateJwks() {
		callAndStopOnFailure(FetchServerKeys.class);
		callAndContinueOnFailure(CheckServerKeysIsValid.class, Condition.ConditionResult.FAILURE);
		// Includes verify-base64url and bare-keys assertions (OIDC test)
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");
	}


	@Override
	public void start() {
		setStatus(Status.RUNNING);
		eventLog.startBlock("Fetch & validate new server keys");
		env.mapKey("server_jwks", "new_jwks");
		fetchAndValidateJwks();
		eventLog.endBlock();

		// note that we don't actually check if the server now uses the new key to sign id_tokens (same as python)
		callAndContinueOnFailure(VerifyNewJwksHasNewSigningKey.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1.1");

		// the python suite did not check this
		callAndContinueOnFailure(VerifyNewJwksStillHasOldSigningKey.class, Condition.ConditionResult.WARNING, "OIDCC-10.1.1");

		fireTestFinished();
	}

}

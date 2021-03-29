package net.openid.conformance.fapir;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.FAPIEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.client.CheckServerKeysIsValid;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.GetStaticServerConfiguration;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.variant.FapiRClientAuthType;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@VariantParameters({
	FapiRClientAuthType.class,
})
@VariantConfigurationFields(parameter = FapiRClientAuthType.class, value = "client_secret_jwt", configurationFields = {
	"client.client_secret",
	"client.client_secret_jwt_alg",
	"client2.client_id", // This is because the 'none' variant doesn't use the second client
	"client2.scope",
	"client2.client_secret",
	"client2.client_secret_jwt_alg"
})
@VariantConfigurationFields(parameter = FapiRClientAuthType.class, value = "private_key_jwt", configurationFields = {
	"client.jwks",
	"client2.client_id",
	"client2.scope",
	"client2.jwks"
})
@VariantConfigurationFields(parameter = FapiRClientAuthType.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"client2.client_id",
	"client2.scope",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
public abstract class AbstractFapiRServerTestModule extends AbstractRedirectServerTestModule {
	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		// It would be better to do this using variants as it's done in
		// the OIDCC test modules.
		callAndContinueOnFailure(GetDynamicServerConfiguration.class);
		callAndContinueOnFailure(GetStaticServerConfiguration.class);

		supportMTLSEndpointAliases();

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);

		callAndContinueOnFailure(CheckServerKeysIsValid.class, Condition.ConditionResult.WARNING);

		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");

		callAndContinueOnFailure(FAPIEnsureMinimumServerKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		setupClient();

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void supportMTLSEndpointAliases() {
		// Support when client auth type is MTLS
	}

	protected abstract void setupClient();
}

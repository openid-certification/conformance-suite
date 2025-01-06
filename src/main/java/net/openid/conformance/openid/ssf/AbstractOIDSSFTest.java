package net.openid.conformance.openid.ssf;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddBasicAuthClientSecretAuthenticationParameters;
import net.openid.conformance.condition.client.AddClientIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddFormBasedClientSecretAuthenticationParameters;
import net.openid.conformance.condition.client.AddScopeToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClientConfiguration;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.GetStaticServerConfiguration;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12OrLater;
import net.openid.conformance.openid.ssf.conditions.OIDSSFExtractTransmitterAccessTokenFromConfig;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFGetDynamicTransmitterConfiguration;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFGetStaticTransmitterConfiguration;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.sequence.client.CreateDpopProofSteps;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

@VariantParameters({
		ServerMetadata.class,
		SsfServerMetadata.class,
		SsfDeliveryMode.class,
		SsfAuthMode.class,
		ClientRegistration.class,
		ClientAuthType.class,
		SsfProfile.class,
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
		"server.token_endpoint",
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "discovery", configurationFields = {
		"server.discoveryUrl"
})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "static", configurationFields = {
		"ssf.transmitter.configuration_metadata_endpoint",
		"ssf.stream.audience",
})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix",
		"ssf.stream.audience",
})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {
		"ssf.transmitter.access_token"
})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "dynamic", configurationFields = {
		"client.scope",
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
		"client.client_id",
		"client.scope",
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_basic", configurationFields = {
		"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_post", configurationFields = {
		"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_jwt", configurationFields = {
		"client.client_secret",
		"client.client_secret_jwt_alg"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "private_key_jwt", configurationFields = {
		"client.jwks"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "mtls", configurationFields = {
		"mtls.key",
		"mtls.cert",
		"mtls.ca"
})
@VariantHidesConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {
		"client.client_id",
		"client.client_secret",
		"client.scope",
		"server.token_endpoint",
})
public abstract class AbstractOIDSSFTest extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		env.putString("base_url", baseUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		env.putString("ssf","profile", getVariant(SsfProfile.class).name());

		exposeEnvString("alias", "config","alias");

		if (Objects.requireNonNull(getVariant(SsfDeliveryMode.class)) == SsfDeliveryMode.PUSH) {
			String pushDeliveryEndpointUrl = createPushDeliveryEndpointUrl(env);
			env.putString("ssf", "push_delivery_endpoint_url", pushDeliveryEndpointUrl);
			exposeEnvString("pushDeliveryEndpointUrl", "ssf", "push_delivery_endpoint_url");
		}

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	@Override
	public void start() {

	}

	protected void fetchTransmitterMetadata() {

		switch (getVariant(SsfServerMetadata.class)) {
			case DISCOVERY:
				callAndStopOnFailure(OIDSSFGetDynamicTransmitterConfiguration.class, "OIDSSF-6.2");
				break;
			case STATIC:
				callAndStopOnFailure(OIDSSFGetStaticTransmitterConfiguration.class, "OIDSSF-6.2");
				break;
		}
	}

	protected void obtainTransmitterAccessToken() {
		switch (getVariant(SsfAuthMode.class)) {
			case STATIC:
				callAndStopOnFailure(OIDSSFExtractTransmitterAccessTokenFromConfig.class);
				return;

			case DYNAMIC:

				switch (getVariant(ServerMetadata.class)) {
					case DISCOVERY:
						callAndStopOnFailure(GetDynamicServerConfiguration.class);
						break;
					case STATIC:
						callAndStopOnFailure(GetStaticServerConfiguration.class);
						break;
				}

				switch (getVariant(ClientRegistration.class)) {
					case STATIC_CLIENT:
						callAndStopOnFailure(GetStaticClientConfiguration.class);
						break;
					case DYNAMIC_CLIENT:
						throw new UnsupportedOperationException("Dynamic clients are not supported for SSF Tests");
					default:
						break;
				}

				callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
				callAndStopOnFailure(AddScopeToTokenEndpointRequest.class);

				switch (getVariant(ClientAuthType.class)) {
					case CLIENT_SECRET_BASIC:
						callAndStopOnFailure(AddBasicAuthClientSecretAuthenticationParameters.class);
						break;
					case CLIENT_SECRET_POST:
						callAndStopOnFailure(AddFormBasedClientSecretAuthenticationParameters.class);
						break;
					case CLIENT_SECRET_JWT:
						throw new UnsupportedOperationException("TODO implement me");
					case PRIVATE_KEY_JWT:

						callAndStopOnFailure(ExtractJWKSDirectFromClientConfiguration.class);
						callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
						call(sequence(CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class));

						boolean dpop = false;
						if (dpop) {
							callAndStopOnFailure(GenerateDpopKey.class);
							call(CreateDpopProofSteps.createTokenEndpointDpopSteps());
						}
						break;
					case MTLS:
						throw new UnsupportedOperationException("TODO implement me");
					case NONE:
					default:
						break;
				}

				// TODO fetch token via client credentials grant

				callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
				callAndStopOnFailure(CallTokenEndpoint.class);
				callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
				callAndStopOnFailure(CheckForAccessTokenValue.class);
				callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
				break;
		}

	}

	protected void validateTlsConnection() {
		callAndContinueOnFailure(EnsureTLS12OrLater.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.1");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.1");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.1");
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if ("ssf-push".equals(path)) {
			env.putObject("ssf", "push_request", requestParts);
			return ResponseEntity.noContent().build();
		}

		return super.handleHttp(path, req, res, session, requestParts);
	}

	protected String createPushDeliveryEndpointUrl(Environment env) {

		String baseUrl = env.getString("base_url");
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}
		return "${baseUrl}/ssf-push".replace("${baseUrl}", baseUrl);
	}

	protected boolean isSsfProfileEnabled(SsfProfile profile) {
		return profile.equals(getVariant(SsfProfile.class));
	}
}

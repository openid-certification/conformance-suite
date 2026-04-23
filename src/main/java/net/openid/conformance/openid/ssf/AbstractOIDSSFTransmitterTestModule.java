package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddBasicAuthClientSecretToRequest;
import net.openid.conformance.condition.client.AddClientIdToRequest;
import net.openid.conformance.condition.client.AddFormBasedClientSecretToRequest;
import net.openid.conformance.condition.client.AddScopeToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200Or404;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204Or404;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClientConfiguration;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.GetStaticServerConfiguration;
import net.openid.conformance.openid.ssf.conditions.OIDSSFConfigurePushDeliveryMethod;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureShortLivedToken;
import net.openid.conformance.openid.ssf.conditions.OIDSSFExtractTransmitterAccessTokenFromConfig;
import net.openid.conformance.openid.ssf.conditions.OIDSSFValidateTlsConnectionConditionSequence;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureAuthorizationHeaderIsPresentInPushRequest;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFEnsureDeliveryMethodIsSupported;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFGetDynamicTransmitterConfiguration;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFGetStaticTransmitterConfiguration;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInjectPushAuthorizationHeader;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.openid.ssf.delivery.SSfPushRequest;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantNotApplicableWhen;
import net.openid.conformance.variant.VariantParameters;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

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
	"ssf.transmitter.configuration_metadata_endpoint"
})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {
	"ssf.transmitter.issuer",
	"ssf.transmitter.metadata_suffix"
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
// When the receiver presents a static access token, the OAuth client / server discovery
// dimensions don't apply — hide their dropdowns from the schedule-test UI.
@VariantNotApplicableWhen(parameter = ServerMetadata.class, values = "*",
	whenParameter = SsfAuthMode.class, hasValues = "static")
@VariantNotApplicableWhen(parameter = ClientRegistration.class, values = "*",
	whenParameter = SsfAuthMode.class, hasValues = "static")
@VariantNotApplicableWhen(parameter = ClientAuthType.class, values = "*",
	whenParameter = SsfAuthMode.class, hasValues = "static")
@VariantNotApplicable(parameter = ClientAuthType.class, values = "client_attestation")
public class AbstractOIDSSFTransmitterTestModule extends AbstractOIDSSFTestModule {

	protected BlockingDeque<SSfPushRequest> pushRequests = new LinkedBlockingDeque<>();

	protected String pushAuthorizationHeader;

	protected SsfDeliveryMode deliveryMode;

	@Override
	public void start() {
		pushAuthorizationHeader = generatePushAuthorizationHeader();
		super.start();
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		deliveryMode = Objects.requireNonNull(getVariant(SsfDeliveryMode.class));
		super.configure(config, baseUrl, externalUrlOverride, baseMtlsUrl);
	}

	protected String generatePushAuthorizationHeader() {
		return "Bearer push_token_" + UUID.randomUUID();
	}

	@Override
	protected void configureServerEndpoints() {
		super.configureServerEndpoints();

		// When we test a transmitter we need to act as a receiver
		if (Objects.requireNonNull(getVariant(SsfDeliveryMode.class)) == SsfDeliveryMode.PUSH) {
			callAndStopOnFailure(OIDSSFConfigurePushDeliveryMethod.class);
			exposeEnvString("pushDeliveryEndpointUrl", "ssf", "push_delivery_endpoint_url");
		}
	}

	protected void configurePushAuthorizationHeader(JsonObject deliveryObject, String pushAuthorizationHeader) {
		if (StringUtils.hasText(pushAuthorizationHeader)) {
			callAndContinueOnFailure(new OIDSSFInjectPushAuthorizationHeader(pushAuthorizationHeader), Condition.ConditionResult.INFO, "OIDSSF-6.1.1");
		}
	}

	protected void fetchTransmitterMetadata() {

		switch (getVariant(SsfServerMetadata.class)) {
			case DISCOVERY:
				callAndStopOnFailure(OIDSSFGetDynamicTransmitterConfiguration.class, "OIDSSF-7.2", "OIDSSF-7.2.3");
				break;
			case STATIC:
				callAndStopOnFailure(OIDSSFGetStaticTransmitterConfiguration.class, "OIDSSF-7.2", "OIDSSF-7.2.3");
				break;
		}
		env.mapKey("endpoint_response", "transmitter_metadata_endpoint_response");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.2.3");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.2.3");
		env.unmapKey("endpoint_response");

		exposeEnvString("ssf_metadata_url", "ssf", "transmitter_metadata_url");

		checkDeliveryMethod();
	}

	protected void checkDeliveryMethod() {
		callAndStopOnFailure(new OIDSSFEnsureDeliveryMethodIsSupported(deliveryMode));
	}

	protected void cleanUpStreamConfigurationIfNecessary() {
		try {
			callAndContinueOnFailure(OIDSSFReadStreamConfigCall.class, Condition.ConditionResult.INFO, "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200Or404.class, Condition.ConditionResult.INFO, "OIDSSF-7.1.1.2");
		} catch (Exception ignore) {
		}
		boolean danglingStreamConfigFound = env.getElementFromObject("ssf", "stream") != null && env.getElementFromObject("ssf", "streams") != null;
		if (danglingStreamConfigFound) {
			try {
				callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO, "CAEPIOP-2.3.8.2");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs204Or404.class, Condition.ConditionResult.INFO, "OIDSSF-7.1.1.5");
			} catch (Exception ignore) {
			}
		}
	}

	protected void obtainTransmitterAccessToken() {

		SsfAuthMode variant = getVariant(SsfAuthMode.class);
		if (SsfAuthMode.STATIC == variant) {
			callAndStopOnFailure(OIDSSFExtractTransmitterAccessTokenFromConfig.class);
			return;
		}

		if (SsfAuthMode.DYNAMIC == variant) {
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
					throw new UnsupportedOperationException("Dynamic clients are not supported for SSF Tests at the moment");
				default:
					break;
			}

			callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
			callAndStopOnFailure(AddScopeToTokenEndpointRequest.class);

			mapClientAuthKeys("token_endpoint_request_form_parameters", "token_endpoint_request_headers");
			switch (getVariant(ClientAuthType.class)) {
				case CLIENT_SECRET_BASIC:
					callAndStopOnFailure(AddBasicAuthClientSecretToRequest.class);
					break;
				case CLIENT_SECRET_POST:
					callAndStopOnFailure(AddFormBasedClientSecretToRequest.class);
					break;
				case CLIENT_SECRET_JWT:
					throw new UnsupportedOperationException("TODO implement me");
				case PRIVATE_KEY_JWT:

					callAndStopOnFailure(ExtractJWKSDirectFromClientConfiguration.class);
					callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
					call(sequence(CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class));
					break;
				case MTLS:
					throw new UnsupportedOperationException("TODO implement me");
				case NONE:
					// no authentication configured, fall-through
				default:
					break;
			}

			callAndStopOnFailure(AddClientIdToRequest.class);
			unmapClientAuthKeys();
			callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
			callAndStopOnFailure(CheckTokenEndpointHttpStatus200.class);
			callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
			callAndStopOnFailure(CheckForAccessTokenValue.class);
			callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
			callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, Condition.ConditionResult.WARNING, "RFC6749-5.1");
			callAndContinueOnFailure(OIDSSFEnsureShortLivedToken.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.7.1");
		}

	}

	/**
	 * Provides a dynamic endpoint for handling SSF Push requests from sent from transmitters.
	 *
	 * @param path         The path that was called
	 * @param req          The request that passed to the server
	 * @param res          A response that will be sent from the server
	 * @param session      Session details
	 * @param requestParts elements from the request parsed out into a json object for use in condition classes
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if ("ssf-push".equals(path)) {
			SSfPushRequest pushRequest = new SSfPushRequest(UUID.randomUUID().toString(), path, Instant.now(), req, res, requestParts);
			pushRequests.offerLast(pushRequest);

			// Do NOT call eventLog.log() here — it is synchronized and will block
			// if the test's main thread is inside a runBlock. The push request will
			// be logged when lookupNextPushRequest() processes it.

			// Mark push request as accepted for now, and validate later
			return ResponseEntity.accepted().build();
		}

		return super.handleHttp(path, req, res, session, requestParts);
	}

	protected void onPushDeliveryReceived(String path, JsonObject requestParts) {
		callAndContinueOnFailure(OIDSSFEnsureAuthorizationHeaderIsPresentInPushRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-6.1.1");
	}

	protected void validateTlsConnection() {
		call(sequence(OIDSSFValidateTlsConnectionConditionSequence.class));
	}

	protected SSfPushRequest lookupNextPushRequest() {
		return lookupNextPushRequest(5);
	}

	protected SSfPushRequest lookupNextPushRequest(int timeoutSeconds) {

		try {
			SSfPushRequest pushRequest = pushRequests.pollFirst(timeoutSeconds, TimeUnit.SECONDS);
			if (pushRequest == null) {
				return pushRequest;
			}

			eventLog.log(getName(), "Processing recorded ssf-push endpoint request with id: " + pushRequest.id());
			env.putObject("ssf", "push_request", pushRequest.requestParts());
			env.putString("ssf", "push_request_received_at", pushRequest.receivedAt().toString());
			onPushDeliveryReceived(pushRequest.path(), pushRequest.requestParts());

			return pushRequest;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns {@code true} if the most recently parsed verification token (stored
	 * at {@code ssf.verification.token.claims}) contains an SSF stream verification
	 * event in its {@code events} claim. Used by callers that iterate SETs from a
	 * poll or push delivery to skip non-verification events during the verification
	 * phase.
	 */
	protected boolean currentEventIsVerificationEvent() {
		JsonElement claimsEl = env.getElementFromObject("ssf", "verification.token.claims");
		if (claimsEl == null || !claimsEl.isJsonObject()) {
			return false;
		}
		JsonObject events = claimsEl.getAsJsonObject().getAsJsonObject("events");
		if (events == null) {
			return false;
		}
		return events.has(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
	}

	/**
	 * Returns {@code true} if the most recently parsed verification event contains
	 * a {@code state} claim. Per SSF 1.0 §8.1.4.2, a verification event with
	 * {@code state} is a solicited response to a receiver-issued verification
	 * request (and the state MUST match what was sent); a verification event
	 * without {@code state} is transmitter-initiated (unsolicited) and MUST NOT
	 * carry one. Callers looping for a specific solicited response use this to
	 * decide whether to stop.
	 */
	protected boolean currentVerificationEventHasState() {
		JsonElement claimsEl = env.getElementFromObject("ssf", "verification.token.claims");
		if (claimsEl == null || !claimsEl.isJsonObject()) {
			return false;
		}
		JsonObject events = claimsEl.getAsJsonObject().getAsJsonObject("events");
		if (events == null) {
			return false;
		}
		JsonElement verificationEventEl = events.get(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
		if (verificationEventEl == null || !verificationEventEl.isJsonObject()) {
			return false;
		}
		return verificationEventEl.getAsJsonObject().has("state");
	}
}

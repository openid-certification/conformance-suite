package net.openid.conformance.openid.ssf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.GenerateAccessTokenExpiration;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIsAnyOf;
import net.openid.conformance.condition.common.CheckIncomingRequestMethodIsGet;
import net.openid.conformance.openid.ssf.conditions.OIDSSFGenerateServerJWKs;
import net.openid.conformance.openid.ssf.conditions.as.OIDSSFStoreIssuedAccessToken;
import net.openid.conformance.openid.ssf.conditions.as.OIDSSFValidateRequestedScope;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamVerificationSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateUnsolicitedStreamVerificationSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleAuthorizationHeader;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePollRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePushDeliveryToReceiver;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamCreateRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamCreateRequestValidation;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamDeleteRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamLookupRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamReplaceRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamRequestBodyParsing;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamStatusLookup;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamStatusUpdateRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamStatusUpdateRequestParsing;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamSubjectAdd;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamSubjectRemove;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamUpdateRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamUpdateRequestValidation;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamVerificationRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamSubjectOperation;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFInMemoryEventStore;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretBasic;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretJWT;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretPost;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.BaseUrlUtil;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.OAuthUriUtil;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantNotApplicableWhen;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;
import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI;

@VariantParameters({SsfProfile.class, SsfDeliveryMode.class, SsfAuthMode.class, ClientAuthType.class,})
@ConfigurationFields({
	"ssf.stream.audience",
	"ssf.subjects.valid",
	"ssf.subjects.invalid",
})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {
	"ssf.transmitter.access_token",
})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "dynamic", configurationFields = {
	"client.client_id",
	"client.scope",
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_basic", configurationFields = {
	"client.client_secret",
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_post", configurationFields = {
	"client.client_secret",
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_jwt", configurationFields = {
	"client.client_secret",
	"client.client_secret_jwt_alg",
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "private_key_jwt", configurationFields = {
	"client.jwks",
})
// In static mode the receiver presents a pre-shared token, so the OAuth client-auth
// dimension does not apply — hide its dropdown from the schedule-test UI.
@VariantNotApplicableWhen(parameter = ClientAuthType.class, values = "*",
	whenParameter = SsfAuthMode.class, hasValues = "static")
// mtls requires certificate-bound-token infrastructure (mTLS endpoint alias + client
// certificate extraction); client_attestation and none are not applicable for SSF.
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "mtls", "client_attestation"
})
public abstract class AbstractOIDSSFReceiverTestModule extends AbstractOIDSSFTestModule {

	protected OIDSSFEventStore eventStore;

	/**
	 * The per-{@link ClientAuthType} sequence used to validate client
	 * authentication on the emulated token endpoint in
	 * {@link SsfAuthMode#DYNAMIC} mode. Set by the {@code @VariantSetup}
	 * initialiser matching the selected {@link ClientAuthType}.
	 */
	protected Class<? extends ConditionSequence> validateClientAuthenticationSteps;

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_basic")
	public void setupClientSecretBasic() {
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretBasic.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_post")
	public void setupClientSecretPost() {
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretPost.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJwt() {
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretJWT.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
	}

	@Override
	protected void configureServerMetadata() {
		super.configureServerMetadata();

		eventStore = createEventStore();

		generateJwks();
		registerEventsSupported();

		String transmitterAccessToken = getTransmitterAccessToken();
		env.putString("ssf", "transmitter_access_token", transmitterAccessToken);
		exposeEnvString("ssf_tx_access_token", "ssf", "transmitter_access_token");

		String issuer = resolveEffectiveIssuer();

		env.putString("ssf", "issuer", issuer);
		exposeEnvString("ssf_issuer", "ssf", "issuer");

		String configurationUrl = OAuthUriUtil.generateWellKnownUrlForPath(issuer, "ssf-configuration");
		env.putString("ssf", "configuration_url", configurationUrl);
		exposeEnvString("ssf_configuration_url", "ssf", "configuration_url");

		JsonObject transmitterMetadata = generateTransmitterMetadata(issuer);
		env.putObject("ssf", "transmitter_metadata", transmitterMetadata);

		env.putString("ssf", "auth_mode", getVariant(SsfAuthMode.class).name());
		configureAuthorizationServer(issuer);
	}

	/**
	 * When {@link SsfAuthMode#DYNAMIC} is selected the conformance suite also acts
	 * as the Authorization Server (CAEP Interop Profile §2.7.1): it registers the
	 * OAuth client the receiver authenticates as, publishes RFC 8414 AS metadata,
	 * and exposes a {@code /token} endpoint. The receiver obtains a short-lived
	 * access token via the {@code client_credentials} grant and presents it on
	 * subsequent SSF API requests.
	 */
	protected void configureAuthorizationServer(String issuer) {

		if (getVariant(SsfAuthMode.class) != SsfAuthMode.DYNAMIC) {
			return;
		}

		// Register the OAuth client the receiver under test will authenticate as,
		// reusing the standard client.* configuration fields.
		String clientId = env.getString("config", "client.client_id");
		if (!StringUtils.hasText(clientId)) {
			clientId = "ssf-test-client";
		}
		String clientSecret = env.getString("config", "client.client_secret");
		if (!StringUtils.hasText(clientSecret)) {
			clientSecret = UUID.randomUUID().toString();
		}
		String scope = env.getString("config", "client.scope");
		if (!StringUtils.hasText(scope)) {
			scope = SsfConstants.SCOPE_SSF_READ + " " + SsfConstants.SCOPE_SSF_MANAGE;
		}

		JsonObject client = new JsonObject();
		client.addProperty("client_id", clientId);
		client.addProperty("client_secret", clientSecret);
		client.addProperty("scope", scope);

		// client_secret_jwt: the registered signing algorithm assertions must use.
		String clientSecretJwtAlg = env.getString("config", "client.client_secret_jwt_alg");
		if (StringUtils.hasText(clientSecretJwtAlg)) {
			client.addProperty("token_endpoint_auth_signing_alg", clientSecretJwtAlg);
		}
		// private_key_jwt: the receiver's public JWKS used to verify the assertion signature.
		JsonElement clientJwks = env.getElementFromObject("config", "client.jwks");
		if (clientJwks != null) {
			client.add("jwks", clientJwks);
		}
		env.putObject("client", client);

		exposeEnvString("ssf_client_id", "client", "client_id");
		exposeEnvString("ssf_client_secret", "client", "client_secret");
		exposeEnvString("ssf_client_scope", "client", "scope");

		// The emulated AS configuration the condition/as/ building blocks read,
		// notably ValidateClientAssertionClaims which validates the assertion
		// audience against the issuer / token_endpoint.
		JsonObject server = new JsonObject();
		server.addProperty("issuer", issuer);
		server.addProperty("token_endpoint", issuer + "/token");
		env.putObject("server", server);

		// In-env store of issued access tokens for the dynamic-mode bearer-token check.
		env.putObject("ssf", "issued_tokens", new JsonObject());

		env.putString("ssf", "token_endpoint", issuer + "/token");
		exposeEnvString("ssf_token_endpoint", "ssf", "token_endpoint");

		// RFC 8414 metadata document URL the receiver can use to discover the AS.
		String authorizationServerMetadataUrl = OAuthUriUtil.generateWellKnownUrlForPath(issuer, "oauth-authorization-server");
		env.putString("ssf", "authorization_server_metadata_url", authorizationServerMetadataUrl);
		exposeEnvString("ssf_authorization_server_url", "ssf", "authorization_server_metadata_url");

		JsonObject asMetadata = generateAuthorizationServerMetadata(issuer);
		env.putObject("ssf", "authorization_server_metadata", asMetadata);
	}

	protected JsonObject generateAuthorizationServerMetadata(String issuer) {

		JsonObject metadata = new JsonObject();
		metadata.addProperty("issuer", issuer);
		metadata.addProperty("token_endpoint", issuer + "/token");
		metadata.add("grant_types_supported", OIDFJSON.convertListToJsonArray(List.of("client_credentials")));
		metadata.add("token_endpoint_auth_methods_supported",
			OIDFJSON.convertListToJsonArray(List.of(mapClientAuthTypeToMetadataValue(getVariant(ClientAuthType.class)))));
		metadata.add("scopes_supported",
			OIDFJSON.convertListToJsonArray(List.of(SsfConstants.SCOPE_SSF_READ, SsfConstants.SCOPE_SSF_MANAGE)));
		metadata.add("response_types_supported", new JsonArray());
		return metadata;
	}

	protected String mapClientAuthTypeToMetadataValue(ClientAuthType clientAuthType) {
		return switch (clientAuthType) {
			case CLIENT_SECRET_BASIC -> "client_secret_basic";
			case CLIENT_SECRET_POST -> "client_secret_post";
			case CLIENT_SECRET_JWT -> "client_secret_jwt";
			case PRIVATE_KEY_JWT -> "private_key_jwt";
			case MTLS -> "tls_client_auth";
			case CLIENT_ATTESTATION -> "attest_jwt_client_auth";
			case NONE -> "none";
		};
	}

	protected String resolveEffectiveIssuer() {

		String issuer = env.getString("config", "ssf.transmitter.issuer_override");
		if (issuer == null) {
			issuer = BaseUrlUtil.resolveEffectiveBaseUrl(env);
		}
		return issuer;
	}

	protected OIDSSFInMemoryEventStore createEventStore() {
		return new OIDSSFInMemoryEventStore();
	}

	protected String getTransmitterAccessToken() {
		return generateTransmitterAccessToken();
	}

	protected String generateTransmitterAccessToken() {
		String transmitterAccessToken = env.getString("config", "ssf.transmitter.access_token");
		if (!StringUtils.hasText(transmitterAccessToken)) {
			transmitterAccessToken = UUID.randomUUID().toString();
		}
		return transmitterAccessToken;
	}

	protected void registerEventsSupported() {

		JsonObject ssfDefaultConfig = new JsonObject();
		ssfDefaultConfig.add("events_supported", OIDFJSON.convertListToJsonArray(getEventsSupported()));
		ssfDefaultConfig.addProperty("supports_multiple_streams_per_receiver", false);

		env.putObject("ssf", "default_config", ssfDefaultConfig);
	}

	protected void generateJwks() {
		callAndStopOnFailure(OIDSSFGenerateServerJWKs.class);
	}

	@Override
	protected void configureServerEndpoints() {
		super.configureServerEndpoints();

		String ssfIssuer = env.getString("ssf", "issuer");

		if (Objects.requireNonNull(getVariant(SsfDeliveryMode.class)) == SsfDeliveryMode.POLL) {
			String pollEndpointUrl = ssfIssuer + "/events";
			env.putString("ssf", "poll_endpoint_url", pollEndpointUrl);

			exposeEnvString("ssf_poll_endpoint", "ssf", "poll_endpoint_url");
		}
	}

	protected JsonObject generateTransmitterMetadata(String issuer) {

		JsonObject metadata = new JsonObject();

		metadata.addProperty("issuer", issuer);
		metadata.addProperty("spec_version", "1_0");
		metadata.addProperty("jwks_uri", issuer + "/jwks");
		metadata.add("delivery_methods_supported", OIDFJSON.convertListToJsonArray(List.of( //
			DELIVERY_METHOD_PUSH_RFC_8935_URI, // PUSH Delivery
			DELIVERY_METHOD_POLL_RFC_8936_URI // POLL Delivery
		)));

		metadata.addProperty("configuration_endpoint", issuer + "/streams");
		metadata.addProperty("status_endpoint", issuer + "/status");

		if (!isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			metadata.addProperty("add_subject_endpoint", issuer + "/add_subject");
			metadata.addProperty("remove_subject_endpoint", issuer + "/remove_subject");
		}

		metadata.addProperty("verification_endpoint", issuer + "/verify");

		JsonObject oauthAuthorizationScheme = new JsonObject();
		oauthAuthorizationScheme.addProperty("spec_urn", "urn:ietf:rfc:6749");
		metadata.add("authorization_schemes", OIDFJSON.convertJsonObjectListToJsonArray(List.of(oauthAuthorizationScheme)));

		return metadata;
	}

	public List<String> getEventsSupported() {
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			List<String> eventsSupported = new ArrayList<>();
			eventsSupported.addAll(SsfEvents.SSF_EVENT_TYPES);
			eventsSupported.addAll(SsfEvents.CAEP_INTEROP_EVENT_TYPES);
			return eventsSupported;
		}
		// Advertise only the event families this emulated transmitter can generate
		// valid example SETs for (see generateSsfEventExample). SCIM events (RFC 9967)
		// are intentionally excluded: they are recognised in the validation allow-list
		// (SsfEvents.STANDARD_EVENT_TYPES) but require SCIM-shaped subjects and
		// event-specific content that we do not yet generate, so advertising them here
		// would let a correct receiver request events we can only deliver as invalid SETs.
		List<String> eventsSupported = new ArrayList<>();
		eventsSupported.addAll(SsfEvents.SSF_EVENT_TYPES);
		eventsSupported.addAll(SsfEvents.CAEP_EVENT_TYPES);
		eventsSupported.addAll(SsfEvents.RISC_EVENT_TYPES);
		return eventsSupported;
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		env.mapKey("incoming_request", requestId);

		if (isFinished()) {
			// ignore requests after the test finished.
			// The transmitter tests might send additional cleanup requests which we don't need to handle here.
			//
			return ResponseEntity.noContent().build();
		}

		setStatus(Status.RUNNING);

		Object response;
		try {
			switch (path) {
				case "ssf-configuration" -> response = handleSsfConfigurationEndpoint(requestId);
				case "jwks" -> response = handleJwksEndpoint();
				// The token endpoint performs its own client authentication, so it is
				// intentionally not wrapped in ensureAuthorized().
				case "token" -> response = handleTokenEndpointRequest(req, requestId);
				case "events" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleStreamPollingRequest(path, req, res, session, requestParts);
				});
				case "streams" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleStreamConfigurationEndpointRequest(path, req, res, session, requestParts);
				});
				case "status" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleStreamStatusEndpointRequest(path, req, res, session, requestParts);
				});
				case "verify" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleVerificationEndpointRequest(path, req, res, session, requestParts);
				});
				case "add_subject" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleSubjectsEndpointRequest(path, req, res, session, requestParts, StreamSubjectOperation.add);
				});
				case "remove_subject" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleSubjectsEndpointRequest(path, req, res, session, requestParts, StreamSubjectOperation.remove);
				});
				default -> response = super.handleHttp(path, req, res, session, requestParts);
			}
		} finally {
			if (!Set.of(Status.WAITING, Status.FINISHED).contains(getStatus())) {
				setStatus(Status.WAITING);
			}
			env.removeObject(requestId);
			env.unmapKey("incoming_request");
		}

		return response;
	}

	@SuppressWarnings("FutureReturnValueIgnored")
	protected void scheduleTask(Callable<String> action, int amount, TimeUnit timeUnit) {

		getTestExecutionManager().scheduleInBackground(() -> {
			Status status = getStatus();
			if (status == Status.FINISHED || status == Status.INTERRUPTED) {
				return "done";
			}

			setStatus(Status.RUNNING);
			String result = action.call();

			if (getStatus() != Status.WAITING) {
				setStatus(Status.WAITING);
			}
			return result;
		}, amount, timeUnit);
	}

	protected ResponseEntity<?> ensureAuthorized(HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts, Supplier<ResponseEntity<?>> requestHandler) {

		callAndStopOnFailure(OIDSSFHandleAuthorizationHeader.class, "CAEPIOP-2.7.3");
		JsonObject authResult = env.getElementFromObject("ssf", "auth_result").getAsJsonObject();

		JsonElement authErrorEl = authResult.get("error");
		if (authErrorEl != null) {
			JsonObject authError = authErrorEl.getAsJsonObject();
			int statusCode = OIDFJSON.getInt(authResult.get("status_code"));
			return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(authError);
		}

		return requestHandler.get();
	}

	@Override
	public Object handleWellKnown(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		env.mapKey("incoming_request", requestId);

		setStatus(Status.RUNNING);

		Object response;
		try {
			if (path.startsWith("/.well-known/ssf-configuration")) {
				response = handleSsfConfigurationEndpoint(requestId);
			} else if (path.startsWith("/.well-known/oauth-authorization-server")) {
				response = handleAuthorizationServerMetadataEndpoint();
			} else {
				response = super.handleWellKnown(path, req, res, session, requestParts);
			}
		} finally {
			setStatus(Status.WAITING);
			env.removeObject(requestId);
			env.unmapKey("incoming_request");
		}

		return response;
	}

	protected ResponseEntity<?> handleSsfConfigurationEndpoint(String requestId) {
		callAndContinueOnFailure(CheckIncomingRequestMethodIsGet.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.2.1");
		JsonObject ssfConfig = getSsfConfiguration();
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ssfConfig);
	}

	protected JsonObject getSsfConfiguration() {
		return env.getElementFromObject("ssf", "transmitter_metadata").getAsJsonObject();
	}

	protected ResponseEntity<?> handleJwksEndpoint() {
		// Serve only the public keys at the transmitter jwks_uri - it must not leak private key material.
		JsonObject publicJwks = JWKUtil.toPublicJWKSet(env.getObject("server_jwks"));
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(publicJwks);
	}

	protected ResponseEntity<?> handleAuthorizationServerMetadataEndpoint() {
		JsonElement asMetadataEl = env.getElementFromObject("ssf", "authorization_server_metadata");
		if (asMetadataEl == null) {
			// Only published in dynamic auth mode.
			return ResponseEntity.notFound().build();
		}
		callAndContinueOnFailure(CheckIncomingRequestMethodIsGet.class, Condition.ConditionResult.FAILURE, "RFC8414-3");
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(asMetadataEl.getAsJsonObject());
	}

	/**
	 * Emulated OAuth token endpoint (RFC 6749) used in {@link SsfAuthMode#DYNAMIC}
	 * mode. Validates client authentication using the per-{@link ClientAuthType}
	 * sequence, validates the requested SSF scope, mints a short-lived bearer
	 * token, stores it for later bearer-token validation, and returns an
	 * RFC 6749 §5.1 token response. Only the {@code client_credentials} grant is
	 * supported in this slice.
	 */
	protected ResponseEntity<?> handleTokenEndpointRequest(HttpServletRequest req, String requestId) {

		if (!"POST".equals(req.getMethod())) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
		}

		env.mapKey("token_endpoint_request", requestId);
		try {
			String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");
			if (!"client_credentials".equals(grantType)) {
				return tokenError("unsupported_grant_type",
					"Only the client_credentials grant is supported", HttpStatus.BAD_REQUEST);
			}

			if (validateClientAuthenticationSteps != null) {
				call(sequence(validateClientAuthenticationSteps));
			}

			callAndStopOnFailure(OIDSSFValidateRequestedScope.class, "CAEPIOP-2.7.2");
			callAndStopOnFailure(GenerateBearerAccessToken.class);
			callAndStopOnFailure(GenerateAccessTokenExpiration.class);
			callAndStopOnFailure(OIDSSFStoreIssuedAccessToken.class);
			callAndStopOnFailure(CreateTokenEndpointResponse.class, "RFC6749-5.1");

			JsonObject tokenResponse = env.getObject("token_endpoint_response");
			return ResponseEntity.ok()
				.header("Cache-Control", "no-store")
				.header("Pragma", "no-cache")
				.contentType(MediaType.APPLICATION_JSON)
				.body(tokenResponse);
		} finally {
			env.unmapKey("token_endpoint_request");
		}
	}

	protected ResponseEntity<?> tokenError(String error, String description, HttpStatus status) {
		JsonObject body = new JsonObject();
		body.addProperty("error", error);
		body.addProperty("error_description", description);
		return ResponseEntity.status(status)
			.header("Cache-Control", "no-store")
			.header("Pragma", "no-cache")
			.contentType(MediaType.APPLICATION_JSON)
			.body(body);
	}

	protected ResponseEntity<?> handleStreamConfigurationEndpointRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String method = req.getMethod();
		switch (method) {

			case "GET": {
				callAndContinueOnFailure(OIDSSFHandleStreamLookupRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
				JsonObject lookupResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();

				JsonElement result = lookupResult.get("result");
				int statusCode = OIDFJSON.getInt(lookupResult.get("status_code"));

				if (result == null) {
					return ResponseEntity.status(statusCode).build();
				}

				JsonElement error = lookupResult.get("error");
				afterStreamLookup(OIDFJSON.tryGetString(lookupResult.get("stream_id")), lookupResult, error);

				return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
			}

			case "POST": {
				callAndContinueOnFailure(OIDSSFHandleStreamRequestBodyParsing.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
				callAndContinueOnFailure(OIDSSFHandleStreamCreateRequestValidation.class, Condition.ConditionResult.FAILURE,"OIDSSF-8.1.1.1");
				callAndContinueOnFailure(OIDSSFHandleStreamCreateRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
				JsonObject createResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = createResult.get("error");
				String createdStreamId = OIDFJSON.tryGetString(createResult.get("stream_id"));
				afterStreamCreation(createdStreamId, createResult, error);

				if (error == null && createdStreamId != null
					&& shouldDeliverUnsolicitedStreamVerificationAfterStreamCreation()) {
					deliverUnsolicitedStreamVerificationEvent(createdStreamId);
				}

				return handleResultWithBody(createResult);
			}

			case "DELETE": {
				callAndContinueOnFailure(new OIDSSFHandleStreamDeleteRequest(eventStore), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");

				JsonObject deleteResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = deleteResult.get("error");
				int statusCode = OIDFJSON.getInt(deleteResult.get("status_code"));
				afterStreamDeletion(OIDFJSON.tryGetString(deleteResult.get("stream_id")), deleteResult, error);
				return ResponseEntity.status(statusCode).build();
			}

			case "PATCH": {
				if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
					return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
				}
				callAndContinueOnFailure(OIDSSFHandleStreamRequestBodyParsing.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
				callAndContinueOnFailure(OIDSSFHandleStreamUpdateRequestValidation.class, Condition.ConditionResult.FAILURE,"OIDSSF-8.1.1.3");
				callAndContinueOnFailure(OIDSSFHandleStreamUpdateRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
				JsonObject updateResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = updateResult.get("error");
				afterStreamUpdate(OIDFJSON.tryGetString(updateResult.get("stream_id")), updateResult, error);
				return handleResultWithBody(updateResult);
			}

			case "PUT": {
				if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
					return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
				}
				callAndContinueOnFailure(OIDSSFHandleStreamRequestBodyParsing.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.4");
				callAndContinueOnFailure(OIDSSFHandleStreamUpdateRequestValidation.class, Condition.ConditionResult.FAILURE,"OIDSSF-8.1.1.4");
				callAndContinueOnFailure(OIDSSFHandleStreamReplaceRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.4");
				JsonObject replaceResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = replaceResult.get("error");
				afterStreamReplace(OIDFJSON.tryGetString(replaceResult.get("stream_id")), replaceResult, error);
				return handleResultWithBody(replaceResult);
			}
		}

		return (ResponseEntity<?>) super.handleHttp(path, req, res, session, requestParts);
	}

	protected void afterStreamLookup(String streamId, JsonObject lookupResult, JsonElement error) {
		// NOOP
	}

	protected void afterStreamReplace(String streamId, JsonObject replaceResult, JsonElement error) {
		// NOOP
	}

	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		// NOOP
	}

	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {
		// NOOP
	}

	protected void afterStreamUpdate(String streamId, JsonObject updateResult, JsonElement error) {
		// NOOP
	}

	/**
	 * Whether the transmitter emulator should deliver an unsolicited stream
	 * verification event — carrying no {@code state} claim — immediately after
	 * a stream is successfully created. See SSF 1.0 §8.1.4-2: a transmitter MAY
	 * deliver a verification event at any time after stream creation, and the
	 * {@code state} member is optional.
	 * <p>
	 * Defaults to {@code false} so existing tests are unaffected. Subclasses may
	 * override to opt in.
	 */
	protected boolean shouldDeliverUnsolicitedStreamVerificationAfterStreamCreation() {
		return false;
	}

	/**
	 * Generates a stream verification SET for {@code streamId} without a
	 * {@code state} claim and enqueues it in the event store. For push delivery,
	 * schedules an immediate push delivery task. For poll delivery, the event
	 * becomes available on the next poll request.
	 * <p>
	 * Spec references:
	 * <ul>
	 *   <li>SSF 1.0 §8.1.4 — "A Transmitter MAY send a Verification Event at any
	 *       time, even if one was not requested by the Event Receiver."
	 *   <li>SSF 1.0 §8.1.4.2 — "If the Verification Event is initiated by the
	 *       Transmitter then this parameter [state] MUST not be set."
	 * </ul>
	 */
	protected void deliverUnsolicitedStreamVerificationEvent(String streamId) {
		callAndContinueOnFailure(new OIDSSFGenerateUnsolicitedStreamVerificationSET(eventStore, streamId),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4", "OIDSSF-8.1.4.2");

		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (OIDSSFStreamUtils.isPushDelivery(streamConfig)) {
			scheduleTask(new OIDSSFHandlePushDeliveryTask(streamId), 1, TimeUnit.SECONDS);
		}
	}

	protected ResponseEntity<?> handleResultWithBody(JsonObject createResult) {
		JsonElement result = createResult.get("result");
		JsonElement error = createResult.get("error");
		int statusCode = OIDFJSON.getInt(createResult.get("status_code"));

		if (error != null) {
			return ResponseEntity.status(statusCode).build();
		}

		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
	}


	protected ResponseEntity<?> handleSubjectsEndpointRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts, StreamSubjectOperation operation) {

		String method = req.getMethod();
		if (!method.equals("POST")) {
			return (ResponseEntity<?>) super.handleHttp(path, req, res, session, requestParts);
		}

		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
		}

		switch (operation) {
			case add -> callAndContinueOnFailure(OIDSSFHandleStreamSubjectAdd.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.3.2");
			case remove -> callAndContinueOnFailure(OIDSSFHandleStreamSubjectRemove.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.3.3");
		}

		JsonObject subjectChangeResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();

		JsonElement result = subjectChangeResult.get("result");
		int statusCode = OIDFJSON.getInt(subjectChangeResult.get("status_code"));

		if (result == null) {
			return ResponseEntity.status(statusCode).build();
		}

		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	protected ResponseEntity<?> handleVerificationEndpointRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		String method = req.getMethod();
		if (!method.equals("POST")) {
			return (ResponseEntity<?>) super.handleHttp(path, req, res, session, requestParts);
		}

		callAndContinueOnFailure(OIDSSFHandleStreamVerificationRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2");
		JsonObject verificationResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();

		JsonElement result = verificationResult.get("result");
		int statusCode = OIDFJSON.getInt(verificationResult.get("status_code"));

		if (HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
			callAndStopOnFailure(new OIDSSFGenerateStreamVerificationSET(eventStore), "OIDSSF-8.1.4.2");

			String streamId = env.getString("incoming_request", "body_json.stream_id");

			if (OIDSSFStreamUtils.isPushDelivery(OIDSSFStreamUtils.getStreamConfig(env, streamId))) {
				scheduleTask(new OIDSSFHandlePushDeliveryTask(streamId), 1, java.util.concurrent.TimeUnit.SECONDS);
			}
		}

		if (result == null) {
			return ResponseEntity.status(statusCode).build();
		}

		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	protected class OIDSSFHandlePushDeliveryTask implements Callable<String> {

		protected final String streamId;

		protected OIDSSFHandlePushDeliveryTask(String streamId) {
			this.streamId = streamId;
		}

		@Override
		public String call() throws Exception {
			OIDSSFEventStore.EventsBatch eventsBatch = eventStore.pollEvents(streamId, 16);
			if (eventsBatch == null) {
				// stream was removed in-between, so we don't need to push data
				return "done";
			}

			// TODO handle SSF PUSH retry???
			for (var event : eventsBatch.events()) {
				callAndContinueOnFailure(new OIDSSFHandlePushDeliveryToReceiver(streamId, event, AbstractOIDSSFReceiverTestModule.this::afterPushDeliverySuccess), Condition.ConditionResult.WARNING, "OIDSSF-6.1.1");
				// RFC 8935 §2.2: "the SET Recipient SHALL acknowledge successful
				// transmission by responding with HTTP Response Status Code 202 (Accepted)."
				// SHALL → FAILURE severity per the conformance-suite convention.
				callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(202), Condition.ConditionResult.FAILURE, "RFC8935-2.2");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// Test finished during the delay — exit gracefully
					Thread.currentThread().interrupt();
					return "done";
				}
			}

			if (eventsBatch.moreAvailable() || eventStore.hasEventsForStream(streamId)) {
				// Reschedule to deliver remaining events. The queue check covers events
				// enqueued by delivery callbacks (e.g. afterPushDeliverySuccess generating
				// new SETs after seeing the verification event) that were not yet visible
				// when the original batch was polled.
				scheduleTask(this, 1, TimeUnit.SECONDS);
			}
			return "done";
		}
	}

	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		// NOOP
	}

	protected ResponseEntity<?> handleStreamStatusEndpointRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String method = req.getMethod();
		if (!Set.of("GET", "POST").contains(method)) {
			return (ResponseEntity<?>) super.handleHttp(path, req, res, session, requestParts);
		}

		boolean isReadStreamStatus = method.equals("GET");
		boolean isUpdateStreamStatus = method.equals("POST");

		if (isReadStreamStatus) {
			callAndContinueOnFailure(OIDSSFHandleStreamStatusLookup.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1");
		} else if (isUpdateStreamStatus) {
			if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
				return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
			}
			callAndContinueOnFailure(OIDSSFHandleStreamStatusUpdateRequestParsing.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.2");
			callAndContinueOnFailure(OIDSSFHandleStreamStatusUpdateRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.2");
		}

		JsonObject statusOpResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();

		JsonElement result = statusOpResult.get("result");
		int statusCode = OIDFJSON.getInt(statusOpResult.get("status_code"));

		if (result == null) {
			return ResponseEntity.status(statusCode).build();
		}

		if (isUpdateStreamStatus) {
			onStreamStatusUpdateSuccess(OIDFJSON.tryGetString(statusOpResult.get("stream_id")), statusOpResult);
		} else {
			onStatusStatusLookup(OIDFJSON.tryGetString(statusOpResult.get("stream_id")), statusOpResult);
		}

		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	protected void onStatusStatusLookup(String streamId, JsonObject statusOpResult) {
		// NOOP
	}

	protected void onStreamStatusUpdateSuccess(String streamId, JsonElement result) {
		// NOOP
	}

	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		// NOOP
	}

	protected void onStreamEventEnqueued(String streamId, String jti) {
		// NOOP
	}

	protected abstract boolean isFinished();

	protected ResponseEntity<?> handleStreamPollingRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String method = req.getMethod();
		if (!Objects.equals("POST", method)) {
			return (ResponseEntity<?>) super.handleHttp(path, req, res, session, requestParts);
		}

		callAndContinueOnFailure(new OIDSSFHandlePollRequest(eventStore, this::onStreamEventAcknowledged), Condition.ConditionResult.FAILURE, "OIDSSF-6.1.2", "RFC8936-2.4");

		JsonObject pollResult = env.getElementFromObject("ssf", "poll_result").getAsJsonObject();

		JsonElement result = pollResult.get("result");
		int statusCode = OIDFJSON.getInt(pollResult.get("status_code"));

		if (result == null) {
			return ResponseEntity.status(statusCode).build();
		}

		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	@Override
	public void cleanup() {
		super.cleanup();
		eventStore.cleanup();
	}

	protected class CheckTestFinishedTask implements Callable<String> {

		protected final Supplier<Boolean> finishedCondition;
		protected final boolean reschedule;

		CheckTestFinishedTask(Supplier<Boolean> finishedCondition, boolean reschedule) {
			this.reschedule = reschedule;
			this.finishedCondition = finishedCondition;
		}

		CheckTestFinishedTask(Supplier<Boolean> finishedCondition) {
			this(finishedCondition, true);
		}

		@Override
		public String call() throws Exception {

			if (finishedCondition.get()) {
				fireTestFinished();
				return "done";
			}

			if (reschedule) {
				reschedule();
			}

			return "done";
		}

		protected void reschedule() {
			scheduleTask(this, 1, TimeUnit.SECONDS);
		}
	}

	protected SsfEvent generateSsfEventExample(String eventType, long timestamp) {
		return switch (eventType) {

			// Examples from CAEP spec below: https://openid.net/specs/openid-caep-1_0-final.html

			case SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
				"initiating_entity", "policy",
				"reason_admin", Map.of("en", "Policy Violation: C076E822"),
				"reason_user", Map.of("en", "This device is no longer compliant.", "it", "Questo dispositivo non e piu conforme."))
				, Set.of("OIDCAEP-3.1", "CAEPIOP-3.1"));

			case SsfEvents.CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp, "claims", Map.of("role", "ro-admin"))
				, Set.of("OIDCAEP-3.2"));

			case SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
				"credential_type", "fido2-roaming",
				"change_type", "create",
				"fido2_aaguid", "accced6a-63f5-490a-9eea-e59bc1896cfc",
				"friendly_name", "Jane's USB authenticator",
				"initiating_entity", "user",
				"reason_admin", Map.of("en", "User self-enrollment"))
				, Set.of("OIDCAEP-3.3", "CAEPIOP-3.2"));

			case SsfEvents.CAEP_ASSURANCE_LEVEL_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
				"namespace", "NIST-AAL",
				"current_level", "nist-aal2",
				"previous_level", "nist-aal1",
				"change_direction", "increase",
				"initiating_entity", "user")
				, Set.of("OIDCAEP-3.4"));

			case SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
				"current_status", "not-compliant",
				"previous_status", "compliant",
				"initiating_entity", "policy",
				"reason_admin", Map.of("en", "Location Policy Violation: C076E8A3"),
				"reason_user", Map.of("en", "Device is no longer in a trusted location."))
				, Set.of("OIDCAEP-3.5", "CAEPIOP-3.3")
			);

			case SsfEvents.CAEP_SESSION_ESTABLISHED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
				"fp_ua", "abb0b6e7da81a42233f8f2b1a8ddb1b9a4c81611",
				"acr", "AAL2",
				"amr", List.of("otp"))
				, Set.of("OIDCAEP-3.6"));

			case SsfEvents.CAEP_SESSION_PRESENTED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
				"fp_ua", "abb0b6e7da81a42233f8f2b1a8ddb1b9a4c81611",
				"ext_id", "12345")
				, Set.of("OIDCAEP-3.7"));

			case SsfEvents.CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
				"current_level", "LOW",
				"previous_level", "HIGH",
				"initiating_entity", "user",
				"principal", "USER",
				"risk_reason", "PASSWORD_FOUND_IN_DATA_BREACH")
				, Set.of("OIDCAEP-3.8"));

			// Examples from RISC spec below: https://openid.net/specs/openid-risc-1_0-final.html

			case SsfEvents.RISC_ACCOUNT_CREDENTIAL_CHANGE_REQUIRED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.1"));

			case SsfEvents.RISC_ACCOUNT_PURGED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.2"));

			case SsfEvents.RISC_ACCOUNT_DISABLED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("reason", "hijacking")
				, Set.of("OIDRISC-2.3"));

			case SsfEvents.RISC_ACCOUNT_ENABLED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.4"));

			case SsfEvents.RISC_IDENTIFIER_CHANGED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("new-value", "new-valid")
				, Set.of("OIDRISC-2.5"));

			case SsfEvents.RISC_IDENTIFIER_RECYCLED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.6"));

			case SsfEvents.RISC_CREDENTIAL_COMPROMISE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("credential_type", "password")
				, Set.of("OIDRISC-2.7"));

			case SsfEvents.RISC_OPT_IN_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.8.1"));

			case SsfEvents.RISC_OPT_OUT_INITIATED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.8.2"));

			case SsfEvents.RISC_OPT_OUT_CANCELLED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.8.3"));

			case SsfEvents.RISC_OPT_OUT_EFFECTIVE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.8.4"));

			case SsfEvents.RISC_RECOVERY_ACTIVATED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.9"));

			case SsfEvents.RISC_RECOVERY_INFORMATION_CHANGED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.10"));

			case SsfEvents.RISC_SESSIONS_REVOKED_DEPRECATED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.11", "OIDCAEP-3.1"));

			default -> new SsfEvent(eventType, Map.of(), Set.of());
		};
	}
}

package net.openid.conformance.openid.ssf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.GenerateAccessTokenExpiration;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIsAnyOf;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.condition.common.CheckIncomingRequestMethodIsGet;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFGenerateServerJWKs;
import net.openid.conformance.openid.ssf.conditions.as.OIDSSFStoreIssuedAccessToken;
import net.openid.conformance.openid.ssf.conditions.as.OIDSSFStopOnFirstFailureSequence;
import net.openid.conformance.openid.ssf.conditions.as.OIDSSFValidateRequestedScope;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureTokenScopeSufficient;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamVerificationSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateUnsolicitedStreamVerificationSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamDeliveryMethodMatchesVariant;
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
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFWarnTransmitterSuppliedPropertiesInStreamCreateRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamSubjectOperation;
import net.openid.conformance.openid.ssf.conditions.subjects.OIDSSFResolveEventSubjects;
import net.openid.conformance.openid.ssf.conditions.subjects.OIDSSFWarnCaepInteropComplexSubjectsConfigured;
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
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.util.BaseUrlUtil;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.OAuthUriUtil;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantNotApplicableWhen;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;
import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI;

@VariantParameters({SsfProfile.class, SsfDeliveryMode.class, SsfAuthMode.class, ClientAuthType.class,})
@ConfigurationFields({
	"ssf.stream.audience",
	"ssf.subjects.valid",
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
// In static mode the hidden ClientAuthType dropdown is still auto-selected to satisfy
// validation, so its per-value fields above would otherwise leak into the config form.
@VariantHidesConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {
	"client.client_secret",
	"client.client_secret_jwt_alg",
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
	 * {@code jti} values of generated events that were never delivered because the receiver
	 * deleted the stream while they were still queued, see
	 * {@link #onEventsUndeliverable(String, List)}.
	 */
	protected final Set<String> undeliveredEventJtis = ConcurrentHashMap.newKeySet();

	/**
	 * {@code jti} values of events the receiver retrieved via poll but neither acknowledged
	 * nor reported via {@code setErrs} before deleting the stream, see
	 * {@link #onEventsUnresolvedAtDeletion(String, List)}. Test modules should stop waiting
	 * for acknowledgements of these events but still grade the missing acknowledgement.
	 */
	protected final Set<String> unresolvedEventJtis = ConcurrentHashMap.newKeySet();

	/**
	 * {@code jti} values of events the receiver resolved by reporting an error via the
	 * {@code setErrs} member of a poll request (RFC 8936 2.4). An error report is a valid
	 * resolution of a SET: no acknowledgement will ever arrive for these events.
	 */
	protected final Set<String> errorReportedEventJtis = ConcurrentHashMap.newKeySet();

	/**
	 * {@code jti} values of events whose push delivery the receiver answered with a non-2xx
	 * status (or no HTTP response at all), see {@link #onPushDeliveryNotAcknowledged(String,
	 * OIDSSFSecurityEvent)}. The RFC 8935 2.2 status check grades these per delivery; the
	 * set only lets test modules stop waiting for acknowledgements that will never arrive.
	 */
	protected final Set<String> rejectedPushEventJtis = ConcurrentHashMap.newKeySet();

	/**
	 * Streams with a push delivery task scheduled or running. Delivery is single-flight per
	 * stream: a second trigger (a verification request, newly generated events, a re-enabled
	 * stream) while a task is active does not start another one, the running task picks the
	 * new events up and reschedules itself when it drains the queue.
	 */
	private final Set<String> pushDeliveryActive = ConcurrentHashMap.newKeySet();

	/** Starts push delivery for the stream unless a delivery task is already active for it. */
	protected void schedulePushDelivery(String streamId) {
		if (pushDeliveryActive.add(streamId)) {
			scheduleTask(new OIDSSFHandlePushDeliveryTask(streamId), 1, TimeUnit.SECONDS);
		}
	}

	/**
	 * Set once the receiver fetches the emulated transmitter's signing keys from the
	 * advertised jwks_uri, see {@link #isJwksEndpointFetched()}.
	 */
	protected volatile boolean jwksEndpointFetched;

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
		env.putArray("ssf", "delivery_methods_supported", OIDFJSON.convertListToJsonArray(getSupportedDeliveryMethods()));

		env.putString("ssf", "auth_mode", getVariant(SsfAuthMode.class).name());
		configureAuthorizationServer(issuer);

		resolveEventSubjects();
	}

	/**
	 * Resolves and validates the subject identifiers declared in the 'SSF valid SubjectId'
	 * configuration field (see {@link OIDSSFResolveEventSubjects}).
	 * Runs at configuration time so a misconfigured subject fails fast with a message pointing
	 * at the test configuration field.
	 */
	protected void resolveEventSubjects() {
		callAndStopOnFailure(OIDSSFResolveEventSubjects.class, "RFC9493-3", "CAEPIOP-2.5");

		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			// Complex Subjects are not yet listed in CAEPIOP §2.5 but expected to be permitted
			// (openid/sharedsignals#351): warn once at configuration time when any are declared.
			// Lower the severity to INFO once the profile permits them.
			callAndContinueOnFailure(OIDSSFWarnCaepInteropComplexSubjectsConfigured.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.5", "OIDSSF-3.3");
		}
	}

	/**
	 * The valid (existing) subject identifiers the receiver under test declared, to generate
	 * (non-verification) events for — as resolved by {@link #resolveEventSubjects()}. Under the
	 * CAEP Interop Profile this covers at least one {@code email} and one {@code iss_sub} subject.
	 */
	protected List<JsonObject> getEventSubjects() {
		JsonElement eventSubjects = env.getElementFromObject("ssf", "event_subjects");
		if (eventSubjects == null || !eventSubjects.isJsonArray()) {
			throw new IllegalStateException("Event subjects have not been resolved, resolveEventSubjects() must run during configuration");
		}
		List<JsonObject> subjects = new ArrayList<>();
		for (JsonElement subject : eventSubjects.getAsJsonArray()) {
			subjects.add(subject.getAsJsonObject());
		}
		return subjects;
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

		// Internal developer knob: deliberately NOT declared in @ConfigurationFields /
		// the config-field catalog - it exists for suite development setups only.
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

		// SSF 1.0 8.1.1.1: a stream created without a delivery object defaults to POLL
		// delivery, regardless of the variant under test - so the poll endpoint URL must
		// always be available (otherwise the stream config would advertise
		// "null?stream_id=..." as the poll endpoint_url).
		String pollEndpointUrl = ssfIssuer + "/events";
		env.putString("ssf", "poll_endpoint_url", pollEndpointUrl);

		if (Objects.requireNonNull(getVariant(SsfDeliveryMode.class)) == SsfDeliveryMode.POLL) {
			exposeEnvString("ssf_poll_endpoint", "ssf", "poll_endpoint_url");
		}
	}

	/**
	 * The emulated transmitter supports only the delivery method the run was scheduled with:
	 * the variant is the contract of the run, so a receiver that picks its method from
	 * {@code delivery_methods_supported} is steered to it, and one that requests the other
	 * method is refused with a 400 (SSF 1.0 8.1.1.1). Both methods when no variant is set.
	 */
	protected List<String> getSupportedDeliveryMethods() {
		SsfDeliveryMode deliveryMode = getVariantOrDefault(SsfDeliveryMode.class, null);
		if (deliveryMode == null) {
			return List.of(DELIVERY_METHOD_PUSH_RFC_8935_URI, DELIVERY_METHOD_POLL_RFC_8936_URI);
		}
		return List.of(deliveryMode.getAlias());
	}

	protected JsonObject generateTransmitterMetadata(String issuer) {

		JsonObject metadata = new JsonObject();

		metadata.addProperty("issuer", issuer);
		metadata.addProperty("spec_version", "1_0");
		metadata.addProperty("jwks_uri", issuer + "/jwks");
		metadata.add("delivery_methods_supported", OIDFJSON.convertListToJsonArray(getSupportedDeliveryMethods()));

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
				case "events" -> response = ensureAuthorized(path, req, res, session, requestParts, () -> {
					return handleStreamPollingRequest(path, req, res, session, requestParts);
				});
				case "streams" -> response = ensureAuthorized(path, req, res, session, requestParts, () -> {
					return handleStreamConfigurationEndpointRequest(path, req, res, session, requestParts);
				});
				case "status" -> response = ensureAuthorized(path, req, res, session, requestParts, () -> {
					return handleStreamStatusEndpointRequest(path, req, res, session, requestParts);
				});
				case "verify" -> response = ensureAuthorized(path, req, res, session, requestParts, () -> {
					return handleVerificationEndpointRequest(path, req, res, session, requestParts);
				});
				case "add_subject" -> response = ensureAuthorized(path, req, res, session, requestParts, () -> {
					return handleSubjectsEndpointRequest(path, req, res, session, requestParts, StreamSubjectOperation.add);
				});
				case "remove_subject" -> response = ensureAuthorized(path, req, res, session, requestParts, () -> {
					return handleSubjectsEndpointRequest(path, req, res, session, requestParts, StreamSubjectOperation.remove);
				});
				// This handler already holds the test lock in RUNNING state, so the stray request
				// is graded in place; the base class's own RUNNING transition would trip the
				// status machine and end the test INTERRUPTED.
				default -> response = reportUnexpectedHttpRequest(path, requestParts);
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

	/**
	 * Delay before generating events once the receiver acknowledged the stream verification
	 * event, see {@link #scheduleAfterStreamVerification(Runnable)}.
	 */
	protected static final int POST_VERIFICATION_EVENT_GENERATION_DELAY_SECONDS = 1;

	/**
	 * Runs the post-verification event generation in a background task instead of inline.
	 * <p>
	 * With POLL delivery the "receiver acknowledged the verification event" cue is raised while
	 * the receiver's poll request is being handled (see {@code OIDSSFHandlePollRequest}).
	 * Generating the events there would hold that HTTP request open for the duration and would
	 * return the freshly generated SETs in the response to the very request that carried the
	 * acknowledgement - i.e. as a combined acknowledge-and-poll response (RFC 8936 2.4).
	 * Neither is something the CAEP Interop Profile requires receivers to support, so the
	 * emulated transmitter behaves like a real one instead: it answers the poll first, and the
	 * events become available for the receiver's next poll.
	 * <p>
	 * With PUSH delivery the cue is raised on the background push task, so callers there can
	 * generate events directly.
	 */
	protected void scheduleAfterStreamVerification(Runnable generateEvents) {
		scheduleTask(() -> {
			generateEvents.run();
			return "done";
		}, POST_VERIFICATION_EVENT_GENERATION_DELAY_SECONDS, TimeUnit.SECONDS);
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

	protected ResponseEntity<?> ensureAuthorized(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts, Supplier<ResponseEntity<?>> requestHandler) {

		// CAEPIOP §2.7.2 "The SSF Transmitter as a Resource Server": validate the bearer token.
		callAndStopOnFailure(OIDSSFHandleAuthorizationHeader.class, "CAEPIOP-2.7.2");
		JsonObject authResult = env.getElementFromObject("ssf", "auth_result").getAsJsonObject();
		if (authResult.has("error")) {
			return errorResponseFromAuthResult(authResult);
		}

		// CAEPIOP §2.7.2: the resource server MUST verify the token's authorization is
		// sufficient for the requested access. Only meaningful in dynamic mode, where the
		// token carries a granted scope (ssf.read for read/status, ssf.manage for management).
		if (SsfAuthMode.DYNAMIC.name().equals(env.getString("ssf", "auth_mode"))) {
			String requiredScope = requiredScopeForOperation(path, req.getMethod());
			if (requiredScope != null) {
				callAndStopOnFailure(new OIDSSFEnsureTokenScopeSufficient(requiredScope), "CAEPIOP-2.7.2", "CAEPIOP-2.7.3");
				if (authResult.has("error")) {
					return errorResponseFromAuthResult(authResult);
				}
			}
		}

		return requestHandler.get();
	}

	protected ResponseEntity<?> errorResponseFromAuthResult(JsonObject authResult) {
		int statusCode = OIDFJSON.getInt(authResult.get("status_code"));
		ResponseEntity.BodyBuilder response = ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON);
		// RFC 6750 3: 401/403 responses to a bearer-token request carry a WWW-Authenticate
		// challenge (CAEP Interop Profile 2.7.2 requires RFC 6750 3.1 errors)
		JsonElement wwwAuthenticate = authResult.get("www_authenticate");
		if (wwwAuthenticate != null) {
			response = response.header(HttpHeaders.WWW_AUTHENTICATE, OIDFJSON.getString(wwwAuthenticate));
		}
		return response.body(authResult.get("error").getAsJsonObject());
	}

	/**
	 * Maps an SSF API request to the OAuth scope it requires (CAEPIOP §2.7.3):
	 * {@code ssf.read} for stream/status read operations, {@code ssf.manage} for
	 * stream-management operations (create/update/replace/delete, verification,
	 * subject changes). Returns {@code null} for paths that are not scope-gated.
	 */
	protected String requiredScopeForOperation(String path, String method) {
		return switch (path) {
			case "streams", "status" -> "GET".equals(method) ? SsfConstants.SCOPE_SSF_READ : SsfConstants.SCOPE_SSF_MANAGE;
			case "verify", "add_subject", "remove_subject" -> SsfConstants.SCOPE_SSF_MANAGE;
			case "events" -> SsfConstants.SCOPE_SSF_READ;
			default -> null;
		};
	}

	@Override
	public Object handleWellKnown(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (isFinished()) {
			// as in handleHttp: a receiver re-reading the metadata after the test finished must not
			// trip the status machine (FINISHED -> RUNNING)
			return ResponseEntity.noContent().build();
		}

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
				// see handleHttp: the lock is held and the status is RUNNING already
				response = reportUnexpectedHttpRequest(path, requestParts);
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
		// CAEP Interop Profile 2.4.2: the receiver MUST obtain the transmitter's signing
		// key(s) via the advertised jwks_uri - record the fetch so tests can assert it.
		jwksEndpointFetched = true;
		// Serve only the public keys at the transmitter jwks_uri - it must not leak private key material.
		JsonObject publicJwks = JWKUtil.toPublicJWKSet(env.getObject("server_jwks"));
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(publicJwks);
	}

	/**
	 * Whether the receiver fetched the transmitter's signing keys from the advertised
	 * jwks_uri at least once during the test run (CAEP Interop Profile 2.4.2).
	 */
	protected boolean isJwksEndpointFetched() {
		return jwksEndpointFetched;
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
				// Receivers may request a fresh token per SSF operation; the extract
				// conditions refuse to overwrite a client_authentication left over
				// from an earlier token request, so clear it before each validation.
				env.removeObject("client_authentication");
				try {
					// The shared sequences continue past a failed step (they serve modules that
					// test the client); this emulated authorization server must refuse the token
					// instead, so run them stopping at the first failure and answer invalid_client.
					call(new OIDSSFStopOnFirstFailureSequence(validateClientAuthenticationSteps));
				} catch (TestFailureException e) {
					if (e.getCause() instanceof ConditionError conditionError && !conditionError.isPreOrPostError()) {
						// A stop-on-failure condition has logged its failure but leaves the result
						// update to the exception it throws; record the refusal explicitly instead.
						callAndContinueOnFailure(new OIDSSFFindingCondition(
								"Refused to issue an access token: client authentication failed, see the preceding failure. "
									+ "The emulated authorization server answers invalid_client (RFC 6749 5.2)."),
							Condition.ConditionResult.FAILURE, "RFC6749-5.2");
						return clientAuthenticationFailed();
					}
					throw e;
				}
			}

			callAndStopOnFailure(OIDSSFValidateRequestedScope.class, "CAEPIOP-2.7.3");
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

	/**
	 * RFC 6749 5.2: {@code invalid_client} - "Client authentication failed". The authorization
	 * server MAY answer 401; if the client authenticated via the Authorization header it MUST,
	 * and MUST include a WWW-Authenticate header matching the client's scheme.
	 */
	protected ResponseEntity<?> clientAuthenticationFailed() {
		JsonObject body = new JsonObject();
		body.addProperty("error", "invalid_client");
		body.addProperty("error_description", "Client authentication failed");
		ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.header("Cache-Control", "no-store")
			.header("Pragma", "no-cache");
		if (OIDCCValidateClientAuthenticationWithClientSecretBasic.class.equals(validateClientAuthenticationSteps)) {
			response.header("WWW-Authenticate", "Basic realm=\"" + env.getString("ssf", "issuer") + "\"");
		}
		return response.contentType(MediaType.APPLICATION_JSON).body(body);
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
				callAndContinueOnFailure(OIDSSFWarnTransmitterSuppliedPropertiesInStreamCreateRequest.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
				callAndContinueOnFailure(OIDSSFHandleStreamCreateRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
				JsonObject createResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = createResult.get("error");
				String createdStreamId = OIDFJSON.tryGetString(createResult.get("stream_id"));
				if (error == null && createdStreamId != null) {
					// the scheduled delivery mode is the contract of the run for every receiver module
					callAndContinueOnFailure(new OIDSSFEnsureStreamDeliveryMethodMatchesVariant(createdStreamId, getVariant(SsfDeliveryMode.class)),
						Condition.ConditionResult.FAILURE, deliveryModeRequirements());
				}
				afterStreamCreation(createdStreamId, createResult, error);

				if (error == null && createdStreamId != null
					&& shouldDeliverUnsolicitedStreamVerificationAfterStreamCreation()) {
					deliverUnsolicitedStreamVerificationEvent(createdStreamId);
				}

				return handleResultWithBody(createResult);
			}

			case "DELETE": {
				callAndContinueOnFailure(new OIDSSFHandleStreamDeleteRequest(eventStore, this::onEventsUndeliverable, this::onEventsUnresolvedAtDeletion), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");

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

		return reportUnexpectedHttpRequest(path, requestParts);
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
			schedulePushDelivery(streamId);
		}
	}

	private String[] deliveryModeRequirements() {
		return isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)
			? new String[] {"OIDSSF-8.1.1.1", "CAEPIOP-2.4.5.1"}
			: new String[] {"OIDSSF-8.1.1.1"};
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
			return reportUnexpectedHttpRequest(path, requestParts);
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
			return reportUnexpectedHttpRequest(path, requestParts);
		}

		callAndContinueOnFailure(OIDSSFHandleStreamVerificationRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2");
		JsonObject verificationResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();

		JsonElement result = verificationResult.get("result");
		int statusCode = OIDFJSON.getInt(verificationResult.get("status_code"));

		if (HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
			callAndStopOnFailure(new OIDSSFGenerateStreamVerificationSET(eventStore), "OIDSSF-8.1.4.2");

			String streamId = env.getString("incoming_request", "body_json.stream_id");

			if (OIDSSFStreamUtils.isPushDelivery(OIDSSFStreamUtils.getStreamConfig(env, streamId))) {
				schedulePushDelivery(streamId);
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
			try {
				deliverQueuedEvents();
			} finally {
				pushDeliveryActive.remove(streamId);
			}
			// Events enqueued after the queue was found empty (the check and this hand-over
			// both run under the test lock) start a fresh task.
			JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
			if (streamConfig != null && OIDSSFStreamUtils.getStreamStatusValue(streamConfig).isEventDeliveryEnabled()
				&& eventStore.hasEventsForStream(streamId)
				&& !Set.of(Status.FINISHED, Status.INTERRUPTED).contains(getStatus())) {
				schedulePushDelivery(streamId);
			}
			return "done";
		}

		private void deliverQueuedEvents() {
			// TODO handle SSF PUSH retry???
			// Events are taken from the queue one at a time, so a stream the receiver pauses,
			// disables or deletes between two deliveries leaves the rest queued: SSF 1.0
			// 8.1.2.1 says a paused or disabled stream "MUST NOT transmit events" and a paused
			// one "SHOULD hold" them, and the stream-delete handler records queued events as
			// undeliverable when it purges the store.
			while (true) {
				JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
				if (streamConfig == null) {
					return;
				}
				OIDSSFStreamUtils.StreamStatusValue streamStatus = OIDSSFStreamUtils.getStreamStatusValue(streamConfig);
				if (!streamStatus.isEventDeliveryEnabled()) {
					if (eventStore.hasEventsForStream(streamId)) {
						eventLog.log(getName(), args("msg", "Stream is " + streamStatus + ": holding queued events until the receiver enables the stream again (SSF 1.0 8.1.2.1)",
							"stream_id", streamId, "held_events", eventStore.getQueuedEvents(streamId).size()));
					}
					return;
				}

				OIDSSFEventStore.EventsBatch eventsBatch = eventStore.pollEvents(streamId, 1);
				if (eventsBatch == null || eventsBatch.events().isEmpty()) {
					return;
				}
				OIDSSFSecurityEvent event = eventsBatch.events().get(0);

				callAndContinueOnFailure(new OIDSSFHandlePushDeliveryToReceiver(streamId, event,
					AbstractOIDSSFReceiverTestModule.this::afterPushDeliverySuccess,
					AbstractOIDSSFReceiverTestModule.this::onPushDeliveryNotAcknowledged), Condition.ConditionResult.WARNING, "OIDSSF-6.1.1");
				// RFC 8935 §2.2: "the SET Recipient SHALL acknowledge successful
				// transmission by responding with HTTP Response Status Code 202 (Accepted)."
				// SHALL → FAILURE severity per the conformance-suite convention, unless the
				// module knows the receiver may legitimately reject this particular SET.
				callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(202), getPushDeliveryRejectionSeverity(event), "RFC8935-2.2");
				// Pace the deliveries with the test lock released: this task holds the lock in
				// RUNNING state, and a raw sleep here would stall every request the receiver
				// makes in the meantime.
				callAndContinueOnFailure(WaitForOneSecond.class, Condition.ConditionResult.INFO);
				if (Set.of(Status.FINISHED, Status.INTERRUPTED).contains(getStatus())) {
					// Test finished during the delay — exit gracefully
					return;
				}
			}
		}
	}

	/**
	 * Restarts push delivery for events held while the stream was paused or disabled, once the
	 * receiver enables it again (SSF 1.0 8.1.2.1: held events "SHOULD [be transmitted] when the
	 * stream's status becomes enabled"). Poll streams need nothing: the next poll returns them.
	 */
	protected void resumePushDeliveryIfEnabled(String streamId) {
		JsonObject streamConfig = streamId == null ? null : OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null || !OIDSSFStreamUtils.isPushDelivery(streamConfig)) {
			return;
		}
		if (OIDSSFStreamUtils.getStreamStatusValue(streamConfig).isEventDeliveryEnabled() && eventStore.hasEventsForStream(streamId)) {
			eventLog.log(getName(), args("msg", "Stream enabled again: delivering the events held while it was paused or disabled",
				"stream_id", streamId, "held_events", eventStore.getQueuedEvents(streamId).size()));
			schedulePushDelivery(streamId);
		}
	}

	/**
	 * Records events that can no longer be delivered or acknowledged because the receiver
	 * deleted the stream: events still queued at deletion time (any delivery mode), events of
	 * the currently-executing push batch (reported by the push task), and - for poll delivery -
	 * events the receiver retrieved but never acknowledged. They will never be acknowledged, so
	 * {@link #getUndeliveredEventJtis()} lets {@code isFinished()} implementations stop waiting
	 * for them instead of blocking until the test times out. Invoked from both the push task
	 * and the stream-delete handler; the underlying set deduplicates overlapping reports.
	 */
	protected void onEventsUndeliverable(String streamId, List<OIDSSFSecurityEvent> events) {
		List<String> jtis = events.stream().map(OIDSSFSecurityEvent::jti).toList();
		undeliveredEventJtis.addAll(jtis);
		eventLog.log(getName(), args(
			"msg", "Stream was deleted before all generated events could be delivered, the remaining events will not be sent",
			"stream_id", streamId,
			"undelivered_event_count", jtis.size(),
			"undelivered_jtis", jtis));
	}

	/**
	 * The {@code jti} values of generated events that could not be delivered because the stream
	 * was deleted, see {@link #onEventsUndeliverable(String, List)}.
	 */
	protected Set<String> getUndeliveredEventJtis() {
		return Set.copyOf(undeliveredEventJtis);
	}

	/**
	 * Records events the receiver retrieved via poll but neither acknowledged nor reported via
	 * {@code setErrs} before deleting the stream. No acknowledgement can arrive for them any
	 * more, so {@code isFinished()} implementations should stop waiting for them - but unlike
	 * {@link #onEventsUndeliverable(String, List) undeliverable} events the receiver DID
	 * receive these, so the missing acknowledgement is the receiver's failure and should still
	 * be graded (RFC 8936 2.4).
	 */
	protected void onEventsUnresolvedAtDeletion(String streamId, List<OIDSSFSecurityEvent> events) {
		List<String> jtis = events.stream().map(OIDSSFSecurityEvent::jti).toList();
		unresolvedEventJtis.addAll(jtis);
		eventLog.log(getName(), args(
			"msg", "Stream was deleted with retrieved events that were neither acknowledged nor reported via setErrs",
			"stream_id", streamId,
			"unresolved_event_count", jtis.size(),
			"unresolved_jtis", jtis));
	}

	/**
	 * The {@code jti} values of events the receiver retrieved but never acknowledged nor
	 * reported before deleting the stream, see {@link #onEventsUnresolvedAtDeletion(String, List)}.
	 */
	protected Set<String> getUnresolvedEventJtis() {
		return Set.copyOf(unresolvedEventJtis);
	}

	/**
	 * The {@code jti} values of events the receiver resolved via {@code setErrs} instead of
	 * acknowledging them, see {@link #onStreamEventErrorReported(String, String, JsonObject)}.
	 */
	protected Set<String> getErrorReportedEventJtis() {
		return Set.copyOf(errorReportedEventJtis);
	}

	/**
	 * The {@code jti} values of events whose push delivery was answered with an error status,
	 * see {@link #onPushDeliveryNotAcknowledged(String, OIDSSFSecurityEvent)}.
	 */
	/**
	 * Severity of a push delivery the receiver did not answer with 202 (RFC 8935 2.2). FAILURE
	 * unless a module knows the receiver may legitimately reject the given SET, e.g. one whose
	 * subject format the certification target does not require it to accept.
	 */
	protected Condition.ConditionResult getPushDeliveryRejectionSeverity(OIDSSFSecurityEvent event) {
		return Condition.ConditionResult.FAILURE;
	}

	protected Set<String> getRejectedPushEventJtis() {
		return Set.copyOf(rejectedPushEventJtis);
	}

	/**
	 * All {@code jti} values for which no acknowledgement can arrive any more: events never
	 * delivered, events resolved via {@code setErrs}, push deliveries the receiver rejected,
	 * and events left unresolved when the stream was deleted. {@code isFinished()}
	 * implementations that wait for acknowledgements should subtract this set from the
	 * expected acks so the test finishes (and grades) instead of stalling until the global
	 * test timeout.
	 */
	protected Set<String> getResolvedWithoutAckJtis() {
		Set<String> resolved = new HashSet<>(undeliveredEventJtis);
		resolved.addAll(unresolvedEventJtis);
		resolved.addAll(errorReportedEventJtis);
		resolved.addAll(rejectedPushEventJtis);
		return resolved;
	}

	/**
	 * Called when a push delivery got an error status (or no HTTP response). The RFC 8935 2.2
	 * status check grades the delivery itself; this only records that no acknowledgement will
	 * ever arrive for the event so waiting test modules can finish instead of timing out.
	 */
	protected void onPushDeliveryNotAcknowledged(String streamId, OIDSSFSecurityEvent event) {
		rejectedPushEventJtis.add(event.jti());
	}

	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		// NOOP
	}

	protected ResponseEntity<?> handleStreamStatusEndpointRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String method = req.getMethod();
		if (!Set.of("GET", "POST").contains(method)) {
			return reportUnexpectedHttpRequest(path, requestParts);
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
			String updatedStreamId = OIDFJSON.tryGetString(statusOpResult.get("stream_id"));
			resumePushDeliveryIfEnabled(updatedStreamId);
			onStreamStatusUpdateSuccess(updatedStreamId, statusOpResult);
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

	/**
	 * Called when the receiver reports an error for a delivered SET via the
	 * {@code setErrs} member of a poll request (RFC 8936 2.4). An error report resolves the
	 * SET - no acknowledgement will follow - so the jti is recorded for
	 * {@link #getResolvedWithoutAckJtis()}. Overriding implementations should call
	 * {@code super} to keep that accounting intact.
	 */
	protected void onStreamEventErrorReported(String streamId, String jti, JsonObject error) {
		errorReportedEventJtis.add(jti);
	}

	protected abstract boolean isFinished();

	protected ResponseEntity<?> handleStreamPollingRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String method = req.getMethod();
		if (!Objects.equals("POST", method)) {
			return reportUnexpectedHttpRequest(path, requestParts);
		}

		callAndContinueOnFailure(new OIDSSFHandlePollRequest(eventStore, this::onStreamEventAcknowledged, this::onStreamEventErrorReported), Condition.ConditionResult.FAILURE, "OIDSSF-6.1.2", "RFC8936-2.4");

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
				"risk_reason", "PASSWORD_FOUND_IN_DATA_BREACH",
				"reason_admin", Map.of("en", "Credential no longer found in breach corpus"))
				// only OIDCAEP-3.8: risk-level-change is not a use case of the published
				// CAEP Interop draft-01 (CAEPIOP-3.4 exists only in the WG head)
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

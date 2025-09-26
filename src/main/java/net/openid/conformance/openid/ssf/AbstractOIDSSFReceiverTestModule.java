package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIsAnyOf;
import net.openid.conformance.condition.common.CheckIncomingRequestMethodIsGet;
import net.openid.conformance.openid.ssf.conditions.OIDSSFGenerateServerJWKs;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamVerificationSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleAuthorizationHeader;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePollRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePushDeliveryToReceiver;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamCreateRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamCreateRequestValidation;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamDeleteRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamDeleteRequestValidation;
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
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.BaseUrlUtil;
import net.openid.conformance.variant.VariantParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

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

@VariantParameters({SsfProfile.class, SsfDeliveryMode.class,})
public abstract class AbstractOIDSSFReceiverTestModule extends AbstractOIDSSFTestModule {

	protected OIDSSFEventStore eventStore;

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

		JsonObject transmitterMetadata = generateTransmitterMetadata(issuer);
		env.putObject("ssf", "transmitter_metadata", transmitterMetadata);

		JsonObject resourceServerMetadata = generateResourceServerMetadata(issuer);
		env.putObject("resource_server_metadata", resourceServerMetadata);
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

	protected JsonObject generateResourceServerMetadata(String issuer) {

		JsonObject resourceServerMetadata = new JsonObject();
		resourceServerMetadata.addProperty("resource", issuer);
		resourceServerMetadata.add("scopes_supported", OIDFJSON.convertListToJsonArray(List.of("ssf.read", "ssf.manage")));

		return resourceServerMetadata;
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

		metadata.addProperty("add_subject_endpoint", issuer + "/add_subject");
		metadata.addProperty("remove_subject_endpoint", issuer + "/remove_subject");

		metadata.addProperty("verification_endpoint", issuer + "/verify");

		JsonObject oauthAuthorizationScheme = new JsonObject();
		oauthAuthorizationScheme.addProperty("spec_urn", "urn:ietf:rfc:6749");
		metadata.add("authorization_schemes", OIDFJSON.convertJsonObjectListToJsonArray(List.of(oauthAuthorizationScheme)));

		return metadata;
	}

	public List<String> getEventsSupported() {
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			return List.copyOf(SsfEvents.CAEP_INTEROP_EVENT_TYPES);
		}
		return List.copyOf(SsfEvents.STANDARD_EVENT_TYPES);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		env.mapKey("incoming_request", requestId);

		setStatus(Status.RUNNING);

		Object response;
		try {
			switch (path) {
				case "ssf-configuration" -> response = handleSsfConfigurationEndpoint(requestId);
				case "jwks" -> response = handleJwksEndpoint();
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
			if (path.startsWith("/.well-known/oauth-protected-resource")) {
				response = oauthProtectedResourceServerMetadata();
			} else if (path.startsWith("/.well-known/ssf-configuration")) {
				response = handleSsfConfigurationEndpoint(requestId);
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
		JsonObject jwks = env.getObject("server_jwks");
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jwks);
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
				afterStreamCreation(OIDFJSON.tryGetString(createResult.get("stream_id")), createResult, error);

				return handleResultWithBody(createResult);
			}

			case "DELETE": {
				callAndContinueOnFailure(OIDSSFHandleStreamDeleteRequestValidation.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
				callAndContinueOnFailure(new OIDSSFHandleStreamDeleteRequest(eventStore), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");

				JsonObject deleteResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = deleteResult.get("error");
				int statusCode = OIDFJSON.getInt(deleteResult.get("status_code"));
				afterStreamDeletion(OIDFJSON.tryGetString(deleteResult.get("stream_id")), deleteResult, error);
				return ResponseEntity.status(statusCode).build();
			}

			case "PATCH": {
				callAndContinueOnFailure(OIDSSFHandleStreamRequestBodyParsing.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
				callAndContinueOnFailure(OIDSSFHandleStreamUpdateRequestValidation.class, Condition.ConditionResult.FAILURE,"OIDSSF-8.1.1.3");
				callAndContinueOnFailure(OIDSSFHandleStreamUpdateRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
				JsonObject updateResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = updateResult.get("error");
				afterStreamUpdate(OIDFJSON.tryGetString(updateResult.get("stream_id")), updateResult, error);
				return handleResultWithBody(updateResult);
			}

			case "PUT": {
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
				callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(200, 202),  Condition.ConditionResult.WARNING, "OIDSSF-8.1.2.2");
			}

			if (eventsBatch.moreAvailable()) {
				// reschedule push task to publish remaining events
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

	protected ResponseEntity<?> oauthProtectedResourceServerMetadata() {
		callAndContinueOnFailure(CheckIncomingRequestMethodIsGet.class, Condition.ConditionResult.FAILURE, "RFC9728-3.1");
		JsonObject resourceServerMetadata = env.getObject("resource_server_metadata");
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resourceServerMetadata);
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
				, Map.of("event_timest a mp", timestamp,
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
				, Set.of("OIDCAEP-3.5")
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

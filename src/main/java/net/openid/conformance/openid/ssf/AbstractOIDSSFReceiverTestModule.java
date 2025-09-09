package net.openid.conformance.openid.ssf;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.SsfConstants.StreamStatus;
import net.openid.conformance.openid.ssf.conditions.OIDSSFGenerateServerJWKs;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamStatusUpdatedSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamVerificationSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleAuthorizationHeader;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePollRequest;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePushDeliveryToReceiver;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamCreate;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamDelete;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamLookup;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamReplace;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamStatusLookup;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamStatusUpdate;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamSubjectAdd;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamSubjectRemove;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamUpdate;
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
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;
import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI;
import static net.openid.conformance.openid.ssf.SsfEvents.getStandardCapeEvents;
import static net.openid.conformance.openid.ssf.SsfEvents.getStandardRiscEvents;

@VariantParameters({SsfProfile.class, SsfDeliveryMode.class,})
public abstract class AbstractOIDSSFReceiverTestModule extends AbstractOIDSSFTestModule {

	protected OIDSSFEventStore eventStore = new OIDSSFInMemoryEventStore();

	protected ConcurrentMap<String, DeferredAction> deferredActions = new ConcurrentHashMap<>();

	@Override
	protected void configureServerMetadata() {
		super.configureServerMetadata();

		generateJwks();
		registerEventsSupported();

		String transmitterAccessToken = getTransmitterAccessToken();
		env.putString("ssf", "transmitter_access_token", transmitterAccessToken);
		exposeEnvString("ssf_tx_access_token", "ssf", "transmitter_access_token");

		String issuer = BaseUrlUtil.resolveEffectiveBaseUrl(env);

		env.putString("ssf", "issuer", issuer);
		exposeEnvString("ssf_issuer", "ssf", "issuer");

		JsonObject transmitterMetadata = generateTransmitterMetadata(issuer);
		env.putObject("ssf", "transmitter_metadata", transmitterMetadata);

		JsonObject resourceServerMetadata = generateResourceServerMetadata(issuer);
		env.putObject("resource_server_metadata", resourceServerMetadata);
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
		String pollEndpointUrl = ssfIssuer + "/events";
		env.putString("ssf", "poll_endpoint_url", pollEndpointUrl);

		exposeEnvString("ssf_poll_endpoint", "ssf", "poll_endpoint_url");
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
		return Streams.concat(getStandardCapeEvents().stream(), getStandardRiscEvents().stream()).toList();
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
				case "__tick" -> response = handleNextAction();
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

	public static abstract class DeferredAction implements Runnable {

		private final String description;

		public DeferredAction(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	@SuppressWarnings("FutureReturnValueIgnored")
	protected void scheduleDeferredAction(DeferredAction action) {
		scheduleDeferredAction(action, 1, TimeUnit.SECONDS);
	}

	@SuppressWarnings("FutureReturnValueIgnored")
	protected void scheduleDeferredAction(DeferredAction action, int amount, TimeUnit timeUnit) {

		String taskId = UUID.randomUUID().toString();
		String issuer = env.getString("ssf", "issuer");
		String tickUri = issuer + "/__tick?task=" + taskId;

		deferredActions.put(taskId, action);

		eventLog.log(getId(), action.getDescription() + " with task_id=" + taskId);

		Executors.newSingleThreadScheduledExecutor().schedule(() -> {

			if (getStatus() == Status.FINISHED) {
				return;
			}

			ResponseEntity<Void> tickStatus = RestClient.create(tickUri).post().retrieve().toBodilessEntity();
			System.out.println("Submitted scheduled task with task_id=" + taskId + " and status=" + tickStatus.getStatusCode());
		}, amount, timeUnit);
	}

	protected Object handleNextAction() {

		JsonElement queryParamEl = env.getElementFromObject("incoming_request", "query_string_params");
		if (queryParamEl == null) {
			return Response.noContent().build();
		}

		JsonObject queryParams = queryParamEl.getAsJsonObject();

		String taskId = OIDFJSON.tryGetString(queryParams.get("task"));

		eventLog.log(getId(), "Processing action for task_id=" + taskId);

		DeferredAction deferredAction = deferredActions.remove(taskId);
		if (deferredAction == null) {
			return Response.noContent().build();
		}

		deferredAction.run();

		return Response.accepted().build();
	}

	protected ResponseEntity<?> ensureAuthorized(HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts, Supplier<ResponseEntity<?>> requestHandler) {

		callAndStopOnFailure(OIDSSFHandleAuthorizationHeader.class, "CAEPIOP-2.7.3");
		JsonObject authResult = env.getElementFromObject("ssf", "auth_result").getAsJsonObject();

		JsonElement authErrorEl = authResult.get("error");
		if (authErrorEl != null) {
			JsonObject authError = authErrorEl.getAsJsonObject();
			int statusCode = authResult.get("status_code").getAsInt();
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
				callAndStopOnFailure(OIDSSFHandleStreamLookup.class, "OIDSSF-8.1.1.2");
				JsonObject lookupResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();

				JsonElement result = lookupResult.get("result");
				int statusCode = lookupResult.get("status_code").getAsInt();

				if (result == null) {
					return ResponseEntity.status(statusCode).build();
				}

				return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
			}

			case "POST": {
				callAndStopOnFailure(OIDSSFHandleStreamCreate.class, "OIDSSF-8.1.1.1");
				JsonObject createResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = createResult.get("error");
				afterStreamCreation(OIDFJSON.tryGetString(createResult.get("stream_id")), createResult, error);

				return handleResultWithBody(createResult);
			}

			case "DELETE": {
				callAndStopOnFailure(new OIDSSFHandleStreamDelete(eventStore), "OIDSSF-8.1.1.5");

				JsonObject deleteResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = deleteResult.get("error");
				int statusCode = deleteResult.get("status_code").getAsInt();
				afterStreamDeletion(OIDFJSON.tryGetString(deleteResult.get("stream_id")), deleteResult, error);
				return ResponseEntity.status(statusCode).build();
			}

			case "PATCH": {
				callAndStopOnFailure(OIDSSFHandleStreamUpdate.class, "OIDSSF-8.1.1.3");
				JsonObject updateResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = updateResult.get("error");
				afterStreamUpdate(OIDFJSON.tryGetString(updateResult.get("stream_id")), updateResult, error);
				return handleResultWithBody(updateResult);
			}

			case "PUT": {
				callAndStopOnFailure(OIDSSFHandleStreamReplace.class, "OIDSSF-8.1.1.4");
				JsonObject replaceResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = replaceResult.get("error");
				afterStreamReplace(OIDFJSON.tryGetString(replaceResult.get("stream_id")), replaceResult, error);
				return handleResultWithBody(replaceResult);
			}
		}

		return (ResponseEntity<?>) super.handleHttp(path, req, res, session, requestParts);
	}

	protected void afterStreamReplace(String streamId, JsonObject replaceResult, JsonElement error) {

	}

	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {

	}

	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {
		// NOOP
	}

	protected void afterStreamUpdate(String streamId, JsonObject updateResult, JsonElement error) {

	}

	protected ResponseEntity<?> handleResultWithBody(JsonObject createResult) {
		JsonElement result = createResult.get("result");
		JsonElement error = createResult.get("error");
		int statusCode = createResult.get("status_code").getAsInt();

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

		if (StreamSubjectOperation.add == operation) {
			callAndStopOnFailure(OIDSSFHandleStreamSubjectAdd.class, "OIDSSF-8.1.3.2");
		} else if (StreamSubjectOperation.remove == operation) {
			callAndStopOnFailure(OIDSSFHandleStreamSubjectRemove.class, "OIDSSF-8.1.3.3");
		}

		JsonObject subjectChangeResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();

		JsonElement result = subjectChangeResult.get("result");
		int statusCode = subjectChangeResult.get("status_code").getAsInt();

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

		callAndStopOnFailure(OIDSSFHandleStreamVerificationRequest.class, "OIDSSF-8.1.4.2");
		JsonObject verificationResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();

		JsonElement result = verificationResult.get("result");
		int statusCode = verificationResult.get("status_code").getAsInt();

		if (HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
			callAndStopOnFailure(new OIDSSFGenerateStreamVerificationSET(eventStore), "OIDSSF-8.1.4.2");

			String streamId = env.getString("incoming_request", "body_json.stream_id");

			if (OIDSSFStreamUtils.isPushDelivery(OIDSSFStreamUtils.getStreamConfig(env, streamId))) {
				scheduleDeferredAction(new OIDSSFHandlePushDeliveryAction(streamId));
			}
		}

		if (result == null) {
			return ResponseEntity.status(statusCode).build();
		}

		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	protected class OIDSSFHandlePushDeliveryAction extends DeferredAction {

		protected final String streamId;

		protected OIDSSFHandlePushDeliveryAction(String streamId) {
			super("Schedule event delivery via push");
			this.streamId = streamId;
		}

		@Override
		public void run() {
			OIDSSFEventStore.EventsBatch eventsBatch = eventStore.pollEvents(streamId, 16);
			if (eventsBatch == null) {
				// stream was removed in-between, so we don't need to push data
				return;
			}

			// TODO handle SSF PUSH retry???
			for (var event : eventsBatch.events()) {
				callAndContinueOnFailure(new OIDSSFHandlePushDeliveryToReceiver(streamId, event, AbstractOIDSSFReceiverTestModule.this::afterStreamVerificationSuccess), Condition.ConditionResult.WARNING, "OIDSSF-6.1.1");
			}

			if (eventsBatch.moreAvailable()) {
				// reschedule action
				scheduleDeferredAction(this);
			}
		}
	}

	protected void afterStreamVerificationSuccess(String streamId) {

	}

	protected ResponseEntity<?> handleStreamStatusEndpointRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String method = req.getMethod();
		if (!Set.of("GET", "POST").contains(method)) {
			return (ResponseEntity<?>) super.handleHttp(path, req, res, session, requestParts);
		}

		if (method.equals("GET")) {
			callAndStopOnFailure(OIDSSFHandleStreamStatusLookup.class, "OIDSSF-8.1.2.1");
		} else if (method.equals("POST")) {
			callAndStopOnFailure(OIDSSFHandleStreamStatusUpdate.class, "OIDSSF-8.1.2.2");
		}

		JsonObject statusOpResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();

		JsonElement result = statusOpResult.get("result");
		int statusCode = statusOpResult.get("status_code").getAsInt();

		// Only emit StreamStatusUpdate if stream is enabled
		String requestedStatus = env.getString("incoming_request", "body_json.status");
		if (method.equals("POST") && HttpStatus.valueOf(statusCode).is2xxSuccessful() && StreamStatus.enabled.name().equals(requestedStatus)) {
			// only emit stream update event on successful status change
			callAndStopOnFailure(new OIDSSFGenerateStreamStatusUpdatedSET(eventStore), "OIDSSF-8.1.5");
		}

		if (result == null) {
			return ResponseEntity.status(statusCode).build();
		}

		onStreamStatusUpdateSuccess(OIDFJSON.tryGetString(statusOpResult.get("stream_id")), statusOpResult);

		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	protected void onStreamStatusUpdateSuccess(String streamId, JsonElement result) {

	}

	protected abstract boolean isFinished();

	protected ResponseEntity<?> handleStreamPollingRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String method = req.getMethod();
		if (!Objects.equals("POST", method)) {
			return (ResponseEntity<?>) super.handleHttp(path, req, res, session, requestParts);
		}

		callAndStopOnFailure(new OIDSSFHandlePollRequest(eventStore), "OIDSSF-6.1.2", "RFC8936-2.4");

		JsonObject pollResult = env.getElementFromObject("ssf", "poll_result").getAsJsonObject();

		JsonElement result = pollResult.get("result");
		int statusCode = pollResult.get("status_code").getAsInt();

		if (result == null) {
			return ResponseEntity.status(statusCode).build();
		}

		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	private ResponseEntity<?> oauthProtectedResourceServerMetadata() {
		JsonObject resourceServerMetadata = env.getObject("resource_server_metadata");
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resourceServerMetadata);
	}

	@Override
	public void fireTestFinished() {
		super.fireTestFinished();

		deferredActions.clear();
	}
}

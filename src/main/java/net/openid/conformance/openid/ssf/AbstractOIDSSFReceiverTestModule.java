package net.openid.conformance.openid.ssf;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.openid.ssf.conditions.OIDSSFGenerateServerJWKs;
import net.openid.conformance.openid.ssf.conditions.streams.AbstractOIDSSFHandleStreamSubjectChange.OIDSSFHandleStreamSubjectAdd;
import net.openid.conformance.openid.ssf.conditions.streams.AbstractOIDSSFHandleStreamSubjectChange.OIDSSFHandleStreamSubjectRemove;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleAuthorizationHeader;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamCreate;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamDelete;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamLookup;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamReplace;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamStatusLookup;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamStatusUpdate;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandleStreamUpdate;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamSubjectOperation;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;
import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI;
import static net.openid.conformance.openid.ssf.SsfEvents.getStandardCapeEvents;
import static net.openid.conformance.openid.ssf.SsfEvents.getStandardRiscEvents;

@VariantParameters({
	SsfProfile.class,
	SsfDeliveryMode.class,
})
public abstract class AbstractOIDSSFReceiverTestModule extends AbstractOIDSSFTestModule {

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
		String pollEndpointUrl = ssfIssuer + "/poll";
		env.putString("ssf", "poll_endpoint_url", pollEndpointUrl);

		exposeEnvString("ssf_poll_endpoint", "ssf", "poll_endpoint_url");
	}

	protected JsonObject generateTransmitterMetadata(String issuer) {

		JsonObject metadata = new JsonObject();

		metadata.addProperty("issuer", issuer);
		metadata.addProperty("spec_version", "1.0");
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
				case "poll" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleStreamPollingRequest(req, session, requestParts);
				});
				case "streams" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleStreamConfigurationEndpointRequest(path, req, res, session, requestParts);
				});
				case "status" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleStreamStatusEndpointRequest(path, req, res, session, requestParts);
				});
				case "verify" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleVerificationEndpointRequest(req, session, requestParts);
				});
				case "add_subject" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleSubjectsEndpointRequest(req, session, requestParts, StreamSubjectOperation.add);
				});
				case "remove_subject" -> response = ensureAuthorized(req, res, session, requestParts, () -> {
					return handleSubjectsEndpointRequest(req, session, requestParts, StreamSubjectOperation.remove);
				});
				default -> response = super.handleHttp(path, req, res, session, requestParts);
			}
		} finally {
			setStatus(Status.WAITING);
			env.removeObject(requestId);
			env.unmapKey("incoming_request");
		}

		return response;
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
				return handleResultWithBody(createResult);
			}

			case "DELETE": {
				callAndStopOnFailure(OIDSSFHandleStreamDelete.class, "OIDSSF-8.1.1.5");

				JsonObject deleteResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				JsonElement error = deleteResult.get("error");
				int statusCode = deleteResult.get("status_code").getAsInt();

				if (error != null) {
					return ResponseEntity.status(statusCode).build();
				}
				return ResponseEntity.status(statusCode).build();
			}

			case "PATCH": {
				callAndStopOnFailure(OIDSSFHandleStreamUpdate.class, "OIDSSF-8.1.1.3");
				JsonObject updateResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				return handleResultWithBody(updateResult);
			}

			case "PUT": {
				callAndStopOnFailure(OIDSSFHandleStreamReplace.class, "OIDSSF-8.1.1.4");
				JsonObject replaceResult = env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
				return handleResultWithBody(replaceResult);
			}
		}

		return (ResponseEntity<?>) super.handleHttp(path, req, res, session, requestParts);
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


	protected ResponseEntity<?> handleSubjectsEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts, StreamSubjectOperation operation) {

		String method = req.getMethod();
		if (!method.equals("POST")) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
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

	protected ResponseEntity<?> handleVerificationEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {
		throw new UnsupportedOperationException("handleVerificationEndpointRequest");
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

		if (result == null) {
			return ResponseEntity.status(statusCode).build();
		}

		return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(result);
	}

	protected ResponseEntity<?> handleStreamPollingRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {
		throw new UnsupportedOperationException("handleStreamPollingRequest");
	}

	private ResponseEntity<?> oauthProtectedResourceServerMetadata() {
		JsonObject resourceServerMetadata = env.getObject("resource_server_metadata");
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resourceServerMetadata);
	}
}

package net.openid.conformance.openid.ssf.mock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.BaseUrlUtil;
import net.openid.conformance.util.JWKUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OIDSSFTransmitterMock {

	private static final Logger log = LoggerFactory.getLogger(OIDSSFTransmitterMock.class);

	protected final Environment env;

	public OIDSSFTransmitterMock(Environment env) {
		this.env = env;
	}

	enum AuthState {
		AUTH_MISSING,
		AUTH_INVALID,
		AUTH_OK
	}

	protected AuthState validateAuthorizationHeader(JsonObject requestParts) {

		JsonElement givenAuthorizationHeaderElement = requestParts.getAsJsonObject("headers").get("authorization");
		if (givenAuthorizationHeaderElement == null) {
			return AuthState.AUTH_MISSING;
		}

		String authorizationHeaderValue = OIDFJSON.getString(givenAuthorizationHeaderElement);
		String expectedAuthorizationHeaderValue = "Bearer " + env.getString("ssf", "transmitter.access_token");

		if (!expectedAuthorizationHeaderValue.equals(authorizationHeaderValue)) {
			return AuthState.AUTH_INVALID;
		}

		return AuthState.AUTH_OK;
	}

	public Map<String, Object> createTransmitterMetadata() {

		String effectiveBaseUrl = BaseUrlUtil.resolveEffectiveBaseUrl(env);

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("spec_version", env.getString("ssf", "spec_version"));
		metadata.put("issuer", env.getString("ssf", "issuer"));
		metadata.put("jwks_uri", effectiveBaseUrl + "/ssf/jwks");
		metadata.put("status_endpoint", effectiveBaseUrl + "/ssf/status");
		metadata.put("configuration_endpoint", effectiveBaseUrl + "/ssf/streams");
		metadata.put("add_subject_endpoint", effectiveBaseUrl + "/ssf/add_subjects");
		metadata.put("remove_subject_endpoint", effectiveBaseUrl + "/ssf/remove_subjects");
		metadata.put("verification_endpoint", effectiveBaseUrl + "/ssf/verify");
		metadata.put("authorization_schemes", getAuthorizationSchemes());
		metadata.put("delivery_methods_supported", getDeliveryMethodsSupported());

		return metadata;
	}

	public List<Map<String, Object>> getAuthorizationSchemes() {
		Map<String, Object> rfc6749 = new LinkedHashMap<>();
		rfc6749.put("spec_urn", "urn:ietf:rfc:6749");
		return List.of(rfc6749);
	}

	public Set<String> getDeliveryMethodsSupported() {
		return Set.of("urn:ietf:rfc:8935", "urn:ietf:rfc:8936");
	}

	public JsonObject loadJwks() {
		return env.getObject("server_public_jwks");
	}

	public Object handlePollingRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {

		JsonObject currentStream = getStreamFromEnv();
		if (currentStream == null) {
			return ResponseEntity.notFound().build();
		}

		JsonObject responseBodyJson = requestParts.getAsJsonObject("body_json");

		JsonObject verificationObject = readCurrentVerification();

		JsonArray acks;
		if (responseBodyJson.has("ack")) {
			acks = responseBodyJson.getAsJsonArray("ack");
			if (acks != null) {
				if (verificationObject != null) {
					String verificationJti = OIDFJSON.getString(verificationObject.get("jti"));
					for (var ack : acks) {
						String ackJti = OIDFJSON.getString(ack);
						if (ackJti.equals(verificationJti)) {
							verificationObject = null;

							onStreamVerificationSuccess();
						}
					}
				}


				JsonElement streamFeedback = env.getElementFromObject("ssf", "stream.feedback");
				if (streamFeedback == null) {
					JsonObject feedback = new JsonObject();
					feedback.add("acks", acks);
					env.putObject("ssf", "stream.feedback", feedback);
				} else {
					JsonObject streamFeedbackObj = streamFeedback.getAsJsonObject();
					JsonArray acksArray = streamFeedbackObj.getAsJsonArray("acks");

					if (acksArray == null) {
						streamFeedbackObj.add("acks", acksArray);
					} else {
						var newAcksArray = new JsonArray();
						for (var ack : acksArray) {
							newAcksArray.add(ack);
						}

						for (var ack : acks) {
							newAcksArray.add(ack);
						}

						streamFeedbackObj.add("acks", newAcksArray);
					}
				}
			}
		}

		JsonArray errs;
		if (responseBodyJson.has("err")) {
			errs = responseBodyJson.getAsJsonArray("err");
			if (errs != null) {
				if (verificationObject != null) {
					String verificationJti = OIDFJSON.getString(verificationObject.get("jti"));
					for (var err : errs) {
						String ackJti = OIDFJSON.getString(err);
						if (ackJti.equals(verificationJti)) {
							writeCurrentVerification(null);

							// TODO emit stream updated event, see: https://openid.net/specs/openid-sharedsignals-framework-1_0-ID3.html#section-7.1.5

							// mark stream paused
							updateStreamStatus(getStreamIdFromEnv(), "paused", "stream verification failed");

							env.removeElement("ssf", "stream.ready");
						}
					}
				}
			}
		}

		int maxEvents = 10;
		if (responseBodyJson.has("maxEvents")) {
			maxEvents = OIDFJSON.getInt(responseBodyJson.getAsJsonPrimitive("maxEvents"));
		}

		if (maxEvents == 0) {
			return ResponseEntity.ok().build();
		}

		Map<String, Object> pollingData = new HashMap<>();
		pollingData.put("sets", Map.of());

		if (verificationObject != null) {
			String streamId = OIDFJSON.getString(verificationObject.get("stream_id"));
			String state = OIDFJSON.getString(verificationObject.get("state"));

			Map<String, Object> eventSets = generateVerificationEventSet(state, streamId);

			pollingData.put("sets", eventSets);
		} else {
			JsonElement setsElem = env.getElementFromObject("ssf", "stream.sets");
			if (setsElem != null) {
				pollingData.put("sets", setsElem.getAsJsonObject());
			}
		}

		env.getLock().lock();
		try {
			env.signalLockCondition("pollingRequestProcessed");
		} finally {
			env.getLock().unlock();
		}

		return ResponseEntity.ok().body(pollingData);
	}

	protected Map<String, Object> generateVerificationEventSet(String state, String streamId) {
		JsonObject verificationObject = readCurrentVerification();
		if (verificationObject == null) {
			return Collections.emptyMap();
		}

		var events = Map.of("https://schemas.openid.net/secevent/ssf/event-type/verification", Map.<String, Object>of("state", state));

		// create subject for stream
		var subject = new JsonObject();
		subject.addProperty("format", "opaque");
		subject.addProperty("id", streamId);

		Map<String, Object> eventSets = generateSets(subject, events);

		String verificationEventJti = eventSets.keySet().stream().findFirst().orElse(null);

		// store generated jti for ack/err handling
		verificationObject.addProperty("jti", verificationEventJti);
		writeCurrentVerification(verificationObject);
		return eventSets;
	}

	protected void onStreamVerificationSuccess() {
		writeCurrentVerification(null);
		// TODO emit stream updated event, see: https://openid.net/specs/openid-sharedsignals-framework-1_0-ID3.html#section-7.1.5

		// mark stream enabled
		updateStreamStatus(getStreamIdFromEnv(), "enabled", null);

		env.putString("ssf", "stream.ready", "true");
	}

	protected Map<String, Object> generateSets(JsonObject subject, Map<String, Map<String, Object>> events) {

		JWK jwk = getSetJWK();

		Map<String, Object> sets = new HashMap<>();

		SignedJWT signedSet = createSetFor(subject, events, jwk);

		String setJwtString = signAndEncodeSet(jwk, signedSet);

		try {
			sets.put(signedSet.getJWTClaimsSet().getJWTID(), setJwtString);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		return sets;
	}

	public String signAndEncodeSet(JWK jwk, SignedJWT signedSet) {
		RSAKey rsaKey = jwk.toRSAKey();
		String setJwtString;
		try {
			JWSSigner signer = new RSASSASigner(rsaKey);
			signedSet.sign(signer);
			setJwtString = signedSet.serialize();
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
		return setJwtString;
	}

	public JWK getSetJWK() {
		JsonObject serverJwksObject = env.getObject("server_jwks");
		JWKSet jwkSet;
		try {
			jwkSet = JWKUtil.parseJWKSet(serverJwksObject.toString());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		JWK jwk = JWKUtil.selectAsymmetricJWSKey(JWSAlgorithm.RS256, jwkSet.getKeys());
		return jwk;
	}

	public SignedJWT createSetFor(JsonObject subjectId, Map<String, Map<String, Object>> events, JWK jwk) {

		String issuer = env.getString("ssf", "issuer");
		String audience = OIDFJSON.getString(env.getElementFromObject("config", "ssf.stream.audience"));

		String jti = UUID.randomUUID().toString();

		Map<String, Object> subjectIdMap = subjectIdObjectToMap(subjectId);

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.jwtID(jti)
			.issuer(issuer)
			.audience(audience)
			.issueTime(Date.from(Instant.now())) // Epoch time in milliseconds
			.claim("sub_id", subjectIdMap)
			.claim("events", events)
			.build();

		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
			.keyID(jwk.getKeyID())
			.type(new JOSEObjectType("secevent+jwt"))
			.build();

		return new SignedJWT(header, claimsSet);
	}

	public Map<String, Object> subjectIdObjectToMap(JsonObject subjectId) {
		Map<String, Object> subjectIdMap = new LinkedHashMap<>();
		for (String key : subjectId.keySet()) {
			subjectIdMap.put(key, OIDFJSON.getString(subjectId.get(key)));
		}
		return subjectIdMap;
	}

	public Object handleStreamConfigurationEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {

		switch (validateAuthorizationHeader(requestParts)) {
			case AUTH_MISSING:
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			case AUTH_INVALID:
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			case AUTH_OK:
				break;
		}

		if (HttpMethod.POST.name().equals(req.getMethod())) {
			JsonObject streamConfigInput = requestParts.getAsJsonObject("body_json");
			JsonObject streamConfig = createStream(streamConfigInput);
			return ResponseEntity.created(null).body(streamConfig);
		} else if (HttpMethod.GET.name().equals(req.getMethod())) {
			JsonElement streamConfig = getStreamFromEnv();
			if (streamConfig == null) {
				return ResponseEntity.notFound().build();
			}

			String currentStreamId = getStreamIdFromEnv();
			String requestStreamId = getStreamIdFromRequestParameters(requestParts);
			if (requestStreamId != null && !currentStreamId.equals(requestStreamId)) {
				return ResponseEntity.notFound().build();
			}

			return ResponseEntity.ok(streamConfig);
		} else if (HttpMethod.DELETE.name().equals(req.getMethod())) {

			String currentStreamId = getStreamIdFromEnv();
			if (currentStreamId == null) {
				return ResponseEntity.notFound().build();
			}
			String requestStreamId = getStreamIdFromRequestParameters(requestParts);
			if (!currentStreamId.equals(requestStreamId)) {
				return ResponseEntity.notFound().build();
			}

			env.removeElement("ssf", "stream.config");
			env.removeElement("ssf", "stream.feedback");

			return ResponseEntity.noContent().build();
		} else if (HttpMethod.PATCH.name().equals(req.getMethod())) {

			JsonObject streamUpdate = requestParts.getAsJsonObject("body_json");
			String requestStreamId = getStreamIdFromBodyJson(streamUpdate);
			if (requestStreamId == null) {
				return ResponseEntity.badRequest().build();
			}
			String currentStreamId = getStreamIdFromEnv();
			if (!currentStreamId.equals(requestStreamId)) {
				return ResponseEntity.notFound().build();
			}

			Set<String> updatableStreamFields = Set.of("events_requested", "description");

			// events_requested, description
			JsonObject currentStream = getStreamFromEnv();
			for (String key : streamUpdate.keySet()) {
				if (updatableStreamFields.contains(key)) {
					JsonElement updatedValue = streamUpdate.get(key);
					if (updatedValue == null) {
						return ResponseEntity.status(400).build();
					}
					currentStream.remove(key);
					currentStream.add(key, updatedValue);
				}
			}

			return ResponseEntity.accepted().body(currentStream);
		} else if (HttpMethod.PUT.name().equals(req.getMethod())) {

			JsonObject streamReplacement = requestParts.getAsJsonObject("body_json");
			String requestStreamId = getStreamIdFromBodyJson(streamReplacement);
			if (requestStreamId == null) {
				return ResponseEntity.badRequest().build();
			}

			String currentStreamId = getStreamIdFromEnv();
			if (!currentStreamId.equals(requestStreamId)) {
				return ResponseEntity.notFound().build();
			}

			env.putObject("ssf", "stream.config", streamReplacement);

			return ResponseEntity.accepted().body(streamReplacement);
		}

		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
	}

	protected JsonObject getStreamFromEnv() {
		JsonElement elementFromObject = env.getElementFromObject("ssf", "stream.config");
		if (elementFromObject == null) {
			return null;
		}
		return elementFromObject.getAsJsonObject();
	}

	protected String getStreamIdFromEnv() {
		JsonObject streamConfigObject = getStreamFromEnv();
		if (streamConfigObject == null) {
			return null;
		}
		JsonElement currentStreamId = streamConfigObject.get("stream_id");
		if (currentStreamId == null) {
			return null;
		}
		return OIDFJSON.getString(currentStreamId);
	}

	protected String getStreamIdFromRequestParameters(JsonObject requestParts) {
		if (requestParts == null) {
			return null;
		}
		JsonObject queryStringParams = requestParts.getAsJsonObject("query_string_params");
		if (queryStringParams == null) {
			return null;
		}
		JsonElement streamIdElement = queryStringParams.get("stream_id");
		if (streamIdElement == null) {
			return null;
		}
		return OIDFJSON.getString(streamIdElement);
	}

	public JsonObject createStream(JsonObject streamConfigInput) {

		String streamId = UUID.randomUUID().toString();
		JsonObject streamConfig = streamConfigInput.deepCopy();
		streamConfig.addProperty("stream_id", streamId);
		streamConfig.addProperty("iss", env.getString("ssf", "issuer"));
		streamConfig.addProperty("aud", OIDFJSON.getString(env.getElementFromObject("config", "ssf.stream.audience")));
		streamConfig.add("events_supported", OIDFJSON.convertListToJsonArray(getEventsSupported()));
		streamConfig.add("events_delivered", OIDFJSON.convertListToJsonArray(getEventsDelivered(streamConfigInput)));

		env.putObject("ssf", "stream.config", streamConfig);

		JsonObject delivery = streamConfig.getAsJsonObject("delivery");
		String deliveryMethod = OIDFJSON.getString(delivery.get("method"));
		switch (deliveryMethod) {
			case "urn:ietf:rfc:8936":
				String pollEndpointUrl = env.getString("ssf", "poll_endpoint_url");
				delivery.addProperty("endpoint_url", pollEndpointUrl);
				break;
			case "urn:ietf:rfc:8935":
				JsonElement endpointUrl = delivery.get("endpoint_url");
				if (endpointUrl == null) {
					throw new IllegalArgumentException("endpoint_url must be set for urn:ietf:rfc:8935 PUSH delivery");
				}
				break;
		}

		updateStreamStatus(streamId, "enabled", "");

		return streamConfig;
	}

	public List<String> getEventsDelivered(JsonObject streamConfigInput) {
		return List.copyOf(Set.of("https://schemas.openid.net/secevent/caep/event-type/session-revoked", "https://schemas.openid.net/secevent/caep/event-type/credential-change", "https://schemas.openid.net/secevent/caep/event-type/device-compliance-change", "https://schemas.openid.net/secevent/caep/event-type/assurance-level-change", "https://schemas.openid.net/secevent/caep/event-type/token-claims-change", "https://schemas.openid.net/secevent/ssf/event-type/verification"));
	}

	public List<String> getEventsSupported() {
		return List.copyOf(Set.of(
			// CAEP events
			"https://schemas.openid.net/secevent/caep/event-type/session-established", "https://schemas.openid.net/secevent/caep/event-type/session-presented", "https://schemas.openid.net/secevent/caep/event-type/session-revoked", "https://schemas.openid.net/secevent/caep/event-type/credential-change", "https://schemas.openid.net/secevent/caep/event-type/device-compliance-change", "https://schemas.openid.net/secevent/caep/event-type/assurance-level-change", "https://schemas.openid.net/secevent/caep/event-type/token-claims-change", "https://schemas.openid.net/secevent/ssf/event-type/verification",
			// RISC events
			"https://schemas.openid.net/secevent/risc/event-type/account-credential-change-required", "https://schemas.openid.net/secevent/risc/event-type/account-disabled", "https://schemas.openid.net/secevent/risc/event-type/account-enabled", "https://schemas.openid.net/secevent/risc/event-type/account-purged", "https://schemas.openid.net/secevent/risc/event-type/credential-compromise", "https://schemas.openid.net/secevent/risc/event-type/identifier-changed", "https://schemas.openid.net/secevent/risc/event-type/identifier-recycled", "https://schemas.openid.net/secevent/risc/event-type/opt-in", "https://schemas.openid.net/secevent/risc/event-type/opt-out-cancelled", "https://schemas.openid.net/secevent/risc/event-type/opt-out-effective", "https://schemas.openid.net/secevent/risc/event-type/opt-out-initiated", "https://schemas.openid.net/secevent/risc/event-type/recovery-activated", "https://schemas.openid.net/secevent/risc/event-type/recovery-information-changed"));
	}

	public Object handleStreamStatusEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {

		String currentStreamId = getStreamIdFromEnv();
		if (currentStreamId == null) {
			return ResponseEntity.notFound().build();
		}

		if (HttpMethod.GET.name().equals(req.getMethod())) {

			String requestStreamId = getStreamIdFromRequestParameters(requestParts);
			if (!currentStreamId.equals(requestStreamId)) {
				return ResponseEntity.notFound().build();
			}

			JsonElement streamStatus = env.getElementFromObject("ssf", "stream.status");
			return ResponseEntity.ok(streamStatus);
		} else if (HttpMethod.POST.name().equals(req.getMethod())) {

			JsonObject jsonBody = requestParts.getAsJsonObject("body_json");
			String requestStreamId = getStreamIdFromBodyJson(jsonBody);
			if (!currentStreamId.equals(requestStreamId)) {
				return ResponseEntity.notFound().build();
			}
			String newStatus = OIDFJSON.getString(jsonBody.get("status"));
			String reason = null;
			if (jsonBody.get("reason") != null) {
				reason = OIDFJSON.getString(jsonBody.get("reason"));
			}

			JsonObject streamStatusObject;
			try {
				streamStatusObject = updateStreamStatus(requestStreamId, newStatus, reason);
			} catch (IllegalArgumentException iae) {
				return ResponseEntity.badRequest().body(Map.of("error", iae.getMessage()));
			}

			return ResponseEntity.accepted().body(streamStatusObject);
		}

		return null;
	}

	protected JsonObject updateStreamStatus(String requestStreamId, String newStatus, String reason) {

		Set<String> supportedStates = Set.of("enabled", "paused", "disabled");
		if (!supportedStates.contains(newStatus)) {
			throw new IllegalArgumentException("Invalid status value: " + newStatus + ". Only the following status values are supported: " + supportedStates);
		}

		JsonObject streamStatusObject = new JsonObject();
		streamStatusObject.addProperty("stream_id", requestStreamId);
		streamStatusObject.addProperty("status", newStatus);
		streamStatusObject.addProperty("reason", reason);
		env.putObject("ssf", "stream.status", streamStatusObject);
		return streamStatusObject;
	}

	public Object handleVerificationEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {

		JsonElement streamConfig = getStreamFromEnv();
		if (streamConfig == null) {
			return ResponseEntity.notFound().build();
		}

		String currentStreamId = getStreamIdFromEnv();
		if (currentStreamId == null) {
			return ResponseEntity.notFound().build();
		}

		JsonObject verificationInput = requestParts.getAsJsonObject("body_json");
		if (verificationInput == null) {
			return ResponseEntity.badRequest().build();
		}
		String requestStreamId = getStreamIdFromBodyJson(verificationInput);
		if (requestStreamId == null) {
			return ResponseEntity.badRequest().build();
		}

		if (!currentStreamId.equals(requestStreamId)) {
			return ResponseEntity.notFound().build();
		}

		JsonElement requestStateElement = verificationInput.get("state");
		if (requestStateElement == null) {
			return ResponseEntity.badRequest().build();
		}

		String state = OIDFJSON.getString(requestStateElement);

		JsonObject verificationObject = new JsonObject();
		verificationObject.addProperty("state", state);
		verificationObject.addProperty("stream_id", currentStreamId);

		writeCurrentVerification(verificationObject);

		// TODO add support for push delivery
		boolean pushDelivery = "urn:ietf:rfc:8935".equals(env.getString("ssf","stream.config.delivery.method"));
		if (pushDelivery) {

			String pushToken = env.getString("ssf","stream.config.delivery.authorization_header");
			String pushUrl = env.getString("ssf", "stream.config.delivery.endpoint_url");
			Map<String, Object> verificationEventSet = generateVerificationEventSet(state, currentStreamId);

			ScheduledFuture<?> unused = Executors.newSingleThreadScheduledExecutor().schedule(() -> {
				String verificationSet = (String) verificationEventSet.entrySet().iterator().next().getValue();
				RestTemplate restTemplate = new RestTemplate();
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.parseMediaType("application/secevent+jwt"));
				headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
				headers.setBearerAuth(pushToken);
				ResponseEntity<?> responseEntity = restTemplate.exchange(pushUrl, HttpMethod.POST, new HttpEntity<>(verificationSet, headers), Map.class);

				log.debug("Delivered verification event to push endpoint. statusCode={}", responseEntity.getStatusCode());
			}, 2, TimeUnit.SECONDS);
		}

		return ResponseEntity.noContent().build();
	}

	protected void writeCurrentVerification(JsonObject verificationObject) {
		env.putObject("ssf", "stream.verification", verificationObject);
	}

	protected JsonObject readCurrentVerification() {
		JsonElement verificationElement = env.getElementFromObject("ssf", "stream.verification");
		if (OIDFJSON.isNull(verificationElement)) {
			return null;
		}
		return verificationElement.getAsJsonObject();
	}

	protected String getStreamIdFromBodyJson(JsonObject bodyJson) {

		if (bodyJson == null) {
			return null;
		}

		JsonElement streamIdElement = bodyJson.get("stream_id");
		if (streamIdElement == null) {
			return null;
		}
		return OIDFJSON.getString(streamIdElement);
	}

	public Object handleSubjectsEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts, String change) {
		// TODO implement me
		return null;
	}
}

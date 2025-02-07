package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.openid.ssf.model.OIDSSFTransmitterMetadata;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.BaseUrlUtil;
import net.openid.conformance.variant.VariantParameters;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@VariantParameters({
	SsfProfile.class,
	SsfDeliveryMode.class,
})
public abstract class AbstractOIDSSFReceiverTestModule extends AbstractOIDSSFTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		super.configure(config, baseUrl, externalUrlOverride, baseMtlsUrl);

		String effectiveBaseUrl = BaseUrlUtil.resolveEffectiveBaseUrl(env);

		String effectiveFrontendHostnameWithTestNameAlias = effectiveBaseUrl.replace("/test/a", "");
		env.putString("ssf", "issuer", effectiveFrontendHostnameWithTestNameAlias);
		exposeEnvString("ssf_issuer", "ssf", "issuer");

		String transmitterMetadataEndpoint = effectiveBaseUrl + "/ssf/ssf-configuration";
		env.putString("ssf", "transmitter_metadata_endpoint", transmitterMetadataEndpoint);
		exposeEnvString("transmitter_metadata_endpoint", "ssf", "transmitter_metadata_endpoint");

		env.putString("ssf", "spec_version", "1_0-ID3");
		exposeEnvString("ssf_spec_version", "ssf", "spec_version");

		if (Objects.requireNonNull(getVariant(SsfDeliveryMode.class)) == SsfDeliveryMode.POLL) {
			String pollUrl = effectiveBaseUrl + "/ssf/poll";
			env.putString("ssf", "poll_endpoint_url", pollUrl);
			exposeEnvString("poll_endpoint_url", "ssf", "poll_endpoint_url");
		}
	}

	protected Object createTransmitterMetadata() {

		String effectiveBaseUrl = BaseUrlUtil.resolveEffectiveBaseUrl(env);

		OIDSSFTransmitterMetadata metadata = new OIDSSFTransmitterMetadata();
		metadata.setSpecVersion(env.getString("ssf", "spec_version"));
		metadata.setIssuer(env.getString("ssf", "issuer"));
		metadata.setJwksUri(effectiveBaseUrl+"/ssf/jwks");
		metadata.setStatusEndpoint(effectiveBaseUrl+"/ssf/status");
		metadata.setConfigurationEndpoint(effectiveBaseUrl+"/ssf/streams");
		metadata.setAddSubjectEndpoint(effectiveBaseUrl+"/ssf/add_subjects");
		metadata.setRemoveSubjectEndpoint(effectiveBaseUrl+"/ssf/remove_subjects");
		metadata.setVerificationEndpoint(effectiveBaseUrl+"/ssf/verify");
		metadata.setAuthorizationSchemes(getAuthorizationSchemes());
		metadata.setDeliveryMethodSupported(getDeliveryMethodsSupported());

		return metadata;
	}

	@NotNull
	protected List<Object> getAuthorizationSchemes() {
		return List.of(Map.of("spec_urn", "urn:ietf:rfc:6749"));
	}

	@NotNull
	protected Set<String> getDeliveryMethodsSupported() {
		return Set.of("urn:ietf:rfc:8935", "urn:ietf:rfc:8936");
	}

	protected JsonObject loadJwks() {
		return env.getObject("server_public_jwks");
	}

	protected Object handlePollingRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {
		return null;
	}

	protected Object handleStreamConfigurationEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {

		if (HttpMethod.POST.name().equals(req.getMethod())) {
			JsonObject streamConfigInput = requestParts.getAsJsonObject("body_json");
			JsonObject streamConfig = createStream(streamConfigInput);
			return ResponseEntity.ok(streamConfig);
		}

		return null;
	}

	protected JsonObject createStream(JsonObject streamConfigInput) {

		String streamId = UUID.randomUUID().toString();
		JsonObject streamConfig = streamConfigInput.deepCopy();
		streamConfig.addProperty("stream_id", streamId);
		streamConfig.addProperty("iss", env.getString("ssf", "issuer"));
		streamConfig.addProperty("aud", OIDFJSON.getString(env.getElementFromObject("config","ssf.stream.audience")));
		streamConfig.add("events_supported", OIDFJSON.convertListToJsonArray(getEventsSupported()));
		streamConfig.add("events_delivered", OIDFJSON.convertListToJsonArray(getEventsDelivered(streamConfigInput)));

		env.putObject("ssf","stream", streamConfig);

		return streamConfig;
	}

	protected List<String> getEventsDelivered(JsonObject streamConfigInput) {
		return List.copyOf(Set.of(
			"https://schemas.openid.net/secevent/caep/event-type/session-revoked",
			"https://schemas.openid.net/secevent/caep/event-type/credential-change",
			"https://schemas.openid.net/secevent/caep/event-type/device-compliance-change",
			"https://schemas.openid.net/secevent/caep/event-type/assurance-level-change",
			"https://schemas.openid.net/secevent/caep/event-type/token-claims-change",
			"https://schemas.openid.net/secevent/ssf/event-type/verification"
		));
	}

	protected List<String> getEventsSupported() {
		return List.copyOf(Set.of(
			// CAEP events
			"https://schemas.openid.net/secevent/caep/event-type/session-established",
			"https://schemas.openid.net/secevent/caep/event-type/session-presented",
			"https://schemas.openid.net/secevent/caep/event-type/session-revoked",
			"https://schemas.openid.net/secevent/caep/event-type/credential-change",
			"https://schemas.openid.net/secevent/caep/event-type/device-compliance-change",
			"https://schemas.openid.net/secevent/caep/event-type/assurance-level-change",
			"https://schemas.openid.net/secevent/caep/event-type/token-claims-change",
			"https://schemas.openid.net/secevent/ssf/event-type/verification",
			// RISC events
			"https://schemas.openid.net/secevent/risc/event-type/account-credential-change-required",
			"https://schemas.openid.net/secevent/risc/event-type/account-disabled",
			"https://schemas.openid.net/secevent/risc/event-type/account-enabled",
			"https://schemas.openid.net/secevent/risc/event-type/account-purged",
			"https://schemas.openid.net/secevent/risc/event-type/credential-compromise",
			"https://schemas.openid.net/secevent/risc/event-type/identifier-changed",
			"https://schemas.openid.net/secevent/risc/event-type/identifier-recycled",
			"https://schemas.openid.net/secevent/risc/event-type/opt-in",
			"https://schemas.openid.net/secevent/risc/event-type/opt-out-cancelled",
			"https://schemas.openid.net/secevent/risc/event-type/opt-out-effective",
			"https://schemas.openid.net/secevent/risc/event-type/opt-out-initiated",
			"https://schemas.openid.net/secevent/risc/event-type/recovery-activated",
			"https://schemas.openid.net/secevent/risc/event-type/recovery-information-changed"
		));
	}

	protected Object handleStreamStatusEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {
		return null;
	}

	protected Object handleVerificationEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts) {
		return null;
	}

	protected Object handleSubjectsEndpointRequest(HttpServletRequest req, HttpSession session, JsonObject requestParts, String change) {
		return null;
	}

	/**
	 * Provides dynamic endpoints to emulate a SSF Transmitter for testing SSF Receivers.
	 *
	 * @param path         The path that was called
	 * @param req          The request that passed to the server
	 * @param res          A response that will be sent from the server
	 * @param session      Session details
	 * @param requestParts elements from the request parsed out into a json object for use in condition classes
	 * @return
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if ("ssf/ssf-configuration".equals(path)) {
			Object transmitterMetadata = createTransmitterMetadata();
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(transmitterMetadata);
		} else if ("ssf/jwks".equals(path)) {
			Object jwks = loadJwks();
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jwks);
		} else if ("ssf/poll".equals(path)) {
			Object pollingResponse = handlePollingRequest(req, session, requestParts);
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(pollingResponse);
		} else if ("ssf/streams".equals(path)) {
			return handleStreamConfigurationEndpointRequest(req, session, requestParts);
		} else if ("ssf/status".equals(path)) {
			return handleStreamStatusEndpointRequest(req, session, requestParts);
		} else if ("ssf/verify".equals(path)) {
			return handleVerificationEndpointRequest(req, session, requestParts);
		} else if ("ssf/add_subjects".equals(path)) {
			return handleSubjectsEndpointRequest(req, session, requestParts, "add");
		} else if ("ssf/remove_subjects".equals(path)) {
			return handleSubjectsEndpointRequest(req, session, requestParts, "remove");
		}

		return super.handleHttp(path, req, res, session, requestParts);
	}
}

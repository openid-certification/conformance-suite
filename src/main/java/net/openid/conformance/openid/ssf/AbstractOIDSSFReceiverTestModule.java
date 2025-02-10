package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.openid.ssf.mock.OIDSSFTransmitterMock;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.util.BaseUrlUtil;
import net.openid.conformance.variant.VariantParameters;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;

@VariantParameters({
	SsfProfile.class,
	SsfDeliveryMode.class,
})
public abstract class AbstractOIDSSFReceiverTestModule extends AbstractOIDSSFTestModule {

	protected OIDSSFTransmitterMock transmitterMock;

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		super.configure(config, baseUrl, externalUrlOverride, baseMtlsUrl);

		env.getLock().lock();
		try {
			env.registerLockCondition("pollingRequestProcessed");
		} finally {
			env.getLock().unlock();
		}

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

		String transmitterAccessToken = env.getString("config", "ssf.transmitter.access_token");
		if (!StringUtils.hasText(transmitterAccessToken)) {
			transmitterAccessToken = UUID.randomUUID().toString();
		}
		env.putString("ssf", "transmitter.access_token", transmitterAccessToken);
		exposeEnvString("transmitter_access_token", "ssf", "transmitter.access_token");

		transmitterMock = new OIDSSFTransmitterMock(env);
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
			Object transmitterMetadata = transmitterMock.createTransmitterMetadata();
			return ResponseEntity.ok().body(transmitterMetadata);
		} else if ("ssf/jwks".equals(path)) {
			Object jwks = transmitterMock.loadJwks();
			return ResponseEntity.ok().body(jwks);
		} else if ("ssf/streams/poll".equals(path)) {
			return transmitterMock.handlePollingRequest(req, session, requestParts);
		} else if ("ssf/streams".equals(path)) {
			return transmitterMock.handleStreamConfigurationEndpointRequest(req, session, requestParts);
		} else if ("ssf/status".equals(path)) {
			return transmitterMock.handleStreamStatusEndpointRequest(req, session, requestParts);
		} else if ("ssf/verify".equals(path)) {
			return transmitterMock.handleVerificationEndpointRequest(req, session, requestParts);
		} else if ("ssf/add_subjects".equals(path)) {
			return transmitterMock.handleSubjectsEndpointRequest(req, session, requestParts, "add");
		} else if ("ssf/remove_subjects".equals(path)) {
			return transmitterMock.handleSubjectsEndpointRequest(req, session, requestParts, "remove");
		}

		return super.handleHttp(path, req, res, session, requestParts);
	}
}

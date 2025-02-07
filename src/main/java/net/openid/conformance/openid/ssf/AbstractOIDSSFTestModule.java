package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.openid.ssf.conditions.OIDSSFConfigurePushDeliveryMethod;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.AbstractTestModule;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

public abstract class AbstractOIDSSFTestModule extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		env.putString("base_url", baseUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		env.putString("ssf", "profile", getVariant(SsfProfile.class).name());

		exposeEnvString("alias", "config", "alias");

		if (Objects.requireNonNull(getVariant(SsfDeliveryMode.class)) == SsfDeliveryMode.PUSH) {
			callAndStopOnFailure(OIDSSFConfigurePushDeliveryMethod.class);
			exposeEnvString("pushDeliveryEndpointUrl", "ssf", "push_delivery_endpoint_url");
		}

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	@Override
	public void start() {
		// NOOP
	}

	protected boolean isSsfProfileEnabled(SsfProfile profile) {
		return profile.equals(getVariant(SsfProfile.class));
	}

	/**
	 * Provides a dynamic endpoint for handling SSF Push requests from sent from transmitters.
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

		if ("ssf-push".equals(path)) {
			env.putObject("ssf", "push_request", requestParts);
			// see: RFC 8935 Push-Based Security Event Token (SET) Delivery Using HTTP
			// https://www.rfc-editor.org/rfc/rfc8935.html#section-2.2
			return ResponseEntity.accepted().build();
		}

		return super.handleHttp(path, req, res, session, requestParts);
	}
}

package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRequestUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import net.openid.conformance.condition.common.CreateRandomRequestUriWithFragment;
import org.springframework.http.ResponseEntity;

/**
 * Generic behaviour required when testing request_uri behaviours
 */
public abstract class AbstractOIDCCRequestUriServerTest extends AbstractOIDCCServerTest {
	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();

		callAndStopOnFailure(CreateRandomRequestUriWithFragment.class, "OIDCC-6.2");
		callAndStopOnFailure(AddRequestUriToDynamicRegistrationRequest.class);
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {

		super.onConfigure(config, baseUrl);
		checkDiscEndpointRequestUriParameterSupported();
	}

	/**
	 * Allow derived tests to decide how to check for request_uri parameter support.
	 */
	protected void checkDiscEndpointRequestUriParameterSupported() {
		callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (path.equals(env.getString("request_uri", "path"))) {
			return handleRequestUriRequest();
		}
		return super.handleHttp(path, req, res, session, requestParts);

	}

	private Object handleRequestUriRequest() {
		String requestObject = env.getString("request_object");

		return ResponseEntity.ok()
			.contentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT)
			.body(requestObject);
	}

	@Override
	protected abstract void createAuthorizationRedirect();

}

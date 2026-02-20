package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObjectOrInvalidClient;
import net.openid.conformance.condition.common.ExpectInvalidRequestOrInvalidClientErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@PublishTestModule(
	testName = "openid-federation-automatic-client-registration-invalid-client-id-in-request-object",
	displayName = "OpenID Federation OP test: Invalid client_id in request object",
	summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
		"deliberately using an invalid entity identifier as the client id in the authorization request object." +
		"<br/><br/>" +
		"If the server does not return an invalid_request, invalid_request_object, invalid_client or a similar well-defined " +
		"and appropriate error back to the client, it must show an error page saying the request is invalid due to " +
		"an invalid client_id — upload a screenshot of the error page.",
	profile = "OIDFED"
)
public class OpenIDFederationAutomaticClientRegistrationInvalidClientIdInRequestObjectTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

	@Override
	protected void buildRequestObject() {
		super.buildRequestObject();
		callAndContinueOnFailure(AddInvalidClientIdToRequestObject.class, Condition.ConditionResult.FAILURE, "OIDFED-12.1.1.1");
	}

	@Override
	protected void createPlaceholder() {
		callAndContinueOnFailure(ExpectInvalidRequestOrInvalidClientErrorPage.class, Condition.ConditionResult.FAILURE);
		env.putString("error_callback_placeholder", env.getString("invalid_request_error"));
	}

	@Override
	protected void redirect(HttpMethod method) {
		performRedirectAndWaitForPlaceholdersOrCallback("error_callback_placeholder", method.name());
	}

	@Override
	protected void processCallback() {
		env.mapKey("authorization_endpoint_response", "callback_query_params");
		performGenericAuthorizationEndpointErrorResponseValidation();
		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObjectOrInvalidClient.class,
			Condition.ConditionResult.WARNING, "OIDFED-12.1.3");
		fireTestFinished();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		return switch (path) {
			case "INVALID/.well-known/openid-federation" ->
				new ResponseEntity<>(Map.of("error", "this entity statement is invalid and registration will fail"), HttpStatus.NOT_FOUND);
			default -> super.handleHttp(path, req, res, session, requestParts);
		};
	}

	@Override
	public Object handleWellKnown(String path,
								  HttpServletRequest req, HttpServletResponse res,
								  HttpSession session,
								  JsonObject requestParts) {
		if (path.startsWith("/.well-known/openid-federation") &&
			path.endsWith("/INVALID")) {
			return new ResponseEntity<>(Map.of("error", "this entity statement is invalid and registration will fail"), HttpStatus.NOT_FOUND);
		}

		return super.handleWellKnown(path, req, res, session, requestParts);
	}

}

package net.openid.conformance.openid.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.OIDCCSignClaimsEndpointResponse;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfoWithDistributedClaims;
import net.openid.conformance.condition.rs.OIDCCValidateBearerAccessTokenInClaimsEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletResponse;

/**
 * rp-claims-distributed in the old python test suite
 */
@PublishTestModule(
	testName = "oidcc-client-test-distributed-claims",
	displayName = "OIDCC: Relying party test, distributed claims",
	summary = "The client is expected to make an authentication request " +
		"(also a token request where applicable) and a userinfo request " +
		"using the selected response_type and other configuration options " +
		"and process a userinfo response with distributed claims " +
		"and send a request to the claims_endpoint to retrieve the claims. " +
		"(This test supports a 'credit_score' distributed claim and always returns the same response " +
		"regardless of requested scopes)",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values = {"id_token"})
public class OIDCCClientTestDistributedClaims extends AbstractOIDCCClientTest {

	boolean receivedClaimsEndpointRequest = false;

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if(receivedAuthorizationRequest && receivedClaimsEndpointRequest) {
			fireTestFinished();
			return true;
		}
		return false;
	}

	@Override
	protected void configureUserInfo() {
		callAndStopOnFailure(OIDCCLoadUserInfoWithDistributedClaims.class);
	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse) {

		if("claims_endpoint".equals(path)) {
			receivedClaimsEndpointRequest = true;
			call(exec().startBlock("Claims endpoint").mapKey("incoming_request", requestId));
			callAndContinueOnFailure(OIDCCValidateBearerAccessTokenInClaimsEndpointRequest.class, Condition.ConditionResult.FAILURE,
									"OIDCC-5.6.2");
			callAndStopOnFailure(OIDCCSignClaimsEndpointResponse.class, "OIDCC-5.6.2");
			String responseBody = env.getString("distributed_claims_endpoint_response");

			call(exec().unmapKey("incoming_request").endBlock());

			ResponseEntity<Object> response = createApplicationJwtResponse(responseBody);
			return response;
		} else {
			return super.handleClientRequestForPath(requestId, path, servletResponse);
		}
	}

	@Override
	protected JsonObject prepareUserinfoResponse() {
		JsonObject user = env.getObject("user_info");
		env.putObject("user_info_endpoint_response", user);
		return user;
	}

}

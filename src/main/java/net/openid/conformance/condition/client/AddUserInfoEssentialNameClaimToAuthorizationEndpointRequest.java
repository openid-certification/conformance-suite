package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddUserInfoEssentialNameClaimToAuthorizationEndpointRequest extends AbstractAddClaimToAuthorizationEndpointRequest {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		/*
		The python suite sends:

		https://fapidev-www.authlete.net/api/authorization?state=9d4Uoc5cCHR3nVLC&nonce=ilTAjEj9bdysSoSX&response_type=code&scope=openid&redirect_uri=https%3A%2F%2Fop.certification.openid.net%3A61757%2Fauthz_cb&claims=%7B%22userinfo%22%3A+%7B%22name%22%3A+%7B%22essential%22%3A+true%7D%7D%7D&client_id=138292314413510

		or the decoded claims value:

		"userinfo": {
			"name": {
				"essential": true
			}
		}

		 */

		return addClaim(env, LocationToRequestClaim.USERINFO, "name", null, true);

	}

}

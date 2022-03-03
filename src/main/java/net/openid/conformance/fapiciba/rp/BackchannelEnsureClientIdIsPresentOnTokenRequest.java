package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelEnsureClientIdIsPresentOnTokenRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	public Environment evaluate(Environment env) {

		String clientId = env.getString("backchannel_endpoint_http_request", "body_form_params.client_id");

		if (Strings.isNullOrEmpty(clientId)) {

			throw error("client_id is required when authenticating clients using mtls");
		}
		logSuccess("Parameter client_id found on the request");
		return env;
	}
}

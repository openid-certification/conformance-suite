package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractParAuthorizationCodeDpopBindingKey extends AbstractCondition {

	// WARNING authorization_code_dpop_jkt can be empty string
	@Override
	@PostEnvironment(strings = {"authorization_code_dpop_jkt"})
	public Environment evaluate(Environment env) {
		// Look in request object
		String dpop_jkt = env.getString("authorization_request_object", "claims.dpop_jkt");
		if (Strings.isNullOrEmpty(dpop_jkt)) {
			// Look in PAR unsigned request
			dpop_jkt = env.getString("par_endpoint_http_request", "body_form_params.dpop_jkt");
		}

		// client may not have sent DPoP proof
		String computed_dpop_jkt = env.getString("incoming_dpop_proof", "computed_dpop_jkt");
		if(!Strings.isNullOrEmpty(dpop_jkt) && !Strings.isNullOrEmpty(computed_dpop_jkt)) {
			if(!dpop_jkt.equals(computed_dpop_jkt)) {
				throw error("dpop_jkt in request doesn't match DPoP proof JWK thumbprint.", args("dpop_jkt", dpop_jkt, "DPoP thumbprint", computed_dpop_jkt));
			} else {
				env.putString("authorization_code_dpop_jkt", computed_dpop_jkt);
				logSuccess("dpop_jkt matches computed JWK thumbprint", args("dpop_jkt", dpop_jkt));
			}
		} else {
			if(!Strings.isNullOrEmpty(computed_dpop_jkt)) {
				env.putString("authorization_code_dpop_jkt", computed_dpop_jkt);
				logSuccess("Using computed_dpop_jkt as dpop_jkt", args("authorization_code_dpop_jkt", computed_dpop_jkt));
			} else if(!Strings.isNullOrEmpty(dpop_jkt)) {
				env.putString("authorization_code_dpop_jkt", dpop_jkt);
				logSuccess("Using dpop_jkt in request", args("dpop_jkt", dpop_jkt));
			} else if(Strings.isNullOrEmpty(dpop_jkt) && Strings.isNullOrEmpty(computed_dpop_jkt)) {
				env.putString("authorization_code_dpop_jkt", "");
				logSuccess("Request does not use DPoP Authorization code binding");
			}
		}
		return env;
	}

}

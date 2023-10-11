package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.nimbusds.jose.JWEObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class EnsureBackchannelRequestObjectWasNotEncrypted extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"backchannel_endpoint_http_request"})
	public Environment evaluate(Environment env) {
		try {
			String request = env.getString("backchannel_endpoint_http_request", "body_form_params.request");
			if (Strings.isNullOrEmpty(request)) {
				throw error("Request object is missing", args("request", request));
			}
			JWEObject.parse(request);
			throw error("Request object was encrypted", args("request", request));
		} catch (ParseException e) {
			logSuccess("Request object was not encrypted");
		}
		return env;
	}

}

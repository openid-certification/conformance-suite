package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * JAR 10.8:
 * never use the Client ID as the "sub" value in a Request Object.
 */
public class EnsureBackchannelRequestObjectDoesNotContainSubWithClientId extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "backchannel_request_object", "client" })
	public Environment evaluate(Environment env) {
		String sub = env.getString("backchannel_request_object", "claims.sub");
		String clientId = env.getString("client", "client_id");

		if (!Strings.isNullOrEmpty(sub) && sub.equals(clientId)) {
			throw error("Request object sub must not be Client ID - this is a security concern as it may allow the request object to be used as a client authentication assertion.",
					args("sub", clientId));
		} else {
			logSuccess("Request object does not contain Client Id in sub");
			return env;
		}

	}

}

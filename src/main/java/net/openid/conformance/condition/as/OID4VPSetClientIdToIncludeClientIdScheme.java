package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OID4VPSetClientIdToIncludeClientIdScheme extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client", strings = "client_id_scheme")
	@PostEnvironment(required = "client", strings = { "client_id", "orig_client_id" })
	public Environment evaluate(Environment env) {
		String scheme = env.getString("client_id_scheme");
		String origClientId = env.getString("client", "client_id");

		// note that this is NOT a url (so there is no need to do any escaping) - it's simply a string where the
		// first ':' is a delimiter
		String newClientId = scheme + ":" + origClientId;

		env.putString("client", "client_id", newClientId);
		env.putString("client_id", newClientId);

		// "orig_client_id" is language used in https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID3.html#section-5.10.1
		env.putString("orig_client_id", origClientId);

		logSuccess("Set client id to include the client id scheme", args("new_client_id", newClientId));
		return env;
	}

}

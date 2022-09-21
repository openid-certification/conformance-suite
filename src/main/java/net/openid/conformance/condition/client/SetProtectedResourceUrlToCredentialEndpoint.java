package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetProtectedResourceUrlToCredentialEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String resourceUrl = "https://issuer.research.identiproof.io/credential";// FIXME env.getString("server", "whatever_endpoint");

		if(Strings.isNullOrEmpty(resourceUrl)){
			throw error("userinfo_endpoint missing from server configuration. The user info is not a mandatory to implement feature in the OpenID Connect specification, but is mandatory for certification.");
		}

		env.putString("protected_resource_url", resourceUrl);

		logSuccess("Set credential endpoint to be used as the protected resource.", args("protected_resource_url", resourceUrl));

		return env;
	}

}

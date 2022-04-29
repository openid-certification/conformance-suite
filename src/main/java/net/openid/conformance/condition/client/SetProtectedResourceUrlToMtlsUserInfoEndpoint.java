package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetProtectedResourceUrlToMtlsUserInfoEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String resourceUrl;

		resourceUrl = env.getString("server", "mtls_endpoint_aliases.userinfo_endpoint");
		if (resourceUrl == null) {
			resourceUrl = env.getString("server", "userinfo_endpoint");
		}


		if(Strings.isNullOrEmpty(resourceUrl)){
			throw error("userinfo_endpoint missing from server configuration. The user info is not a mandatory to implement feature in the OpenID Connect specification, but is mandatory for certification.");
		}

		env.putString("protected_resource_url", resourceUrl);

		logSuccess("userinfo_endpoint will be used to test access token. The user info is not a mandatory to implement feature in the OpenID Connect specification, but is mandatory for certification.", args("protected_resource_url", resourceUrl));

		return env;
	}

}

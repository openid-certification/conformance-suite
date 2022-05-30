package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public abstract class AbstractCallProtectedResourceWithBearerToken extends AbstractCallProtectedResource {

	@Override
	protected HttpHeaders getHeaders(Environment env) {

		HttpHeaders headers = super.getHeaders(env);


		String accessToken = env.getString("access_token", "value");
		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Access token not found");
		}

		String tokenType = env.getString("access_token", "type");
		if (Strings.isNullOrEmpty(tokenType)) {
			throw error("Token type not found");
		} else if (tokenType.equalsIgnoreCase("Bearer") || tokenType.equalsIgnoreCase("DPoP")) {
			headers.set("Authorization", tokenType + " " + accessToken);
		} else {
			throw error("Access token is neither a bearer nor a dpop token", args("token_type", tokenType));
		}


		return headers;
	}
}

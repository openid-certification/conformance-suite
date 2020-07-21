package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class EnsureOPDoesNotUseDefaultRedirectUriInCaseOfInvalidRedirectUri extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("An invalid redirect_uri was included in the authorization request but the OP redirected to a " +
			"default redirect_uri instead of displaying an error page");
	}

}

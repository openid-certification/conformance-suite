package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractEnsureMinimumEntropy;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * PAR-6.0: This class checks for required entropy in request_uri from PAR response.
 */
public class EnsureMinimumRequestUriEntropy extends AbstractEnsureMinimumEntropy {

	/*
	 draft-ietf-oauth-jwsreq-20 : 12.2.1 : A general guidance for the
	 validity time would be less than a minute and the Request Object URI
	 is to include a cryptographic random value of 128bit or more.
	 We can't accurately measure entropy so a bit of slop is allowed for.
	 */
	private final double requiredEntropy = 96;

	/* (non-Javadoc)
	 * @see Condition#evaluate(Environment)
	 */
	@Override
	@PreEnvironment(required = CallPAREndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {
		String requestUri = env.getString(CallPAREndpoint.RESPONSE_KEY, "body_json.request_uri");

		if (Strings.isNullOrEmpty(requestUri)) {
			throw error("Can't find requestUri ");
		}

		return ensureMinimumEntropy(env, requestUri, requiredEntropy);
	}

}

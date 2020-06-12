package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateFrontchannelLogoutIss extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "frontchannel_logout_request", "server" } )
	public Environment evaluate(Environment env) {

		String issuer = env.getString("server", "issuer"); // to validate the issuer
		String issuerInRequest = env.getString("frontchannel_logout_request", "query_string_params.iss");

		if (Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find issuer");
		}

		if (Strings.isNullOrEmpty(issuerInRequest)) {
			throw error("'iss' missing from frontchannel logout request");
		}

		if (!issuer.equals(issuerInRequest)) {
			throw error("Issuer mismatch", args("expected", issuer, "actual", issuerInRequest));
		}

		logSuccess("'iss' in frontchannel logout request matches server issuer");
		return env;

	}

}

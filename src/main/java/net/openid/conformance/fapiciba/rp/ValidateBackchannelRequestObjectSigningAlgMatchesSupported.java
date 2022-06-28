package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.stream.StreamSupport;

public class ValidateBackchannelRequestObjectSigningAlgMatchesSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	public Environment evaluate(Environment env) {

		String algUsed = env.getString("authorization_request_object", "header.alg");
		JsonArray supportedAlgs =
			env.getElementFromObject("server", "backchannel_authentication_request_signing_alg_values_supported").getAsJsonArray();

		if (algUsed == null || Strings.isNullOrEmpty(algUsed)) {
			throw error("Request does not advertise signature algorithm in header");
		}

		boolean isSupported = StreamSupport
				.stream(supportedAlgs.spliterator(), false)
				.anyMatch(alg -> algUsed.equals(OIDFJSON.getString(alg)));

		if(!isSupported) {
			throw error("Request is not signed using one of the backchannel_authentication_request_signing_alg_values_supported algorithms.",
					args("supported", supportedAlgs, "actual", algUsed));
		}

		logSuccess("Request is signed using one of the backchannel_authentication_request_signing_alg_values_supported algorithms",
				args("supported", supportedAlgs, "actual", algUsed));

		return env;
	}
}

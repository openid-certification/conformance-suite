package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class CheckDiscEndpointRequestObjectSigningAlgValuesSupportedIncludesRS256 extends AbstractValidateJsonArray {

	private static final String environmentVariable = "request_object_signing_alg_values_supported";

	private static final String[] SET_VALUES = { "RS256" };
	private static final int minimumMatchesRequired = SET_VALUES.length;

	private static final String errorMessageNotEnough = "The server does not support RS256; this is a 'should' in https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata - note that support for 'none' (unsigned request objects) is not required as use of this is discouraged in many circumstances, see https://gitlab.com/openid/conformance-suite/-/issues/826";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired, errorMessageNotEnough);
	}
}

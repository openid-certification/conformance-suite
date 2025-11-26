package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class AustraliaConnectIdCheckTrustFrameworkSupported extends AbstractValidateJsonArray {


	private static final String environmentVariable = "trust_frameworks_supported";

	public static final String ConnectIdTrustFramework = "au_connectid";

	private static final int minimumMatchesRequired = 1;

	private static final String errorMessageNotEnough = "The server does not support the expected trust_framework";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, List.of(ConnectIdTrustFramework), minimumMatchesRequired, errorMessageNotEnough);
	}
}

package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class AustraliaConnectIdCheckVerifiedClaimsSupported extends AbstractValidateJsonArray {

	private static final String environmentVariable = "claims_in_verified_claims_supported";

	public static final List<String> ConnectIdVerifiedClaims = List.of (
		"over16",
		"over18",
		"over21",
		"over25",
		"over65",
		"beneficiary_account_au",
		"beneficiary_account_au_payid",
		"beneficiary_account_international"
	);

	private static final int minimumMatchesRequired = 1;

	private static final String errorMessageNotEnough = "The server does not support the expected verified claims.";
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, ConnectIdVerifiedClaims, minimumMatchesRequired, errorMessageNotEnough);
	}
}

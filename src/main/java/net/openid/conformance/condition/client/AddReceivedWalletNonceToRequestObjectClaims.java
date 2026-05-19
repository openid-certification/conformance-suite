package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddReceivedWalletNonceToRequestObjectClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims", strings = "received_wallet_nonce")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		String walletNonce = env.getString("received_wallet_nonce");
		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		requestObjectClaims.addProperty("wallet_nonce", walletNonce);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added wallet_nonce claim to request object",
			args("wallet_nonce", walletNonce));

		return env;
	}
}

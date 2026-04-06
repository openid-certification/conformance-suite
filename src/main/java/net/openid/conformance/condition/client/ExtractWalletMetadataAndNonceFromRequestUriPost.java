package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractWalletMetadataAndNonceFromRequestUriPost extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		JsonObject formParams = env.getElementFromObject("incoming_request", "body_form_params").getAsJsonObject();

		String walletNonce = null;
		if (formParams.has("wallet_nonce")) {
			walletNonce = OIDFJSON.getString(formParams.get("wallet_nonce"));
			env.putString("received_wallet_nonce", walletNonce);
		}

		JsonObject walletMetadata = null;
		if (formParams.has("wallet_metadata")) {
			String walletMetadataStr = OIDFJSON.getString(formParams.get("wallet_metadata"));
			try {
				JsonElement parsed = JsonParser.parseString(walletMetadataStr);
				walletMetadata = parsed.getAsJsonObject();
				env.putObject("received_wallet_metadata", walletMetadata);
			} catch (JsonSyntaxException | IllegalStateException e) {
				throw error("wallet_metadata is not valid JSON", args("wallet_metadata", walletMetadataStr));
			}
		}

		logSuccess("Extracted parameters from request_uri POST body",
			args("wallet_nonce", walletNonce, "wallet_metadata", walletMetadata));

		return env;
	}

}

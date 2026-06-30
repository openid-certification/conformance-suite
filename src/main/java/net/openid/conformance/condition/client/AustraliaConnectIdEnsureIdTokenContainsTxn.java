package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.util.StringUtils;

public class AustraliaConnectIdEnsureIdTokenContainsTxn extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		JsonElement txnElement = env.getElementFromObject("id_token", "claims.txn");
		if (txnElement == null) {
			throw error("id_token does not contain txn claim");
		}

		String txn = OIDFJSON.getString(txnElement);
		if (!StringUtils.hasText(txn)) {
			throw error("id_token contains an empty txn claim");
		}

		logSuccess("id_token contains the required ConnectID txn claim", args("txn", txn));
		return env;
	}
}

package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIExtractTxCodeFromRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonElement codeEl = env.getElementFromObject("client_request", "query_string_params.code");
		if (codeEl == null) {
			throw error("Could not find code in query_string_params",
				args("query_string_params", env.getElementFromObject("client_request", "query_string_params")));
		}

		String txCode = OIDFJSON.getString(codeEl);
		env.putString("vci", "tx_code", txCode);

		log("Use static_tx_code from config", args("tx_code", txCode));

		return env;
	}
}

package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIUseStaticTxCodeFromConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config"})
	public Environment evaluate(Environment env) {

		JsonElement staticTxCodeEl = env.getElementFromObject("config", "vci.static_tx_code");
		if (staticTxCodeEl == null) {
			throw error("Could not find vci.static_tx_code in config.");
		}

		String txCode = OIDFJSON.getString(staticTxCodeEl);
		env.putString("vci", "tx_code", txCode);

		log("Use static_tx_code from config", args("tx_code", txCode));

		return env;
	}
}

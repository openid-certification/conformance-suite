package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Objects;

public class VCIValidateTxCode extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_request", "vci"})
	public Environment evaluate(Environment env) {

		JsonElement txCodeEl = env.getElementFromObject("incoming_request", "body_form_params.tx_code");
		if (txCodeEl == null) {
			throw error("Couldn't find tx_code in request body form params");
		}

		String txCode = OIDFJSON.getString(txCodeEl);

		JsonElement expectedTxCodeEl = env.getElementFromObject("vci", "pre-authorized_code_tx_code_value");
		if (expectedTxCodeEl == null) {
			throw error("Couldn't find expected tx_code in env");
		}

		String expectedTxCode = OIDFJSON.getString(expectedTxCodeEl);
		if (!Objects.equals(expectedTxCode, txCode)) {
			throw error("Received tx_code does not match expected tx_code", args("expected_tx_code", expectedTxCode, "actual_tx_code", txCode));
		}

		logSuccess("Received tx_code matches expected tx_code", args("expected_tx_code", expectedTxCode, "actual_tx_code", txCode));

		return env;
	}
}

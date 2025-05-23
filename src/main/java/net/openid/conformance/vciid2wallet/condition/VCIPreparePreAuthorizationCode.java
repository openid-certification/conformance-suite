package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class VCIPreparePreAuthorizationCode extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String preAuthCode = RandomStringUtils.secure().nextAlphanumeric(32);

		env.putString("vci","pre-authorized_code", preAuthCode);

		String code = RandomStringUtils.secure().nextNumeric(6);
		int length = code.length();
		String inputMode = "numeric";
		String description = "Input the one-time code: " + code + " testing purposes";

		JsonObject txCode = new JsonObject();
		txCode.addProperty("length", length);
		txCode.addProperty("input_mode", inputMode);
		txCode.addProperty("description", description);

		env.putString("vci","pre-authorized_code_tx_code_value", code);
		env.putObject("vci","pre-authorized_code_tx_code", txCode);

		logSuccess("Prepared pre-authorized code", args("pre-authorized_code", preAuthCode, "tx_code", txCode));

		return env;
	}
}

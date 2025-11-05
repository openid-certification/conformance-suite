package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class VCIEnsureX5cHeaderPresentForSdJwtCredential extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {

		JsonObject credentialHeader = env.getElementFromObject("sdjwt", "credential.header").getAsJsonObject();

		if (!credentialHeader.has("x5c")) {
			throw error("Credential MUST contain an x5c in the header", args("credential_header", credentialHeader));
		}

		List<String> x5c = OIDFJSON.convertJsonArrayToList(credentialHeader.getAsJsonArray("x5c"));

		logSuccess("Found credential x5c claim in header", args("x5c", x5c));

		return env;
	}

}

package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractCheckIdTokenSignatureAlgorithm extends AbstractCondition {

	protected Environment checkIdTokenSignatureAlgorithm(Environment env, String expectedAlg) {

		String alg = env.getString("id_token", "header.alg");
		if (Strings.isNullOrEmpty(alg)) {
			throw error("alg not present in ID token header", args("header", env.getElementFromObject("id_token", "header")));
		}

		if (alg.equals(expectedAlg)) {
			logSuccess("ID token was signed with \"" + expectedAlg + "\" as expected");
		} else {
			throw error("ID token signature algorithm is not \"" + expectedAlg + "\"", args("alg", alg));
		}

		return env;
	}

}

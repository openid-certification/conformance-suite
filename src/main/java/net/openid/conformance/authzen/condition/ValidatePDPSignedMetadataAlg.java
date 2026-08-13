package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWAUtil;


public class ValidatePDPSignedMetadataAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		String alg = env.getString("pdp_signed_metadata", "header.alg");
		// §9.1.3 requires signed_metadata to be signed or MACed using JWS, so `alg` has to name an
		// algorithm registered for JWS use. JWAUtil.isJwsAlgorithm() alone would reject everything
		// below; the first two checks run ahead of it only to name the two failures worth telling
		// apart — no `alg` at all, and an explicitly unsecured JWT.
		if (Strings.isNullOrEmpty(alg)) {
			throw error("PDP signed_metadata has no `alg` header parameter, so it is not a JWS", args("alg", alg));
		}
		if ("none".equalsIgnoreCase(alg)) {
			throw error("PDP signed_metadata uses `alg: none`, i.e. it is unsecured; it must be signed or MACed "
				+ "using JWS", args("alg", alg));
		}
		// Anything else unusable — an unregistered name, or a JWE key management algorithm such as
		// 'dir' or 'A128KW' — names no key that could verify the signature, so reject it here rather
		// than letting the downstream signature verification fail on it.
		if (!JWAUtil.isJwsAlgorithm(alg)) {
			throw error("PDP signed_metadata 'alg' is not a registered JWS signature or MAC algorithm, "
				+ "so its signature cannot be verified", args("alg", alg));
		}
		logSuccess("PDP signed_metadata signed with valid alg", args("alg", alg));
		return env;
	}
}

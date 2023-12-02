package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class EnsureDpopProofJtiNotUsed extends AbstractCondition {

	private static final int CACHE_SIZE = 256;
	private static final List<String> jtiCache = new ArrayList<>(CACHE_SIZE);

	@Override
	@PreEnvironment(required = "incoming_dpop_proof")
	public Environment evaluate(Environment env) {
		String jti = env.getString("incoming_dpop_proof", "claims.jti");
		if(Strings.isNullOrEmpty(jti)) {
			throw error("DPoP Proof jti not found in request");
		}
		synchronized (jtiCache) {
			if(jtiCache.contains(jti)) {
				throw error("DPoP Proof jti has been used", args("jti", jti));
			} else {
				if(jtiCache.size() >= CACHE_SIZE) {
					jtiCache.subList(0, 50).clear();
				}
				jtiCache.add(jti);
				logSuccess("DPoP Proof jti has not been used", args("jti", jti));
			}
		}
		return env;
	}

}

package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddQueryAndFragmentToDpopHtu extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dpop_proof_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		String htu = OIDFJSON.getString(claims.get("htu"));
		htu = htu + "?allthedoorsonthisspaceshiphavebeen#programmedtohaveacheeryandsunnydisposition";
		claims.addProperty("htu", htu);

		logSuccess("Added query/fragment (which must be ignored as per 4.3-9 in DPoP spec) to htu in DPoP proof claims", claims);

		return env;

	}
}

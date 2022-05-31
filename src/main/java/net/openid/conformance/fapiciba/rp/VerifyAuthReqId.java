package net.openid.conformance.fapiciba.rp;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VerifyAuthReqId extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "auth_req_id")
	public Environment evaluate(Environment env) {

		String expectedAuthReqId = env.getString("auth_req_id");
		String actualAuthReqId = env.getString("token_endpoint_request", "body_form_params.auth_req_id");

		if (!expectedAuthReqId.equals(actualAuthReqId)) {
			throw error("Mismatch in auth_req_id", args("expected", expectedAuthReqId, "actual", actualAuthReqId));
		}

		Boolean authReqIdRedeemed = env.getBoolean("auth_req_id_redeemed");
		if (authReqIdRedeemed != null && authReqIdRedeemed) {
			throw error("The auth_req_id has already been redeemed", args("expected", expectedAuthReqId, "actual", actualAuthReqId));
		}

		logSuccess("Expected auth_req_id found in the request", ImmutableMap.of("auth_req_id", actualAuthReqId));
		return env;
	}
}

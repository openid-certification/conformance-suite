package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RedeemAuthReqId extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.putBoolean("auth_req_id_redeemed", true);
		logSuccess("The auth_req_id has been redeemed for a successful token response");
		return env;
	}
}

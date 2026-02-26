package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.rs.EnsureBearerAccessTokenNotInParams;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10wallet.VCIErrorResponseUtil;

public class VCIEnsureBearerAccessTokenNotInParams extends EnsureBearerAccessTokenNotInParams {

	@Override
	public Environment evaluate(Environment env) {
		try {
			return super.evaluate(env);
		} catch(ConditionError e) {
			VCIErrorResponseUtil.setErrorResponse(env, "invalid_request", e.getMessage());
			throw e;
		}
	}
}

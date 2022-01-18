package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureNoRejectionReasonIFStatusIsNotRJCT extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("consent_endpoint_response");
		JsonObject data = response.getAsJsonObject("data");
		String status = OIDFJSON.getString(data.get("status"));

		JsonElement rejectionReasonElement = data.get("rejectionReason");

		if (!status.equals("RJCT") && rejectionReasonElement != null){
			throw error("A rejection reason should only be sent with a status of RJCT");
		}
		return env;
	}
}

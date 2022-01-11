package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureACCDoesNotHaveRejectionReason extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("consent_endpoint_response");
		JsonObject data = response.getAsJsonObject("data");

		String status;
		String rejectionReason = null;
		try {
			status = OIDFJSON.getString(data.get("status"));
		} catch (NullPointerException e){
			throw error("No status found inside response");
		}

		try {
			rejectionReason = OIDFJSON.getString(data.get("rejectionReason"));
		} catch (NullPointerException e){
			log("No rejection reason found in response");
		}

		if (status.equals("ACCC") && rejectionReason != null){
			throw error("A status of ACCC should not be sent with a rejection reason");
		} else {
			logSuccess("Status ACCC is not sent with rejection reason");
		}

		return env;
	}
}

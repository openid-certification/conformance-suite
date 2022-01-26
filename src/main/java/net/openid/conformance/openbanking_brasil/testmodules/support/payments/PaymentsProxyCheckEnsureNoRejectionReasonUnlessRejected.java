package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class PaymentsProxyCheckEnsureNoRejectionReasonUnlessRejected extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject responseBody = env.getObject("resource_endpoint_response");
		JsonObject data = responseBody.getAsJsonObject("data");
		String status = OIDFJSON.getString(data.get("status"));

		if (status.equals("RJCT")) {
			log("Payment rejected - not checking any further");
		} else {
			boolean rejectionReason = data.has("rejectionReason");
			if(rejectionReason) {
				throw error("Payment is in " + status + " state but has a rejection reason. This should only be present once payment is rejected");
			}
		}
		return env;
	}

}

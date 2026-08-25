package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIVerifyCredentialOfferRequestId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String requestUrl = env.getString("incoming_request", "request_url");
		String requestedId = requestUrl == null ? null : requestUrl.substring(requestUrl.lastIndexOf('/') + 1);

		// "vci" is deliberately not in @PreEnvironment: a pre-environment failure is
		// converted to a hard test failure regardless of the caller's WARNING severity.
		String expectedId = env.getString("vci", "credential_offer_id");
		if (expectedId == null) {
			throw error("No credential offer has been created by this test, so there is nothing to serve; the endpoint will return a 404",
				args("requested_id", requestedId));
		}

		// Clear any sentinel a previous request left so a mismatch can't inherit an
		// earlier match's 200. (expectedId != null implies the "vci" object exists,
		// so removeElement cannot throw; a missing leaf is a no-op.)
		env.removeElement("vci", "credential_offer_id_matched");

		if (expectedId.equals(requestedId)) {
			env.putString("vci", "credential_offer_id_matched", requestedId);
			logSuccess("credential_offer_uri id matches the credential offer this test created",
				args("requested_id", requestedId));
			return env;
		}
		throw error("The id in the credential_offer_uri path does not match the credential offer this test created; the endpoint will return a 404",
			args("requested_id", requestedId, "expected_id", expectedId));
	}
}

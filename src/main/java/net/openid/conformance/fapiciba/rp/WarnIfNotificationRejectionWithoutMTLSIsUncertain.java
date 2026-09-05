package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class WarnIfNotificationRejectionWithoutMTLSIsUncertain extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "notification_without_mtls_rejection")
	public Environment evaluate(Environment env) {
		if ("connection_closed".equals(env.getString("notification_without_mtls_rejection"))) {
			throw error("The notification connection closed without an HTTP response. This can be a TLS client " +
				"certificate rejection, but it can also be a transport failure. Confirm that the endpoint " +
				"accepts a valid mTLS notification and rejects this request because its certificate is missing.");
		}
		logSuccess("The notification endpoint returned an HTTP or TLS rejection");
		return env;
	}
}

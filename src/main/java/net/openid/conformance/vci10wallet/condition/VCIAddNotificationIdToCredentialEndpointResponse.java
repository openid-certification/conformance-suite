package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class VCIAddNotificationIdToCredentialEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"credential_endpoint_response"})
	public Environment evaluate(Environment env) {

		var response = env.getObject("credential_endpoint_response");

		String notificationId = RandomStringUtils.secure().nextAlphanumeric(22);
		response.addProperty("notification_id", notificationId);
		env.putString("notification_id", notificationId);

		log("Added notification_id to credential response object",
			args("credential_endpoint_response", response,
				"notification_id", notificationId));

		return env;
	}
}

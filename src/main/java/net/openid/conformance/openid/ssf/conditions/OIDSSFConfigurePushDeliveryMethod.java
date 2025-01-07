package net.openid.conformance.openid.ssf.conditions;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFConfigurePushDeliveryMethod extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String pushDeliveryEndpointUrl = createPushDeliveryEndpointUrl(env);
		env.putString("ssf", "push_delivery_endpoint_url", pushDeliveryEndpointUrl);

		logSuccess("Configured push delivery endpoint url", args("push_delivery_endpoint_url", pushDeliveryEndpointUrl));

		return env;
	}

	protected String createPushDeliveryEndpointUrl(Environment env) {

		String baseUrl = env.getString("base_url");
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}
		return "${baseUrl}/ssf-push".replace("${baseUrl}", baseUrl);
	}
}

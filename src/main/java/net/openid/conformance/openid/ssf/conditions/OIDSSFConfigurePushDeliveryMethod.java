package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.BaseUrlUtil;

public class OIDSSFConfigurePushDeliveryMethod extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String pushDeliveryEndpointUrl = createPushDeliveryEndpointUrl(env);
		env.putString("ssf", "push_delivery_endpoint_url", pushDeliveryEndpointUrl);

		log("Configured push delivery endpoint url", args("push_delivery_endpoint_url", pushDeliveryEndpointUrl));

		return env;
	}

	protected String createPushDeliveryEndpointUrl(Environment env) {

		String receiverBaseUrl = env.getString("config", "ssf.receiver.base_url_override");
		if (receiverBaseUrl == null) {
			receiverBaseUrl = BaseUrlUtil.resolveEffectiveBaseUrl(env);
		}
		String pushEndpointUrl = receiverBaseUrl + "/ssf-push";
		return pushEndpointUrl;
	}

}

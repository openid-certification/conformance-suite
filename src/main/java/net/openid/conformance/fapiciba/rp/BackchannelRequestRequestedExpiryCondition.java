package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Splitter;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.openqa.selenium.json.Json;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.Charset;
import java.util.List;

public class BackchannelRequestRequestedExpiryCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	public Environment evaluate(Environment env) {

		String requestedExpiryString = env.getString("backchannel_endpoint_http_request", "body_form_params.requested_expiry");

		if(requestedExpiryString == null) {
			logSuccess("Backchannel authentication request does not contain optional parameter 'requested_expiry'");
			return env;
		} else {
			try {
				Integer requestedExpiryValue = Integer.parseInt(requestedExpiryString);
				if(requestedExpiryValue <= 0) {
					throw error("The 'requested_expiry' must be a positive integer when present.");
				}
			} catch (NumberFormatException nfe) {
				throw error("The 'requested_expiry' must be a positive integer when present.");
			}
		}

		logSuccess("Backchannel authentication request contains valid parameter 'requested_expiry'");
		return env;
	}
}

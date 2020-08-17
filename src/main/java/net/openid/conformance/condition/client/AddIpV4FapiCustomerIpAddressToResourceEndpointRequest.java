package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.client.utils.DateUtils;

import java.util.Date;

public class AddIpV4FapiCustomerIpAddressToResourceEndpointRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getObject("resource_endpoint_request_headers");

		headers.addProperty("x-fapi-customer-ip-address", "198.51.100.119");

		log("Added x-fapi-customer-ip-address to resource endpoint request headers", args("resource_endpoint_request_headers", headers));

		return env;
	}

}

package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIpV6FapiCustomerIpAddressToResourceEndpointRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getObject("resource_endpoint_request_headers");

		String ip = env.getString("resource", "x_fapi_customer_ipv4_address");
		if (Strings.isNullOrEmpty(ip)) {
			ip = "2001:DB8::1893:25c8:1946";
		}
		headers.addProperty("x-fapi-customer-ip-address", ip);

		log("Added x-fapi-customer-ip-address containing IPv6 address to resource endpoint request headers", args("resource_endpoint_request_headers", headers));

		return env;
	}

}

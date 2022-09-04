package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.InetAddress;

public class ExtractFapiIpAddressHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "fapi_customer_ip_address")
	public Environment evaluate(Environment env) {

		String header = env.getString("incoming_request", "headers.x-fapi-customer-ip-address");
		if (Strings.isNullOrEmpty(header)) {
			throw error("Couldn't find FAPI ip address header");
		} else {

			// try to parse it to make sure it's in the right format
			try {

				InetAddress addr = InetAddresses.forString(header);

				env.putString("fapi_customer_ip_address", header);
				logSuccess("Found a FAPI ip address header", args("fapi_customer_ip_address", header, "addr", addr.getHostAddress()));

				return env;

			} catch (IllegalArgumentException e) {
				throw error("Could not parse FAPI ip address header", args("fapi_customer_ip_address", header));
			}

		}

	}

}

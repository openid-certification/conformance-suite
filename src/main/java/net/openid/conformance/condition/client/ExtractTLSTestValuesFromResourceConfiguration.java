package net.openid.conformance.condition.client;

import java.net.MalformedURLException;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.TLSTestValueExtractor;
import net.openid.conformance.testmodule.Environment;

public class ExtractTLSTestValuesFromResourceConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource")
	@PostEnvironment(required = "resource_endpoint_tls")
	public Environment evaluate(Environment env) {

		try {
			String resourceEndpoint = env.getString("resource", "resourceUrl");
			if (Strings.isNullOrEmpty(resourceEndpoint)) {
				throw error("Resource endpoint not found");
			}

			JsonObject resourceEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(resourceEndpoint);

			env.putObject("resource_endpoint_tls", resourceEndpointTls);

			logSuccess("Extracted TLS information from resource endpoint", args(
					"resource_endpoint", resourceEndpointTls
				));

			return env;
		} catch (MalformedURLException e) {
			throw error("URL not properly formed", e);
		}
	}

}

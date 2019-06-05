package io.fintechlabs.testframework.condition.client;

import java.net.MalformedURLException;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.util.TLSTestValueExtractor;
import io.fintechlabs.testframework.testmodule.Environment;

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

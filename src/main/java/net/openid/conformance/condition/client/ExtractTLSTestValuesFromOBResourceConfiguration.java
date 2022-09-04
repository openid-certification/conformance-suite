package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.TLSTestValueExtractor;
import net.openid.conformance.openbanking.FAPIOBGetResourceEndpoint;
import net.openid.conformance.testmodule.Environment;

import java.net.MalformedURLException;

public class ExtractTLSTestValuesFromOBResourceConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource")
	@PostEnvironment(required = {"accounts_resource_endpoint_tls", "accounts_request_endpoint_tls"})
	public Environment evaluate(Environment env) {
		try {

			String accountsResourceEndpoint = FAPIOBGetResourceEndpoint.getBaseResourceURL(env, FAPIOBGetResourceEndpoint.Endpoint.ACCOUNTS_RESOURCE);
			if (Strings.isNullOrEmpty(accountsResourceEndpoint)) {
				throw error("Accounts resource endpoint not found");
			}

			JsonObject accountsResourceEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(accountsResourceEndpoint);

			env.putObject("accounts_resource_endpoint_tls", accountsResourceEndpointTls);

			String accountsRequestEndpoint = FAPIOBGetResourceEndpoint.getBaseResourceURL(env, FAPIOBGetResourceEndpoint.Endpoint.ACCOUNT_REQUESTS);
			if (Strings.isNullOrEmpty(accountsRequestEndpoint)) {
				throw error("Accounts resource endpoint not found");
			}

			JsonObject accountsRequestEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(accountsRequestEndpoint);

			env.putObject("accounts_request_endpoint_tls", accountsRequestEndpointTls);


			logSuccess("Extracted TLS information from resource endpoint", args(
					"accounts_resource_endpoint", accountsResourceEndpointTls,
					"accounts_request_endpoint", accountsRequestEndpointTls
				));

			return env;
		} catch (MalformedURLException e) {
			throw error("URL not properly formed", e);
		}
	}

}

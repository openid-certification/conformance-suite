package io.fintechlabs.testframework.condition.client;

import java.net.MalformedURLException;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.util.TLSTestValueExtractor;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.openbanking.FAPIOBGetResourceEndpoint;
import io.fintechlabs.testframework.openbanking.FAPIOBGetResourceEndpoint.Endpoint;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractTLSTestValuesFromOBResourceConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractTLSTestValuesFromOBResourceConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "resource")
	@PostEnvironment(required = {"accounts_resource_endpoint_tls", "accounts_request_endpoint_tls"})
	public Environment evaluate(Environment env) {
		try {

			String accountsResourceEndpoint = FAPIOBGetResourceEndpoint.getBaseResourceURL(env, Endpoint.ACCOUNTS_RESOURCE);
			if (Strings.isNullOrEmpty(accountsResourceEndpoint)) {
				throw error("Accounts resource endpoint not found");
			}

			JsonObject accountsResourceEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(accountsResourceEndpoint);

			env.putObject("accounts_resource_endpoint_tls", accountsResourceEndpointTls);

			String accountsRequestEndpoint = FAPIOBGetResourceEndpoint.getBaseResourceURL(env, Endpoint.ACCOUNT_REQUESTS);
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

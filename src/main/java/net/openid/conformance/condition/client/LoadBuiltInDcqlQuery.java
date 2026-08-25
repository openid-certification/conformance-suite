package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.testmodule.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads one of the suite's built-in DCQL queries from the classpath, so that a tester who is
 * using a well known credential type does not have to write a DCQL query by hand.
 *
 * The resource to load is chosen by the test module (from its credential type variant) and
 * passed in the environment; the query is stored under the same environment key that
 * {@link ExtractDCQLQueryFromClientConfiguration} uses, so everything downstream — including
 * the modules that mutate the query — is unaffected by which of the two produced it.
 */
public class LoadBuiltInDcqlQuery extends AbstractCondition {

	public static final String RESOURCE_ENV_KEY = "dcql_builtin_resource";

	@Override
	@PreEnvironment(strings = RESOURCE_ENV_KEY)
	@PostEnvironment(required = ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY)
	public Environment evaluate(Environment env) {
		String resource = env.getString(RESOURCE_ENV_KEY);

		JsonObject dcql = loadResource(resource);

		env.putObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY, dcql);

		logSuccess("Using the conformance suite's built-in DCQL query for the selected credential type; "
			+ "select the 'custom' credential type if you need to supply your own query",
			args("dcql", dcql, "resource", resource));

		return env;
	}

	private JsonObject loadResource(String resource) {
		try (InputStream is = getClass().getResourceAsStream(resource)) {
			if (is == null) {
				throw error("Could not find the built-in DCQL query resource", args("resource", resource));
			}
			String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			return JsonParser.parseString(json).getAsJsonObject();
		} catch (IOException e) {
			throw error("Failed to read the built-in DCQL query resource", e, args("resource", resource));
		}
	}
}

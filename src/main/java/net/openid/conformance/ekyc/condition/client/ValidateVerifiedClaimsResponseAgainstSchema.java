package net.openid.conformance.ekyc.condition.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.Set;

public class ValidateVerifiedClaimsResponseAgainstSchema extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonObject verifiedClaimsResponse = env.getObject("verified_claims_response");
		JsonElement claimsElement = null;
		String location = "";
		//TODO I assumed id_token will be processed before userinfo so if we have userinfo then just process it
		// otherwise process id_token
		if(verifiedClaimsResponse.has("userinfo")) {
			claimsElement = verifiedClaimsResponse.get("userinfo");
			location = "userinfo";
		} else {
			claimsElement = verifiedClaimsResponse.get("id_token");
			location = "id_token";
		}
		if(claimsElement==null) {
			throw error("Could not find verified_claims");
		}
		//we add the outer {"verified_claims":...} here
		String claimsJson = "{\"verified_claims\":" + claimsElement.toString() + "}";

		Set<ValidationMessage> errors = checkSchema(claimsJson);
		if (!errors.isEmpty()) {
			JsonArray jsonErrors = new JsonArray();
			for (ValidationMessage error: errors) {
				jsonErrors.add(error.toString());
			}
			throw error("Failed to validate verified_claims against schema",
				args("verified_claims", new JsonParser().parse(claimsJson),
					"errors", jsonErrors));
		}
		logSuccess("Verified claims are valid", args("location", location, "verified_claims", claimsElement));
		return env;
	}

	private ObjectMapper mapper = new ObjectMapper();

	protected Set<ValidationMessage> checkSchema(String verifiedClaimsJson) {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("json-schemas/ekyc/verified_claims-12.json");

		// fixme load to node first then use         JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(jsonNode));
		JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7); // current ekyc schemas use draft 7
		JsonSchema schema = factory.getSchema(inputStream);

		JsonNode node;
		try {
			node = mapper.readTree(verifiedClaimsJson);
		} catch (JsonProcessingException e) {
			throw error("Failed to parse JSON", e);
		}

		Set<ValidationMessage> errors = schema.validate(node);

		return errors;
	}

}

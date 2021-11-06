package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.xpath.operations.Bool;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;

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
		try {
			checkSchema(claimsJson);
		} catch (ValidationException ex) {
			throw error("Failed to validate verified_claims against schema", ex,
				args("verified_claims", claimsJson,
					"errors", new JsonParser().parse(ex.toJSON().toString())));
		}
		logSuccess("Verified claims are valid", args("location", location, "verified_claims", claimsElement));
		return env;
	}

	protected void checkSchema(String verifiedClaimsJson) {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("json-schemas/ekyc/verified_claims-12.json");

		JSONObject jsonSchema = new JSONObject(new JSONTokener(inputStream));
		JSONObject jsonSubject = new JSONObject(new JSONTokener(verifiedClaimsJson));
		Schema schema = SchemaLoader.load(jsonSchema);
		schema.validate(jsonSubject);

	}

}

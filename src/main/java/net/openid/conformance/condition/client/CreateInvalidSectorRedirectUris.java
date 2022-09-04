package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateInvalidSectorRedirectUris extends AbstractCondition {

	@Override
	@PostEnvironment(required = "sector_redirect_uris")
	public Environment evaluate(Environment env) {

		JsonArray sectorRedirectUris = new JsonArray();
		sectorRedirectUris.add("https://example.com/op");

		JsonObject obj = new JsonObject();
		obj.add("value", sectorRedirectUris);

		env.putObject("sector_redirect_uris", obj);

		log("Created invalid sector redirect URIs", args("sector_redirect_uris", obj));

		return env;
	}

}

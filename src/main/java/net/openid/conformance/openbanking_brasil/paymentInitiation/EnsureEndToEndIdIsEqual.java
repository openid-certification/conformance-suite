package net.openid.conformance.openbanking_brasil.paymentInitiation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Base64;

public class EnsureEndToEndIdIsEqual extends AbstractJsonAssertingCondition {

	private static String endToEndId;

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full", strings = "endToEndId")
	public Environment evaluate(Environment environment) {
		String requestEntity = environment.getString("resource_request_entity");
		if (requestEntity != null) {
			JsonObject requestBody = new Gson().fromJson(new String(Base64.getUrlDecoder().decode(requestEntity.split("\\.")[1].getBytes())), JsonObject.class);
			JsonObject data = requestBody.getAsJsonObject("data");
			endToEndId = OIDFJSON.getString(data.get("endToEndId"));
		} else {
			endToEndId = environment.getString("endToEndId");
		}
		log("endToEndId in request: " + endToEndId);

		JsonObject body = environment.getObject("resource_endpoint_response_full");
		String jwtBody = OIDFJSON.getString(body.get("body"));
		JsonObject newBody = new Gson().fromJson(new String(Base64.getUrlDecoder().decode(jwtBody.split("\\.")[1].getBytes())), JsonObject.class);
		assertHasField(newBody, ROOT_PATH);
		assertField(newBody, new ObjectField.Builder(ROOT_PATH).setValidator(this::assertInnerFields).build());

		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("endToEndId")
				.setPattern(endToEndId)
				.build());
	}
}

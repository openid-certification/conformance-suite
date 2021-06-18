package net.openid.conformance.openbanking_brasil.consent;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.fields.StringField;

/**
 * This is validator for API - Delete/Revoke the consent identified by consentId."
 * See https://openbanking-brasil.github.io/areadesenvolvedor/?java#deletar-revogar-o-consentimento-identificado-por-consentid
 **/
@ApiName("Delete Consent By Id")
public class DeleteConsentByIdResponseWithErrorValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, "$.errors");

		assertJsonArrays(body, "$.errors", this::assertInnerFields);
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertStringField(body,
			new StringField
				.Builder("code")
				.build());

		assertStringField(body,
			new StringField
				.Builder("title")
				.build());

		assertStringField(body,
			new StringField
				.Builder("detail")
				.build());
	}
}

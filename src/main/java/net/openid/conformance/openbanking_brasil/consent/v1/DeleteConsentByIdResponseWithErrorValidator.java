package net.openid.conformance.openbanking_brasil.consent.v1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

/**
 * This class corresponds to {@link CreateNewConsentValidator}
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_consents_apis.yaml
 * Api endpoint: /consents/{consentId}
 * Api git hash: 152a9f02d94d612b26dbfffb594640f719e96f70
 */
@ApiName("Delete Consent By Id")
public class DeleteConsentByIdResponseWithErrorValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("errors")
				.setValidator(this::assertInnerFields)
				.setMinItems(1)
				.setMaxItems(13)
				.build());

		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("code")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(255)
				.build());

		assertField(body,
			new StringField
				.Builder("title")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(255)
				.build());

		assertField(body,
			new StringField
				.Builder("detail")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(2048)
				.build());
	}
}

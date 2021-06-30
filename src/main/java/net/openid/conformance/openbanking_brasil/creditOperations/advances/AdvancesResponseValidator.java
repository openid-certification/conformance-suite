package net.openid.conformance.openbanking_brasil.creditOperations.advances;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * This is validator for API-Adiantamento a Depositantes - Advances to Depositors"
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#adiantamento-a-depositantes
 **/

@ApiName("Advances")
public class AdvancesResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);

		assertField(body,
			new ArrayField
				.Builder(ROOT_PATH)
				.setMinItems(1)
				.build());

		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);
		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		Set<String> enumProductType = Sets.newHashSet("ADIANTAMENTO_A_DEPOSITANTES");
		Set<String> enumProductSubType = Sets.newHashSet("ADIANTAMENTO_A_DEPOSITANTES");

		assertField(body,
			new StringField
				.Builder("contractId")
				.setMaxLength(100)
				.build());

		assertField(body,
			new StringField
				.Builder("brandName")
				.setMaxLength(80)
				//.setPattern("\\w*\\W*") TODO wrong enum
				.build());

		assertField(body,
			new StringField
				.Builder("companyCnpj")
				.setMaxLength(14)
				.setPattern("\\d{14}|^NA$")
				.build());

		assertField(body,
			new StringField
				.Builder("productType")
				.setEnums(enumProductType)
				.build());

		assertField(body,
			new StringField
				.Builder("productSubType")
				.setEnums(enumProductSubType)
				.build());

		assertField(body,
			new StringField
				.Builder("ipocCode")
				.setMaxLength(67)
				.build());
	}
}

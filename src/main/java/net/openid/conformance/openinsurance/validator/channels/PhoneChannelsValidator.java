package net.openid.conformance.openinsurance.validator.channels;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api source: swagger/openinsurance/swagger-channels.yaml
 * Api endpoint: /phone-channels
 * Api version: 1.0.2
 * Api git hash: b5dcb30363a2103b9d412bc3c79040696d2947d2
 */
@ApiName("Phone Channels")
public class PhoneChannelsValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> IDENTIFICATION_TYPES = Sets.newHashSet("CENTRAL_TELEFONICA", "SAC", "OUVIDORIA");
	private final ChannelsCommonParts parts;

	public PhoneChannelsValidator() {
		parts = new ChannelsCommonParts(this);
	}
	private static class Fields extends CommonFields {
	}

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body, new ObjectField.Builder(ROOT_PATH).setValidator(
			data -> assertField(data, new ObjectField.Builder("brand").setValidator(
				brand -> {
					assertField(brand, Fields.name().build());
					assertField(brand,
						new ObjectArrayField.Builder("companies")
							.setMinItems(1)
							.setValidator(this::assertCompanies)
							.build());}
			).build())
		).build());
		new OpenInsuranceLinksAndMetaValidator(this).assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.cnpjNumber().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("phoneChannels")
				.setValidator(this::assertPhoneChannels)
				.setMinItems(1)
				.build());
	}

	private void assertPhoneChannels(JsonObject phoneChannels) {
		assertField(phoneChannels,
			new ObjectField
				.Builder("identification")
				.setValidator(this::assertIdentification)
				.build());

		parts.assertCommonServices(phoneChannels);
		parts.assertAvailability(phoneChannels);
	}

	private void assertIdentification(JsonObject identification) {
		assertField(identification,
			new StringField
				.Builder("type")
				.setEnums(IDENTIFICATION_TYPES)
				.build());

		assertField(identification,
			new ObjectArrayField
				.Builder("phones")
				.setValidator(this::assertPhoneChannelsPhones)
				.setMinItems(1)
				.setOptional()
				.build());
	}

	private void assertPhoneChannelsPhones(JsonObject phones) {
		assertField(phones,
			new StringField
				.Builder("countryCallingCode")
				.setMaxLength(4)
				.setPattern("^\\d{1,4}$|^NA$")
				.build());

		assertField(phones,
			new StringField
				.Builder("areaCode")
				.setMaxLength(2)
				.setPattern("^\\d{2}$|^NA$")
				.build());

		assertField(phones,
			new StringField
				.Builder("number")
				.setMaxLength(13)
				.setPattern("^([0-9]{8,11})$|^NA$")
				.build());
	}
}

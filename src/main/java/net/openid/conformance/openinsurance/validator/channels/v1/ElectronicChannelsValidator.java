package net.openid.conformance.openinsurance.validator.channels.v1;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api source: swagger/openinsurance/channels/v1/swagger-channels.yaml
 * Api endpoint: /electronic-channels
 * Api version: 1.2.0
 * Api git hash: a0cf93fb358df175adea537178f1980078014836
 */

@ApiName("Electronic Channels")
public class ElectronicChannelsValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> IDENTIFICATION_TYPES = Sets.newHashSet("INTERNET", "MOBILE", "CHAT", "WHATSAPP", "CONSUMIDOR", "OUTROS");
	public static final Set<String> ACCESS_TYPE = Sets.newHashSet("EMAIL","INTERNET", "APP", "CHAT", "WHATSAPP", "CONSUMIDOR", "OUTROS");
	private final ChannelsCommonParts parts;

	public ElectronicChannelsValidator() {
		parts = new ChannelsCommonParts(this);
	}
	private static class Fields extends CommonFields {
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body, new ObjectField.Builder(ROOT_PATH).setValidator(
			data -> assertField(data, new ObjectField.Builder("brand").setValidator(
				brand -> {
					assertField(brand, Fields.name().build());
					assertField(brand,
						new ObjectArrayField
							.Builder("companies")
							.setMinItems(1)
							.setValidator(this::assertCompanies)
							.build());
				}).build())
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
				.Builder("electronicChannels")
				.setValidator(this::assertElectronicChannels)
				.setMinItems(1)
				.setMaxItems(99)
				.build());
	}

	private void assertElectronicChannels(JsonObject electronicChannels) {
		assertField(electronicChannels,
			new ObjectField
				.Builder("identification")
				.setValidator(this::assertIdentification)
				.build());

		parts.assertCommonServices(electronicChannels);
		parts.assertAvailability(electronicChannels);
	}

	private void assertIdentification(JsonObject identification) {
		assertField(identification,
			new StringField
				.Builder("type")
				.setEnums(IDENTIFICATION_TYPES)
				.build());

		assertField(identification,
			new StringField
				.Builder("accessType")
				.setEnums(ACCESS_TYPE)
				.setOptional()
				.build());

		assertField(identification,
			new StringArrayField
				.Builder("urls")
				.setPattern("[\\w\\W]*")
				.setMaxLength(1024)
				.setMinItems(1)
				.build());
	}
}

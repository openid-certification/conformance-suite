package net.openid.conformance.openinsurance.validator.channels;

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
 * Api source: swagger/openinsurance/swagger-channels.yaml
 * Api endpoint: /electronic-channels
 * Api version: 1.0.2
 * Api git hash: b5dcb30363a2103b9d412bc3c79040696d2947d2
 */

//TODO: the problem meet of swagger schema
//data, brand, name, companies, name, cnpjNumber, urlComplementaryList, electronicChannels, identification, type, urls, services, name, code, links, self, first, prev, next, last, meta, totalRecords, totalPages
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
		assertField(body, new ObjectField
			.Builder("data").setValidator(data -> {
				assertField(data,
					new ObjectField
						.Builder("brand").setValidator(
							brand -> {
								assertField(brand, Fields.name().build());
								assertField(brand,
									new ObjectArrayField
										.Builder("companies")
										.setMinItems(1)
										.setValidator(this::assertCompanies)
										.build());
							})
						.setOptional()
						.build());
			})
			.build());



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

package net.openid.conformance.openbanking_brasil.channels;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_channels_apis.yaml
 * Api endpoint: /phone-channels
 * Api version: 1.0.2
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 *
 */
@ApiName("Banking Agents Channels")
public class PhoneChannelsValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> IDENTIFICATION_TYPES = Sets.newHashSet("CENTRAL_TELEFONICA", "SAC", "OUVIDORIA", "OUTROS");
	private final ChannelsCommonParts parts;

	public PhoneChannelsValidator() {
		parts = new ChannelsCommonParts(this);
	}
	private static class Fields extends CommonFields {
	}

	@Override
	public Environment evaluate(Environment environment) {
		setLogOnlyFailure();
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

		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.cnpjNumber().build());
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.urlComplementaryList().build());

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

		parts.assertCommonServices(phoneChannels,true);
	}

	private void assertIdentification(JsonObject identification) {

		assertField(identification,
			new StringField
				.Builder("type")
				.setEnums(IDENTIFICATION_TYPES)
				.build());

		assertField(identification,
			new StringField
				.Builder("additionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(300)
				.setOptional()
				.build());

		assertField(identification,
			new ObjectArrayField
				.Builder("phones")
				.setValidator(this::assertPhones)
				.setMinItems(1)
				.build());
	}

	public void assertPhones(JsonObject phones) {
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
				.setMaxLength(11)
				.setPattern("^([0-9]{8,11})$")
				.build());

		assertField(phones,
			new StringField
				.Builder("additionalInfo")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(300)
				.setOptional()
				.build());
	}
}

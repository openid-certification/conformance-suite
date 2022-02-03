package net.openid.conformance.openbanking_brasil.channels;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_channels_apis.yaml
 * Api endpoint: /electronic-channels
 * Api version: 1.0.2
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 */

@ApiName("Electronic Channels")
public class ElectronicChannelsValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> IDENTIFICATION_TYPES = Sets.newHashSet("INTERNET_BANKING", "MOBILE_BANKING", "SAC", "OUVIDORIA", "CHAT", "OUTROS");
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
				.Builder("electronicChannels")
				.setValidator(this::assertElectronicChannels)
				.setMinItems(1)
				.setMaxItems(4)
				.build());
	}

	private void assertElectronicChannels(JsonObject electronicChannels) {
		assertField(electronicChannels,
			new ObjectField
				.Builder("identification")
				.setValidator(this::assertIdentification)
				.build());

		parts.assertCommonServices(electronicChannels, true);
	}

	private void assertIdentification(JsonObject electronicChannels) {

		assertField(electronicChannels,
			new StringField
				.Builder("type")
				.setEnums(IDENTIFICATION_TYPES)
				.build());

		assertField(electronicChannels,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(300)
				.setPattern("\\w*\\W*")
				.setOptional()
				.build());

		assertField(electronicChannels,
			new StringArrayField
				.Builder("urls")
				.setMinItems(1)
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(1024)
				.build());
	}
}

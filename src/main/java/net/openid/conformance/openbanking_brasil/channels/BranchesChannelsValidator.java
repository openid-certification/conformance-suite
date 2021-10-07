package net.openid.conformance.openbanking_brasil.channels;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api url: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_channels_apis.yaml
 * Api endpoint: /branches
 * Api version: 1.0.2
 * Api git hash: ba747ce30bdf7208a246ebf1e8a2313f85263d91
 *
 */

@ApiName("Branches Channels")
public class BranchesChannelsValidator extends AbstractJsonAssertingCondition {
	public static final Set<String> WEEKDAY_ENUM = Sets.newHashSet("DOMINGO", "SEGUNDA_FEIRA", "TERCA_FEIRA", "QUARTA_FEIRA", "QUINTA_FEIRA", "SEXTA_FEIRA", "SABADO");
	public static final Set<String> IDENTIFICATION_TYPES = Sets.newHashSet("AGENCIA", "POSTO_ATENDIMENTO", "POSTO_ATENDIMENTO_ELETRONICO", "UNIDADE_ADMINISTRATIVA_DESMEMBRADA");
	public static final Set<String> PHONES_ENUM = Sets.newHashSet("FIXO", "MOVEL");
	private final ChannelsCommonParts parts;

	public BranchesChannelsValidator() {
		parts = new ChannelsCommonParts(this);
	}
	private static class Fields extends CommonFields {
	}
	@Override
	public Environment evaluate(Environment environment) {
		setLogOnlyFailure();
		JsonObject body = bodyFrom(environment);

		assertJsonObject(body, ROOT_PATH,
			data -> assertField(data, new ObjectField.Builder("brand").setValidator(
				brand -> {
					assertField(brand, Fields.name().build());
					assertField(brand,
						new ObjectArrayField.Builder("companies")
							.setMinItems(1)
							.setValidator(this::assertCompanies)
							.build());
				}
			).build())
		);
		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.cnpjNumber().build());
		assertField(companies, Fields.name().build());
		assertField(companies, Fields.urlComplementaryList().build());

		assertField(companies,
			new ObjectArrayField
				.Builder("branches")
				.setValidator(this::assertBranches)
				.setMinItems(1)
				.setOptional()
				.build());
	}

	private void assertBranches(JsonObject branches) {
		assertField(branches,
			new ObjectField
				.Builder("identification")
				.setValidator(this::assertIdentification)
				.build());

		parts.assertPostalAddress(branches, true);

		assertField(branches,
			new ObjectField
				.Builder("availability")
				.setValidator(availability -> {
					assertField(availability,
						new ObjectArrayField
							.Builder("standards")
							.setValidator(this::assertStandards)
							.setMinItems(1)
							.setMaxItems(7)
							.build());

					assertField(availability,
						new StringField
							.Builder("exception")
							.setMaxLength(2000)
							.setPattern("[\\w\\W\\s]*")
							.build());

					assertField(availability,
						new BooleanField
							.Builder("isPublicAccessAllowed")
							.setOptional()
							.build());
				})
				.build());

		assertField(branches,
			new ObjectArrayField
				.Builder("phones")
				.setValidator(this::assertPhones)
				.setMinItems(1)
				.setOptional()
				.build());

		parts.assertCommonServices(branches, false);
	}

	public void assertStandards(JsonObject standards) {

		assertField(standards,
			new StringField
				.Builder("weekday")
				.setEnums(WEEKDAY_ENUM)
				.build());

		assertField(standards,
			new StringField
				.Builder("openingTime")
				.setMaxLength(13)
				//.setPattern("^(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)|Z$|^NA$")
				.build());

		assertField(standards,
			new StringField
				.Builder("closingTime")
				.setMaxLength(13)
				//.setPattern("^(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)|Z$|^NA$")
				.build());
	}

	private void assertIdentification(JsonObject identification) {

		assertField(identification,
			new StringField
				.Builder("type")
				.setEnums(IDENTIFICATION_TYPES)
				.build());

		assertField(identification,
			new StringField
				.Builder("code")
				.setMaxLength(4)
				.setPattern("^\\d{4}$|^NA$")
				.build());

		assertField(identification,
			new StringField
				.Builder("checkDigit")
				.setMaxLength(1)
				.setPattern("\\w*\\W*")
				.build());

		assertField(identification,
			new StringField
				.Builder("name")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(identification,
			new StringField
				.Builder("relatedBranch")
				.setMaxLength(4)
				.setPattern("^\\d{4}$")
				.setOptional()
				.build());

		assertField(identification,
			new StringField
				.Builder("openingDate")
				.setMaxLength(10)
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setOptional()
				.build());
	}

	public void assertPhones(JsonObject phones) {

		assertField(phones,
			new StringField
				.Builder("type")
				.setEnums(PHONES_ENUM)
				.build());

		assertField(phones,
			new StringField
				.Builder("countryCallingCode")
				.setMaxLength(4)
				.setPattern("^\\d{1,4}$")
				.build());

		assertField(phones,
			new StringField
				.Builder("areaCode")
				.setMaxLength(2)
				.setPattern("^\\d{2}$")
				.setOptional()
				.build());

		assertField(phones,
			new StringField
				.Builder("number")
				.setMaxLength(11)
				.setPattern("^([0-9]{8,11})$")
				.setOptional()
				.build());
	}
}

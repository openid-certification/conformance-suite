package net.openid.conformance.openinsurance.validator.channels;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * https://gitlab.com/obb1/certification/-/blob/master/src/main/resources/swagger/openinsurance/swagger-channels.yaml
 * Api endpoint: /branches
 * Git hash: 17d932e0fac28570a0bf2a8b8e292a65b816f278
 */
@ApiName("Branches Channels")
public class BranchesValidator extends AbstractJsonAssertingCondition {
	public static final Set<String> WEEKDAY_ENUM = Sets.newHashSet("DOMINGO", "SEGUNDA_FEIRA", "TERCA_FEIRA", "QUARTA_FEIRA", "QUINTA_FEIRA", "SEXTA_FEIRA", "SABADO");
	public static final Set<String> IDENTIFICATION_TYPES = Sets.newHashSet("POSTO_ATENDIMENTO", "UNIDADE_ADMINISTRATIVA_DESMEMBRADA");
	public static final Set<String> PHONES_ENUM = Sets.newHashSet("FIXO", "MOVEL");
	public static final Set<String> NAMES_ENUM = Sets.newHashSet("ALTERACOES_FORMA_PAGAMENTO", "AVISO_SINISTRO", "CANCELAMENTO_SUSPENSAO_PAGAMENTO_PREMIOS_CONTRIBUICAO", "EFETIVACAO_APORTE", "ENDOSSO", "ENVIO_DOCUMENTOS", "INFORMACOES_GERAIS_DUVIDAS", "INFORMACOES_INTERMEDIARIOS", "INFORMACOES_SOBRE_SERVICOS_ASSISTENCIAS", "INFORMACOES_SOBRE_SORTEIOS", "OUVIDORIA_RECEPCAO_SUGESTOES_ELOGIOS", "OUVIDORIA_SOLUCAO_EVENTUAIS_DIVERGENCIAS_SOBRE_CONTRATO_SEGURO_CAPITALIZAÇÃO_PREVIDÊNCIA_APOS_ESGOTADOS_CANAIS_REGULARES_ATENDIMENTO_AQUELAS_ORIUNDAS_ORGAOS_REGULADORES_OU_INTEGRANTES_SISTEMA_NACIONAL_DEFESA_CONSUMIDOR", "OUVIDORIA_TRATAMENTO_INSATISFACAO_CONSUMIDOR_RELACAO_ATENDIMENTO_RECEBIDO_CANAIS_REGULARES_ATENDIMENTO", "OUVIDORIA_TRATAMENTO_RECLAMACOES_SOBRE_IRREGULARDADES_CONDUTA_COMPANHIA", "PORTABILIDADE", "RECLAMACAO", "RESGATE", "SEGUNDA_VIA_DOCUMENTOS_CONTRATUAIS", "SUGESTOES_ELOGIOS");
	public static final Set<String> CODES_ENUM = Sets.newHashSet("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19");

	private static class Fields extends CommonFields { }

	@Override
	public Environment evaluate(Environment environment) {
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
		assertField(companies, Fields.cnpjNumber().setMinLength(14).build());
		assertField(companies, Fields.name().build());

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
				.setOptional()
				.build());

		assertField(branches,
			new ObjectField
				.Builder("postalAddress")
				.setValidator(this::assertInnerPostalAddress)
				.build());

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

		assertField(branches,
			new ObjectArrayField
				.Builder("services")
				.setValidator(services -> {
					assertField(services,
						new StringField
							.Builder("name")
							.setEnums(NAMES_ENUM)
							.build());

					assertField(services,
						new StringField
							.Builder("code")
							.setEnums(CODES_ENUM)
							.build());
				})
				.setMinItems(1)
				.setMaxItems(20)
				.build());
	}

	private void assertInnerPostalAddress(JsonObject postalAddress) {
		assertField(postalAddress,
			new StringField
				.Builder("address")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(150)
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(30)
				.setPattern("[\\w\\W\\s]*")
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("districtName")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("townName")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("ibgeCode")
				.setMaxLength(7)
				.setPattern("^\\d{7}$")
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("countrySubDivision")
				.setMaxLength(2)
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("postCode")
				.setMaxLength(8)
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("country")
				.setMaxLength(80)
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("countryCode")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertGeographicCoordinates(postalAddress);
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
				.setPattern("^(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)|Z$|^NA$")
				.build());

		assertField(standards,
			new StringField
				.Builder("closingTime")
				.setMaxLength(13)
				.setPattern("^(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)|Z$|^NA$")
				.build());
	}

	private void assertIdentification(JsonObject identification) {

		assertField(identification,
			new StringField
				.Builder("type")
				.setEnums(IDENTIFICATION_TYPES)
				.setOptional()
				.build());

		assertField(identification,
			new StringField
				.Builder("code")
				.setMaxLength(4)
				.setPattern("^\\d{4}$|^NA$")
				.setOptional()
				.build());

		assertField(identification,
			new StringField
				.Builder("checkDigit")
				.setMaxLength(1)
				.setPattern("\\w*\\W*")
				.setOptional()
				.build());

		assertField(identification,
			new StringField
				.Builder("name")
				.setMaxLength(100)
				.setPattern("[\\w\\W\\s]*")
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
				.setOptional()
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

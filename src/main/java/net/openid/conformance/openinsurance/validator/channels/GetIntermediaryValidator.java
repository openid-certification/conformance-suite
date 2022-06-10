package net.openid.conformance.openinsurance.validator.channels;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.productsNServices.ProductNServicesCommonFields;
import net.openid.conformance.openinsurance.validator.OpenInsuranceLinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api Source: swagger/openinsurance/swagger-intermediary.yaml
 * Api endpoint: /intermediary/{countrySubDivision}
 * Git hash: 45ff0d424353d93b5eb478a0da902d7b3d6e4764
 * Api version: 1.1.0
 */

@ApiName("Channels Intermediary")
public class GetIntermediaryValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields {
	}

	public static final Set<String> TYPE = SetUtils.createSet("CORRETOR_DE_SEGUROS, REPRESENTANTE_DE_SEGUROS, AGENTES_DE_SEGUROS, DISTRIBUIDOR_DE_TITULO_DE_CAPITALIZACAO");
	public static final Set<String> WEEKDAY_ENUM = SetUtils.createSet("DOMINGO, SEGUNDA_FEIRA, TERCA_FEIRA, QUARTA_FEIRA, QUINTA_FEIRA, SEXTA_FEIRA, SABADO");
	public static final Set<String> COUNTRY_SUB_DIVISION_ENUM = SetUtils.createSet("AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO");
	public static final Set<String> NAMES_ENUM = SetUtils.createSet("ANGARIACAO_PROMOCAO_INTERMEDIACAO_OU_DISTRIBUICAO_DE_PRODUTOS, ACONSELHAMENTO_SOBRE_PRODUTOS_OFERTADOS, " +
		"RECEPCAO_DE_PROPOSTAS_E_EMISSAO_DE_DOCUMENTOS_CONTRATUAIS, SUBSCRICAO_DE_RISCOS_RELACIONADOS_A_PRODUTOS_DE_SEGUROS, " +
		"COLETA_E_FORNECIMENTO_A_SOCIEDADE_PARTICIPANTE_DE_DADOS_CADASTRAIS_E_DE_DOCUMENTACAO_DE_CLIENTES_E_SE_FOR_O_CASO_ESTIPULANTES_CORRETORES_DE_SEGUROS_E_SEUS_PREPOSTOS, " +
		"RECOLHIMENTO_DE_PREMIOS_E_CONTRIBUICOES, RECEBIMENTO_DE_AVISOS_DE_SINISTROS, REGULACAO_DE_SINISTROS, PAGAMENTO_DE_INDENIZACAO_BENEFICIO, ORIENTACAO_E_ASSISTENCIA_AOS_CLIENTES_NO_QUE_COMPETE_AOS_CONTRATOS_COMERCIALIZADOS, " +
		"APOIO_LOGISTICO_E_OPERACIONAL_A_SOCIEDADE_PARTICIPANTE_NA_GESTAO_E_EXECUCAO_DE_CONTRATOS, OUTROS");
	public static final Set<String> LINE = SetUtils.createSet("LISTAGEM_DE_RAMOS_DE_SEGUROS_CONFORME_REGULAMENTACAO_ESPECIFICA_SOBRE_CONTABILIZACAO_EM_RAMOS, PREVIDENCIA_COMPLEMENTAR_ABERTA, CAPITALIZACAO");

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body, new ObjectField
			.Builder("data")
			.setValidator(data -> assertField(data, new ObjectField
				.Builder("brand")
				.setValidator(brand -> {
					assertField(brand, Fields.name().setMaxLength(80).build());
					assertField(brand,
						new ObjectArrayField
							.Builder("companies")
							.setValidator(this::assertCompanies)
							.build());
				})
				.build())).build());
		new OpenInsuranceLinksAndMetaValidator(this).assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertCompanies(JsonObject companies) {
		assertField(companies, Fields.name().setMaxLength(80).build());
		assertField(companies, Fields.cnpjNumber().setMaxLength(14).build());

		assertField(companies,
			new ObjectArrayField
				.Builder("identification")
				.setValidator(this::assertIdentification)
				.build());
	}

	private void assertIdentification(JsonObject identification) {
		assertField(identification, Fields.name().setMaxLength(80).build());
		assertField(identification,
			new StringField
				.Builder("nameOther")
				.setMaxLength(80)
				.setOptional()
				.build());

		assertField(identification,
			new StringField
				.Builder("documentNumber")
				.setMaxLength(14)
				.setOptional()
				.setPattern("^\\d{11}|\\d{14}$")
				.build());

		assertField(identification,
			new StringField
				.Builder("type")
				.setMaxLength(39)
				.setEnums(TYPE)
				.build());

		assertField(identification,
			new StringField
				.Builder("SUSEP")
				.setMaxLength(20)
				.setOptional()
				.build());

		assertField(identification,
			new ObjectArrayField
				.Builder("postalAddress")
				.setValidator(this::assertPostalAddress)
				.build());

		assertField(identification,
			new ObjectField
				.Builder("access")
				.setValidator(this::assertAccess)
				.setOptional()
				.build());

		assertField(identification,
			new ObjectArrayField
				.Builder("services")
				.setValidator(this::assertServices)
				.build());
	}

	private void assertPostalAddress(JsonObject postalAddress) {
		assertField(postalAddress,
			new StringField
				.Builder("address")
				.setMaxLength(150)
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("additionalInfo")
				.setMaxLength(30)
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("districtName")
				.setMaxLength(50)
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("townName")
				.setMaxLength(50)
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("ibgeCode")
				.setMaxLength(7)
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("countrySubDivision")
				.setEnums(COUNTRY_SUB_DIVISION_ENUM)
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
				.setMaxLength(46)
				.setEnums(Fields.COUNTRY_250)
				.setOptional()
				.build());

		assertField(postalAddress,
			new StringField
				.Builder("countryCode")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(postalAddress,
			new ObjectField
				.Builder("geographicCoordinates")
				.setOptional()
				.setValidator(geo -> {
					assertField(geo,
						new LatitudeField.Builder()
							.setMaxLength(11)
							.setOptional()
							.build());
					assertField(geo,
						new LongitudeField.Builder()
							.setMaxLength(11)
							.setOptional()
							.build());
				})
				.build());
	}

	private void assertAccess(JsonObject access) {
		assertField(access,
			new ObjectArrayField
				.Builder("standards")
				.setValidator(this::assertStandards)
				.setOptional()
				.build());

		assertField(access,
			new StringField
				.Builder("email")
				.setMaxLength(50)
				.setOptional()
				.build());

		assertField(access,
			new StringField
				.Builder("site")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(access,
			new ObjectArrayField
				.Builder("phones")
				.setValidator(this::assertPhones)
				.setOptional()
				.build());
	}

	private void assertStandards(JsonObject standards) {
		assertField(standards,
			new StringField
				.Builder("openingTime")
				.setMaxLength(9)
				.setOptional()
				.setPattern("^\\d{2}:\\d{2}:\\d{2}Z$")
				.build());

		assertField(standards,
			new StringField
				.Builder("closingTime")
				.setMaxLength(9)
				.setOptional()
				.setPattern("^\\d{2}:\\d{2}:\\d{2}Z$")
				.build());

		assertField(standards,
			new StringField
				.Builder("weekday")
				.setMaxLength(13)
				.setEnums(WEEKDAY_ENUM)
				.setOptional()
				.build());
	}

	public void assertPhones(JsonObject phones) {
		assertField(phones,
			new StringField
				.Builder("type")
				.setMaxLength(13)
				.setOptional()
				.build());

		assertField(phones,
			new StringField
				.Builder("countryCallingCode")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(phones,
			new StringField
				.Builder("areaCode")
				.setMaxLength(2)
				.setOptional()
				.build());

		assertField(phones,
			new StringField
				.Builder("number")
				.setMaxLength(11)
				.setOptional()
				.build());
	}

	private void assertServices(JsonObject data) {
		assertField(data,
			new StringField
				.Builder("name")
				.setEnums(NAMES_ENUM)
				.setMaxLength(164)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("nameOthers")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("line")
				.setEnums(LINE)
				.setMaxLength(93)
				.build());
	}
}

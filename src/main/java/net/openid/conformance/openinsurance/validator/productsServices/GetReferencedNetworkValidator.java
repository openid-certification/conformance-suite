package net.openid.conformance.openinsurance.validator.productsServices;

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
 * Api Source: swagger/openinsurance/productsServices/swagger-referenced-network.yaml
 * Api endpoint: /referenced-network/{countrySubDivision}/{serviceType}
 * Api version: 1.0.0
 */

@ApiName("ProductsServices Referenced Network")
public class GetReferencedNetworkValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields {}

	public static final Set<String> TYPE = SetUtils.createSet("ASSISTENCIA_AUTO, ASSISTENCIA_RE, ASSISTENCIA_VIDA, " +
		"BENEFICIOS, DESPACHANTE, LOCACAO_DE_VEICULOS, REPAROS_AUTOMOTIVOS, REPAROS_EMERGENCIAIS, SERVICO_DE_MANUTENCAO, " +
		"SERVICO_EM_CASO_DE_SINISTRO, TRANSPORTE_DO_EMERGENCIAL, OUTROS");
	public static final Set<String> WEEKDAY_ENUM = SetUtils.createSet("DOMINGO, SEGUNDA_FEIRA, TERCA_FEIRA, QUARTA_FEIRA, QUINTA_FEIRA, SEXTA_FEIRA, SABADO");
	public static final Set<String> COUNTRY_SUB_DIVISION_ENUM = SetUtils.createSet("AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO");
	public static final Set<String> NAMES_ENUM = SetUtils.createSet("ACIONAMENTO_E_OU_AGENDAMENTO_DE_LEVA_E_TRAZ, " +
		"AMPARO_DE_CRIANCAS, APLICACAO_DE_VACINAS_EM_DOMICILIO, AQUECEDORES, ASSISTENCIA_A_ELETRODOMESTICOS, " +
		"ASSISTENCIA_AUTO_E_OU_MOTO, ASSISTENCIA_BIKE, ASSISTENCIA_EM_VIAGEM, ASSISTENCIA_ESCOLAR, ASSISTENCIA_FUNERAL, " +
		"ASSISTENCIA_FUNERAL_PET, ASSISTENCIA_INFORMATICA, ASSISTENCIA_NUTRICIONAL, ASSISTENCIA_PET, ASSISTENCIA_RESIDENCIAL, " +
		"ASSISTENCIA_SUSTENTAVEL, ASSISTENCIA_VETERINARIA_EMERGENCIAL, ASSISTENCIAS_SAUDE_E_BEM_ESTAR, BABY_SITTER, CACAMBA, " +
		"CARRO_RESERVA, CESTA_BASICA, CESTA_DE_ALIMENTOS, CESTA_NATALIDADE, CHAVEIRO, CHECK_UP, COBERTURA_PROVISORIA_DE_TELHADO, CONCIERGE, CONSERTO_DE_AR_CONDICIONADO, " +
		"CONSERTO_DE_ELETRODOMESTICOS_LINHA_BRANCA, CONSERTO_DE_ELETROELETRONICO_LINHA_MARROM, CONSERTO_DE_PORTA_ONDULADA, CONSULTAS_VETERINARIAS, CONSULTORIA_ORCAMENTARIA, " +
		"CONVENIENCIA_EM_VIAGEM, DEDETIZACAO, DESATOLAMENTO, DESCARTE_RESPONSAVEL, DESCONTOS_EM_CONSULTAS_E_EXAMES, DESCONTOS_EM_MEDICAMENTOS, DESENTUPIMENTO, " +
		"DESINSETIZACAO_E_DESRATIZACAO, DESPACHANTE, DESPESAS_FARMACEUTICAS, DESPESAS_MEDICAS_CIRURGICAS_E_DE_HOSPITALIZACAO, DESPESAS_ODONTOLOGICAS, ELETRICISTA, " +
		"EMERGENCIAS, ENCANADOR, ENVIO_DE_ACOMPANHANTE_EM_CASO_DE_ACIDENTE, ENVIO_DE_FAMILIAR_PARA_ACOMPANHAMENTO_DE_MENORES_DE_ANOS, ENVIO_DE_RACAO, " +
		"ESCRITORIO_VIRTUAL, GUARDA_DE_ANIMAIS, GUARDA_DO_VEICULO, GUINCHO, HELP_DESK, HIDRAULICA, HOSPEDAGEM, HOSPEDAGEM_DE_ANIMAIS, INDICACAO_DE_BANHO_E_TOSA, " +
		"INDICACAO_DE_PROFISSIONAIS, INFORMACAO_SOBRE_RACAS_DE_CAES, INFORMACAO_SOBRE_VENDA_DE_FILHOTES, INFORMACOES_SOBRE_VACINAS, INFORMACOES_VETERINARIAS_UTEIS, " +
		"INSTALACAO_RESIDENCIA, INSTALACAO_DE_CHUVEIRO_ELETRICO_E_OU_TROCA_DE_RESISTENCIA, INSTALACAO_DE_SUPORTE_TV_ATE, LIMPEZA, LIMPEZA_DE_AR_CONDICIONADO, LIMPEZA_DE_CAIXA_D_AGUA, " +
		"LIMPEZA_DE_CALHAS, LIMPEZA_DE_RALOS_E_SIFOES, LOCACAO_DE_ELETRODOMESTICOS, LOCACAO_DE_VEICULOS, LOCALIZACAO_DE_BAGAGEM, MANUTENCAO, MARTELINHO_E_REPARO_RAPIDO, MECANICO, " +
		"MEIO_DE_TRANSPORTE, MONITORACAO_MEDICA, MOTO, MOTORISTA_AMIGO, MOTORISTA_SUBSTITUTO, MTA_MEIO_DE_TRANSPORTE_ALTERNATIVO, MUDANCA_E_GUARDA_DE_MOVEIS, ORGANIZACAO, " +
		"ORIENTACAO_EM_CASO_DE_PERDA_DE_DOCUMENTOS, ORIENTACAO_MEDICA, ORIENTACAO_PSICOLOGICA, PERSONAL_FITNESS, REBOQUE, REBOQUE_BIKE, RECUPERACAO_DO_VEICULO, " +
		"REGRESSO_ANTECIPADO_EM_CASO_DE_FALECIMENTO_DE_PARENTES, REGRESSO_DO_USUARIO_APOS_ALTA_HOSPITALAR, REINSTALACAO_E_REPARO_DO_VENTILADOR_DE_TETO, " +
		"REMANEJAMENTO_DE_MOVEIS, REMOCAO_HOSPITALAR, REMOCAO_MEDICA, REMOCAO_MEDICA_INTER_HOSPITALAR, REPARACAO_AUTOMOTIVA, REPARO_DE_TELEFONIA, " +
		"REPARO_EM_PORTOES_AUTOMATICOS, REPARO_FIXACAO_DE_ANTENAS, REPAROS_ELETRICOS, RETORNO_ANTECIPADO_AO_DOMICILIO, REVERSAO_DE_FOGAO, " +
		"REVISAO_DE_INSTALACAO_ELETRICA, SEGUNDA_OPINIAO_MEDICA_INTERNACIONAL, SEGURANCA, SERRALHEIRO, SERVICO_DE_INDICACAO_MEDICA, " +
		"SERVICO_DE_LIMPEZA, SERVICOS_AUTO, SERVICOS_ESPECIAIS_FIXACAO_DE_OBJETOS, SERVICOS_GERAIS, SUBSTITUICAO_DE_PNEUS, " +
		"SUBSTITUICAO_DE_TELHAS, TAXI, TELEMEDICINA, TRANSMISSAO_DE_MENSAGENS_URGENTES, TRANSPORTE_E_ENVIO_DE_FAMILIAR, " +
		"TRANSPORTE_E_GUARDA_MOVEIS, TRANSPORTE_ESCOLAR_PESSOAS, TRANSPORTE_VETERINARIO_EMERGENCIAL, TRASLADO_DE_CORPO, " +
		"TROCA_DE_BATERIA, TROCA_DE_PNEUS, VERIFICACAO_DE_POSSIVEIS_VAZAMENTOS, VIGILANCIA_E_SEGURANCA, OUTRAS");

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
		assertField(identification, Fields.cnpjNumber().setMaxLength(14).build());

		assertField(identification,
			new ObjectArrayField
				.Builder("products")
				.setValidator(this::assertProducts)
				.build());

		assertField(identification,
			new ObjectArrayField
				.Builder("postalAddress")
				.setValidator(this::assertPostalAddress)
				.build());

		assertField(identification,
			new ObjectArrayField
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

	private void assertProducts(JsonObject products) {
		assertField(products, Fields.name().setMaxLength(80).build());
		assertField(products, Fields.code().setMaxLength(100).build());

		assertField(products,
			new StringArrayField
				.Builder("coverage")
				.setMaxLength(150)
				.setOptional()
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
			new BooleanField
				.Builder("restrictionIndicator")
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
				.setEnums(WEEKDAY_ENUM)
				.setOptional()
				.build());
	}

	public void assertPhones(JsonObject phones) {
		assertField(phones,
			new StringField
				.Builder("type")
				.setMaxLength(50)
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
				.Builder("type")
				.setEnums(TYPE)
				.setMaxLength(27)
				.build());

		assertField(data,
			new StringField
				.Builder("typeOthers")
				.setMaxLength(3000)
				.setOptional()
				.build());

		assertField(data,
			new StringArrayField
				.Builder("name")
				.setEnums(NAMES_ENUM)
				.setMaxLength(57)
				.build());

		assertField(data,
			new StringField
				.Builder("description")
				.setMaxLength(5000)
				.build());
	}
}

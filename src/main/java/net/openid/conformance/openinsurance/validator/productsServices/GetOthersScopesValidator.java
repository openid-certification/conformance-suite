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
 * Api Source: swagger/openinsurance/productsServices/swagger-others-scopes.yaml
 * Api endpoint: /others-scopes/
 * Api version: 1.0.0
 */

@ApiName("ProductsServices Others Scopes")
public class GetOthersScopesValidator extends AbstractJsonAssertingCondition {

	private static class Fields extends ProductNServicesCommonFields { }

	public static final Set<String> COVERAGE = SetUtils.createSet("CASCO, RESPONSABILIDADE_CIVIL_FACULTATIVA, " +
		"RESPONSABILIDADE_CIVIL_AEROPORTUARIA, RESPONSABILIDADE_DO_EXPLORADOR_E_TRANSPORTADOR_AEREO_RETA, " +
		"BASICA_DANOS_MATERIAIS_A_EMBARCACAO_COBERTURA_DE_ASSISTENCIA_E_SALVAMENTO_COBERTURA_DE_COLOCACAO_E_RETIRADA_DA_AGUA, " +
		"BASICA_DE_OPERADOR_PORTUARIO_DANOS_MATERIAIS_E_CORPORAIS_A_TERCEIROS, DANOS_ELETRICOS, DANOS_FISICOS_A_BENS_MOVEIS_E_IMOVEIS, " +
		"DANOS_MORAIS, DESPESAS_COM_HONORARIOS_DE_ESPECIALISTAS, EXTENSAO_DO_LIMITE_DE_NAVEGACAO, GUARDA_DE_EMBARCACOES, LIMITE_DE_NAVEGACAO, " +
		"PARTICIPACAO_EM_EVENTOS_FEIRAS_EXPOSICOES_REGATAS_A_VELA_COMPETICOES_DE_PESCA_OU_COMPETICOES_DE_VELOCIDADE, " +
		"PERDA_DE_RECEITA_BRUTA_E_OU_DESPESAS_ADICIONAIS_OU_EXTRAORDINARIAS, PERDA_E_OU_PAGAMENTO_DE_ALUGUEL, " +
		"REMOCAO_DE_DESTROCOS, RESPONSABILIDADE_CIVIL, ROUBO_E_OU_FURTO_QUALIFICADO_ACESSORIOS_FIXOS_OU_NAO_FIXOS_TOTAL_OU_PARCIAL, " +
		"SEGURO_DE_CONSTRUTORES_NAVAIS, TRANSPORTE_TERRESTRE, RISCOS_DE_PETROLEO, RISCOS_NUCLEARES, OUTRAS");
	public static final Set<String> TARGET_AUDIENCE = SetUtils.createSet("PESSOA_FISICA, PESSOA_JURIDICA");
	public static final Set<String> TERMS = SetUtils.createSet("ANUAL, ANUAL_INTERMITENTE, PLURIANUAL, PLURIANUAL_INTERMITENTE, MENSAL, MENSAL_INTERMITENTE, DIARIO, DIARIO_INTERMITENTE, OUTROS");

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
				.Builder("products")
				.setValidator(this::assertProducts)
				.build());
	}

	private void assertProducts(JsonObject products) {
		assertField(products, Fields.name().setMaxLength(80).setOptional().build());
		assertField(products, Fields.code().setMaxLength(80).build());

		assertField(products,
			new ObjectArrayField
				.Builder("coverages")
				.setValidator(this::assertCoverages)
				.build());

		assertField(products,
			new BooleanField
				.Builder("traits")
				.build());

		assertField(products,
			new ObjectField
				.Builder("termsAndConditions")
				.setValidator(termsAndConditions -> {
					assertField(termsAndConditions,
						new StringField
							.Builder("susepProcessNumber")
							.setMaxLength(20)
							.setOptional()
							.build());

					assertField(termsAndConditions,
						new StringField
							.Builder("definition")
							.setMaxLength(1024)
							.setOptional()
							.build());
				})
				.build());

		assertField(products,
			new ObjectField
				.Builder("validity")
				.setValidator(validity -> {
					assertField(validity,
						new StringArrayField
							.Builder("term")
							.setMaxLength(23)
							.setEnums(TERMS)
							.build());

					assertField(validity,
						new StringField
							.Builder("termOthers")
							.setMaxLength(100)
							.setOptional()
							.build());
				})
				.build());

		assertField(products,
			new StringArrayField
				.Builder("premiumRates")
				.setMaxLength(1024)
				.setOptional()
				.build());

		assertField(products,
			new ObjectField.Builder("minimumRequirements")
				.setValidator(minimumRequirements ->
					assertField(minimumRequirements,
						new StringArrayField
							.Builder("targetAudiences")
							.setEnums(TARGET_AUDIENCE)
							.setMaxLength(15)
							.build()))
				.build());
	}

	private void assertCoverages(JsonObject coverages) {
		assertField(coverages,
			new StringField
				.Builder("coverage")
				.setMaxLength(115)
				.setEnums(COVERAGE)
				.build());

		assertField(coverages,
			new StringField
				.Builder("coverageDescription")
				.setMaxLength(3000)
				.build());

		assertField(coverages,
			new BooleanField
				.Builder("allowApartPurchase")
				.build());

		assertField(coverages,
			new ObjectField
				.Builder("coverageAttributes")
				.setValidator(this::assertCoverageAttributes)
				.build());
	}

	private void assertCoverageAttributes(JsonObject coverageAttributes) {
		assertField(coverageAttributes,
			new ObjectField
				.Builder("maxLMI")
				.setValidator(this::assertValue)
				.build());
	}

	public void assertValue(JsonObject minValue) {
		assertField(minValue,
			new NumberField
				.Builder("amount")
				.setMaxLength(36)
				.build());

		assertField(minValue,
			new ObjectField
				.Builder("unit")
				.setValidator(this::assertUnit)
				.build());
	}

	public void assertUnit(JsonObject unit) {
		assertField(unit,
			new StringField
				.Builder("code")
				.setMaxLength(2)
				.build());

		assertField(unit,
			new StringField
				.Builder("description")
				.setMaxLength(5)
				.build());
	}
}

package net.openid.conformance.openbanking_brasil.creditOperations.financing.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.openbanking_brasil.LinksAndMetaValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SetUtils;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api: swagger/openinsurance/financings/v2/swagger_financings_apis-v2.yaml
 * Api endpoint: /contracts/{contractId}/warranties
 * Api version: 2.0.1.final
 */

@ApiName("Financing Guarantees V2")
public class FinancingGuaranteesResponseValidatorV2 extends AbstractJsonAssertingCondition {
	private final LinksAndMetaValidator linksAndMetaValidator = new LinksAndMetaValidator(this);
	public static final Set<String> ENUM_WARRANTY_TYPE = SetUtils.createSet("CESSAO_DIREITOS_CREDITORIOS, CAUCAO, PENHOR, ALIENACAO_FIDUCIARIA, HIPOTECA, OPERACOES_GARANTIDAS_PELO_GOVERNO, OUTRAS_GARANTIAS_NAO_FIDEJUSSORIAS, SEGUROS_ASSEMELHADOS, GARANTIA_FIDEJUSSORIA, BENS_ARRENDADOS, GARANTIAS_INTERNACIONAIS, OPERACOES_GARANTIDAS_OUTRAS_ENTIDADES, ACORDOS_COMPENSACAO");
	public static final Set<String> ENUM_WARRANTY_SUB_TYPE = SetUtils.createSet("ACOES_DEBENTURES, ACORDOS_COMPENSACAO_LIQUIDACAO_OBRIGACOES, APLICACOES_FINANCEIRAS_RENDA_FIXA, APLICACOES_FINANCEIRAS_RENDA_VARIAVEL, APOLICES_CREDITO_EXPORTACAO, CCR_CONVENIO_CREDITOS_RECIPROCOS, CHEQUES, CIVIL, DIREITOS_SOBRE_ALUGUEIS, DEPOSITOS_A_VISTA_A_PRAZO_POUPANCA_OURO_TITULOS_PUBLICOS_FEDERAIS_ART_36, DEPOSITO_TITULOS_EMITIDOS_ENTIDADES_ART_23, DUPLICATAS, EMD_ENTIDADES_MULTILATERAIS_DESENVOLVIMENTO_ART_37, EQUIPAMENTOS, ESTADUAL_OU_DISTRITAL, FATURA_CARTAO_CREDITO, FEDERAL, FCVS_FUNDO_COMPENSACAO_VARIACOES_SALARIAIS, FGI_FUNDO_GARANTIDOR_INVESTIMENTOS, FGPC_FUNDO_GARANTIA_PROMOCAO_COMPETIT, FGTS_FUNDO_GARANTIA_TEMPO_SERVICO, FUNDO_GARANTIDOR_AVAL, GARANTIA_PRESTADA_FGPC_LEI_9531_ART_37, GARANTIA_PRESTADA_FUNDOS_QUAISQUER_OUTROS_MECANISMOS_COBERTURA_RISCO_CREDITO_ART_37, GARANTIA_PRESTADA_TESOURO_NACIONAL_OU_BACEN_ART_37_BENS_DIREITOS_INTEGRANTES_PATRIMONIO_AFETACAO, IMOVEIS, IMOVEIS_RESIDENCIAIS, MITIGADORAS, MUNICIPAL, NAO_MITIGADORAS, NOTAS_PROMISSORIAS_OUTROS_DIREITOS_CREDITO, OUTRAS, OUTROS, OUTROS_BENS, OUTROS_GRAUS, OUTROS_IMOVEIS, OUTROS_SEGUROS_ASSEMELHADOS, PESSOA_FISICA, PESSOA_FISICA_EXTERIOR, PESSOA_JURIDICA, PESSOA_JURIDICA_EXTERIOR, PRIMEIRO_GRAU_BENS_DIREITOS_INTEGRANTES_PATRIMONIO_AFETACAO, PRIMEIRO_GRAU_IMOVEIS_RESIDENCIAIS, PRIMEIRO_GRAU_OUTROS, PROAGRO, PRODUTOS_AGROPECUARIOS_COM_WARRANT, PRODUTOS_AGROPECUARIOS_SEM_WARRANT, SBCE_SOCIEDADE_BRASILEIRA_CREDITO_EXPORTAÇÃO, SEGURO_RURAL, SEM_SUB_TIPO_GARANTIA, TRIBUTOS_RECEITAS_ORCAMENTARIAS, VEICULOS, VEICULOS_AUTOMOTORES");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder(ROOT_PATH)
				.setValidator(this::assertData)
				.setMinItems(0)
				.build());
		linksAndMetaValidator.assertMetaAndLinks(body);
		logFinalStatus();
		return environment;
	}

	private void assertData(JsonObject element) {
		assertField(element,
			new StringField
				.Builder("currency")
				.setPattern("^(\\w{3}){1}$")
				.setMaxLength(3)
				.setOptional()
				.build());

		assertField(element,
			new StringField
				.Builder("warrantyType")
				.setEnums(ENUM_WARRANTY_TYPE)
				.build());

		assertField(element,
			new StringField
				.Builder("warrantySubType")
				.setEnums(ENUM_WARRANTY_SUB_TYPE)
				.build());

		assertField(element,
			new StringField
				.Builder("warrantyAmount")
				.setPattern("^\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setMinLength(4)
				.setOptional()
				.build());
	}
}

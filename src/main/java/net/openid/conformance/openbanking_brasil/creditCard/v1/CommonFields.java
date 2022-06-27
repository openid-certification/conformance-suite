package net.openid.conformance.openbanking_brasil.creditCard.v1;

import com.google.common.collect.Sets;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

public class CommonFields extends net.openid.conformance.openbanking_brasil.CommonFields {
	private static final Set<String> enumFeeType = Sets.newHashSet("ANUIDADE", "SAQUE_CARTAO_BRASIL", "SAQUE_CARTAO_EXTERIOR", "AVALIACAO_EMERGENCIAL_CREDITO", "EMISSAO_SEGUNDA_VIA", "TARIFA_PAGAMENTO_CONTAS", "SMS", "OUTRA");
	private static final Set<String> enumLineName = Sets.newHashSet("CREDITO_A_VISTA", "CREDITO_PARCELADO", "SAQUE_CREDITO_BRASIL", "SAQUE_CREDITO_EXTERIOR", "EMPRESTIMO_CARTAO_CONSIGNADO", "OUTROS");
	private static final Set<String> enumCreditsType = Sets.newHashSet("CREDITO_ROTATIVO", "PARCELAMENTO_FATURA", "EMPRESTIMO", "OUTROS");
	private static final Set<String> enumPaymentType = Sets.newHashSet("A_VISTA", "A_PRAZO");
	private static final Set<String> enumTransactionType = Sets.newHashSet("PAGAMENTO", "TARIFA", "OPERACOES_CREDITO_CONTRATADAS_CARTAO", "ESTORNO", "CASHBACK", "OUTROS");


	public static StringField.Builder feeType() {
		return new StringField
			.Builder("feeType")
			.setEnums(enumFeeType)
			.setMaxLength(29);
	}

	public static StringField.Builder lineName() {
		return new StringField
			.Builder("lineName")
			.setMaxLength(28)
			.setEnums(enumLineName)
			.setOptional();
	}

	public static StringField.Builder otherCreditsType() {
		return new StringField
			.Builder("otherCreditsType")
			.setEnums(enumCreditsType)
			.setMaxLength(19);
	}

	public static StringField.Builder paymentType() {
		return new StringField
			.Builder("paymentType")
			.setEnums(enumPaymentType)
			.setMaxLength(7);
	}

	public static StringField.Builder transactionType() {
		return new StringField
			.Builder("transactionType")
			.setEnums(enumTransactionType)
			.setMaxLength(36);
	}

}

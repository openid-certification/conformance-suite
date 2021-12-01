package net.openid.conformance.openbanking_brasil.productsNServices;

import com.google.common.collect.Sets;
import net.openid.conformance.util.field.Field;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

public class ProductNServicesCommonFields {

	public static final Set<String> CURRENCY = Sets.newHashSet("AFN", "ALL", "DZD", "USD", "EUR", "AOA", "XCD", "XCD", "ARS", "AMD", "AWG", "AUD", "EUR", "AZN", "BSD", "BHD", "BDT", "BBD", "BYN", "EUR", "BZD", "XOF", "BMD", "BTN", "BOB", "BOV", "USD", "BAM", "BWP", "NOK", "BRL", "USD", "BND", "BGN", "XOF", "BIF", "CVE", "KHR", "XAF", "CAD", "KYD", "XAF", "XAF", "CLF", "CLP", "CNY", "AUD", "AUD", "COP", "COU", "KMF", "CDF", "XAF", "NZD", "CRC", "HRK", "CUC", "CUP", "ANG", "EUR", "CZK", "XOF", "DKK", "DJF", "XCD", "DOP", "USD", "EGP", "SVC", "USD", "XAF", "ERN", "EUR", "ETB", "EUR", "FKP", "DKK", "FJD", "EUR", "EUR", "EUR", "XPF", "EUR", "XAF", "GMD", "GEL", "EUR", "GHS", "GIP", "EUR", "DKK", "XCD", "EUR", "USD", "GTQ", "GBP", "GNF", "XOF", "GYD", "HTG", "USD", "AUD", "EUR", "HNL", "HKD", "HUF", "ISK", "INR", "IDR", "XDR", "IRR", "IQD", "EUR", "GBP", "ILS", "EUR", "JMD", "JPY", "GBP", "JOD", "KZT", "KES", "AUD", "KPW", "KRW", "KWD", "KGS", "LAK", "EUR", "LBP", "LSL", "ZAR", "LRD", "LYD", "CHF", "EUR", "EUR", "MOP", "MGA", "MWK", "MYR", "MVR", "XOF", "EUR", "USD", "EUR", "MRU", "MUR", "EUR", "XUA", "MXN", "MXV", "USD", "MDL", "EUR", "MNT", "EUR", "XCD", "MAD", "MZN", "MMK", "NAD", "ZAR", "AUD", "NPR", "EUR", "XPF", "NZD", "NIO", "XOF", "NGN", "NZD", "AUD", "USD", "NOK", "OMR", "PKR", "USD", "PAB", "USD", "PGK", "PYG", "PEN", "PHP", "NZD", "PLN", "EUR", "USD", "QAR", "MKD", "RON", "RUB", "RWF", "EUR", "EUR", "SHP", "XCD", "XCD", "EUR", "EUR", "XCD", "WST", "EUR", "STN", "SAR", "XOF", "RSD", "SCR", "SLL", "SGD", "ANG", "XSU", "EUR", "EUR", "SBD", "SOS", "ZAR", "SSP", "EUR", "LKR", "SDG", "SRD", "NOK", "SZL", "SEK", "CHE", "CHF", "CHW", "SYP", "TWD", "TJS", "TZS", "THB", "USD", "XOF", "NZD", "TOP", "TTD", "TND", "TRY", "TMT", "USD", "AUD", "UGX", "UAH", "AED", "GBP", "USD", "USD", "USN", "UYI", "UYU", "UZS", "VUV", "VEF", "VND", "USD", "USD", "XPF", "MAD", "YER", "ZMW", "ZWL");
	public static final Set<String> EXCLUDED_RISKS = Sets.newHashSet("ATO_RECONHECIMENTO_PERIGOSO", "ATO_ILICITO_DOLOSO_PRATICADO_SEGURADO", "OPERACOES_DE_GUERRA", "FURACOES_CICLONES_TERREMOTOS", "MATERIAL_NUCLEAR", "DOENCAS_LESOES_PREEXISTENTES", "EPIDEMIAS_PANDEMIAS", "SUICIDIO", "ATO_ILICITO_DOLOSO_PRATICADO_CONTROLADOR", "OUTROS");

	public static Field.FieldBuilder name() {
		return new StringField
			.Builder("name");
	}

	public static Field.FieldBuilder code() {
		return new StringField
			.Builder("code");
	}

	public static Field.FieldBuilder cnpjNumber() {
		return new StringField
			.Builder("cnpjNumber");
	}
}

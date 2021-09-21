package net.openid.conformance.openbanking_brasil.productsNServices;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

public class CommonValidatorParts {
	private static class Fields extends CommonFields {}

	Set<String> intervals = Sets.newHashSet("1_FAIXA", "2_FAIXA", "3_FAIXA", "4_FAIXA");

	private final AbstractJsonAssertingCondition validator;

	public CommonValidatorParts(AbstractJsonAssertingCondition validator) {
		this.validator = validator;
	}

	public void applyAssertingForCommonRates(JsonObject rates, String fieldNaming,
											 boolean isOptional) {
		if (isOptional) {
			validator.assertField(rates,
				new ObjectArrayField
					.Builder(fieldNaming)
					.setValidator(this::assertInnerRates)
					.setMinItems(1)
					.setOptional()
					.build());
		} else {
			validator.assertField(rates,
				new ObjectArrayField
					.Builder(fieldNaming)
					.setValidator(this::assertInnerRates)
					.setMinItems(1)
					.build());
		}
	}

	private void assertInnerRates(JsonObject innerRates) {
		Set<String> rateIndexers = Sets.newHashSet("SEM_INDEXADOR_TAXA", "PRE_FIXADO",
			"POS_FIXADO_TR_TBF", "POS_FIXADO_TJLP", "POS_FIXADO_LIBOR", "POS_FIXADO_TLP", "OUTRAS_TAXAS_POS_FIXADAS",
			"FLUTUANTES_CDI", "FLUTUANTES_SELIC", "OUTRAS_TAXAS_FLUTUANTES", "INDICES_PRECOS_IGPM",
			"INDICES_PRECOS_IPCA", "INDICES_PRECOS_IPCC", "OUTROS_INDICES_PRECO", "CREDITO_RURAL_TCR_PRE",
			"CREDITO_RURAL_TCR_POS", "CREDITO_RURAL_TRFC_PRE", "CREDITO_RURAL_TRFC_POS", "OUTROS_INDEXADORES");

		validator.assertField(innerRates,
			new StringField
				.Builder("referentialRateIndexer")
				.setEnums(rateIndexers)
				.build());

		validator.assertField(innerRates,
			new StringField
				.Builder("rate")
				.setPattern("(^[0-1](\\.[0-9]{2})$|^NA$)")
				.setMaxLength(4)
				.build());

		validator.assertField(innerRates,
			new ObjectArrayField
				.Builder("applications")
				.setValidator(this::assertApplications)
				.setMinItems(4)
				.setMaxItems(4)
				.build());

		validator.assertField(innerRates,
			new StringField
				.Builder("minimumRate")
				.setPattern("(^[0-9](\\.[0-9]{4})$|^NA$)")
				.build());

		validator.assertField(innerRates,
			new StringField
				.Builder("maximumRate")
				.setPattern("(^[0-9](\\.[0-9]{4})$|^NA$)")
				.build());
	}

	private void assertApplications(JsonObject applications) {
		validator.assertField(applications,
			new StringField
				.Builder("interval")
				.setEnums(intervals)
				.build());


		validator.assertField(applications,
			new ObjectField
				.Builder("indexer")
				.setValidator(indexer -> validator.assertField(indexer, Fields.rate().setOptional().build()))
				.build());

		validator.assertField(applications,
			new ObjectField
				.Builder("customers")
				.setValidator( customers -> validator.assertField(customers, Fields.rate().build()) )
				.build());
	}

	public void assertPrices(JsonObject body) {
		validator.assertField(body,
			new ObjectArrayField
				.Builder("prices")
				.setValidator(this::assertPrice)
				.setMinItems(4)
				.setMaxItems(4)
				.build());
	}

	public void assertMonthlyPrices(JsonObject body) {
		validator.assertField(body,
			new ObjectArrayField
				.Builder("prices")
				.setValidator(this::assertMonthlyPrice)
				.setMinItems(4)
				.setMaxItems(4)
				.build());
	}

	private void assertPrice(JsonObject price) {
		validator.assertField(price,
			new StringField
				.Builder("interval")
				.setEnums(intervals)
				.build());

		validator.assertField(price, Fields.value().build());
		validator.assertField(price, Fields.currency().build());

		validator.assertField(price,
			new ObjectField
				.Builder("customers")
				.setValidator(this::assertPricesCustomers)
				.build());
	}

	private void assertMonthlyPrice(JsonObject price) {
		validator.assertField(price,
			new StringField
				.Builder("interval")
				.setEnums(intervals)
				.build());

		validator.assertField(price, Fields.monthlyFee().build());
		validator.assertField(price, Fields.currency().build());

		validator.assertField(price,
			new ObjectField
				.Builder("customers")
				.setValidator(this::assertPricesCustomers)
				.build());
	}

	private void assertPricesCustomers(JsonObject customers) {
	validator.assertField(customers, Fields.rate().build());
	}

	public void applyAssertingForCommonMinimumAndMaximum(JsonObject body) {
		validator.assertField(body,
			new ObjectField
				.Builder("minimum")
				.setValidator(this::assertInnerFieldsMinimumAndMaximum)
				.build());

		validator.assertField(body,
			new ObjectField
				.Builder("maximum")
				.setValidator(this::assertInnerFieldsMinimumAndMaximum)
				.build());
	}

	private void assertInnerFieldsMinimumAndMaximum(JsonObject jsonObject) {
		validator.assertField(jsonObject, Fields.value().build());
		validator.assertField(jsonObject, Fields.currency().build());
	}
}

package net.openid.conformance.fapiciba;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FAPICIBAConfigurationFields_UnitTest {

	private static final Set<String> BRAZIL_DIRECTORY_DCR_FIELDS = Set.of(
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase"
	);

	private static VariantService.TestPlanHolder plan;
	private static JsonObject variantSummary;

	@BeforeAll
	static void setUp() {
		VariantService variantService = new VariantService(holder -> true);
		plan = variantService.getTestPlan("fapi-ciba-id1-test-plan");
		assertNotNull(plan);
		variantSummary = new Gson().toJsonTree(plan.getVariantSummary()).getAsJsonObject();
	}

	@Test
	void dynamicBrazilClientShowsDirectoryDcrFields() {
		Set<String> fields = effectiveFields(
			ClientRegistration.DYNAMIC_CLIENT, FAPICIBAProfile.OPENBANKING_BRAZIL);

		assertTrue(fields.containsAll(BRAZIL_DIRECTORY_DCR_FIELDS));
	}

	@Test
	void staticBrazilClientDoesNotShowDirectoryDcrFields() {
		Set<String> fields = effectiveFields(
			ClientRegistration.STATIC_CLIENT, FAPICIBAProfile.OPENBANKING_BRAZIL);

		assertTrue(Collections.disjoint(fields, BRAZIL_DIRECTORY_DCR_FIELDS));
	}

	@Test
	void dynamicPlainFapiClientDoesNotShowBrazilDirectoryDcrFields() {
		Set<String> fields = effectiveFields(
			ClientRegistration.DYNAMIC_CLIENT, FAPICIBAProfile.PLAIN_FAPI);

		assertTrue(Collections.disjoint(fields, BRAZIL_DIRECTORY_DCR_FIELDS));
	}

	private static Set<String> effectiveFields(
		ClientRegistration clientRegistration, FAPICIBAProfile profile) {
		Set<String> fields = new HashSet<>(plan.configurationFields());
		for (Map<String, Object> module : plan.getTestModulesWithConfigFields()) {
			Object moduleFields = module.get("configurationFields");
			if (moduleFields instanceof Collection<?> collection) {
				collection.stream().map(String::valueOf).forEach(fields::add);
			}
		}

		Set<String> hiddenFields = new HashSet<>(plan.hidesConfigurationFields());
		Map<String, String> selection = Map.of(
			"client_registration", clientRegistration.toString(),
			"fapi_ciba_profile", profile.toString()
		);
		selection.forEach((parameter, value) -> {
			JsonObject variantValue = variantSummary
				.getAsJsonObject(parameter)
				.getAsJsonObject("variantValues")
				.getAsJsonObject(value);
			addStrings(fields, variantValue.getAsJsonArray("configurationFields"));
			addStrings(hiddenFields, variantValue.getAsJsonArray("hidesConfigurationFields"));
		});

		fields.removeAll(hiddenFields);
		return fields;
	}

	private static void addStrings(Set<String> target, JsonArray values) {
		values.forEach(value -> target.add(OIDFJSON.getString(value)));
	}
}

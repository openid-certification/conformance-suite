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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FAPICIBAConfigurationFields_UnitTest {

	private static final Set<String> BRAZIL_DIRECTORY_DCR_FIELDS = Set.of(
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase"
	);
	private static final Set<String> CLIENT_CREDENTIAL_FIELDS = Set.of(
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca"
	);
	private static final String BRAZIL_CIBA_MAXIMUM_EXPIRY_FIELD = "client.brazil_ciba_maximum_expiry";
	private static final Set<String> NOTIFICATION_MTLS_FIELDS = Set.of(
		"mtls.key",
		"mtls.cert",
		"mtls.ca"
	);

	private static VariantService.TestPlanHolder plan;
	private static JsonObject variantSummary;
	private static VariantService.TestPlanHolder clientPlan;
	private static JsonObject clientVariantSummary;

	@BeforeAll
	static void setUp() {
		VariantService variantService = new VariantService(holder -> true);
		plan = variantService.getTestPlan("fapi-ciba-id1-test-plan");
		assertNotNull(plan);
		variantSummary = new Gson().toJsonTree(plan.getVariantSummary()).getAsJsonObject();
		clientPlan = variantService.getTestPlan("fapi-ciba-id1-client-test-plan");
		assertNotNull(clientPlan);
		clientVariantSummary = new Gson().toJsonTree(clientPlan.getVariantSummary()).getAsJsonObject();
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

	@Test
	void dynamicBrazilClientShowsClientCredentialFields() {
		Set<String> fields = effectiveFields(
			ClientRegistration.DYNAMIC_CLIENT, FAPICIBAProfile.OPENBANKING_BRAZIL);

		assertTrue(fields.containsAll(CLIENT_CREDENTIAL_FIELDS));
	}

	@ParameterizedTest
	@EnumSource(ClientRegistration.class)
	void brazilClientShowsMaximumExpiryField(ClientRegistration clientRegistration) {
		Set<String> fields = effectiveFields(clientRegistration, FAPICIBAProfile.OPENBANKING_BRAZIL);

		assertTrue(fields.contains(BRAZIL_CIBA_MAXIMUM_EXPIRY_FIELD));
	}

	@ParameterizedTest
	@EnumSource(value = FAPICIBAProfile.class, names = {
		"PLAIN_FAPI", "OPENBANKING_UK", "CONNECTID_AU"
	})
	void nonBrazilClientDoesNotShowMaximumExpiryField(FAPICIBAProfile profile) {
		Set<String> fields = effectiveFields(ClientRegistration.STATIC_CLIENT, profile);

		assertFalse(fields.contains(BRAZIL_CIBA_MAXIMUM_EXPIRY_FIELD));
	}

	@Test
	void brazilRpEmulatorShowsMaximumExpiryField() {
		Set<String> fields = effectiveFields(
			clientPlan,
			clientVariantSummary,
			Map.of("fapi_ciba_profile", FAPICIBAProfile.OPENBANKING_BRAZIL.toString()));

		assertTrue(fields.contains(BRAZIL_CIBA_MAXIMUM_EXPIRY_FIELD));
	}

	@Test
	void brazilRpEmulatorShowsNotificationMtlsFields() {
		Set<String> fields = effectiveFields(
			clientPlan,
			clientVariantSummary,
			Map.of("fapi_ciba_profile", FAPICIBAProfile.OPENBANKING_BRAZIL.toString()));

		assertTrue(fields.containsAll(NOTIFICATION_MTLS_FIELDS));
	}

	@ParameterizedTest
	@EnumSource(value = FAPICIBAProfile.class, names = {
		"PLAIN_FAPI", "OPENBANKING_UK", "CONNECTID_AU"
	})
	void nonBrazilRpEmulatorDoesNotShowNotificationMtlsFields(FAPICIBAProfile profile) {
		Set<String> fields = effectiveFields(
			clientPlan,
			clientVariantSummary,
			Map.of("fapi_ciba_profile", profile.toString()));

		assertTrue(Collections.disjoint(fields, NOTIFICATION_MTLS_FIELDS));
	}

	@Test
	void plainFapiRpEmulatorDoesNotShowMaximumExpiryField() {
		Set<String> fields = effectiveFields(
			clientPlan,
			clientVariantSummary,
			Map.of("fapi_ciba_profile", FAPICIBAProfile.PLAIN_FAPI.toString()));

		assertFalse(fields.contains(BRAZIL_CIBA_MAXIMUM_EXPIRY_FIELD));
	}

	@ParameterizedTest
	@EnumSource(FAPICIBAProfile.class)
	void staticClientShowsClientCredentialFields(FAPICIBAProfile profile) {
		Set<String> fields = effectiveFields(ClientRegistration.STATIC_CLIENT, profile);

		assertTrue(fields.containsAll(CLIENT_CREDENTIAL_FIELDS));
	}

	@ParameterizedTest
	@EnumSource(value = FAPICIBAProfile.class, names = {
		"PLAIN_FAPI", "OPENBANKING_UK", "CONNECTID_AU"
	})
	void dynamicNonBrazilClientDoesNotShowClientCredentialFields(FAPICIBAProfile profile) {
		Set<String> fields = effectiveFields(ClientRegistration.DYNAMIC_CLIENT, profile);

		assertTrue(Collections.disjoint(fields, CLIENT_CREDENTIAL_FIELDS));
	}

	private static Set<String> effectiveFields(
		ClientRegistration clientRegistration, FAPICIBAProfile profile) {
		return effectiveFields(
			plan,
			variantSummary,
			Map.of(
				"client_registration", clientRegistration.toString(),
				"fapi_ciba_profile", profile.toString()));
	}

	private static Set<String> effectiveFields(
		VariantService.TestPlanHolder selectedPlan,
		JsonObject selectedVariantSummary,
		Map<String, String> selection) {
		Set<String> fields = new HashSet<>(selectedPlan.configurationFields());
		for (Map<String, Object> module : selectedPlan.getTestModulesWithConfigFields()) {
			Object moduleFields = module.get("configurationFields");
			if (moduleFields instanceof Collection<?> collection) {
				collection.stream().map(String::valueOf).forEach(fields::add);
			}
		}

		Set<String> hiddenFields = new HashSet<>(selectedPlan.hidesConfigurationFields());
		selection.forEach((parameter, value) -> {
			JsonObject variantValue = selectedVariantSummary
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

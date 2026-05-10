package net.openid.conformance.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.openid.conformance.variant.VariantService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

class ConfigurationFieldsSnapshot_UnitTest {

	private static final List<String> IN_SCOPE_PLANS = List.of(
		"fapi2-security-profile-final-test-plan",
		"fapi2-security-profile-final-brazil-dcr-test-plan",
		"fapi2-message-signing-final-test-plan",
		"oid4vci-1_0-issuer-test-plan",
		"oid4vci-1_0-issuer-haip-test-plan"
	);

	@Test
	void dumpInScopePlans() throws IOException {
		VariantService variantService = new VariantService(holder -> true);

		Map<String, Object> snapshot = new TreeMap<>();
		for (String planName : IN_SCOPE_PLANS) {
			VariantService.TestPlanHolder plan = variantService.getTestPlan(planName);
			assertNotNull(plan, () -> "in-scope plan '" + planName + "' not found; "
				+ "IN_SCOPE_PLANS is out of sync with the codebase, update the test");
			Map<String, Object> data = new TreeMap<>();
			data.put("configurationFields", deepSort(plan.configurationFields()));
			data.put("hidesConfigurationFields", deepSort(plan.hidesConfigurationFields()));
			data.put("modules", deepSort(plan.getTestModulesWithConfigFields()));
			data.put("variants", deepSort(plan.getVariantSummary()));
			data.put("effectiveByVariantSelection", deepSort(effectiveFieldsByVariant(plan)));
			snapshot.put(planName, data);
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
		Path outPath = Path.of("target", "configuration-fields-snapshot.json");
		Files.createDirectories(outPath.getParent());
		Files.writeString(outPath, gson.toJson(snapshot));
		System.out.println("Wrote configuration fields snapshot to " + outPath.toAbsolutePath());
	}

	// Compute the effective UI fields for the full Cartesian product of selectable variant
	// values for this plan. This mirrors schedule-test.html's updateConfigFieldVisibility:
	// start with plan.configurationFields ∪ each module's combinedConfigurationFields,
	// add the configurationFields for each selected variant value, then subtract
	// plan.hidesConfigurationFields ∪ each selected variant value's hidesConfigurationFields.
	@SuppressWarnings("unchecked")
	private static Map<String, List<String>> effectiveFieldsByVariant(VariantService.TestPlanHolder plan) {
		Set<String> base = new TreeSet<>();
		base.addAll(plan.configurationFields());
		for (Map<String, Object> module : plan.getTestModulesWithConfigFields()) {
			Object fields = module.get("configurationFields");
			if (fields instanceof Collection<?> coll) {
				for (Object f : coll) {
					base.add(String.valueOf(f));
				}
			}
		}
		Set<String> baseHides = new TreeSet<>(plan.hidesConfigurationFields());

		Map<String, Map<String, Object>> variantSummary = (Map<String, Map<String, Object>>) plan.getVariantSummary();
		List<String> variantNames = new ArrayList<>(variantSummary.keySet());
		variantNames.sort(String::compareTo);

		// Cartesian product across selectable variants.
		List<Map<String, String>> selections = new ArrayList<>();
		selections.add(new LinkedHashMap<>());
		for (String variantName : variantNames) {
			Map<String, Object> info = variantSummary.get(variantName);
			Map<String, Object> values = (Map<String, Object>) info.get("variantValues");
			if (values == null || values.isEmpty()) {
				continue;
			}
			List<Map<String, String>> next = new ArrayList<>();
			for (Map<String, String> base0 : selections) {
				for (String value : new TreeSet<>(values.keySet())) {
					Map<String, String> extended = new LinkedHashMap<>(base0);
					extended.put(variantName, value);
					next.add(extended);
				}
			}
			selections = next;
		}

		Map<String, List<String>> out = new TreeMap<>();
		for (Map<String, String> sel : selections) {
			Set<String> fields = new TreeSet<>(base);
			Set<String> hides = new TreeSet<>(baseHides);
			for (Map.Entry<String, String> e : sel.entrySet()) {
				Map<String, Object> info = variantSummary.get(e.getKey());
				Map<String, Object> values = (Map<String, Object>) info.get("variantValues");
				Map<String, Object> valData = (Map<String, Object>) values.get(e.getValue());
				if (valData != null) {
					Object cf = valData.get("configurationFields");
					if (cf instanceof Collection<?> coll) {
						for (Object f : coll) {
							fields.add(String.valueOf(f));
						}
					}
					Object hf = valData.get("hidesConfigurationFields");
					if (hf instanceof Collection<?> coll) {
						for (Object f : coll) {
							hides.add(String.valueOf(f));
						}
					}
				}
			}
			fields.removeAll(hides);
			String key = sel.entrySet().stream()
				.map(en -> en.getKey() + "=" + en.getValue())
				.reduce((a, b) -> a + "&" + b)
				.orElse("(no-variants)");
			out.put(key, new ArrayList<>(fields));
		}
		return out;
	}

	// Recursively sort Maps (-> TreeMap) and Collections of strings (-> sorted ArrayList).
	// Collections of Maps are sorted by their "testModule" key if present, otherwise by stringified value;
	// this is enough to make module lists deterministic without imposing a fragile global ordering.
	@SuppressWarnings("unchecked")
	private static Object deepSort(Object in) {
		if (in == null) {
			return null;
		}
		if (in instanceof Map<?, ?> map) {
			Map<String, Object> out = new TreeMap<>();
			map.forEach((k, v) -> out.put(String.valueOf(k), deepSort(v)));
			return out;
		}
		if (in instanceof Collection<?> coll) {
			List<Object> sorted = new ArrayList<>();
			for (Object item : coll) {
				sorted.add(deepSort(item));
			}
			sorted.sort((a, b) -> {
				String sa = sortKey(a);
				String sb = sortKey(b);
				return sa.compareTo(sb);
			});
			return sorted;
		}
		return in;
	}

	@SuppressWarnings("unchecked")
	private static String sortKey(Object o) {
		if (o instanceof Map<?, ?> map) {
			Object tm = map.get("testModule");
			if (tm != null) {
				return "module:" + tm;
			}
			return "map:" + map.toString();
		}
		return String.valueOf(o);
	}
}

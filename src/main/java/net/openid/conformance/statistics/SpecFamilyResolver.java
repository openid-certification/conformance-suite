package net.openid.conformance.statistics;

import net.openid.conformance.info.Plan;
import net.openid.conformance.plan.TestPlan.SpecFamilyNames;
import net.openid.conformance.variant.VariantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Maps a test plan name (as stored on {@code TEST_PLAN.planName}) to the spec family
 * the plan belongs to, for the admin statistics page.
 *
 * <p>It also maps a plan name to what that plan puts under test - the profile the plan is
 * published under, e.g. "Test an OpenID Provider / Authorization Server" - and a test
 * module name to the plans, and so the families, it is run under. A module listed by plans
 * of several families belongs to all of them.
 *
 * <p>Two synthetic families sit alongside the {@link SpecFamilyNames} constants:
 * {@link #NO_PLAN} for standalone test runs (no plan at all) and {@link #OTHER_RETIRED}
 * for runs whose plan can no longer be resolved. {@link #OTHER_RETIRED} stands in for an
 * unknown entity under test too, for the same reason.
 *
 * <p>A plan the registry does not have is looked up once more, in the generated map of
 * retired and renamed plan names at {@value #ALIASES} - see
 * {@code scripts/generate-legacy-plan-aliases.sh} - so that a run recorded under a name the
 * suite has since dropped is still charted under the family it was run for. An aliased plan
 * is not a plan the suite can run, so {@link #isKnownPlan(String)}, {@link #plansIn(String)}
 * and the module maps stay registry-only: what a family <em>offers</em> to run is the
 * registry's answer alone. What a family has ever <em>had</em> runs under is
 * {@link #plansEverIn(String)}, which includes the aliases, so that clicking a family's bar
 * lists the same plans the bar counted.
 *
 * <p><b>Caveat:</b> the registry this is built from ({@link VariantService}) filters plans
 * by {@code fintechlabs.profiles.visible}, so plans hidden by that property - as well as
 * plans that have been renamed or deleted since the runs were recorded and that the alias
 * map does not name - resolve to {@link #OTHER_RETIRED}.
 */
@Component
public class SpecFamilyResolver {

	/** Family for test runs that were not part of a test plan. */
	public static final String NO_PLAN = "No plan";

	/** Family for runs whose plan is not in the registry (renamed, deleted or hidden). */
	public static final String OTHER_RETIRED = "Other / retired";

	/** The generated map of retired and renamed plan names to their family. */
	static final String ALIASES = "/statistics/legacy-plan-families.properties";

	private static final Logger logger = LoggerFactory.getLogger(SpecFamilyResolver.class);

	/** The {@link SpecFamilyNames} constants, in declaration order. */
	private static final List<String> DECLARED_FAMILIES = List.of(
		SpecFamilyNames.authzen,
		SpecFamilyNames.ekyc,
		SpecFamilyNames.fapi1Advanced,
		SpecFamilyNames.fapi2SecurityProfile,
		SpecFamilyNames.fapi2MessageSigning,
		SpecFamilyNames.fapiCiba,
		SpecFamilyNames.federation,
		SpecFamilyNames.oid4vci,
		SpecFamilyNames.oid4vp,
		SpecFamilyNames.oidcc,
		SpecFamilyNames.oidccLogout,
		SpecFamilyNames.oidccSessionManagement,
		SpecFamilyNames.ssf);

	private final Map<String, String> planNameToFamily;

	private final Map<String, String> legacyPlanNameToFamily;

	private final Map<String, String> planNameToEntity;

	private final Map<String, Set<String>> familyToPlanNames;

	private final Map<String, Set<String>> familyToPlanNamesEver;

	private final Map<String, Set<String>> moduleToPlanNames;

	private final Map<String, Set<String>> moduleToFamilies;

	private final List<String> familyOrder;

	@Autowired
	public SpecFamilyResolver(VariantService variantService) {
		this(familiesFromRegistry(variantService.getTestPlans()), entitiesFromRegistry(variantService.getTestPlans()),
			modulesFromRegistry(variantService.getTestPlans()), aliasesFromClasspath());
	}

	SpecFamilyResolver(Map<String, String> planNameToFamily) {
		this(planNameToFamily, Map.of());
	}

	SpecFamilyResolver(Map<String, String> planNameToFamily, Map<String, String> planNameToEntity) {
		this(planNameToFamily, planNameToEntity, Map.of());
	}

	SpecFamilyResolver(Map<String, String> planNameToFamily, Map<String, String> planNameToEntity,
			Map<String, List<String>> modulesByPlan) {
		this(planNameToFamily, planNameToEntity, modulesByPlan, Map.of());
	}

	/**
	 * @param planNameToFamily       the family of each plan the registry publishes
	 * @param planNameToEntity       what each of those plans puts under test
	 * @param modulesByPlan          the test module names each plan runs; inverted here,
	 *                               because the statistics ask what a module belongs to rather
	 *                               than what a plan contains
	 * @param legacyPlanNameToFamily the family of each plan name the registry has lost, from
	 *                               {@value #ALIASES}; entries naming a plan the registry still
	 *                               publishes, or a family no chart has a series for, are
	 *                               dropped here rather than at every lookup
	 */
	SpecFamilyResolver(Map<String, String> planNameToFamily, Map<String, String> planNameToEntity,
			Map<String, List<String>> modulesByPlan, Map<String, String> legacyPlanNameToFamily) {
		this.planNameToFamily = Map.copyOf(planNameToFamily);
		this.planNameToEntity = Map.copyOf(planNameToEntity);
		this.familyToPlanNames = buildFamilyToPlanNames(planNameToFamily);
		this.moduleToPlanNames = buildModuleToPlanNames(modulesByPlan);
		this.moduleToFamilies = buildModuleToFamilies(this.moduleToPlanNames, this.planNameToFamily);
		this.familyOrder = buildFamilyOrder(this.planNameToFamily.values());
		this.legacyPlanNameToFamily = usableAliases(legacyPlanNameToFamily, this.planNameToFamily.keySet(),
			this.familyOrder);
		this.familyToPlanNamesEver = buildFamilyToPlanNamesEver(this.familyToPlanNames, this.legacyPlanNameToFamily);
	}

	/**
	 * @return the family for {@code planName}: the registry's answer, then what history says
	 *         the name used to belong to, then {@link #OTHER_RETIRED} if neither knows it or
	 *         {@code planName} is null.
	 */
	public String familyForPlan(String planName) {
		if (planName == null) {
			return OTHER_RETIRED;
		}
		String family = planNameToFamily.get(planName);
		if (family != null) {
			return family;
		}
		return legacyPlanNameToFamily.getOrDefault(planName, OTHER_RETIRED);
	}

	/**
	 * @return what {@code planName} tests - the profile it is published under - or
	 *         {@link #OTHER_RETIRED} if the plan is unknown or {@code planName} is null.
	 */
	public String entityForPlan(String planName) {
		if (planName == null) {
			return OTHER_RETIRED;
		}
		return planNameToEntity.getOrDefault(planName, OTHER_RETIRED);
	}

	/** @return true if {@code planName} is a plan the running suite still publishes. */
	public boolean isKnownPlan(String planName) {
		return planName != null && planNameToFamily.containsKey(planName);
	}

	/**
	 * @return true if the running suite no longer publishes {@code planName} but the alias map
	 *         still names the family it belonged to, so that {@link #familyForPlan(String)}
	 *         answers with something other than {@link #OTHER_RETIRED}. False for an alias
	 *         entry that was dropped as unusable, which is the same answer the statistics give
	 *         it: unresolved.
	 */
	public boolean isAliased(String planName) {
		return planName != null && legacyPlanNameToFamily.containsKey(planName);
	}

	/**
	 * The inverse of {@link #familyForPlan(String)}, for turning a family the user picked
	 * into something a database query can be written against.
	 *
	 * @param family a family name as {@link #familyOrder()} lists it
	 * @return the names of the plans of that family, in registry order; empty for a family
	 *         that no longer has any plans, which includes {@link #NO_PLAN} and
	 *         {@link #OTHER_RETIRED} - both stand for plans that are not in the registry, so
	 *         there are no names to list
	 */
	public Set<String> plansIn(String family) {
		return familyToPlanNames.getOrDefault(family, Set.of());
	}

	/**
	 * The inverse of {@link #familyForPlan(String)} over everything it can answer, which is
	 * what a drill-down has to ask: a family's bar counts the runs of its retired plan names
	 * too, so listing only {@link #plansIn(String)} would show fewer runs than the bar that
	 * was clicked - the older the period, the more of them missing.
	 *
	 * <p>Kept apart from {@link #plansIn(String)} because the two answer different questions.
	 * A filter that <em>offers</em> plans must offer only plans that exist; a filter that
	 * <em>selects</em> stored runs must name every plan those runs were recorded under.
	 *
	 * @param family a family name as {@link #familyOrder()} lists it
	 * @return the names of every plan of that family, in registry order and then alias order;
	 *         empty for a family that never had any, which includes {@link #NO_PLAN} and
	 *         {@link #OTHER_RETIRED} - both stand for runs whose plan cannot be named at all
	 */
	public Set<String> plansEverIn(String family) {
		return familyToPlanNamesEver.getOrDefault(family, Set.of());
	}

	/**
	 * @param testName a test module name, as stored on {@code TEST_INFO.testName}
	 * @return the names of the plans that run it, in registry order; empty for a module the
	 *         registry no longer has, which is what a run of a renamed or deleted module
	 *         leaves behind
	 */
	public Set<String> plansForModule(String testName) {
		return moduleToPlanNames.getOrDefault(testName, Set.of());
	}

	/**
	 * @param testName a test module name, as stored on {@code TEST_INFO.testName}
	 * @return the families of the plans that run it; a module listed by plans of several
	 *         families belongs to all of them, so a family filter on the modules table can
	 *         only ever be a membership test. Empty for an unknown module.
	 */
	public Set<String> familiesForModule(String testName) {
		return moduleToFamilies.getOrDefault(testName, Set.of());
	}

	/**
	 * @return every family a chart may need a series for, in a stable order so the client
	 *         colour mapping does not move: the {@link SpecFamilyNames} constants in
	 *         declaration order, then any family the registry uses that {@code TestPlan}
	 *         does not declare (alphabetically, so a new family is never dropped), then
	 *         {@link #NO_PLAN} and {@link #OTHER_RETIRED}.
	 */
	public List<String> familyOrder() {
		return familyOrder;
	}

	/**
	 * @return the generated map of retired and renamed plan names, or an empty map if it is
	 *         not on the classpath or cannot be read - the statistics are a reporting page, so
	 *         a missing alias file costs accuracy, not availability
	 */
	private static Map<String, String> aliasesFromClasspath() {
		Properties properties = new Properties();
		try (InputStream stream = SpecFamilyResolver.class.getResourceAsStream(ALIASES)) {
			if (stream == null) {
				logger.info("No {} on the classpath: runs of retired test plans are counted under \"{}\"",
					ALIASES, OTHER_RETIRED);
				return Map.of();
			}
			properties.load(stream);
		} catch (IOException e) {
			logger.warn("Could not read {}: runs of retired test plans are counted under \"{}\"",
				ALIASES, OTHER_RETIRED, e);
			return Map.of();
		}
		Map<String, String> aliases = new TreeMap<>();
		for (String planName : properties.stringPropertyNames()) {
			aliases.put(planName, properties.getProperty(planName));
		}
		logger.info("Loaded {} retired test plan name(s) from {}", aliases.size(), ALIASES);
		return aliases;
	}

	/**
	 * @return the alias entries worth keeping: the registry answers first, so an alias for a
	 *         plan it publishes is never read, and a value that is not a spec family would put
	 *         its runs somewhere no retired plan belongs - a bucket no chart draws, or one of
	 *         the two synthetic families, which would count a retired plan's runs as standalone
	 *         or hide them from the unresolved plan names the page reports. All of them are
	 *         dropped, which leaves the plans they name under {@link #OTHER_RETIRED} - where
	 *         they were before the alias map.
	 */
	private static Map<String, String> usableAliases(Map<String, String> aliases, Set<String> registered,
			List<String> families) {
		Map<String, String> usable = new LinkedHashMap<>();
		Set<String> unusableFamilies = new TreeSet<>();
		int unusable = 0;
		for (Map.Entry<String, String> alias : aliases.entrySet()) {
			if (registered.contains(alias.getKey())) {
				continue;
			}
			String family = alias.getValue();
			// familyOrder() ends with the two synthetic families, so membership of it is not
			// enough on its own: NO_PLAN is where runs that had no plan at all go, and
			// OTHER_RETIRED is the answer this map exists to replace
			if (NO_PLAN.equals(family) || OTHER_RETIRED.equals(family) || !families.contains(family)) {
				unusableFamilies.add(family);
				unusable++;
				continue;
			}
			usable.put(alias.getKey(), family);
		}
		if (!unusableFamilies.isEmpty()) {
			logger.warn("Ignoring {} retired test plan name(s) of {}: {} are not spec families a retired plan "
				+ "can be charted under, so their runs stay under \"{}\"",
				unusable, ALIASES, unusableFamilies, OTHER_RETIRED);
		}
		return Collections.unmodifiableMap(usable);
	}

	private static Map<String, String> familiesFromRegistry(Collection<VariantService.TestPlanHolder> plans) {
		Map<String, String> families = new LinkedHashMap<>();
		for (VariantService.TestPlanHolder plan : plans) {
			String family = plan.info.specFamily();
			if (family != null && !family.isBlank()) {
				families.put(plan.info.testPlanName(), family);
			}
		}
		return families;
	}

	private static Map<String, String> entitiesFromRegistry(Collection<VariantService.TestPlanHolder> plans) {
		Map<String, String> entities = new LinkedHashMap<>();
		for (VariantService.TestPlanHolder plan : plans) {
			String entity = plan.info.profile();
			if (entity != null && !entity.isBlank()) {
				entities.put(plan.info.testPlanName(), entity);
			}
		}
		return entities;
	}

	/** @return the test module names each plan runs, in registry order */
	private static Map<String, List<String>> modulesFromRegistry(Collection<VariantService.TestPlanHolder> plans) {
		Map<String, List<String>> modules = new LinkedHashMap<>();
		for (VariantService.TestPlanHolder plan : plans) {
			List<String> testNames = new ArrayList<>();
			// the plan's own module list, already deduplicated across conditional entries
			for (Plan.Module module : plan.getTestModules()) {
				testNames.add(module.getTestModule());
			}
			modules.put(plan.info.testPlanName(), testNames);
		}
		return modules;
	}

	private static Map<String, Set<String>> buildModuleToPlanNames(Map<String, List<String>> modulesByPlan) {
		Map<String, Set<String>> plansOfModule = new LinkedHashMap<>();
		modulesByPlan.forEach((planName, testNames) -> {
			for (String testName : testNames) {
				plansOfModule.computeIfAbsent(testName, module -> new LinkedHashSet<>()).add(planName);
			}
		});
		plansOfModule.replaceAll((testName, planNames) -> Collections.unmodifiableSet(planNames));
		return Collections.unmodifiableMap(plansOfModule);
	}

	/**
	 * @return the families of each module, resolved once here rather than per cell: the
	 *         statistics walk tens of thousands of module cells per request and every one
	 *         of them asks what its module belongs to
	 */
	private static Map<String, Set<String>> buildModuleToFamilies(Map<String, Set<String>> moduleToPlanNames,
			Map<String, String> planNameToFamily) {
		Map<String, Set<String>> familiesOfModule = new LinkedHashMap<>();
		moduleToPlanNames.forEach((testName, planNames) -> {
			Set<String> families = new LinkedHashSet<>();
			for (String planName : planNames) {
				String family = planNameToFamily.get(planName);
				// a plan with no declared family stands for no family, not for OTHER_RETIRED:
				// its modules are simply not offered under any family filter. That is
				// deliberately asymmetric with familyForPlan, which does answer OTHER_RETIRED
				// for such a plan; unreachable today, since every published plan declares a
				// specFamily, and a module of an unfamilied plan belongs under no filter
				// rather than under the bucket for plans the registry has lost
				if (family != null) {
					families.add(family);
				}
			}
			if (!families.isEmpty()) {
				familiesOfModule.put(testName, Collections.unmodifiableSet(families));
			}
		});
		return Collections.unmodifiableMap(familiesOfModule);
	}

	private static Map<String, Set<String>> buildFamilyToPlanNames(Map<String, String> planNameToFamily) {
		Map<String, Set<String>> plansOfFamily = new LinkedHashMap<>();
		// the registry map is iterated in registry order, so the plan names stay in it too
		planNameToFamily.forEach((planName, family) ->
			plansOfFamily.computeIfAbsent(family, f -> new LinkedHashSet<>()).add(planName));
		plansOfFamily.replaceAll((family, planNames) -> Collections.unmodifiableSet(planNames));
		return Collections.unmodifiableMap(plansOfFamily);
	}

	/**
	 * @param familyToPlanNames      the registry's plans per family
	 * @param legacyPlanNameToFamily the usable aliases, which are sorted by plan name
	 * @return the plans of each family with the aliased names added, the registry's first so
	 *         that a listing still opens with the plans the suite publishes
	 */
	private static Map<String, Set<String>> buildFamilyToPlanNamesEver(Map<String, Set<String>> familyToPlanNames,
			Map<String, String> legacyPlanNameToFamily) {
		Map<String, Set<String>> plansOfFamily = new LinkedHashMap<>();
		familyToPlanNames.forEach((family, planNames) -> plansOfFamily.put(family, new LinkedHashSet<>(planNames)));
		legacyPlanNameToFamily.forEach((planName, family) ->
			plansOfFamily.computeIfAbsent(family, f -> new LinkedHashSet<>()).add(planName));
		plansOfFamily.replaceAll((family, planNames) -> Collections.unmodifiableSet(planNames));
		return Collections.unmodifiableMap(plansOfFamily);
	}

	private static List<String> buildFamilyOrder(Collection<String> knownFamilies) {
		List<String> order = new ArrayList<>(DECLARED_FAMILIES);
		TreeSet<String> extras = new TreeSet<>(knownFamilies);
		extras.removeAll(DECLARED_FAMILIES);
		extras.remove(NO_PLAN);
		extras.remove(OTHER_RETIRED);
		order.addAll(extras);
		order.add(NO_PLAN);
		order.add(OTHER_RETIRED);
		return List.copyOf(order);
	}
}

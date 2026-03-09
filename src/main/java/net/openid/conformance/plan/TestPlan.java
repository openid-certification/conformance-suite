package net.openid.conformance.plan;

import net.openid.conformance.testmodule.TestModule;

import java.util.List;

/**
 * A collection of test modules intended to be run with a single
 * test configuration.
 */
public interface TestPlan {
	interface ProfileNames {
		String rptest = "Test a Relying Party / OAuth2 Client";
		String rplogouttest = "Test a Relying Party / OAuth2 Client Logout Support";
		String optest = "Test an OpenID Provider / Authorization Server";
		String ssftest = "Test Shared Signals Framework Support";
		String ekyctest = "Test an eKYC & IDA OpenID Provider";
		String vciissuer = "Test a OpenID4VCI issuer";
		String vciwallet = "Test a OpenID4VCI wallet";
		String wallettest = "Test a OpenID4VP wallet";
		String verifierTest = "Test a OpenID4VP Verifier";
		String federationTest = "Test an OpenID Federation entity";
		String authzenTest = "Test an Authzen PDP server";
	}

	/**
	 * A set of variants and values to use to run a particular test module.
	 *
	 * @see ModuleListEntry
	 */
	class Variant {
		public final Class<? extends Enum<?>> key;
		public final String value;
		public Variant(Class<? extends Enum<?>> key, String value) {
			this.key = key;
			this.value = value;
		}
	}

	/**
	 * A condition that must be met for a {@link ModuleListEntry} to be applicable.
	 * The entry is only used when the user-selected variant for the given parameter
	 * matches one of the specified values.
	 */
	class VariantCondition {
		public final Class<? extends Enum<?>> parameter;
		public final List<String> values;

		public VariantCondition(Class<? extends Enum<?>> parameter, String... values) {
			this.parameter = parameter;
			this.values = List.of(values);
		}
	}

	/**
	 * A holder for one or more test modules and the variants they should be run with
	 *
	 * A list of these is returned by testModulesWithVariants(), as an alternative to listing the test modules in the
	 * PublishTestPlan Annotation.
	 *
	 * Optionally, {@code applicableWhen} conditions can restrict this entry to only apply when
	 * the user-selected variants match. All conditions must be satisfied (AND logic);
	 * each condition matches if the user's value is any of the condition's values (OR logic).
	 */
	class ModuleListEntry {
		public final List<Class<? extends TestModule>> testModules;
		public final List<Variant> variant;
		public final List<VariantCondition> applicableWhen;

		public ModuleListEntry(List<Class<? extends TestModule>> testModules,
							   List<Variant> variant) {
			this(testModules, variant, List.of());
		}

		public ModuleListEntry(List<Class<? extends TestModule>> testModules,
							   List<Variant> variant,
							   List<VariantCondition> applicableWhen) {
			this.testModules = testModules;
			this.variant = variant;
			this.applicableWhen = applicableWhen;
		}
	}

	/* Instead of defined test modules in the @PublishTestModule annotation, TestPlans can implement the
	testModulesWithVariants() method, which allows them to define that test modules will be run with multiple
	variants:

	public static List<ModuleListEntry> testModulesWithVariants()

	To define a certification profile name (used in the certification submission) implement:

	public static String certificationProfileName(VariantSelection variant) {

	*/

}

package net.openid.conformance.plan;

import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;

import java.util.Collections;
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

	interface SpecFamilyNames {
		String authzen = "AuthZen";
		String ekyc = "eKYC & Identity Assurance";
		String fapi1Advanced = "FAPI1 Advanced";
		String fapi2SecurityProfile = "FAPI2 Security Profile";
		String fapi2MessageSigning = "FAPI2 Message Signing";
		String fapiCiba = "FAPI-CIBA";
		String federation = "OpenID Federation";
		String oid4vci = "OID4VCI";
		String oid4vp = "OID4VP";
		String oidcc = "OpenID Connect Core";
		String oidccLogout = "OpenID Connect Logout";
		String oidccSessionManagement = "OpenID Connect Session Management";
		String ssf = "Shared Signals Framework";
	}

	interface SpecVersionNames {
		// FAPI2 Security Profile
		String fapi2SpFinal = "Final";
		String fapi2SpId2 = "ID2";
		// FAPI2 Message Signing
		String fapi2MsFinal = "Final";
		String fapi2MsId1 = "ID1";
		// OID4VP
		String oid4vp1Final = "1.0 Final";
		String oid4vpId2 = "ID2";
		String oid4vpId3 = "ID3";
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

	/**
	 * Override to define test modules with specific variant combinations, as an alternative to
	 * listing them in the {@link PublishTestPlan} annotation.
	 *
	 * @return list of module/variant entries, or null to use the annotation's testModules instead
	 */
	default List<ModuleListEntry> testModulesWithVariants() {
		return null;
	}

	/**
	 * Override to define a certification profile name (used in the certification submission)
	 * for the given variant selection.
	 *
	 * @return list of certification profile names, or empty list if none
	 */
	default List<String> certificationProfileName(VariantSelection variant) {
		return Collections.emptyList();
	}

	/**
	 * Override to exclude specific variant values from being shown in the test plan UI.
	 * This is used when a plan shares modules with other plans but needs to restrict
	 * which variant values are available for selection.
	 *
	 * @return list of variant parameter/value pairs to exclude, or empty list if none
	 */
	default List<Variant> variantsNotApplicable() {
		return Collections.emptyList();
	}

}

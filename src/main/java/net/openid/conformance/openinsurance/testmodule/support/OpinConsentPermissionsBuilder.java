package net.openid.conformance.openinsurance.testmodule.support;

import net.openid.conformance.ConditionSequenceRepeater;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalServerTestModule;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.testmodule.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpinConsentPermissionsBuilder extends AbstractFAPI1AdvancedFinalServerTestModule {

	private final Logger logger = LoggerFactory.getLogger(ConditionSequenceRepeater.class);

	private Set<String> permissionsSet;
	private Set<String> includedPermissionsGroup;

	private boolean setsSet = false;

	public OpinConsentPermissionsBuilder(Environment env, String id, TestInstanceEventLog eventLog,
										 TestInfoService testInfo, TestExecutionManager executionManager) {

		super.setProperties(id, null, eventLog , null, testInfo, executionManager, null);
		this.env = env;

		logger.info("OpinConsentPermissionsBuilder was correctly instantiated");
	}

	public OpinConsentPermissionsBuilder resetPermissions() {
		this.permissionsSet = new HashSet<>();
		this.includedPermissionsGroup = new HashSet<>();

		//All calls to Consents API needs RESOURCES_READ permission
		includedPermissionsGroup.add(PermissionsGroup.RESOURCES_ALL);

		setsSet = true;
		logger.info("permissionsSet and includedPermissionsGroup sets were correctly instantiated");
		return this;
	}

	private boolean isSetsSet() {
		return setsSet;
	}

	public OpinConsentPermissionsBuilder addAllPermissions() {
		if(!isSetsSet()) {
			resetPermissions();
		}

		String[] permissions = {"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ", "RESOURCES_READ", "CUSTOMERS_PERSONAL_QUALIFICATION_READ",
			"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ", "CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ","CUSTOMERS_BUSINESS_QUALIFICATION_READ",
			"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ", "CAPITALIZATION_TITLES_READ", "CAPITALIZATION_TITLES_POLICYINFO_READ",
		"CAPITALIZATION_TITLES_PREMIUM_READ", "CAPITALIZATION_TITLES_CLAIM_READ", "PENSION_RISK_READ", "PENSION_RISK_CONTRACTINFO_READ",
			"PENSION_RISK_CONTRIBUTIONS_READ", "DAMAGES_AND_PEOPLE_PATRIMONIAL_READ", "DAMAGES_AND_PEOPLE_PATRIMONIAL_POLICYINFO_READ",
			"DAMAGES_AND_PEOPLE_PATRIMONIAL_PREMIUM_READ", "DAMAGES_AND_PEOPLE_PATRIMONIAL_CLAIM_READ", "DAMAGES_AND_PEOPLE_AERONAUTICAL_READ",
			"DAMAGES_AND_PEOPLE_AERONAUTICAL_POLICYINFO_READ", "DAMAGES_AND_PEOPLE_AERONAUTICAL_PREMIUM_READ", "DAMAGES_AND_PEOPLE_AERONAUTICAL_CLAIM_READ",
			"DAMAGES_AND_PEOPLE_NAUTICAL_READ", "DAMAGES_AND_PEOPLE_NAUTICAL_POLICYINFO_READ", "DAMAGES_AND_PEOPLE_NAUTICAL_PREMIUM_READ",
			"DAMAGES_AND_PEOPLE_NAUTICAL_CLAIM_READ", "DAMAGES_AND_PEOPLE_OIL_READ", "DAMAGES_AND_PEOPLE_OIL_POLICYINFO_READ", "DAMAGES_AND_PEOPLE_OIL_PREMIUM_READ",
			"DAMAGES_AND_PEOPLE_OIL_CLAIM_READ", "DAMAGES_AND_PEOPLE_RESPONSIBILITY_READ","DAMAGES_AND_PEOPLE_RESPONSIBILITY_POLICYINFO_READ",
			"DAMAGES_AND_PEOPLE_RESPONSIBILITY_PREMIUM_READ", "DAMAGES_AND_PEOPLE_RESPONSIBILITY_CLAIM_READ","DAMAGES_AND_PEOPLE_TRANSPORT_READ",
			"DAMAGES_AND_PEOPLE_TRANSPORT_POLICYINFO_READ","DAMAGES_AND_PEOPLE_TRANSPORT_PREMIUM_READ", "DAMAGES_AND_PEOPLE_TRANSPORT_CLAIM_READ",
			"DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_READ", "DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_POLICYINFO_READ","DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_PREMIUM_READ",
			"DAMAGES_AND_PEOPLE_FINANCIAL_RISKS_CLAIM_READ","DAMAGES_AND_PEOPLE_RURAL_READ", "DAMAGES_AND_PEOPLE_RURAL_POLICYINFO_READ", "DAMAGES_AND_PEOPLE_RURAL_PREMIUM_READ",
			"DAMAGES_AND_PEOPLE_RURAL_CLAIM_READ","DAMAGES_AND_PEOPLE_AUTO_READ", "DAMAGES_AND_PEOPLE_AUTO_POLICYINFO_READ","DAMAGES_AND_PEOPLE_AUTO_PREMIUM_READ",
			"DAMAGES_AND_PEOPLE_AUTO_CLAIM_READ","DAMAGES_AND_PEOPLE_HOUSING_READ","DAMAGES_AND_PEOPLE_HOUSING_POLICYINFO_READ","DAMAGES_AND_PEOPLE_HOUSING_PREMIUM_READ",
			"DAMAGES_AND_PEOPLE_HOUSING_CLAIM_READ","DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_READ","DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_POLICYINFO_READ",
			"DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_PREMIUM_READ","DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD_CLAIM_READ",
			"DAMAGES_AND_PEOPLE_PERSON_READ", "DAMAGES_AND_PEOPLE_PERSON_POLICYINFO_READ", "DAMAGES_AND_PEOPLE_PERSON_PREMIUM_READ","DAMAGES_AND_PEOPLE_PERSON_CLAIM_READ"
		};

		permissionsSet.addAll(List.of(permissions));
		includedPermissionsGroup.add(PermissionsGroup.ALL);

		logger.info("All Permissions were correctly added");
		return this;
	}
	public OpinConsentPermissionsBuilder addAllCustomersPersonalPermissions() {
		if(!isSetsSet()) {
			resetPermissions();
		}

		String[] permissions = {"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ", "RESOURCES_READ", "CUSTOMERS_PERSONAL_QUALIFICATION_READ",
			"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ" };

		permissionsSet.addAll(List.of(permissions));
		includedPermissionsGroup.add(PermissionsGroup.CUSTOMERS_PERSONAL);

		logger.info("All Customers Personal Permissions were correctly added");
		return this;
	}

	public OpinConsentPermissionsBuilder addAllCustomersBusinessPermissions() {
		if(!isSetsSet()) {
			resetPermissions();
		}

		String[] permissions = {"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ", "RESOURCES_READ", "CUSTOMERS_BUSINESS_QUALIFICATION_READ",
			"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ" };

		permissionsSet.addAll(List.of(permissions));
		includedPermissionsGroup.add(PermissionsGroup.CUSTOMERS_BUSINESS);

		logger.info("All Customers Business Permissions were correctly added");
		return this;
	}

	public OpinConsentPermissionsBuilder addAllCustomersPermissions() {
		if(!isSetsSet()) {
			resetPermissions();
		}

		addAllCustomersPersonalPermissions();
		addAllCustomersBusinessPermissions();

		logger.info("All Customers Business Permissions were correctly set");
		return this;
	}

	public OpinConsentPermissionsBuilder removePermissions(String... permissionGroup) {
		for (String permission : permissionGroup) {
			permissionsSet.removeIf(s -> s.startsWith(permission));
			includedPermissionsGroup.removeIf(s -> s.startsWith(permission));
		}

		return this;
	}

	public OpinConsentPermissionsBuilder set(String... permissions) {
		resetPermissions();
		permissionsSet.addAll(List.of(permissions));
		//No group associated, adding all permissions
		includedPermissionsGroup.addAll(List.of(permissions));
		return this;
	}

	public String getLogMessage() {

		String result = String.join(" ", includedPermissionsGroup) ;
		if (includedPermissionsGroup.contains(PermissionsGroup.ALL)) {
			result = "ALL PERMISSIONS";
		}
		String msg = String.format("Providing permissions %s\n", result);

		return msg;
	}

	public void build() {
		env.putString("consent_permissions", String.join(" ", permissionsSet));
		env.putString("consent_permissions_log", getLogMessage());
		call(condition(OpinLogConsentPermissions.class));
	}

}

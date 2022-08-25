package net.openid.conformance.openinsurance.testmodule.support;

import net.openid.conformance.ConditionSequenceRepeater;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalServerTestModule;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.testmodule.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OpinConsentPermissionsBuilder extends AbstractFAPI1AdvancedFinalServerTestModule {

	private final Logger logger = LoggerFactory.getLogger(ConditionSequenceRepeater.class);
	private Set<String> permissionsSet;
	private Set<PermissionsGroup> includedPermissionsGroups;
	private boolean setsSet = false;

	public OpinConsentPermissionsBuilder(Environment env, String id, TestInstanceEventLog eventLog,
										 TestInfoService testInfo, TestExecutionManager executionManager) {

		super.setProperties(id, null, eventLog , null, testInfo, executionManager, null);
		this.env = env;

		logger.info("OpinConsentPermissionsBuilder was correctly instantiated");
	}

	public OpinConsentPermissionsBuilder resetPermissions() {

		this.permissionsSet = new HashSet<>();
		this.includedPermissionsGroups = new HashSet<>();
		setsSet = true;
		logger.info("permissionsSet and includedPermissionsGroup sets were correctly instantiated");
		return this;
	}

	private boolean isSetsSet() {
		return setsSet;
	}

	public OpinConsentPermissionsBuilder set(String... permissions) {

		resetPermissions();
		permissionsSet.addAll(List.of(permissions));
		return this;
	}

	public OpinConsentPermissionsBuilder addPermissionsGroup(PermissionsGroup... permissionsGroups) {

		if(!isSetsSet()) {
			resetPermissions();
		}

		for (PermissionsGroup permissionsGroup : permissionsGroups) {
			permissionsSet.addAll(List.of(permissionsGroup.getPermissions()));
			includedPermissionsGroups.add(permissionsGroup);
		}
		return  this;
	}
	public OpinConsentPermissionsBuilder removePermissionsGroups(PermissionsGroup... permissionGroups) {

		for (PermissionsGroup permissionsGroup : permissionGroups) {
			List<String> permissions = new ArrayList<>(List.of(permissionsGroup.getPermissions()));
			permissions.remove("RESOURCES_READ");
			permissionsSet.removeAll(permissions);
		}
		return this;
	}

	public OpinConsentPermissionsBuilder removePermission(String... permissions) {

		for (String permission : permissions) {
			includedPermissionsGroups.removeIf(permissionsGroup -> permissionsGroup.name().startsWith(permission));
		}
		permissionsSet.removeAll(List.of(permissions));
		return this;
	}


	public String getLogMessage() {

		String result;
		if (includedPermissionsGroups.isEmpty()) {
			result = "CUSTOM";
		} else {
			result = String.join(" ", includedPermissionsGroups.toString()) ;
			if (includedPermissionsGroups.contains(PermissionsGroup.ALL)) {
				result = "ALL PERMISSIONS";
			}
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

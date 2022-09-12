package net.openid.conformance.openinsurance.testmodule.support;

import org.apache.commons.lang3.ArrayUtils;

public enum PermissionsGroup {
	ALL,
	All_PERSONAL,
	ALL_BUSINESS,
	RESOURCES,
	CUSTOMERS_PERSONAL,
	CUSTOMERS_BUSINESS,
	CAPITALIZATION_TITLES,
	PENSION_RISK,
	DAMAGES_AND_PEOPLE_PATRIMONIAL,
	DAMAGES_AND_PEOPLE_AERONAUTICAL,
	DAMAGES_AND_PEOPLE_NAUTICAL,
	DAMAGES_AND_PEOPLE_NUCLEAR,
	DAMAGES_AND_PEOPLE_OIL,
	DAMAGES_AND_PEOPLE_RESPONSIBILITY,
	DAMAGES_AND_PEOPLE_TRANSPORT,
	DAMAGES_AND_PEOPLE_FINANCIAL_RISKS,
	DAMAGES_AND_PEOPLE_RURAL,
	DAMAGES_AND_PEOPLE_AUTO,
	DAMAGES_AND_PEOPLE_HOUSING,
	DAMAGES_AND_PEOPLE_ACCEPTANCE_AND_BRANCHES_ABROAD,
	DAMAGES_AND_PEOPLE_PERSON
	;


	public String[] getPermissions() {
		String[] permissions = {};

		if (this.equals(ALL) || this.equals(All_PERSONAL) || this.equals(ALL_BUSINESS)) {
			for (PermissionsGroup permissionsGroup : PermissionsGroup.values()) {
				if (permissionsGroup.equals(ALL) ||
					(this.equals(All_PERSONAL) && permissionsGroup.equals(CUSTOMERS_BUSINESS)) ||
					(this.equals(ALL_BUSINESS) && permissionsGroup.equals(CUSTOMERS_PERSONAL))) {
					continue;
				}

				permissions = ArrayUtils.addAll(permissions, getIndividualPermissions(permissionsGroup));
			}
		} else {
			permissions = getIndividualPermissions(this);
		}

		return permissions;
	}

	private String[] getIndividualPermissions(PermissionsGroup permissionsGroup) {
		String[] permissions;
		switch (permissionsGroup) {
			case CUSTOMERS_PERSONAL:
				permissions = new String[]{permissionsGroup.name()+"_IDENTIFICATIONS_READ", permissionsGroup.name()+"_QUALIFICATION_READ",
					permissionsGroup.name()+"_ADITTIONALINFO_READ", "RESOURCES_READ"};
				break;

			case CUSTOMERS_BUSINESS:
				permissions = new String[]{permissionsGroup.name()+"_IDENTIFICATIONS_READ", permissionsGroup.name()+"_QUALIFICATION_READ",
					permissionsGroup.name()+"_ADITTIONALINFO_READ", "RESOURCES_READ"};
				break;

			case PENSION_RISK:
				permissions = new String[]{permissionsGroup.name() + "_READ", permissionsGroup.name()+"_CONTRACTINFO_READ",
					permissionsGroup.name()+"_CONTRIBUTIONS_READ", "RESOURCES_READ"};
				break;

			case RESOURCES:
				permissions = new String[]{"RESOURCES_READ"};
				break;

			default:
				permissions = new String[]{permissionsGroup.name() + "_READ", permissionsGroup.name() + "_POLICYINFO_READ",
					permissionsGroup.name() + "_PREMIUM_READ", permissionsGroup.name() + "_CLAIM_READ", "RESOURCES_READ"};
				break;
		}

		return permissions;
	}
}

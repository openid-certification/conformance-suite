package net.openid.conformance.raidiam.validators.organisationsExport;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: ****
 * Api endpoint: GET /organisations/export/roles
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations Export Roles")
public class GetOrganisationsExportRolesValidator extends AbstractJsonAssertingCondition {

	private static final Set<String> STATUS = Sets.newHashSet("Active", "Pending", "Withdrawn");

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = initBodyArray(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertInnerFields)
				.build());

		return environment;
	}

	private void assertInnerFields(JsonObject data) {
		assertField(data, CommonFields.getOrganisationId());

		assertField(data,
			new StringField
				.Builder("Status")
				.setEnums(STATUS)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("RegistrationNumber")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("RegisteredName")
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("ParentOrganisationReference")
				.setOptional()
				.build());

		assertField(data,
			new  ObjectArrayField
				.Builder("OrgDomainClaims")
				.setValidator(domainClaims -> {
					assertField(domainClaims,
						new StringField
							.Builder("AuthorisationDomainName")
							.setOptional()
							.build());

					assertField(domainClaims, CommonFields.getStatus());
				})
				.setOptional()
				.build());

		assertField(data,
			new  ObjectArrayField
				.Builder("OrgDomainRoleClaims")
				.setValidator(domainClaims -> {
					assertField(domainClaims,
						new StringField
							.Builder("AuthorisationDomainName")
							.setOptional()
							.build());

					assertField(domainClaims,
						new StringField
							.Builder("AuthorisationDomainRoleName")
							.setOptional()
							.build());

					assertField(domainClaims, CommonFields.getStatus());
				})
				.setOptional()
				.build());
	}
}

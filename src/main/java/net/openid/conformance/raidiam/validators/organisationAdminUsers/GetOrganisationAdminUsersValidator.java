package net.openid.conformance.raidiam.validators.organisationAdminUsers;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api url: ****
 * Api endpoint: GET /organisations/{OrganisationId}/adminusers
 * Api git hash: ****
 *
 */
public class GetOrganisationAdminUsersValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetOrganisationAdminUsersValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(data -> {
					assertField(data, CommonFields.getStatus());
					assertField(data, CommonFields.getUserEmail());
					parts.assertDomainRoleDetails(data);
				})
				.setOptional()
				.build());

		return environment;
	}
}

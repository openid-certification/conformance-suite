package net.openid.conformance.raidiam.validators.organisationAdminUsers;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
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
@ApiName("Raidiam Directory GET Organisation Admin Users")
public class GetOrganisationAdminUsersValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetOrganisationAdminUsersValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
				new ObjectArrayField
						.Builder("$")
						.setValidator(array -> {
							assertField(array, CommonFields.getStatus());
							assertField(array, CommonFields.getUserEmail());
							parts.assertDomainRoleDetails(array);
						})
						.build());

		return environment;
	}
}

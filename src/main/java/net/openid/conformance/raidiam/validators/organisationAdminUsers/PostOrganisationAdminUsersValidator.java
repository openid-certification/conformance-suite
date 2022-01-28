package net.openid.conformance.raidiam.validators.organisationAdminUsers;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api url: ****
 * Api endpoint: POST /organisations/{OrganisationId}/adminusers
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST Organisation Admin Users")
public class PostOrganisationAdminUsersValidator extends AbstractJsonAssertingCondition {

    private final CommonParts parts;

    public PostOrganisationAdminUsersValidator() {
        parts = new CommonParts(this);
    }

    @Override
    public Environment evaluate(Environment environment) {
        JsonElement body = bodyFrom(environment);
        assertField(body, CommonFields.getStatus());
        assertField(body, CommonFields.getUserEmail());
        parts.assertDomainRoleDetails(body);

        return environment;
    }
}
package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

public class EnsureResponseHasLinks extends AbstractJsonAssertingCondition {

    @Override
	public Environment evaluate(Environment environment) {

		log("CHeck for navigation Links in the response body.");
		
        JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);

        assertHasField(body, "$.links");
		assertField(body, new StringField.Builder("$.links.self").setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.first").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.prev").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.next").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());
		assertField(body, new StringField.Builder("$.links.last").setOptional().setPattern("^(https:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$").build());

        return environment;
	}
    
}

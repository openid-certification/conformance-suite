package net.openid.conformance.apis.raidiam.contacts;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.contacts.GetContactsByContactIdValidator;
import net.openid.conformance.raidiam.validators.contacts.GetContactsValidator;
import net.openid.conformance.raidiam.validators.contacts.PostContactsValidator;
import net.openid.conformance.raidiam.validators.contacts.PutContactsByContactIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class ContactsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/contacts/GetContactsByContactIdResponse.json")
	public void validateGetContactsByContactIdValidator() {
		GetContactsByContactIdValidator condition = new GetContactsByContactIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/contacts/GetContactsResponse.json")
	public void validateGetContactsValidator() {
		GetContactsValidator condition = new GetContactsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/contacts/PostContactsResponse.json")
	public void validatePostContactsValidator() {
		PostContactsValidator condition = new PostContactsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/contacts/PutContactsByContactIdResponse.json")
	public void validatePutContactsByContactIdValidator() {
		PutContactsByContactIdValidator condition = new PutContactsByContactIdValidator();
		run(condition);
	}
}

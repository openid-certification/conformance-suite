package net.openid.conformance.apis.consent;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseHasLinks;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/metaData/goodMetaLinksBodyResponse.json")
public class EnsureResponseHasLinksTest extends AbstractJsonResponseConditionUnitTest {

    @Test
	public void validateMetaDataAndLinks() {
		EnsureResponseHasLinks condition = new EnsureResponseHasLinks();
		run(condition);
	}

    @Test
	@UseResurce("jsonResponses/metaData/badResponseWithoutSelfLink.json")
	public void validateStructureWitoutSelfLink() {
		EnsureResponseHasLinks condition = new EnsureResponseHasLinks();
		run(condition);
	}
}



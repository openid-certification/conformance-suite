package net.openid.conformance.openbanking_brasil.testmodules.support.resource;

import net.openid.conformance.condition.client.AbstractEnsureHttpStatusCode;
import org.springframework.http.HttpStatus;

public class EnsureHttpStatusIs422 extends AbstractEnsureHttpStatusCode {

	@Override
	protected int getExpectedStatusCode() {
		return HttpStatus.UNPROCESSABLE_ENTITY.value();
	}
}

package net.openid.conformance;

import net.openid.conformance.fapirwid2.AbstractFAPIRWID2ServerTestModule;

public abstract class AbstractFunctionalTestModule extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void requestProtectedResource() {

		super.requestProtectedResource();
		eventLog.startBlock(currentClientString() + "Validate response");
		validateResponse();
		eventLog.endBlock();

	}

	protected abstract void validateResponse();

}

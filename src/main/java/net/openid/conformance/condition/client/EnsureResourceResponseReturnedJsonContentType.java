package net.openid.conformance.condition.client;

import java.nio.charset.Charset;

import net.openid.conformance.testmodule.Environment;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;

public class EnsureResourceResponseReturnedJsonContentType extends AbstractEnsureResourceResponseReturnedContentType {

	@Override
	protected String expectedSubtype() {
		return "json";
	}

}

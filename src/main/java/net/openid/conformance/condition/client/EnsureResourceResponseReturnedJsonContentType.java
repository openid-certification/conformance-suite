package net.openid.conformance.condition.client;

import java.nio.charset.Charset;

import net.openid.conformance.testmodule.Environment;
import org.springframework.http.InvalidMediaTypeException;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;

public class EnsureResourceResponseReturnedJsonContentType extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String contentTypeStr = env.getString("resource_endpoint_response_headers", "content-type");

		if (!Strings.isNullOrEmpty(contentTypeStr)) {
			try {
				MediaType parsedType = MediaType.parse(contentTypeStr);

				Optional<Charset> charset = parsedType.charset();
				if (charset.isPresent()) {
					String charsetName = charset.get().name();
					if (!charsetName.equals("UTF-8")) {
						throw error("Response charset is not UTF-8", args("content_type", contentTypeStr, "charset", charset.get().name()));
					}
				}

				if ("application".equals(parsedType.type()) && "json".equals(parsedType.subtype())) {
					logSuccess("Response content type is JSON", args("content_type", contentTypeStr));
					return env;
				} else {
					throw error("Response content type is not JSON", args("content_type", contentTypeStr));
				}
			} catch (IllegalArgumentException e) {
				throw error("Unable to parse content type", args("content_type", contentTypeStr));
			}
		}

		throw error("Resource endpoint did not declare a content type");
	}

}

/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.conformance.support.networknt;

import com.networknt.schema.SchemaException;
import com.networknt.schema.SpecificationVersion;
import tools.jackson.databind.JsonNode;

/**
 * Detects the JSON Schema specification version from a schema document's {@code $schema} tag.
 *
 * <p>This is a local adaptation of {@code com.networknt.schema.SpecificationVersionDetector}, which
 * networknt moved into their <em>test</em> module (see
 * <a href="https://github.com/networknt/json-schema-validator/issues/1206">networknt/json-schema-validator#1206</a>)
 * and is therefore not available on the runtime classpath of json-schema-validator 3.x.
 *
 * <p>Adapted from the upstream source: the test-only {@code Path}-based detection helpers and the
 * unused optional/non-throwing detection variant are omitted, and the
 * {@code SchemaRegistry.normalizeDialectId(String)} call ({@code protected}, so inaccessible here)
 * is replaced with the equivalent local {@link #normalizeDialectId(String)} (stripping the trailing
 * {@code '#'} fragment that draft-04/06/07 append to their {@code $schema} URIs).
 */
public final class SpecificationVersionDetector {

	private static final String SCHEMA_TAG = "$schema";

	private SpecificationVersionDetector() {
		// Prevent instantiation of this utility class
	}

	/**
	 * Detects schema version based on the schema tag: if the schema tag is not present or is not a
	 * recognized JSON Schema dialect, throws {@link SchemaException} with the corresponding message,
	 * otherwise - returns the detected spec version.
	 *
	 * @param jsonNode JSON Node to read from
	 * @return Spec version if present, otherwise throws an exception
	 */
	public static SpecificationVersion detect(JsonNode jsonNode) {
		JsonNode schemaTag = jsonNode.get(SCHEMA_TAG);
		if (schemaTag == null) {
			throw new SchemaException("'" + SCHEMA_TAG + "' tag is not present");
		}
		String schemaTagValue = schemaTag.asString();
		return SpecificationVersion.fromDialectId(normalizeDialectId(schemaTagValue))
				.orElseThrow(() -> new SchemaException("'" + schemaTagValue + "' is unrecognizable schema"));
	}

	/**
	 * Local equivalent of the {@code protected} {@code SchemaRegistry.normalizeDialectId(String)}: canonicalises a
	 * {@code $schema} URI to the <em>exact</em> dialect id that {@link SpecificationVersion#fromDialectId(String)}
	 * matches (which compares for equality). Drafts 4/6/7 keep the trailing {@code '#'}; 2019-09/2020-12 use the
	 * {@code https} form without it. Matching by the {@code /draft-XX/} (or {@code /draft/YYYY-MM/}) path segment
	 * tolerates http/https and trailing-fragment variations in the declared {@code $schema}.
	 */
	private static String normalizeDialectId(String dialectId) {
		if (dialectId == null || !dialectId.contains("://json-schema.org/draft")) {
			return dialectId;
		}
		if (dialectId.contains("/draft-04/")) {
			return "http://json-schema.org/draft-04/schema#";
		}
		if (dialectId.contains("/draft-06/")) {
			return "http://json-schema.org/draft-06/schema#";
		}
		if (dialectId.contains("/draft-07/")) {
			return "http://json-schema.org/draft-07/schema#";
		}
		if (dialectId.contains("/draft/2019-09/")) {
			return "https://json-schema.org/draft/2019-09/schema";
		}
		if (dialectId.contains("/draft/2020-12/")) {
			return "https://json-schema.org/draft/2020-12/schema";
		}
		return dialectId;
	}
}

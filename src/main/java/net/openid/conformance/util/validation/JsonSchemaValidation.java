package net.openid.conformance.util.validation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.path.PathType;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.ResourceLoader;
import net.openid.conformance.support.networknt.SpecificationVersionDetector;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class JsonSchemaValidation {

	// Jackson 3 mappers are immutable and thread-safe, so a shared default is safe
	private static final ObjectMapper DEFAULT_MAPPER = new JsonMapper();

	// networknt 3.x's default instance-path format differs from 1.5.x; the conformance suite and its
	// tests expect the classic dotted form (e.g. $.credentials[0].unexpected), i.e. PathType.LEGACY.
	private static final SchemaRegistryConfig LEGACY_PATH_CONFIG =
		SchemaRegistryConfig.builder().pathType(PathType.LEGACY).build();

	// Scheme the strictness-stripping schema id resolvers rewrite resource:/classpath: refs to; the
	// library's built-in classpath handling runs before custom resource loaders, so without the
	// rewrite the stripping loader below would never be consulted for those IRIs.
	private static final String STRIPPED_RESOURCE_SCHEME = "stripped-resource:";

	private final ObjectMapper mapper;

	private final Resource schemaResource;

	private Consumer<SchemaRegistry.Builder> schemaBuilderCustomizer;

	private boolean ignoreUnknownPropertyStrictness;

	public JsonSchemaValidation(ObjectMapper mapper, Resource schemaResource) {
		this.mapper = mapper;
		this.schemaResource = schemaResource;
	}

	public JsonSchemaValidation(Resource schemaResource) {
		this(DEFAULT_MAPPER, schemaResource);
	}

	public JsonSchemaValidation(String schemaResource) {
		this(new ClassPathResource(schemaResource));
	}

	public Consumer<SchemaRegistry.Builder> getSchemaBuilderCustomizer() {
		return schemaBuilderCustomizer;
	}

	public void setSchemaBuilderCustomizer(Consumer<SchemaRegistry.Builder> schemaBuilderCustomizer) {
		this.schemaBuilderCustomizer = schemaBuilderCustomizer;
	}

	/**
	 * When enabled, {@code "additionalProperties": false} and {@code "unevaluatedProperties": false}
	 * are removed from the schema before validating, so unknown properties do not cause validation
	 * failures. Filtering the resulting messages by type (see
	 * {@link JsonSchemaValidationResult#withoutUnknownPropertyErrors()}) is not sufficient for this:
	 * when the strict keyword sits inside a {@code oneOf}/{@code anyOf}/{@code allOf} branch, an
	 * unknown property makes the whole branch fail, and the sibling branches' errors (e.g. a
	 * {@code type} mismatch from the other branch) escape the type-based filter. Conditions that
	 * validate structure (and run with FAILURE) enable this so that unknown properties are surfaced
	 * only by the dedicated unknown-property conditions (which run with WARNING).
	 *
	 * <p>The transformation is applied to the root schema document and, via a scheme rewrite plus a
	 * custom resource loader, to any schema documents pulled in through cross-document $refs resolved
	 * from the classpath (which is what the suite's schema id resolvers translate cross-schema refs
	 * to).</p>
	 *
	 * <p>If schema transformation ever becomes unworkable, the intended replacement is
	 * classification of the validation output instead: networknt's {@code OutputFormat.HIERARCHICAL}
	 * returns a per-branch error tree that, unlike the flat message set used here, records which
	 * oneOf/anyOf branch each error belongs to, so composite failures caused solely by unknown
	 * properties can be told apart from genuine structural errors.</p>
	 */
	public void setIgnoreUnknownPropertyStrictness(boolean ignoreUnknownPropertyStrictness) {
		this.ignoreUnknownPropertyStrictness = ignoreUnknownPropertyStrictness;
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	public JsonSchemaValidationResult validate(JsonObject jsonObject) throws IOException {
		return validate(jsonObject.toString());
	}

	public JsonSchemaValidationResult validate(String jsonInput) throws IOException {

		JsonNode schemaNode = mapper.readTree(schemaResource.getInputStream());
		Consumer<SchemaRegistry.Builder> customizer = schemaBuilderCustomizer;
		if (ignoreUnknownPropertyStrictness) {
			removeUnknownPropertyStrictness(schemaNode);
			Consumer<SchemaRegistry.Builder> refStripper = builder -> {
				// Registered after the caller's customizer so e.g. the eKYC https->resource: resolver has
				// already produced the classpath form these rewrites match on.
				builder.schemaIdResolvers(resolvers -> {
					resolvers.mapPrefix("resource:", STRIPPED_RESOURCE_SCHEME);
					resolvers.mapPrefix("classpath:", STRIPPED_RESOURCE_SCHEME);
				});
				builder.resourceLoaders(loaders -> loaders.add(new StrictnessStrippingClasspathResourceLoader(mapper)));
			};
			customizer = customizer == null ? refStripper : customizer.andThen(refStripper);
		}

		SpecificationVersion specVersion = SpecificationVersionDetector.detect(schemaNode);
		SchemaRegistry registry = createRegistry(specVersion, customizer);
		// Provide the schema's resource URI as the base location so internal `#/definitions/...` $refs
		// (and any cross-document refs) resolve. Loading from a bare JsonNode gives no base IRI, which
		// silently skips ref-guarded constraints — see networknt/json-schema-validator quickstart.
		SchemaLocation baseLocation = SchemaLocation.of(schemaResource.getURI().toString());
		Schema schema = registry.getSchema(baseLocation, schemaNode);

		JsonNode inputNode = mapper.readTree(jsonInput);

		var errors = schema.validate(inputNode);

		return new JsonSchemaValidationResult(errors);
	}

	/**
	 * Builds a SchemaRegistry with the suite-wide registry configuration (legacy dotted instance
	 * paths, see {@link #LEGACY_PATH_CONFIG}) plus an optional caller-supplied customizer. All schema
	 * validation in the suite should construct its registry through this method so error paths render
	 * consistently in {@link JsonSchemaValidationResult}. Note the customizer runs last, so a
	 * customizer that calls {@code schemaRegistryConfig(...)} would replace the suite-wide config.
	 */
	public static SchemaRegistry createRegistry(SpecificationVersion specVersion, Consumer<SchemaRegistry.Builder> customizer) {
		return SchemaRegistry.withDefaultDialect(specVersion, builder -> {
			builder.schemaRegistryConfig(LEGACY_PATH_CONFIG);
			if (customizer != null) {
				customizer.accept(builder);
			}
		});
	}

	/**
	 * Purely syntactic transformation: walks the whole document rather than only schema keyword
	 * positions, which is sufficient for the schemas owned by the test suite (it would misfire on a
	 * schema declaring a property literally named "additionalProperties" with a boolean-false schema).
	 * Only removes the boolean {@code false} form; schema-valued forms constrain the values of extra
	 * properties and are kept.
	 */
	private static void removeUnknownPropertyStrictness(JsonNode node) {
		if (node instanceof ObjectNode objectNode) {
			removeIfFalse(objectNode, "additionalProperties");
			removeIfFalse(objectNode, "unevaluatedProperties");
		}
		for (JsonNode child : node) {
			removeUnknownPropertyStrictness(child);
		}
	}

	private static void removeIfFalse(ObjectNode node, String keyword) {
		JsonNode value = node.get(keyword);
		if (value != null && value.isBoolean() && !value.booleanValue()) {
			node.remove(keyword);
		}
	}

	/**
	 * Applies {@link #removeUnknownPropertyStrictness} to schema documents pulled in through
	 * cross-document $refs. Handles only {@link #STRIPPED_RESOURCE_SCHEME} IRIs — the rewritten form
	 * of the classpath IRIs the suite's schema id resolvers translate cross-schema refs to; anything
	 * else falls through to the validator library's default loaders.
	 */
	private static class StrictnessStrippingClasspathResourceLoader implements ResourceLoader {

		private final ObjectMapper mapper;

		StrictnessStrippingClasspathResourceLoader(ObjectMapper mapper) {
			this.mapper = mapper;
		}

		@Override
		public InputStreamSource getResource(AbsoluteIri absoluteIri) {
			String iri = absoluteIri.toString();
			if (!iri.startsWith(STRIPPED_RESOURCE_SCHEME)) {
				return null;
			}
			String name = iri.substring(STRIPPED_RESOURCE_SCHEME.length());
			String resourceName = name.startsWith("/") ? name.substring(1) : name;
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if (classLoader.getResource(resourceName) == null) {
				return null;
			}
			return () -> {
				try (InputStream in = classLoader.getResourceAsStream(resourceName)) {
					JsonNode node = mapper.readTree(in);
					removeUnknownPropertyStrictness(node);
					return new ByteArrayInputStream(mapper.writeValueAsBytes(node));
				}
			};
		}
	}

	/**
	 * Returns the json path to the actual problem instance
	 * @param path
	 * @param property
	 * @return
	 */
	public static String toInstancePropertyPath(NodePath path, String property) {

		String propertyPath = path.toString();
		if (property != null) {
			propertyPath += "." + property;
		}

		return propertyPath;
	}

}

package net.openid.conformance.info;

import net.openid.conformance.security.AuthenticationFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Temporary OFBR CIBA participant lookup. Delete this class and its matching unit test when it is no longer needed.
 */
@RestController
@RequestMapping("/api/plan/ofbr-ciba-participants")
public class TemporaryOfbrCibaParticipantPlanApi {

	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 500;
	private static final Pattern PARTICIPANT_MARKER = Pattern.compile("OFB-[A-Za-z0-9][A-Za-z0-9_-]*");

	private final MongoTemplate mongoTemplate;
	private final AuthenticationFacade authenticationFacade;
	private final String baseUrl;

	public TemporaryOfbrCibaParticipantPlanApi(
		MongoTemplate mongoTemplate,
		AuthenticationFacade authenticationFacade,
		@Value("${fintechlabs.base_url}") String baseUrl) {
		this.mongoTemplate = mongoTemplate;
		this.authenticationFacade = authenticationFacade;
		this.baseUrl = baseUrl;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ParticipantPlans> getParticipantPlans(
		@RequestParam(name = "limit", required = false) String limit) {

		if (!authenticationFacade.isAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		int effectiveLimit = parseLimit(limit);
		Query query = new Query(new Criteria().andOperator(
			Criteria.where("planName").in("fapi-ciba-id1-test-plan", "fapi-ciba-id1-client-test-plan"),
			new Criteria().orOperator(
				Criteria.where("variant.fapi_ciba_profile").is("openbanking_brazil"),
				Criteria.where("variant.variant.fapi_ciba_profile").is("openbanking_brazil")),
			Criteria.where("description").regex(PARTICIPANT_MARKER)))
			.with(Sort.by(Sort.Direction.DESC, "started"))
			.limit(effectiveLimit);
		query.fields().include("_id", "description", "started", "planName");

		List<ParticipantPlan> plans = mongoTemplate.find(query, Plan.class, DBTestPlanService.COLLECTION).stream()
			.map(plan -> new ParticipantPlan(
				plan.getId(),
				baseUrl + "/plan-detail.html?plan=" + plan.getId(),
				plan.getDescription(),
				plan.getStarted(),
				plan.getPlanName()))
			.toList();

		return ResponseEntity.ok(new ParticipantPlans(effectiveLimit, plans));
	}

	private static int parseLimit(String limit) {
		try {
			int parsed = Integer.parseInt(limit);
			return parsed > 0 ? Math.min(parsed, MAX_LIMIT) : DEFAULT_LIMIT;
		} catch (NumberFormatException e) {
			return DEFAULT_LIMIT;
		}
	}

	public record ParticipantPlans(int limit, List<ParticipantPlan> data) { }

	public record ParticipantPlan(String id, String planUrl, String description, String started, String planName) { }
}

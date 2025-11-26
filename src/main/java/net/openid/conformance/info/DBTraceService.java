package net.openid.conformance.info;

import net.openid.conformance.logging.DBEventLog;
import net.openid.conformance.security.AuthenticationFacade;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;

/**
 * Service for retrieving Playwright trace files from MongoDB.
 */
@Service
public class DBTraceService implements TraceService {

	private static final Logger logger = LoggerFactory.getLogger(DBTraceService.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Override
	public Optional<byte[]> getTraceForTestId(String testId, boolean assumeAdmin) {
		Criteria criteria = Criteria.where("testId").is(testId)
				.and("trace").exists(true);

		// Apply access control if not admin
		if (!assumeAdmin && !authenticationFacade.isAdmin()) {
			criteria = criteria.and("testOwner").is(authenticationFacade.getPrincipal());
		}

		Query query = Query.query(criteria);
		query.fields().include("trace");

		Document result = mongoTemplate.findOne(query, Document.class, DBEventLog.COLLECTION);

		if (result == null) {
			logger.debug("No trace found for testId: {}", testId);
			return Optional.empty();
		}

		String traceBase64 = result.getString("trace");
		if (traceBase64 == null || traceBase64.isEmpty()) {
			logger.debug("Trace field is empty for testId: {}", testId);
			return Optional.empty();
		}

		try {
			byte[] traceBytes = Base64.getDecoder().decode(traceBase64);
			logger.debug("Found trace for testId: {}, size: {} bytes", testId, traceBytes.length);
			return Optional.of(traceBytes);
		} catch (IllegalArgumentException e) {
			logger.error("Failed to decode base64 trace for testId: {}", testId, e);
			return Optional.empty();
		}
	}
}

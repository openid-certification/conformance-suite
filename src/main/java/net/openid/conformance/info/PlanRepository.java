package net.openid.conformance.info;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Map;
import java.util.Optional;

public interface PlanRepository extends PagingAndSortingRepository<Plan, String>, CrudRepository<Plan, String> {

	@Query("{ $text: { $search: ?0 } }")
	Page<Plan> findAllSearch(String search, Pageable pageable);

	@Query("{ owner: ?0 }")
	Page<Plan> findAllByOwner(Map<String, String> owner, Pageable pageable);

	@Query("{ owner: ?0, $text: { $search: ?1 } }")
	Page<Plan> findAllByOwnerSearch(Map<String, String> owner, String search, Pageable pageable);

	@Query("{ publish: { $in: [ 'summary', 'everything' ] } }")
	Page<PublicPlan> findAllPublic(Pageable pageable);

	@Query("{ publish: { $in: [ 'summary', 'everything' ] }, $text: { $search: ?0 } }")
	Page<PublicPlan> findAllPublicSearch(String search, Pageable pageable);

	@Query("{ _id: ?0, owner: ?1 }")
	Optional<Plan> findByIdAndOwner(String id, Map<String, String> owner);

	@Query("{ _id: ?0, publish: { $in: [ 'summary', 'everything' ] } }")
	Optional<PublicPlan> findByIdPublic(String id);
}

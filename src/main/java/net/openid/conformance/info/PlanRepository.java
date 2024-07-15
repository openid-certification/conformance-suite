package net.openid.conformance.info;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Map;
import java.util.Optional;

public interface PlanRepository extends PagingAndSortingRepository<Plan, String>, CrudRepository<Plan, String> {

	Slice<Plan> findAllBy(Pageable pageable);

	@Query(value = "{ $text: { $search: ?0 } }", count = true)
	Long countAllSearch(String search);

	@Query("{ $text: { $search: ?0 } }")
	Slice<Plan> findAllSearch(String search, Pageable pageable);

	@Query(value = "{ owner: ?0 }", count = true)
	Long countAllByOwner(Map<String, String> owner);

	@Query("{ owner: ?0 }")
	Slice<Plan> findAllByOwner(Map<String, String> owner, Pageable pageable);

	@Query(value = "{ owner: ?0, $text: { $search: ?1 } }", count = true)
	Long countAllByOwnerSearch(Map<String, String> owner, String search);

	@Query("{ owner: ?0, $text: { $search: ?1 } }")
	Slice<Plan> findAllByOwnerSearch(Map<String, String> owner, String search, Pageable pageable);

	@Query(value = "{ publish: { $in: [ 'summary', 'everything' ] } }", count = true)
	Long countAllPublic();

	@Query("{ publish: { $in: [ 'summary', 'everything' ] } }")
	Slice<PublicPlan> findAllPublic(Pageable pageable);

	@Query(value = "{ publish: { $in: [ 'summary', 'everything' ] }, $text: { $search: ?0 } }", count = true)
	Long countAllPublicSearch(String search);

	@Query("{ publish: { $in: [ 'summary', 'everything' ] }, $text: { $search: ?0 } }")
	Slice<PublicPlan> findAllPublicSearch(String search, Pageable pageable);

	@Query("{ _id: ?0, owner: ?1 }")
	Optional<Plan> findByIdAndOwner(String id, Map<String, String> owner);

	@Query("{ _id: ?0, publish: { $in: [ 'summary', 'everything' ] } }")
	Optional<PublicPlan> findByIdPublic(String id);
}

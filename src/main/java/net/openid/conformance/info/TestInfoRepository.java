package net.openid.conformance.info;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Map;
import java.util.Optional;

public interface TestInfoRepository extends PagingAndSortingRepository<TestInfo, String>, CrudRepository<TestInfo, String> {

	@Query("{ $text: { $search: ?0 } }")
	Page<TestInfo> findAllSearch(String search, Pageable pageable);

	@Query("{ owner: ?0 }")
	Iterable<TestInfo> findAllByOwner(Map<String, String> owner);

	@Query("{ owner: ?0 }")
	Page<TestInfo> findAllByOwner(Map<String, String> owner, Pageable pageable);

	@Query("{ owner: ?0, $text: { $search: ?1 } }")
	Page<TestInfo> findAllByOwnerSearch(Map<String, String> owner, String search, Pageable pageable);

	@Query("{ publish: { $in: [ 'summary', 'everything' ] } }")
	Page<PublicTestInfo> findAllPublic(Pageable pageable);

	@Query("{ publish: { $in: [ 'summary', 'everything' ] }, $text: { $search: ?0 } }")
	Page<PublicTestInfo> findAllPublicSearch(String search, Pageable pageable);

	@Query("{ _id: ?0, owner: ?1 }")
	Optional<TestInfo> findByIdAndOwner(String id, Map<String, String> owner);

	@Query("{ _id: ?0, publish: { $in: [ 'summary', 'everything' ] } }")
	Optional<PublicTestInfo> findByIdPublic(String id);
}

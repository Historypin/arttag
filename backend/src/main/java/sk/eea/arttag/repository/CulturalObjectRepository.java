/**
 * 
 */
package sk.eea.arttag.repository;

import java.util.List;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.eea.arttag.model.CulturalObject;

/**
 * @author Maros Strmensky
 *
 */
@Repository
public interface CulturalObjectRepository extends JpaRepository<CulturalObject, Long> {

	@Modifying
	@Query("update CulturalObject cu set cu.active=false where cu.batchId = :batchId")
	void stopEnrichingBatch(@Param("batchId") Long batchId);

	@Query(value = "select nextval('hibernate_sequence')", nativeQuery = true)
	Long getNextSequence();

    List<CulturalObject> findByBatchId(Long batchId);

    CulturalObject findTopByOrderByLastSelectedAsc();
    CulturalObject findTopByIdNotInOrderByLastSelectedAsc(List<Long> ids);
}

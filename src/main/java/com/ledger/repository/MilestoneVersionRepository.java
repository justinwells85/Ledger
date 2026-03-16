package com.ledger.repository;

import com.ledger.entity.MilestoneVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Repository for MilestoneVersion entity.
 * Provides the core time-machine version queries (BR-40, BR-41).
 * Spec: 01-domain-model.md Section 2.6, 04-milestone-versioning.md
 */
@Repository
public interface MilestoneVersionRepository extends JpaRepository<MilestoneVersion, UUID> {

    /**
     * Returns the current (highest version_number) version of a milestone.
     * BR-40: "Current version is always the highest version_number."
     */
    @Query("SELECT mv FROM MilestoneVersion mv WHERE mv.milestone.milestoneId = :milestoneId " +
           "ORDER BY mv.versionNumber DESC LIMIT 1")
    Optional<MilestoneVersion> findCurrentVersion(@Param("milestoneId") UUID milestoneId);

    /**
     * Returns the version effective on or before asOfDate (time machine query).
     * BR-41: "Time machine uses effective_date for plan queries."
     * Returns empty if no version existed yet on that date.
     */
    @Query("SELECT mv FROM MilestoneVersion mv WHERE mv.milestone.milestoneId = :milestoneId " +
           "AND mv.effectiveDate <= :asOfDate ORDER BY mv.versionNumber DESC LIMIT 1")
    Optional<MilestoneVersion> findVersionAsOfDate(@Param("milestoneId") UUID milestoneId,
                                                   @Param("asOfDate") LocalDate asOfDate);

    /**
     * Returns all versions for a milestone, ordered by version_number ascending.
     */
    @Query("SELECT mv FROM MilestoneVersion mv WHERE mv.milestone.milestoneId = :milestoneId " +
           "ORDER BY mv.versionNumber ASC")
    List<MilestoneVersion> findVersionHistory(@Param("milestoneId") UUID milestoneId);

    /**
     * Returns the current (latest) version for ALL milestones in a single query.
     * Eliminates the N+1 problem in ReportService and ReconciliationService.
     */
    @Query("SELECT mv FROM MilestoneVersion mv WHERE mv.versionNumber = " +
           "(SELECT MAX(mv2.versionNumber) FROM MilestoneVersion mv2 " +
           " WHERE mv2.milestone = mv.milestone)")
    List<MilestoneVersion> findAllCurrentVersions();

    /**
     * Returns the version effective on or before asOfDate for ALL milestones in a single query.
     */
    @Query("SELECT mv FROM MilestoneVersion mv WHERE mv.versionNumber = " +
           "(SELECT MAX(mv2.versionNumber) FROM MilestoneVersion mv2 " +
           " WHERE mv2.milestone = mv.milestone AND mv2.effectiveDate <= :asOfDate)")
    List<MilestoneVersion> findAllCurrentVersionsAsOf(@Param("asOfDate") LocalDate asOfDate);
}

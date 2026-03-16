package com.ledger.repository;

import com.ledger.entity.RefFundingSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefFundingSourceRepository extends JpaRepository<RefFundingSource, String> {
    List<RefFundingSource> findAllByOrderBySortOrderAsc();
}

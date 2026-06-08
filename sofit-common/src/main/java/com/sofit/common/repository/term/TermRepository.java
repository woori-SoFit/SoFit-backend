package com.sofit.common.repository.term;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.term.enums.TermType;

public interface TermRepository extends JpaRepository<Term, Long> {

    List<Term> findByTermTypeAndIsActiveTrue(TermType termType);

    List<Term> findAllByTermIdInAndIsActiveTrue(List<Long> termIds);
}

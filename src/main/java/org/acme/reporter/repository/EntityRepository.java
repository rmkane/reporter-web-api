package org.acme.reporter.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EntityRepository<T, ID> {

  Optional<T> findById(ID id);

  Page<T> findAll(Pageable pageable);

  T save(T entity);

  void deleteById(ID id);

  long count();
}

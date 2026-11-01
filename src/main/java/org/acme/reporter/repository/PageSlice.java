package org.acme.reporter.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/** Pages an in-memory list. Callers decide the order; this only sorts and cuts. */
@UtilityClass
public class PageSlice {

  public <T> Page<T> slice(List<T> items, Pageable pageable, Comparator<T> order) {
    List<T> sorted = new ArrayList<>(items);
    sorted.sort(order);
    if (pageable.isUnpaged()) {
      return new PageImpl<>(List.copyOf(sorted));
    }
    int start = (int) Math.min(pageable.getOffset(), sorted.size());
    int end = Math.min(start + pageable.getPageSize(), sorted.size());
    return new PageImpl<>(List.copyOf(sorted.subList(start, end)), pageable, sorted.size());
  }
}

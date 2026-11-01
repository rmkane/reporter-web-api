package org.acme.reporter.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class PageSliceTest {

  private final List<String> items = List.of("delta", "alpha", "echo", "charlie", "bravo");

  @Test
  void firstPageIsSortedAndCut() {
    Page<String> page = PageSlice.slice(items, PageRequest.of(0, 2), Comparator.naturalOrder());

    assertThat(page.getContent()).containsExactly("alpha", "bravo");
    assertThat(page.getTotalElements()).isEqualTo(5);
    assertThat(page.getTotalPages()).isEqualTo(3);
    assertThat(page.getNumber()).isZero();
  }

  @Test
  void laterPagesKeepTheOrder() {
    Page<String> page =
        PageSlice.slice(items, PageRequest.of(1, 2), Comparator.<String>naturalOrder().reversed());

    assertThat(page.getContent()).containsExactly("charlie", "bravo");
  }

  @Test
  void lastPageMayBeShort() {
    Page<String> page = PageSlice.slice(items, PageRequest.of(2, 2), Comparator.naturalOrder());

    assertThat(page.getContent()).containsExactly("echo");
    assertThat(page.isLast()).isTrue();
  }

  @Test
  void unpagedReturnsEverythingSorted() {
    Page<String> page = PageSlice.slice(items, Pageable.unpaged(), Comparator.naturalOrder());

    assertThat(page.getContent()).containsExactly("alpha", "bravo", "charlie", "delta", "echo");
    assertThat(page.getTotalElements()).isEqualTo(5);
  }

  @Test
  void offsetPastTheEndIsEmptyButKeepsTheTotal() {
    Page<String> page = PageSlice.slice(items, PageRequest.of(9, 10), Comparator.naturalOrder());

    assertThat(page.getContent()).isEmpty();
    assertThat(page.getTotalElements()).isEqualTo(5);
  }

  @Test
  void theInputListIsNotModified() {
    List<String> input = new ArrayList<>(items);

    PageSlice.slice(input, PageRequest.of(0, 2), Comparator.naturalOrder());

    assertThat(input).containsExactlyElementsOf(items);
  }
}

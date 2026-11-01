package org.acme.reporter.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.acme.reporter.error.InvalidReportQueryException;
import org.acme.reporter.error.ReportNotFoundException;
import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;
import org.acme.reporter.service.ReportService;

@WebMvcTest(ReportController.class)
@Import({ApiExceptionHandler.class, DataWebAutoConfiguration.class})
class ReportControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReportService reports;

  @Test
  void listReturnsAPage() throws Exception {
    when(reports.list(any()))
        .thenReturn(new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1));

    mockMvc
        .perform(
            get("/api/reports")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "updatedOn,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value("RPT-1"));
  }

  @Test
  void searchReturnsAPage() throws Exception {
    when(reports.search(any(), any()))
        .thenReturn(new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1));

    mockMvc
        .perform(
            post("/api/reports/search")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "title,asc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"type":"clause","field":"title","operator":"=","value":"Login"}
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Login"));
  }

  @Test
  void blankTitleIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"title":" ","description":"Body","author":"john.doe"}
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }

  @Test
  void missingReportIsNotFound() throws Exception {
    when(reports.get("missing")).thenThrow(new ReportNotFoundException("missing"));

    mockMvc
        .perform(get("/api/reports/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Report missing was not found"));
  }

  @Test
  void deletingAMissingReportIsNotFound() throws Exception {
    org.mockito.Mockito.doThrow(new ReportNotFoundException("missing"))
        .when(reports)
        .delete("missing");

    mockMvc.perform(delete("/api/reports/missing")).andExpect(status().isNotFound());
  }

  @Test
  void blankUpdateFieldIsRejected() throws Exception {
    mockMvc
        .perform(
            patch("/api/reports/RPT-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"  ","priority":"HIGH"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("title"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
  }

  @Test
  void nullUpdateFieldsAreAllowed() throws Exception {
    when(reports.update(eq("RPT-1"), any())).thenReturn(sample());

    mockMvc
        .perform(
            patch("/api/reports/RPT-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":null,"priority":"HIGH"}
                    """))
        .andExpect(status().isOk());
  }

  @Test
  void malformedBodyIsABadRequest() throws Exception {
    mockMvc
        .perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Request body could not be read"));
  }

  @Test
  void invalidQueryIsABadRequest() throws Exception {
    when(reports.search(any(), any()))
        .thenThrow(new InvalidReportQueryException("Unknown priority: URGENT"));

    mockMvc
        .perform(
            post("/api/reports/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"clause","field":"priority","operator":"=","value":"URGENT"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Unknown priority: URGENT"));
  }

  @Test
  void unexpectedErrorsHideTheirDetails() throws Exception {
    when(reports.get("RPT-1")).thenThrow(new IllegalStateException("password=hunter2"));

    mockMvc
        .perform(get("/api/reports/RPT-1"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("Unexpected error"))
        .andExpect(content().string(not(containsString("hunter2"))));
  }

  private static Report sample() {
    Instant now = Instant.parse("2026-05-01T00:00:00Z");
    return new Report(
        "RPT-1",
        "Login",
        "Body",
        ReportStatus.OPEN,
        List.of("login"),
        "john.doe",
        "alex.kim",
        Priority.HIGH,
        ReportType.BUG,
        now,
        now);
  }
}

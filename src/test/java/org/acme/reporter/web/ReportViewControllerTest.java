package org.acme.reporter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.acme.reporter.config.ReporterProperties;
import org.acme.reporter.config.SwaggerUiConfig;
import org.acme.reporter.error.ReportNotFoundException;
import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.model.ReportType;
import org.acme.reporter.service.ReportService;

@WebMvcTest(ReportViewController.class)
class ReportViewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReportService reports;

  @MockitoBean private ReporterProperties properties;

  @MockitoBean private SwaggerUiConfig swagger;

  @Test
  void dashboardCountsStatusesAndOrdersTasks() throws Exception {
    when(reports.list(any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    report("RPT-1", ReportStatus.OPEN, Priority.LOW, "2026-01-05T00:00:00Z"),
                    report(
                        "RPT-2",
                        ReportStatus.IN_PROGRESS,
                        Priority.CRITICAL,
                        "2026-01-01T00:00:00Z"),
                    report("RPT-3", ReportStatus.OPEN, Priority.HIGH, "2026-01-02T00:00:00Z"),
                    report("RPT-4", ReportStatus.OPEN, Priority.HIGH, "2026-01-03T00:00:00Z"),
                    report("RPT-5", ReportStatus.OPEN, null, "2026-01-09T00:00:00Z"),
                    report(
                        "RPT-6", ReportStatus.RESOLVED, Priority.CRITICAL, "2026-01-01T00:00:00Z"),
                    report("RPT-7", ReportStatus.CLOSED, Priority.HIGH, "2026-01-01T00:00:00Z"))));

    MvcResult result =
        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andReturn();

    Dashboard dashboard = (Dashboard) result.getModelAndView().getModel().get("dashboard");
    assertThat(dashboard.open()).isEqualTo(4);
    assertThat(dashboard.inProgress()).isEqualTo(1);
    assertThat(dashboard.resolved()).isEqualTo(1);
    assertThat(dashboard.closed()).isEqualTo(1);
    // Highest priority first, newest first within a priority, no priority last.
    assertThat(dashboard.tasks())
        .extracting(Report::id)
        .containsExactly("RPT-2", "RPT-4", "RPT-3", "RPT-1", "RPT-5");
  }

  @Test
  void reportsPageRenders() throws Exception {
    mockMvc
        .perform(get("/reports"))
        .andExpect(status().isOk())
        .andExpect(view().name("reports"))
        .andExpect(
            model()
                .attribute(
                    "crumbs", List.of(new Crumb("Dashboard", "/"), new Crumb("Reports", null))));
  }

  @Test
  void reportPageShowsTheReport() throws Exception {
    Report report = report("RPT-1", ReportStatus.OPEN, Priority.HIGH, "2026-01-01T00:00:00Z");
    when(reports.get("RPT-1")).thenReturn(report);

    mockMvc
        .perform(get("/reports/RPT-1"))
        .andExpect(status().isOk())
        .andExpect(view().name("report"))
        .andExpect(model().attribute("report", report))
        // The raw instant stays in the page for the browser to format.
        .andExpect(content().string(containsString("<time datetime=\"2026-01-01T00:00:00Z\">")))
        .andExpect(
            model()
                .attribute(
                    "crumbs",
                    List.of(
                        new Crumb("Dashboard", "/"),
                        new Crumb("Reports", "/reports"),
                        new Crumb("RPT-1", null))));
  }

  @Test
  void editPageIsPrefilled() throws Exception {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    when(reports.get("RPT-1"))
        .thenReturn(
            new Report(
                "RPT-1",
                "Login",
                "Body",
                ReportStatus.IN_PROGRESS,
                List.of("auth", "login"),
                "john.doe",
                "sam.patel",
                Priority.HIGH,
                ReportType.BUG,
                now,
                now));

    mockMvc
        .perform(get("/reports/RPT-1/edit"))
        .andExpect(status().isOk())
        .andExpect(view().name("edit"))
        .andExpect(
            model()
                .attribute(
                    "crumbs",
                    List.of(
                        new Crumb("Dashboard", "/"),
                        new Crumb("Reports", "/reports"),
                        new Crumb("RPT-1", "/reports/RPT-1"),
                        new Crumb("Edit", null))))
        .andExpect(
            content()
                .string(
                    allOf(
                        containsString("<option value=\"BUG\" selected=\"selected\">Bug</option>"),
                        containsString(
                            "<option value=\"IN_PROGRESS\" selected=\"selected\">In Progress</option>"),
                        containsString(
                            "<option value=\"HIGH\" selected=\"selected\">High</option>"),
                        containsString("value=\"auth, login\""),
                        containsString("value=\"sam.patel\" required=\"required\""),
                        // Set fields cannot be cleared, so there is no None option.
                        not(containsString(">None</option>")))));
  }

  @Test
  void editPageOffersNoneForEmptyFields() throws Exception {
    when(reports.get("RPT-1"))
        .thenReturn(report("RPT-1", ReportStatus.OPEN, null, "2026-01-01T00:00:00Z"));

    mockMvc
        .perform(get("/reports/RPT-1/edit"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">None</option>")))
        .andExpect(
            content().string(not(containsString("required=\"required\" placeholder=\"optional\""))))
        .andExpect(
            content()
                .string(
                    not(containsString("name=\"assignee\" placeholder=\"optional\" required"))));
  }

  @Test
  void editingAnUnknownReportShowsTheErrorPage() throws Exception {
    when(reports.get("RPT-404")).thenThrow(new ReportNotFoundException("RPT-404"));

    mockMvc
        .perform(get("/reports/RPT-404/edit"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("error"));
  }

  @Test
  void unknownReportShowsTheErrorPage() throws Exception {
    when(reports.get("RPT-404")).thenThrow(new ReportNotFoundException("RPT-404"));

    mockMvc
        .perform(get("/reports/RPT-404"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("error"));
  }

  @Test
  void unknownPathIsHtmlForBrowsers() throws Exception {
    mockMvc
        .perform(get("/nope").accept(MediaType.TEXT_HTML))
        .andExpect(status().isNotFound())
        .andExpect(view().name("error"));
  }

  @Test
  void unknownPathIsJsonForApiClients() throws Exception {
    mockMvc
        .perform(get("/nope").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404));
  }

  private static Report report(String id, ReportStatus status, Priority priority, String updated) {
    Instant updatedOn = Instant.parse(updated);
    return new Report(
        id,
        "Title " + id,
        "Body",
        status,
        List.of(),
        "john.doe",
        null,
        priority,
        ReportType.BUG,
        updatedOn,
        updatedOn);
  }
}

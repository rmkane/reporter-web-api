package org.acme.reporter.web;

import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.acme.reporter.model.Priority;
import org.acme.reporter.model.Report;
import org.acme.reporter.model.ReportStatus;
import org.acme.reporter.service.ReportService;

@Controller
@RequiredArgsConstructor
public class ReportViewController {

  private final ReportService reports;

  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("dashboard", dashboard());
    return "index";
  }

  @GetMapping("/reports")
  public String reports(Model model) {
    model.addAttribute("crumbs", trail(new Crumb("Reports", "/reports")));
    return "reports";
  }

  @GetMapping("/reports/{id}")
  public String report(@PathVariable String id, Model model) {
    Report report = reports.get(id);
    model.addAttribute("report", report);
    model.addAttribute(
        "crumbs", trail(new Crumb("Reports", "/reports"), new Crumb(report.id(), null)));
    return "report";
  }

  private static List<Crumb> trail(Crumb... pages) {
    Crumb[] crumbs = new Crumb[pages.length + 1];
    crumbs[0] = new Crumb("Dashboard", "/");
    System.arraycopy(pages, 0, crumbs, 1, pages.length);
    int last = crumbs.length - 1;
    crumbs[last] = new Crumb(crumbs[last].label(), null);
    return List.of(crumbs);
  }

  private Dashboard dashboard() {
    List<Report> reports = this.reports.list(Pageable.unpaged()).getContent();
    List<Report> tasks =
        reports.stream()
            .filter(ReportViewController::needsAttention)
            .sorted(
                Comparator.comparingInt(ReportViewController::priorityRank)
                    .thenComparing(
                        Report::updatedOn, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    return new Dashboard(
        tasks,
        count(reports, ReportStatus.OPEN),
        count(reports, ReportStatus.IN_PROGRESS),
        count(reports, ReportStatus.RESOLVED),
        count(reports, ReportStatus.CLOSED));
  }

  private static boolean needsAttention(Report report) {
    return report.status() == ReportStatus.OPEN || report.status() == ReportStatus.IN_PROGRESS;
  }

  private static int priorityRank(Report report) {
    Priority priority = report.priority();
    if (priority == null) {
      return Priority.values().length;
    }
    return Priority.values().length - 1 - priority.ordinal();
  }

  private static long count(List<Report> reports, ReportStatus status) {
    return reports.stream().filter(report -> report.status() == status).count();
  }
}

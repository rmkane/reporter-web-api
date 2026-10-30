package org.acme.reporter.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.acme.reporter.service.ReportService;

@Controller
public class ReportViewController {

  private final ReportService reports;

  public ReportViewController(ReportService reports) {
    this.reports = reports;
  }

  @GetMapping("/")
  public String home() {
    return "index";
  }

  @GetMapping("/reports")
  public String reports() {
    return "reports";
  }

  @GetMapping("/reports/{id}")
  public String report(@PathVariable String id, Model model) {
    model.addAttribute("report", reports.get(id));
    return "report";
  }
}

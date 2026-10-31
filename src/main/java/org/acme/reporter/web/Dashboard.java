package org.acme.reporter.web;

import java.util.List;

import org.acme.reporter.model.Report;

public record Dashboard(
    List<Report> tasks, long open, long inProgress, long resolved, long closed) {}

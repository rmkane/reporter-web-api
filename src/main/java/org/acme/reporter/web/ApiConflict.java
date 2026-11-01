package org.acme.reporter.web;

import org.acme.reporter.model.Report;

/** A 409 body: the report as it is now, so the client can reconcile its edit. */
public record ApiConflict(int status, String message, Report current) {}

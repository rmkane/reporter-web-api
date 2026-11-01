package org.acme.reporter.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.acme.reporter.error.ReportNotFoundException;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class PageErrors {

  @ExceptionHandler(NoResourceFoundException.class)
  public Object missingPage(NoResourceFoundException exception, HttpServletRequest request) {
    if (acceptsHtml(request)) {
      return errorPage();
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(HttpStatus.NOT_FOUND.value(), exception.getBody().getDetail()));
  }

  @ExceptionHandler(ReportNotFoundException.class)
  public ModelAndView missingReport() {
    return errorPage();
  }

  private static ModelAndView errorPage() {
    ModelAndView model = new ModelAndView("error");
    model.setStatus(HttpStatus.NOT_FOUND);
    model.addObject("status", HttpStatus.NOT_FOUND.value());
    model.addObject("error", HttpStatus.NOT_FOUND.getReasonPhrase());
    return model;
  }

  private static boolean acceptsHtml(HttpServletRequest request) {
    String accept = request.getHeader(HttpHeaders.ACCEPT);
    return accept != null && accept.contains("text/html");
  }
}

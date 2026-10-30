package org.acme.reporter.query;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.JsonNode;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ReportQuery.And.class, name = "and"),
  @JsonSubTypes.Type(value = ReportQuery.Or.class, name = "or"),
  @JsonSubTypes.Type(value = ReportQuery.Not.class, name = "not"),
  @JsonSubTypes.Type(value = ReportQuery.Clause.class, name = "clause")
})
public sealed interface ReportQuery
    permits ReportQuery.And, ReportQuery.Or, ReportQuery.Not, ReportQuery.Clause {

  record And(List<ReportQuery> children) implements ReportQuery {}

  record Or(List<ReportQuery> children) implements ReportQuery {}

  record Not(ReportQuery child) implements ReportQuery {}

  record Clause(String field, String operator, JsonNode value) implements ReportQuery {}
}

package no.einnsyn.backend.common.search.parser;

import java.util.List;

/** Base interface for query AST nodes. */
public interface QueryNode {

  /** Terminal node representing a word or quoted phrase. */
  record TermNode(String value, boolean isPhrase) implements QueryNode {}

  /** Binary node representing AND operation (all children must match). */
  record AndNode(List<QueryNode> children) implements QueryNode {}

  /** Binary node representing OR operation (at least one child must match). */
  record OrNode(List<QueryNode> children) implements QueryNode {}

  /** Unary node representing NOT operation (child must not match). */
  record NotNode(QueryNode child) implements QueryNode {}
}

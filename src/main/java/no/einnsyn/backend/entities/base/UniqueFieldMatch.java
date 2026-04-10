package no.einnsyn.backend.entities.base;

/**
 * Represents a match on one of the unique-identifying fields used to look up an existing object.
 *
 * @param field The unique field that matched
 * @param object The matching object
 */
public record UniqueFieldMatch<O>(String field, O object) {}

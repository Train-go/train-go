package com.voyagego.traingo.model.dto;

/**
 * One row of a "count per owner" query: the owner's id and how many rows
 * belong to it. Spring Data fills it from the aliases {@code id} and
 * {@code total} of a JPQL select.
 */
public interface IdCount {

	Long getId();

	long getTotal();

}

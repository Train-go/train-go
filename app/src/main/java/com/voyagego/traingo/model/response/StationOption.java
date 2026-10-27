package com.voyagego.traingo.model.response;

/**
 * A station in the customer's search forms: the code is the option's value
 * and goes into the /trips link, the name is what the option shows.
 */
public record StationOption(String code, String name) {
}

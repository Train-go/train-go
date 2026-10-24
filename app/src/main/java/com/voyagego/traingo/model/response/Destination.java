package com.voyagego.traingo.model.response;

/**
 * A card in the homepage's "Đi đâu cũng có tàu" block. {@code stationName}
 * is the value of the search form's station options, so picking the card can
 * fill the form; {@code place} is the name a traveller knows, "Nha Trang"
 * rather than "Ga Nha Trang".
 */
public record Destination(String stationName, String place, StationPhoto photo) {
}

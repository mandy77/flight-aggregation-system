package com.ota.flights.infrastructure.provider;

import com.ota.flights.domain.model.CabinClass;
import com.ota.flights.domain.model.FlightSegment;
import com.ota.flights.domain.model.Itinerary;
import com.ota.flights.domain.port.FlightProviderPort;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock-Beta: deeply nested XML structures.
 * Latency 300–2500 ms, ~10% failure rate.
 */
public class BetaProvider implements FlightProviderPort {

    private final int volume;

    public BetaProvider(int volume) {
        this.volume = volume;
    }

    @Override
    public String getName() {
        return "Mock-Beta";
    }

    @Override
    public List<Itinerary> search(String origin, String destination, String departureDate) {
        ProviderSupport.simulateLatency(300, 2500);
        ProviderSupport.maybeFail(0.10);

        List<Itinerary> raw = ProviderSupport.generateItineraries(
                volume, origin, destination, departureDate, getName());
        String xml = toXml(raw);
        return fromXml(xml);
    }

    private String toXml(List<Itinerary> itineraries) {
        StringBuilder sb = new StringBuilder();
        sb.append("<FlightSearchResponse><Results>");
        for (Itinerary itinerary : itineraries) {
            sb.append("<Offer>");
            sb.append("<OfferId>").append(itinerary.getId()).append("</OfferId>");
            sb.append("<Source>").append(itinerary.getProvider()).append("</Source>");
            sb.append("<Price><Amount>").append(itinerary.getBasePrice().toPlainString())
                    .append("</Amount><Currency>").append(itinerary.getCurrency()).append("</Currency></Price>");
            sb.append("<Cabin>").append(itinerary.getCabinClass().name().toLowerCase()).append("</Cabin>");
            sb.append("<Availability>").append(itinerary.getSeatsAvailable()).append("</Availability>");
            sb.append("<Journey>");
            for (FlightSegment s : itinerary.getSegments()) {
                sb.append("<Leg>");
                sb.append("<MarketingCarrier><Code>").append(s.getAirlineCode())
                        .append("</Code><FlightNumber>").append(s.getFlightNumber())
                        .append("</FlightNumber></MarketingCarrier>");
                sb.append("<Departure><Airport>").append(s.getOrigin())
                        .append("</Airport><DateTime>").append(s.getDepartureTime())
                        .append("</DateTime></Departure>");
                sb.append("<Arrival><Airport>").append(s.getDestination())
                        .append("</Airport><DateTime>").append(s.getArrivalTime())
                        .append("</DateTime></Arrival>");
                sb.append("<DurationMinutes>").append(s.getDurationMinutes()).append("</DurationMinutes>");
                if (s.getAircraftType() != null) {
                    sb.append("<Equipment>").append(s.getAircraftType()).append("</Equipment>");
                }
                sb.append("</Leg>");
            }
            sb.append("</Journey></Offer>");
        }
        sb.append("</Results></FlightSearchResponse>");
        return sb.toString();
    }

    private List<Itinerary> fromXml(String xml) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            List<Itinerary> result = new ArrayList<>();
            NodeList offers = doc.getElementsByTagName("Offer");
            for (int i = 0; i < offers.getLength(); i++) {
                Element offer = (Element) offers.item(i);
                String id = text(offer, "OfferId");
                String provider = text(offer, "Source");
                BigDecimal amount = new BigDecimal(text(offer, "Amount"));
                String currency = text(offer, "Currency");
                CabinClass cabin = CabinClass.from(text(offer, "Cabin"));
                int seats = Integer.parseInt(text(offer, "Availability"));

                List<FlightSegment> segments = new ArrayList<>();
                NodeList legs = offer.getElementsByTagName("Leg");
                for (int j = 0; j < legs.getLength(); j++) {
                    Element leg = (Element) legs.item(j);
                    segments.add(new FlightSegment(
                            text(leg, "Code"),
                            text(leg, "FlightNumber"),
                            firstAirport(leg, "Departure"),
                            firstAirport(leg, "Arrival"),
                            LocalDateTime.parse(firstDateTime(leg, "Departure")),
                            LocalDateTime.parse(firstDateTime(leg, "Arrival")),
                            Integer.parseInt(text(leg, "DurationMinutes")),
                            textOrNull(leg, "Equipment")
                    ));
                }
                result.add(new Itinerary(id, segments, amount, currency, provider, cabin, seats));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Beta XML parse error", e);
        }
    }

    private static String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent();
    }

    private static String textOrNull(Element parent, String tag) {
        String t = text(parent, tag);
        return t.isEmpty() ? null : t;
    }

    private static String firstAirport(Element leg, String section) {
        NodeList sections = leg.getElementsByTagName(section);
        if (sections.getLength() == 0) return "XXX";
        return text((Element) sections.item(0), "Airport");
    }

    private static String firstDateTime(Element leg, String section) {
        NodeList sections = leg.getElementsByTagName(section);
        if (sections.getLength() == 0) return LocalDateTime.now().toString();
        return text((Element) sections.item(0), "DateTime");
    }
}

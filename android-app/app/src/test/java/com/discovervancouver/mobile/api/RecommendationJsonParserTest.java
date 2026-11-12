package com.discovervancouver.mobile.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import com.discovervancouver.mobile.model.Recommendation;

import org.json.JSONException;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RecommendationJsonParserTest {

    private final RecommendationJsonParser parser =
        new RecommendationJsonParser();

    @Test
    public void parsesBackendRecommendationContract() throws Exception {
        List<Recommendation> results = parser.parse(validResponse());

        assertEquals(1, results.size());
        assertEquals("Burrard Coffee", results.get(0).getName());
        assertEquals(5, results.get(0).getWalkingMinutes());
        assertFalse(results.get(0).getReasons().isEmpty());
    }

    @Test
    public void rejectsMalformedResponse() {
        assertThrows(
            JSONException.class,
            () -> parser.parse("{not-an-array}")
        );
    }

    @Test
    public void parsesEndToEndResponseWhenProvided() throws Exception {
        String responseFile = System.getProperty("e2e.response.file", "");
        String response;
        if (responseFile.isBlank()) {
            response = validResponse();
        } else {
            response = Files.readString(Path.of(responseFile));
        }

        assertFalse(parser.parse(response).isEmpty());
    }

    private String validResponse() {
        return """
            [{
              "placeId":"place-1",
              "name":"Burrard Coffee",
              "category":"cafe",
              "description":"A quiet downtown cafe",
              "address":"100 Burrard Street",
              "priceLevel":1,
              "indoor":true,
              "openNow":true,
              "tags":["quiet"],
              "score":91,
              "distanceMeters":350,
              "walkingMinutes":5,
              "latitude":49.2827,
              "longitude":-123.1207,
              "rainyWeather":false,
              "reasons":["Matches your cafe preference"]
            }]
            """;
    }
}

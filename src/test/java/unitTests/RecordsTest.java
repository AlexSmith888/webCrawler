package unitTests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.database.DbQueueItem;
import server.raw.RawQueueItem;
import server.transformation.TransformQueueItem;

import static org.junit.jupiter.api.Assertions.*;

public class RecordsTest {

    @Test
    void testTransformQueueItem() {
        TransformQueueItem item = new TransformQueueItem("http://example.com", "<html>...</html>");
        assertEquals("http://example.com", item.link());
        assertEquals("<html>...</html>", item.body());
    }

    @Test
    void testRawQueueItem() {
        RawQueueItem raw = new RawQueueItem("http://example.com", "http://example.com", 2);
        assertEquals("http://example.com", raw.message());
        assertEquals(2, raw.level());
    }
    @Test
    void testDbQueueItemWithJsonNode() throws Exception {
        String expectedLink = "http://example.com";
        String jsonString = "{\"json\":\"value\"}";

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expectedJson = objectMapper.readTree(jsonString);

        DbQueueItem db = new DbQueueItem(expectedLink, expectedJson);

        assertEquals(expectedLink, db.link(), "Link should match the input");
        assertEquals(expectedJson, db.json(), "JsonNode should match the input");
    }
}

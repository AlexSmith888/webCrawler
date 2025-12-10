package server.database;
import com.fasterxml.jackson.databind.JsonNode;

public record DbQueueItem(String link, JsonNode json) {}


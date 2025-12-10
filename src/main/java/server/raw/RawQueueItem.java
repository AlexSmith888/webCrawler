package server.raw;

public record RawQueueItem(String parent, String message, Integer level) {}

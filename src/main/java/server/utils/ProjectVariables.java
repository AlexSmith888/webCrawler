package server.utils;

public enum ProjectVariables {
    DB_HOST("DB_HOST"),
    DB_PORT("DB_PORT"),
    DB_NAME("DB_NAME"),
    COLLECTION("COLLECTION");

    public final String label;
    private ProjectVariables(String label) {
        this.label = label;
    }
}
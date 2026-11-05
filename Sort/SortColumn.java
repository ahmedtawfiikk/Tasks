// Every tasks column a sort may use in SQL. ORDER BY is built only from these fixed texts,
// never from user input or free text, so a sort can't inject SQL.
// (A PreparedStatement parameter can't be a column name, so ORDER BY can't use "?".)
enum SortColumn {
    ID("t.id"),
    TITLE("t.title"),
    STATUS("t.status"),
    TYPE("t.type"),
    SEVERITY("t.severity"),
    ESTIMATE_HOURS("t.estimate_hours"),
    DUE_DATE("t.due_date"),
    CREATED_AT("t.created_at"),
    COMPLETED_AT("t.completed_at");

    private final String sql;

    SortColumn(String sql) {
        this.sql = sql;
    }

    String sql() {
        return sql;
    }
}

import java.math.BigDecimal;

// One row of report 1: how far a project is.
record ProjectProgress(String projectKey, String projectName, long total, long done,
                       long inProgress, long pending, BigDecimal percentDone) {
}

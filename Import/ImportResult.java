import java.util.List;

// What the CSV import did: how many tasks it saved, or, if any row was bad,
// every bad row with its line number and reason (and then nothing was saved).
record ImportResult(int importedCount, List<String> errors) {
}

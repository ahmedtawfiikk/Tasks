// One data line of the CSV file, split into its columns.
// lineNumber is the line in the file (the header is line 1), so errors can point at it.
// columnCount is how many columns the line really had; a missing column is "".
record CsvRow(int lineNumber, int columnCount,
              String type, String title, String description, String severity,
              String estimateHours, String assigneeEmail, String dueDate, String tags) {
}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Reads the CSV file into rows. It only reads and splits; CsvValidator checks the values.
// The columns are:
//   type,title,description,severity,estimate_hours,assignee_email,due_date,tags
// The first line is the header and is skipped. A value can't contain a comma.
class CsvReader {

    static final int COLUMNS = 8;

    List<CsvRow> read(String filePath) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(filePath));
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't read the file " + filePath);
        }

        List<CsvRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {   // i = 0 is the header
            // -1 keeps empty columns at the end, like the empty tags in "bug,Title,,low,,,,"
            String[] parts = lines.get(i).split(",", -1);
            String[] values = new String[COLUMNS];
            for (int col = 0; col < COLUMNS; col++) {
                values[col] = col < parts.length ? parts[col].trim() : "";
            }
            rows.add(new CsvRow(i + 1, parts.length,
                                values[0], values[1], values[2], values[3],
                                values[4], values[5], values[6], values[7]));
        }
        return rows;
    }
}

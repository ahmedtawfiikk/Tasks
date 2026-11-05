// Checks the in-memory repositories share. In PostgreSQL the database does these
// (CHECK constraints and foreign keys); in memory, the repositories play the database,
// so they check the same rules by hand. Each failed check throws IllegalArgumentException.
class InMemoryChecks {

    // like CHECK (btrim(column) <> '')
    static void notBlank(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(what + " can't be empty");
        }
    }

    // like a foreign key to users (id)
    static void userExists(UserRepository users, long userId) {
        if (users.findById(userId) == null) {
            throw new IllegalArgumentException("No user with id " + userId);
        }
    }
}

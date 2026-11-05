import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

// Keeps users in memory. Used when STORAGE=memory.
// Checks the same rules as the users table: name and email not empty, email unique ignoring case.
class InMemoryUserRepository implements UserRepository {

    // id -> user. HashMap gives findById in O(1).
    private final Map<Long, User> users = new HashMap<>();

    // the next id to hand out, like the database's identity column
    private long nextId = 1;

    @Override
    public User create(String fullName, String email) {
        InMemoryChecks.notBlank(fullName, "Full name");
        InMemoryChecks.notBlank(email, "Email");
        for (User user : users.values()) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                throw new IllegalArgumentException("Email " + email + " is already used");
            }
        }

        User user = new User(nextId++, fullName, email, OffsetDateTime.now());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User findById(long id) {
        return users.get(id);   // null if not found
    }
}

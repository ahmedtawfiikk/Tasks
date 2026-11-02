import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

// Keeps users in memory. Used when STORAGE=memory.
class InMemoryUserRepository implements UserRepository {

    // id -> user. HashMap gives findById in O(1).
    private final Map<Long, User> users = new HashMap<>();

    // the next id to hand out, like the database's identity column
    private long nextId = 1;

    @Override
    public User create(String fullName, String email) {
        User user = new User(nextId++, fullName, email, OffsetDateTime.now());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User findById(long id) {
        return users.get(id);   // null if not found
    }
}

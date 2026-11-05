import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keeps projects and their members in memory. Used when STORAGE=memory.
// Checks the same rules as the projects and project_members tables.
class InMemoryProjectRepository implements ProjectRepository {

    // id -> project
    private final Map<Long, Project> projects = new HashMap<>();

    // project id -> its members
    private final Map<Long, List<ProjectMember>> members = new HashMap<>();

    // owners and members must be real users, like the foreign key to users
    private final UserRepository users;

    private long nextId = 1;

    InMemoryProjectRepository(UserRepository users) {
        this.users = users;
    }

    @Override
    public Project create(String name, String key, long ownerId) {
        // the same rules as the projects table
        InMemoryChecks.notBlank(name, "Project name");
        if (key == null || !key.matches("[A-Z]{2,5}")) {
            throw new IllegalArgumentException("Key must be 2 to 5 capital letters");
        }
        for (Project project : projects.values()) {
            if (project.getName().equals(name)) {
                throw new IllegalArgumentException("Project name " + name + " is already used");
            }
            if (project.getKey().equals(key)) {
                throw new IllegalArgumentException("Project key " + key + " is already used");
            }
        }
        InMemoryChecks.userExists(users, ownerId);

        OffsetDateTime now = OffsetDateTime.now();
        Project project = new Project(nextId++, name, key, ownerId, now);

        // the project and its owner member are saved together
        List<ProjectMember> projectMembers = new ArrayList<>();
        projectMembers.add(new ProjectMember(project.getId(), ownerId, Role.OWNER, now));

        projects.put(project.getId(), project);
        members.put(project.getId(), projectMembers);
        return project;
    }

    @Override
    public Project findById(long id) {
        return projects.get(id);   // null if not found
    }

    @Override
    public Project findByKey(String key) {
        for (Project project : projects.values()) {
            if (project.getKey().equals(key)) {
                return project;
            }
        }
        return null;
    }

    @Override
    public void addMember(long projectId, long userId, Role role) {
        if (role == Role.OWNER) {
            throw new IllegalArgumentException("The owner is set only when the project is created");
        }
        List<ProjectMember> projectMembers = members.get(projectId);
        if (projectMembers == null) {
            throw new IllegalArgumentException("No project with id " + projectId);
        }
        InMemoryChecks.userExists(users, userId);
        // like the primary key (project_id, user_id): a user joins a project once
        if (isMember(projectId, userId)) {
            throw new IllegalArgumentException("User " + userId + " is already a member of project " + projectId);
        }
        projectMembers.add(new ProjectMember(projectId, userId, role, OffsetDateTime.now()));
    }

    // Used by InMemoryTaskRepository: an assignee must be a member of the task's project.
    // Not in ProjectRepository, because PostgreSQL checks this with a foreign key.
    boolean isMember(long projectId, long userId) {
        for (ProjectMember member : members.getOrDefault(projectId, List.of())) {
            if (member.getUserId() == userId) {
                return true;
            }
        }
        return false;
    }
}

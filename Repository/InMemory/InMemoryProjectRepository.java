import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keeps projects and their members in memory. Used when STORAGE=memory.
class InMemoryProjectRepository implements ProjectRepository {

    // id -> project
    private final Map<Long, Project> projects = new HashMap<>();

    // project id -> its members
    private final Map<Long, List<ProjectMember>> members = new HashMap<>();

    private long nextId = 1;

    @Override
    public Project create(String name, String key, long ownerId) {
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
        projectMembers.add(new ProjectMember(projectId, userId, role, OffsetDateTime.now()));
    }
}

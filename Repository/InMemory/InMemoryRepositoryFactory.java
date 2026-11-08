// Creates the in-memory set of repositories. Used when STORAGE=memory.
// The three repositories share data, like tables in one database, so each is made once:
// projects check that owners and members are users, and tasks check users and memberships.
class InMemoryRepositoryFactory implements RepositoryFactory {

    private final UserRepository users = new InMemoryUserRepository();
    private final InMemoryProjectRepository projects = new InMemoryProjectRepository(users);
    private final TaskRepository tasks = new InMemoryTaskRepository(users, projects);

    @Override
    public UserRepository createUserRepository() {
        return users;
    }

    @Override
    public ProjectRepository createProjectRepository() {
        return projects;
    }

    @Override
    public TaskRepository createTaskRepository() {
        return tasks;
    }

    @Override
    public ImportRepository createImportRepository() {
        return new InMemoryImportRepository();
    }
}

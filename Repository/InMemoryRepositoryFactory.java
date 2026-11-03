// Creates the in-memory set of repositories. Used when STORAGE=memory.
class InMemoryRepositoryFactory implements RepositoryFactory {

    // the task repository needs this same user repository to find assignee names
    private final UserRepository users = new InMemoryUserRepository();

    @Override
    public UserRepository createUserRepository() {
        return users;
    }

    @Override
    public ProjectRepository createProjectRepository() {
        return new InMemoryProjectRepository();
    }

    @Override
    public TaskRepository createTaskRepository() {
        return new InMemoryTaskRepository(users);
    }
}

// Abstract Factory pattern: one factory per storage creates the whole set of repositories.
// The repositories of one set work together (the task repository reads assignee names
// from the user repository), so they always come from the same factory and are never mixed.
interface RepositoryFactory {

    UserRepository createUserRepository();

    ProjectRepository createProjectRepository();

    TaskRepository createTaskRepository();
}

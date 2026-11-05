// Creates the PostgreSQL set of repositories. Used when STORAGE=postgres.
// SQL_DEBUG=true prints every SQL statement.
class PostgresRepositoryFactory implements RepositoryFactory {
    private final Database database = new Database(new SqlLogger("true".equalsIgnoreCase(System.getenv("SQL_DEBUG"))));
    private final RowMapper mapper = new RowMapper();

    @Override
    public UserRepository createUserRepository() {
        return new PostgresUserRepository(database, mapper);
    }

    @Override
    public ProjectRepository createProjectRepository() {
        return new PostgresProjectRepository(database, mapper);
    }

    @Override
    public TaskRepository createTaskRepository() {
        return new PostgresTaskRepository(database, mapper);
    }
}

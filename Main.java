import java.util.Scanner;

// Starts the app: picks the storage from STORAGE, builds TaskManager, runs the console.
public class Main {

    public static void main(String[] args) {
        RepositoryFactory repositories = chooseRepositoryFactory(System.getenv("STORAGE"));
        TaskManager manager = new TaskManager(repositories.createUserRepository(),
                                              repositories.createProjectRepository(),
                                              repositories.createTaskRepository(),
                                              repositories.createImportRepository(),
                                              repositories.createReportRepository());
        new ConsoleApp(manager, new Scanner(System.in)).run();
    }

    // STORAGE=memory (or not set) uses memory; STORAGE=postgres uses PostgreSQL
    private static RepositoryFactory chooseRepositoryFactory(String storage) {
        if (storage == null || storage.equalsIgnoreCase("memory")) {
            return new InMemoryRepositoryFactory();
        }
        if (storage.equalsIgnoreCase("postgres")) {
            return new PostgresRepositoryFactory();
        }
        throw new IllegalArgumentException("Unknown STORAGE value '" + storage + "'. Use: memory or postgres");
    }
}

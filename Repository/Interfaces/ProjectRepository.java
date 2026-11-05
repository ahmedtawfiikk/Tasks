interface ProjectRepository {

    // Saves the project AND adds ownerId as its OWNER member, together:
    // both are saved or neither is (one transaction in Postgres).
    // projects has no owner column, so this is what makes sure every project
    // has an owner (docs/SCHEMA.md, hard rule 3).
    Project create(String name, String key, long ownerId);

    // returns null if there is no project with this id.
    // ownerId is read from the project_members row with the OWNER role.
    Project findById(long id);

    // returns null if there is no project with this key.
    // ownerId is read from the project_members row with the OWNER role.
    Project findByKey(String key);

    // Adds a MAINTAINER or MEMBER.
    // Throws IllegalArgumentException for OWNER: the owner is set only by create.
    // There is no method to remove or demote the owner.
    void addMember(long projectId, long userId, Role role);
}

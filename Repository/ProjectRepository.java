interface ProjectRepository {

    // also adds the owner as a member with the OWNER role
    Project create(String name, String key, long ownerId);

    // returns null if there is no project with this id
    Project findById(long id);

    // returns null if there is no project with this key
    Project findByKey(String key);

    void addMember(long projectId, long userId, Role role);
}

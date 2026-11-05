interface UserRepository {

    User create(String fullName, String email);

    // returns null if there is no user with this id
    User findById(long id);
}

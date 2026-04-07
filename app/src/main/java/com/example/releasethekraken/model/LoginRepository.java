package com.example.releasethekraken.model;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 * Default class made by Android Studio when creating login fragment.
 */
public class LoginRepository {

    private static volatile LoginRepository instance;

    private LoginDataSource dataSource;

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private LoggedInUser user = null;

    /**
     * Private constructor to enforce singleton access.
     * @param dataSource The data source for authentication.
     */
    private LoginRepository(LoginDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns the singleton instance of LoginRepository, creating it if it doesn't exist.
     * @param dataSource The data source to use if creating a new instance.
     * @return The singleton LoginRepository instance.
     */
    public static LoginRepository getInstance(LoginDataSource dataSource) {
        if (instance == null) {
            instance = new LoginRepository(dataSource);
        }
        return instance;
    }

    /**
     * Checks if a user is currently logged in.
     * @return true if a user is logged in, false otherwise.
     */
    public boolean isLoggedIn() {
        return user != null;
    }

    /**
     * Logs out the current user and clears cached credentials.
     */
    public void logout() {
        user = null;
        dataSource.logout();
    }

    /**
     * Sets the currently logged-in user.
     * @param user The user who has successfully logged in.
     */
    private void setLoggedInUser(LoggedInUser user) {
        this.user = user;
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    /**
     * Performs a login attempt using the provided credentials.
     * @param username The user's username.
     * @param password The user's password.
     * @return A Result object containing the LoggedInUser on success or an error.
     */
    public Result<LoggedInUser> login(String username, String password) {
        // handle login
        Result<LoggedInUser> result = dataSource.login(username, password);
        if (result instanceof Result.Success) {
            setLoggedInUser(((Result.Success<LoggedInUser>) result).getData());
        }
        return result;
    }
}

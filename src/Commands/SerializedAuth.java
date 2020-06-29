package Commands;

public class SerializedAuth {
    private String login;
    private String password;

    SerializedAuth(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getLogin() {
        return login;
    }
}

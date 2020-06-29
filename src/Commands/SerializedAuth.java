package Commands;

import java.io.Serializable;

public class SerializedAuth implements Serializable {
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

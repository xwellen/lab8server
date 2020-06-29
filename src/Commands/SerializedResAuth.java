package Commands;

import java.io.Serializable;

public class SerializedResAuth implements Serializable {
    private boolean res;

    SerializedResAuth(boolean res) {
        this.res = res;
    }

    public boolean getRes() {
        return res;
    }
}

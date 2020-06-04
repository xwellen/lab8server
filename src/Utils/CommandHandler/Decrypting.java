package Utils.CommandHandler;

import Commands.Command;
import Commands.SerializedCommand;

import java.io.IOException;
import java.net.Socket;

public class Decrypting {
    private final Socket socket;

    public Decrypting(Socket socket) {
        this.socket = socket;
    }

    public void decrypt(Object o) throws IOException {
        if (o instanceof SerializedCommand) {
            SerializedCommand serializedCommand = (SerializedCommand) o;
            Command command = serializedCommand.getCommand();
            command.execute(serializedCommand, socket);
        }
    }
}

package Utils.CommandHandler;

import Commands.Command;
import Commands.SerializedCommand;
import Exceptions.DatabaseException;

import java.io.IOException;
import java.net.Socket;

public class Decrypting {
    private final Socket socket;

    public Decrypting(Socket socket) {
        this.socket = socket;
    }

    public void decrypt(Object o) throws IOException, InterruptedException, ClassNotFoundException, DatabaseException {
        if (o instanceof SerializedCommand) {
            SerializedCommand serializedCommand = (SerializedCommand) o;
            Command command = serializedCommand.getCommand();
            command.execute(serializedCommand, socket);
        }
    }
}

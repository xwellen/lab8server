package Utils.CommandHandler;

import Commands.Command;
import Commands.SerializedAuth;
import Commands.SerializedCommand;
import Exceptions.DatabaseException;
import Interfaces.CommandReceiver;
import Interfaces.Decrypting;
import com.google.inject.Inject;

import java.io.IOException;
import java.net.Socket;

public class DecryptingImp implements Decrypting {
    private final CommandReceiver commandReceiver;

    @Inject
    public DecryptingImp(CommandReceiver commandReceiver) {
        this.commandReceiver = commandReceiver;
    }

    @Override
    public void decrypt(Object o, Socket socket) throws IOException, InterruptedException, ClassNotFoundException, DatabaseException {
        if (o instanceof SerializedCommand) {
            SerializedCommand serializedCommand = (SerializedCommand) o;
            Command command = serializedCommand.getCommand();
            command.execute(serializedCommand, socket, commandReceiver);
        }

        if (o instanceof SerializedAuth) {
            commandReceiver.
        }
    }
}

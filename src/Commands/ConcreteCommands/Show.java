package Commands.ConcreteCommands;

import Commands.Command;
import Commands.CommandReceiver;
import Commands.SerializedCommand;
import Exceptions.DatabaseException;

import java.io.IOException;
import java.net.Socket;

/**
 * Конкретная команда показа содержания коллекции.
 */
public class Show extends Command {
    private static final long serialVersionUID = 32L;

    @Override
    public void execute(Object argObject, Socket socket) throws IOException, DatabaseException {
        SerializedCommand objectCommand = (SerializedCommand) argObject;
        CommandReceiver commandReceiver = new CommandReceiver(socket, objectCommand.getLogin(), objectCommand.getPassword());
        commandReceiver.show();
    }
}

package Commands.ConcreteCommands;

import Commands.Command;
import Commands.CommandReceiver;
import Commands.SerializedCommand;
import Exceptions.DatabaseException;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

/**
 * Конкретная команда получения информации о коллекции.
 */
public class Info extends Command implements Serializable {
    private static final long serialVersionUID = 32L;

    @Override
    public void execute(Object argObject, Socket socket) throws IOException, DatabaseException {
        SerializedCommand objectCommand = (SerializedCommand) argObject;
        CommandReceiver commandReceiver = new CommandReceiver(socket, objectCommand.getLogin(), objectCommand.getPassword());
        commandReceiver.info();
    }
}

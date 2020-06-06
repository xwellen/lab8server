package Commands.ConcreteCommands;

import BasicClasses.StudyGroup;
import Commands.Command;
import Commands.CommandReceiver;
import Commands.SerializedCommands.SerializedObjectCommand;
import Exceptions.DatabaseException;

import java.io.IOException;
import java.net.Socket;

/**
 * Конкретная команда удаления объектов, превышающих заданный.
 */
public class RemoveGreater extends Command {
    private static final long serialVersionUID = 32L;

    @Override
    public void execute(Object argObject, Socket socket) throws IOException, DatabaseException {
        SerializedObjectCommand objectCommand = (SerializedObjectCommand) argObject;
        Object arg = objectCommand.getObject();
        CommandReceiver commandReceiver = new CommandReceiver(socket, objectCommand.getLogin(), objectCommand.getPassword());
        commandReceiver.removeGreater((StudyGroup) arg);
    }
}

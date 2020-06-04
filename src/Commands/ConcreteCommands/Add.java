package Commands.ConcreteCommands;

import Commands.Command;
import Commands.CommandReceiver;
import Commands.SerializedCommands.SerializedObjectCommand;

import java.io.IOException;
import java.net.Socket;

/**
 * Конкретная команда добавления в коллекцию.
 */
public class Add extends Command {
    private static final long serialVersionUID = 32L;

    @Override
    public void execute(Object argObject, Socket socket) throws IOException {
        SerializedObjectCommand objectCommand = (SerializedObjectCommand) argObject;
        Object object = objectCommand.getObject();
        CommandReceiver commandReceiver = new CommandReceiver(socket);
        commandReceiver.add(object);
    }
}

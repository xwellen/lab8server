package Commands.ConcreteCommands;

import Commands.Command;
import Commands.CommandReceiver;
import Commands.SerializedCommands.SerializedArgumentCommand;
import Exceptions.DatabaseException;

import java.io.IOException;
import java.net.Socket;

/**
 * Конкретная команда удаления по ID.
 */
public class RemoveByID extends Command {
    private static final long serialVersionUID = 32L;

    @Override
    public void execute(Object object, Socket socket) throws IOException, DatabaseException {
        SerializedArgumentCommand argumentCommand = (SerializedArgumentCommand) object;
        String arg = argumentCommand.getArg();
        if (arg.split(" ").length == 1) {
            CommandReceiver commandReceiver = new CommandReceiver(socket, argumentCommand.getLogin(), argumentCommand.getPassword());
            commandReceiver.removeById(arg);
        }
        else { System.out.println("Некорректное количество аргументов. Для справки напишите help."); }
    }
}

package Commands.ConcreteCommands;

import Commands.Command;
import Commands.CommandReceiver;
import Commands.SerializedCommands.SerializedAuthOrRegisterCommand;
import Exceptions.DatabaseException;

import java.io.IOException;
import java.net.Socket;

/**
 * Конкретная команда для регистрации.
 */
public class Register extends Command {
    private static final long serialVersionUID = 32L;

    @Override
    public void execute(Object argObject, Socket socket) throws IOException, ClassNotFoundException, InterruptedException, DatabaseException {
        SerializedAuthOrRegisterCommand objectCommand = (SerializedAuthOrRegisterCommand) argObject;
        String login = objectCommand.getLogin();
        String password = objectCommand.getPassword();
        CommandReceiver commandReceiver = new CommandReceiver(socket, login, password);
        commandReceiver.register(login, password);
    }
}

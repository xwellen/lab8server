package Utils.CommandHandler;

import Commands.Command;
import Commands.SerializedAuthOrReg;
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

        if (o instanceof SerializedAuthOrReg) {
            SerializedAuthOrReg serializedAuth = (SerializedAuthOrReg) o;
            if (serializedAuth.getType().equals("auth")) commandReceiver.tryAuth(serializedAuth.getLogin(), serializedAuth.getPassword(), socket);
            if (serializedAuth.getType().equals("reg")) commandReceiver.register(serializedAuth.getLogin(), serializedAuth.getPassword(), socket);
        }
    }
}

package Commands;

import BasicClasses.Person;
import BasicClasses.StudyGroup;
import Commands.SerializedCommands.*;
import Exceptions.DatabaseException;
import Interfaces.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Ресивер(получатель), отправляет серилизованные объекты на сервер.
 */
@Singleton
public class CommandReceiverImp implements CommandReceiver {
    private static final Logger logger = LoggerFactory.getLogger(CommandReceiverImp.class);
    private final CollectionManager collectionManager;
    private final CollectionUtils collectionUtils;
    private final DatabaseManager databaseManager;
    private final Validator validator;
    public final static ForkJoinPool forkJoinPool = new ForkJoinPool(2);
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

    @Inject
    public CommandReceiverImp(CollectionManager collectionManager, CollectionUtils collectionUtils, DatabaseManager databaseManager, Validator validator) {
        this.collectionManager = collectionManager;
        this.collectionUtils = collectionUtils;
        this.databaseManager = databaseManager;
        this.validator = validator;
    }

    @Override
    public void sendObject(Socket socket, SerializedMessage serializedMessage) throws IOException, DatabaseException {
        executor.submit(() -> {
            try {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(serializedMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean checkUser(String login, String password, Socket socket) throws DatabaseException, IOException {
        boolean exist = databaseManager.validateUserData(login, password);

        if (exist) {
            logger.info(String.format("Пользователь %s:%s, живущий по адресу %s:%s - прошел проверку на реального ИТМОшника", login, password, socket.getInetAddress(), socket.getPort()));
            return true;
        } else {
            sendObject(socket, new SerializedMessage("Дядя, ты не зарегистрирован в нашем гей-кружке, проваливай отсюда!"));
            logger.info(String.format("Товарищ %s:%s ошибся дверью, клуб кожевного ремесла два блока вниз.", socket.getInetAddress(), socket.getPort()));
        }

        return false;
    }

    @Override
    public void info(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            sendObject(socket, new SerializedMessage(collectionManager.getInfo()));

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды INFO", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void show(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            sendObject(socket, new SerializedMessage(collectionManager.show()));

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды SHOW", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void add(SerializedObjectCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            try {
                StudyGroup studyGroup = (StudyGroup) command.getObject();
                studyGroup.setId(databaseManager.addElement(studyGroup, command.getLogin()));
                collectionManager.add(studyGroup);

                sendObject(socket, new SerializedMessage("Элемент добавлен в коллекцию."));
            } catch (Exception e) {
                sendObject(socket, new SerializedMessage("Полученный элемент не добавлен."));
                e.printStackTrace();
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды ADD", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void update(SerializedCombinedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            Integer groupId;
            try {
                groupId = Integer.parseInt(command.getArg());
                if (collectionUtils.checkExist(groupId)) {
                    try {
                        StudyGroup studyGroup = (StudyGroup) command.getObject();
                        databaseManager.updateById(studyGroup, groupId, command.getLogin());
                        collectionManager.update(studyGroup, groupId);

                        sendObject(socket, new SerializedMessage("Команда update выполнена."));
                    } catch (Exception e){
                        e.printStackTrace();

                        sendObject(socket, new SerializedMessage("Элемент не обновлен."));
                    }
                } else {
                    sendObject(socket, new SerializedMessage("Элемента с таким ID нет в коллекции."));
                }
            } catch (NumberFormatException e) {
                sendObject(socket, new SerializedMessage("Команда не выполнена. Вы ввели некорректный аргумент."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды UPDATE", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void removeById(SerializedArgumentCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            Integer groupId;
            try {
                groupId = Integer.parseInt(command.getArg());
                if (collectionUtils.checkExist(groupId)) {
                    databaseManager.removeById(groupId, command.getLogin());
                    collectionManager.removeById(groupId);
                    sendObject(socket, new SerializedMessage("Элемент с ID " + groupId + " успешно удален из коллекции."));
                } else {
                    sendObject(socket, new SerializedMessage("Элемента с таким ID нет в коллекции."));
                }
            } catch (NumberFormatException e) {
                sendObject(socket, new SerializedMessage("Команда не выполнена. Вы ввели некорректный аргумент."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды REMOVE_BY_ID", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void clear(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            List<Integer> deleteID = databaseManager.clear(command.getLogin());
            deleteID.forEach(collectionManager::removeById);

            sendObject(socket, new SerializedMessage("Ваши элементы колекции удалены."));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды CLEAR", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void head(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            sendObject(socket, new SerializedMessage(collectionManager.head()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды HEAD", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void removeGreater(SerializedObjectCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {

            StudyGroup studyGroup = (StudyGroup) command.getObject();
            if (validator.validateStudyGroup(studyGroup)) {
                List<Integer> ids = collectionManager.removeGreater(studyGroup, databaseManager.getIdOfUserElements(command.getLogin()));
                if (ids.isEmpty()) sendObject(socket, new SerializedMessage("Таких элементов не найдено"));
                else sendObject(socket, new SerializedMessage("Из коллекции удалены элементы с ID: " +
                        ids.toString().replaceAll("[\\[\\]]", "")));

                ids.forEach(id -> {
                    try {
                        databaseManager.removeById(id, command.getLogin());
                    } catch (DatabaseException e) {
                        try {
                            sendObject(socket, new SerializedMessage("Ошибка при удалении из бд элемента с id="+ id + "\n" + e));
                        } catch (IOException | DatabaseException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } else {
                sendObject(socket, new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды REMOVE_GREATER", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void removeLower(SerializedObjectCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            StudyGroup studyGroup = (StudyGroup) command.getObject();
            if (validator.validateStudyGroup(studyGroup)) {
                List<Integer> ids = collectionManager.removeLower(studyGroup, databaseManager.getIdOfUserElements(command.getLogin()));
                if (ids.isEmpty()) sendObject(socket, new SerializedMessage("Таких элементов не найдено"));
                else sendObject(socket, new SerializedMessage("Из коллекции удалены элементы с ID: " +
                        ids.toString().replaceAll("[\\[\\]]", "")));

                ids.forEach(id -> {
                    try {
                        databaseManager.removeById(id, command.getLogin());
                    } catch (DatabaseException e) {
                        try {
                            sendObject(socket, new SerializedMessage("Ошибка при удалении из бд элемента с id="+ id + "\n" + e));
                        } catch (IOException | DatabaseException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } else {
                sendObject(socket, new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды REMOVE_LOWER", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void minBySemesterEnum(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            sendObject(socket, new SerializedMessage(collectionManager.minBySemesterEnum()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды MIN_BY_SEMESTER_ENUM", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public  void maxByGroupAdmin(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            sendObject(socket, new SerializedMessage(collectionManager.maxByGroupAdmin()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды MAX_BY_GROUP_ADMIN", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void countByGroupAdmin(SerializedObjectCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            Person groupAdmin = (Person) command.getObject();
            if (validator.validatePerson(groupAdmin)) {
                sendObject(socket, new SerializedMessage(collectionManager.countByGroupAdmin(groupAdmin)));
            } else {
                sendObject(socket, new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды COUNT_BY_GROUP_ADMIN", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void register(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (!databaseManager.doesUserExist(command.getLogin())) {
            databaseManager.addUser(command.getLogin(), command.getPassword());
            sendObject(socket, new SerializedMessage("Пользователь с логином " + command.getLogin() + " успешно создан!"));
            logger.info(String.format("Пользователь %s успешно зарегистрирован!", command.getLogin()));
        } else { sendObject(socket, new SerializedMessage("Пользователь с таким логином уже существует!")); }
        logger.info(String.format("Клиенту %s:%s отправлен результат попытки регистрации", socket.getInetAddress(), socket.getPort()));
    }
}

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

/**
 * Ресивер(получатель), отправляет серилизованные объекты на сервер.
 */
@Singleton
public class CommandReceiver {
    private static final Logger logger = LoggerFactory.getLogger(CommandReceiver.class);
    private final CollectionManager collectionManager;
    private final CollectionUtils collectionUtils;
    private final DatabaseManager databaseManager;
    private final Validator validator;

    @Inject
    public CommandReceiver(CollectionManager collectionManager, CollectionUtils collectionUtils, DatabaseManager databaseManager, Validator validator) {
        this.collectionManager = collectionManager;
        this.collectionUtils = collectionUtils;
        this.databaseManager = databaseManager;
        this.validator = validator;
    }

    @Override
    public boolean checkUser(String login, String password, Socket socket) throws DatabaseException, IOException {
        boolean exist = databaseManager.validateUserData(login, password);

        if (exist) {
            logger.info(String.format("Пользователь %s:%s, живущий по адресу %s:%s - прошел проверку на реального ИТМОшника", login, password, socket.getInetAddress(), socket.getPort()));
            return true;
        } else {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(new SerializedMessage("Дядя, ты не зарегистрирован в нашем гей-кружке, проваливай отсюда!"));
            logger.info(String.format("Товарищ %s:%s ошибся дверью, клуб кожевного ремесла два блока вниз.", socket.getInetAddress(), socket.getPort()));
        }

        return false;
    }

    @Override
    public void info(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.getInfo()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды INFO", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void show(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.show()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды SHOW", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void add(SerializedObjectCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            try {
                StudyGroup studyGroup = (StudyGroup) command.getObject();
                studyGroup.setId(databaseManager.addElement(studyGroup, command.getLogin()));
                collectionManager.add(studyGroup);
                out.writeObject(new SerializedMessage("Элемент добавлен в коллекцию."));
            } catch (Exception e){
                e.printStackTrace();
                out.writeObject(new SerializedMessage("Полученный элемент не добавлен: " + e));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды ADD", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void update(SerializedCombinedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            Integer groupId;
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            try {
                groupId = Integer.parseInt(command.getArg());
                if (collectionUtils.checkExist(groupId)) {
                    try {
                        StudyGroup studyGroup = (StudyGroup) command.getObject();
                        databaseManager.updateById(studyGroup, groupId, command.getLogin());
                        collectionManager.update(studyGroup, groupId);
                        out.writeObject(new SerializedMessage("Команда update выполнена."));
                    } catch (Exception e){
                        e.printStackTrace();
                        out.writeObject(new SerializedMessage("Элемент не обновлен: " + e));
                    }
                } else {
                    out.writeObject(new SerializedMessage("Элемента с таким ID нет в коллекции."));
                }
            } catch (NumberFormatException e) {
                out.writeObject(new SerializedMessage("Команда не выполнена. Вы ввели некорректный аргумент."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды UPDATE", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void removeById(SerializedArgumentCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            Integer groupId;
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            try {
                groupId = Integer.parseInt(command.getArg());
                if (collectionUtils.checkExist(groupId)) {
                    databaseManager.removeById(groupId, command.getLogin());
                    collectionManager.removeById(groupId);
                    out.writeObject(new SerializedMessage("Элемент с ID " + groupId + " успешно удален из коллекции."));
                } else {
                    out.writeObject(new SerializedMessage("Элемента с таким ID нет в коллекции."));
                }
            } catch (NumberFormatException e) {
                out.writeObject(new SerializedMessage("Команда не выполнена. Вы ввели некорректный аргумент."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды REMOVE_BY_ID", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void clear(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            List<Integer> deleteID = databaseManager.clear(command.getLogin());
            deleteID.forEach(collectionManager::removeById);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage("Ваши элементы колекции удалены."));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды CLEAR", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void head(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.head()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды HEAD", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void removeGreater(SerializedObjectCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            StudyGroup studyGroup = (StudyGroup) command.getObject();
            if (validator.validateStudyGroup(studyGroup)) {
                List<Integer> ids = collectionManager.removeGreater(studyGroup, databaseManager.getIdOfUserElements(command.getLogin()));
                if (ids.isEmpty()) out.writeObject(new SerializedMessage("Таких элементов не найдено"));
                else out.writeObject(new SerializedMessage("Из коллекции удалены элементы с ID: " +
                        ids.toString().replaceAll("[\\[\\]]", "")));

                ids.forEach(id -> {
                    try {
                        databaseManager.removeById(id, command.getLogin());
                    } catch (DatabaseException e) {
                        try {
                            out.writeObject(new SerializedMessage("Ошибка при удалении из бд элемента с id="+ id + "\n" + e));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } else {
                out.writeObject(new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды REMOVE_GREATER", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void removeLower(SerializedObjectCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            StudyGroup studyGroup = (StudyGroup) command.getObject();
            if (validator.validateStudyGroup(studyGroup)) {
                List<Integer> ids = collectionManager.removeLower(studyGroup, databaseManager.getIdOfUserElements(command.getLogin()));
                if (ids.isEmpty()) out.writeObject(new SerializedMessage("Таких элементов не найдено"));
                else out.writeObject(new SerializedMessage("Из коллекции удалены элементы с ID: " +
                        ids.toString().replaceAll("[\\[\\]]", "")));

                ids.forEach(id -> {
                    try {
                        databaseManager.removeById(id, command.getLogin());
                    } catch (DatabaseException e) {
                        try {
                            out.writeObject(new SerializedMessage("Ошибка при удалении из бд элемента с id="+ id + "\n" + e));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } else {
                out.writeObject(new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды REMOVE_LOWER", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void minBySemesterEnum(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.minBySemesterEnum()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды MIN_BY_SEMESTER_ENUM", socket.getInetAddress(), socket.getPort()));
        }
    }

    public  void maxByGroupAdmin(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.maxByGroupAdmin()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды MAX_BY_GROUP_ADMIN", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void countByGroupAdmin(SerializedObjectCommand command, Socket socket) throws IOException, DatabaseException {
        if (checkUser(command.getLogin(), command.getPassword(), socket)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            Person groupAdmin = (Person) command.getObject();
            if (validator.validatePerson(groupAdmin)) {
                out.writeObject(new SerializedMessage(collectionManager.countByGroupAdmin(groupAdmin)));
            } else {
                out.writeObject(new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды COUNT_BY_GROUP_ADMIN", socket.getInetAddress(), socket.getPort()));
        }
    }

    @Override
    public void register(SerializedCommand command, Socket socket) throws IOException, DatabaseException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

        if (!databaseManager.doesUserExist(command.getLogin())) {
            databaseManager.addUser(command.getLogin(), command.getPassword());
            out.writeObject(new SerializedMessage("Пользователь с логином " + command.getLogin() + " успешно создан!"));
            logger.info(String.format("Пользователь %s успешно зарегистрирован!", command.getLogin()));
        } else { out.writeObject(new SerializedMessage("Пользователь с таким логином уже существует!")); }
        logger.info(String.format("Клиенту %s:%s отправлен результат попытки регистрации", socket.getInetAddress(), socket.getPort()));
    }
}

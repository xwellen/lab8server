import Collection.CollectionManagerImp;
import Collection.CollectionUtilsImp;
import ServerSocket.ControllerImp;
import com.google.inject.AbstractModule;
import Interfaces.*;

/**
 * Серверный модуль для Guice Dependency Injection.
 */
public class ServerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Controller.class).to(ControllerImp.class);
        bind(CollectionManager.class).to(CollectionManagerImp.class);
        bind(CollectionUtils.class).to(CollectionUtilsImp.class);
    }
}
package com.artipie.maven.lib;

import java.nio.file.Path;
import java.util.List;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;

/**
 * Wraps and configures org.eclipse.aether and org.apache.maven classes
 */
public class ServiceLocatorFacade implements ServiceLocator {
    private final ServiceLocator serviceLocator;
    private final LocalRepository localRepository;

    private ServiceLocatorFacade(ServiceLocator serviceLocator, LocalRepository localRepository) {
        this.serviceLocator = serviceLocator;
        this.localRepository = localRepository;
    }

    public static ServiceLocatorFacade create(Path localRepositoryPath) {
        var serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
        serviceLocator.setService(LocalRepositoryManagerFactory.class, SimpleLocalRepositoryManagerFactory.class);
        serviceLocator.setService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        serviceLocator.setService(TransporterFactory.class, WagonTransporterFactory.class);
        serviceLocator.setService(WagonProvider.class, CustomWagonProvider.class);

        var localRepository = new LocalRepository(localRepositoryPath.toFile());
        return new ServiceLocatorFacade(serviceLocator, localRepository);
    }

    @Override
    public <T> T getService(Class<T> type) {
        return serviceLocator.getService(type);
    }

    @Override
    public <T> List<T> getServices(Class<T> type) {
        return serviceLocator.getServices(type);
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public LocalRepository getLocalRepository() {
        return localRepository;
    }

    public RepositorySystemSession createSession() {
        return createSession(getService(LocalRepositoryManagerFactory.class), localRepository);
    }

    public LocalRepositoryManager createLocalRepositoryManager() {
        return createLocalRepositoryManager(getService(LocalRepositoryManagerFactory.class), localRepository);
    }

    public static RepositorySystemSession createSession(LocalRepositoryManagerFactory localRepositoryManagerFactory, LocalRepository localRepository) {
        try {
            var session = MavenRepositorySystemUtils.newSession();
            var lrm = localRepositoryManagerFactory.newInstance(session, localRepository);
            session.setLocalRepositoryManager(lrm);
            return session;
        } catch (NoLocalRepositoryManagerException e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalRepositoryManager createLocalRepositoryManager(LocalRepositoryManagerFactory localRepositoryManagerFactory, LocalRepository localRepository) {
        try {
            return localRepositoryManagerFactory.newInstance(null, localRepository);
        } catch (NoLocalRepositoryManagerException e) {
            throw new IllegalStateException(e);
        }
    }

}

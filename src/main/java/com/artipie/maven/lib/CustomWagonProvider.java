package com.artipie.maven.lib;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.eclipse.aether.transport.wagon.WagonProvider;

/**
 * Extension point for pluggable storage and transport
 */
public class CustomWagonProvider implements WagonProvider {

    @Override
    public Wagon lookup(String roleHint) throws Exception {
        switch (roleHint) {
            case "file": return new FileWagon();
            default: throw new IllegalStateException("unknown roleHint "+roleHint);
        }
    }

    @Override
    public void release(Wagon wagon) {
        try {
            wagon.disconnect();
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.mycompany.standalone;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = Service.class)
public class ServiceImpl implements Service{

    @Override public void action() {
        org.openide.util.Utilities.isUnix();
    }
}

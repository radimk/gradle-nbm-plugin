package com.mycompany.standalone;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
//import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Help",
        id = "com.mycompany.standalone.HelloAction"
)
@ActionRegistration(
        displayName = "#CTL_HelloAction"
)
@ActionReference(path = "Menu/Help", position = 100)
//@Messages("CTL_HelloAction=Say hello") // moved to Bundle.properties of the same package under the resouces folder
public final class HelloAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
    }
}

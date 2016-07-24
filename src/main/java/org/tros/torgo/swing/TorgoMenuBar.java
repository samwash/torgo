/*
 * Copyright 2015-2016 Matthew Aguirre
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tros.torgo.swing;

import org.tros.torgo.Controller;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.tros.utils.logging.LogConsole;

/**
 * Builds the base menu.
 *
 * @author matta
 */
public class TorgoMenuBar extends JMenuBar {

    protected final Controller controller;

    protected final Component parent;

    /**
     * Constructor.
     *
     * @param parent
     * @param controller
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public TorgoMenuBar(Component parent, Controller controller) {
        this.controller = controller;
        this.parent = parent;

        try {
            add(setupFileMenu());
            add(setupInterpreterMenu());
        } catch (IOException ex) {
            Logger.getLogger(TorgoMenuBar.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private JMenu setupInterpreterMenu() throws IOException {
        LogConsole.CONSOLE.setVisible(false);
        JMenu interpreterMenu = new JMenu("Interpreter");

        JMenuItem fileStart = new JMenuItem("Start");
        java.util.Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources("projectui/runProject.png");
        ImageIcon ico = new ImageIcon(resources.nextElement());
        fileStart.setIcon(ico);

        fileStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        resources = ClassLoader.getSystemClassLoader().getResources("projectui/stop.png");
        ico = new ImageIcon(resources.nextElement());
        JMenuItem fileStop = new JMenuItem("Stop");
        fileStop.setIcon(ico);
        fileStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        JMenuItem fileDebug = new JMenuItem("Debug");
        resources = ClassLoader.getSystemClassLoader().getResources("debugging/debugProject.png");
        ico = new ImageIcon(resources.nextElement());
        fileDebug.setIcon(ico);
        fileDebug.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        JMenuItem filePause = new JMenuItem("Pause");
        resources = ClassLoader.getSystemClassLoader().getResources("debugging/actions/Pause.png");
        ico = new ImageIcon(resources.nextElement());
        filePause.setIcon(ico);
        filePause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        JMenuItem fileStep = new JMenuItem("Step");
        resources = ClassLoader.getSystemClassLoader().getResources("debugging/actions/StepOver.png");
        ico = new ImageIcon(resources.nextElement());
        fileStep.setIcon(ico);
        fileStep.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        interpreterMenu.add(fileStart);
        interpreterMenu.add(fileStop);
        interpreterMenu.add(fileDebug);
        interpreterMenu.add(filePause);
        interpreterMenu.add(fileStep);

        interpreterMenu.setMnemonic('I');

        return interpreterMenu;
    }

    /**
     * Initializer.
     *
     * @return
     */
    private JMenu setupFileMenu() throws IOException {
        LogConsole.CONSOLE.setVisible(false);
        JMenu fileMenu = new JMenu(Localization.getLocalizedString("FileMenu"));

        JMenuItem fileNew = new JMenuItem(Localization.getLocalizedString("FileNew"));
        java.util.Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources("projectui/newFile.png");
        ImageIcon ico = new ImageIcon(resources.nextElement());
        fileNew.setIcon(ico);

        JMenuItem fileOpen = new JMenuItem(Localization.getLocalizedString("FileOpen"));
        resources = ClassLoader.getSystemClassLoader().getResources("projectui/open.png");
        ico = new ImageIcon(resources.nextElement());
        fileOpen.setIcon(ico);

        JMenuItem fileClose = new JMenuItem(Localization.getLocalizedString("FileClose"));
        JMenuItem fileSave = new JMenuItem(Localization.getLocalizedString("FileSave"));
        resources = ClassLoader.getSystemClassLoader().getResources("actions/save.png");
        ico = new ImageIcon(resources.nextElement());
        fileSave.setIcon(ico);

        JMenuItem fileSaveAs = new JMenuItem(Localization.getLocalizedString("FileSaveAs"));
        resources = ClassLoader.getSystemClassLoader().getResources("profiler/actions/icons/saveAs.png");
        ico = new ImageIcon(resources.nextElement());
        fileSaveAs.setIcon(ico);

        JMenuItem fileQuit = new JMenuItem(Localization.getLocalizedString("FileQuit"));

        JMenuItem logConsole = new JMenuItem("View Log Console");

        fileNew.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                controller.newFile();
            }
        });
        fileOpen.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                controller.openFile();
            }
        });
        fileClose.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                controller.close();
            }
        });
        fileSave.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                controller.saveFile();
            }
        });
        fileSaveAs.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                controller.saveFileAs();
            }
        });
        fileQuit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        });

        logConsole.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                LogConsole.CONSOLE.setVisible(true);
            }
        });

        fileNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK));
        fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        fileClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK));
        fileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        fileSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
        fileQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        logConsole.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));

        fileNew.setMnemonic('N');
        fileOpen.setMnemonic('O');
        fileClose.setMnemonic('C');
        fileSave.setMnemonic('S');
        fileSaveAs.setMnemonic('A');
        fileQuit.setMnemonic('Q');
        logConsole.setMnemonic('L');

        fileMenu.add(fileNew);
        fileMenu.add(fileOpen);
        fileMenu.add(fileClose);
        fileMenu.addSeparator();
        fileMenu.add(fileSave);
        fileMenu.add(fileSaveAs);
        fileMenu.addSeparator();
        fileMenu.add(logConsole);
        fileMenu.addSeparator();
        fileMenu.add(fileQuit);

        fileMenu.setMnemonic('F');

        return (fileMenu);
    }
}

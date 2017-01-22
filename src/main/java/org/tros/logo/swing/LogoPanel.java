/*
 * Copyright 2015-2017 Matthew Aguirre
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
package org.tros.logo.swing;

import org.tros.torgo.swing.BufferedImageProvider;
import org.tros.logo.LogoCanvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.event.EventListenerSupport;
import org.tros.torgo.TorgoScreen;

import org.tros.torgo.TorgoTextConsole;

/**
 * I'd like to see if there is a way to clean up the multiple anonymous nested
 * drawable classes. These are used when exporting to file so that state between
 * the app and export is no longer shared.
 *
 * @author matta
 */
public class LogoPanel extends JPanel implements TorgoScreen, LogoCanvas, BufferedImageProvider, Drawable {

    protected final EventListenerSupport<DrawListener> listeners
            = EventListenerSupport.create(DrawListener.class);

    private final TorgoTextConsole console;
    private BufferedImage turtle;

    private final ArrayList<Drawable> queuedCommands = new ArrayList<>();
    private final ArrayList<Drawable> commands = new ArrayList<>();

    private TurtleState turtleState;

    /**
     * Constructor.
     *
     * @param textOutput
     */
    public LogoPanel(TorgoTextConsole textOutput) {
        console = textOutput;
        turtleState = new TurtleState();
        turtleState.penup = false;
        turtleState.showTurtle = true;
        URL resource = ClassLoader.getSystemClassLoader().getResource("turtle.png");
        try {
            turtle = ImageIO.read(resource);
        } catch (IOException ex) {
            org.tros.utils.logging.Logging.getLogFactory().getLogger(LogoPanel.class).fatal(null, ex);
        }
    }

    @Override
    public void addListener(DrawListener listener) {
        listeners.addListener(listener);
    }

    @Override
    public void removeListener(DrawListener listener) {
        listeners.removeListener(listener);
    }

    /**
     * Paint.
     *
     * @param g
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        turtleState.width = getWidth();
        turtleState.height = getHeight();

        draw(g2d, turtleState);

        if (turtleState.showTurtle) {
            double x = turtleState.penX - (turtle.getWidth() / 2.0);
            double y = turtleState.penY - (turtle.getHeight() / 2.0);
            AffineTransform translateInstance = AffineTransform.getRotateInstance(turtleState.angle + (Math.PI / 2.0), turtleState.penX, turtleState.penY);
            AffineTransform saveXform = g2d.getTransform();
            g2d.transform(translateInstance);
            g2d.drawImage(turtle, (int) x, (int) y, null);
            g2d.setTransform(saveXform);
        }
    }

    /**
     *
     * @param g2d
     * @param turtleState
     */
    @Override
    public void draw(Graphics2D g2d, TurtleState turtleState) {
        //since this list can be written to, do not swith to for-each
        for (int ii = 0; ii < queuedCommands.size(); ii++) {
            queuedCommands.get(ii).draw(g2d, turtleState);
            listeners.fire().drawn(this);
        }
    }

    @Override
    public Drawable cloneDrawable() {
        Drawable d = new Drawable() {

            protected final EventListenerSupport<DrawListener> listenersCopy
                    = EventListenerSupport.create(DrawListener.class);
            private final ArrayList<Drawable> queuedCommandsCopy = new ArrayList<>();

            /**
             * Anonymous class initializer.
             * Clone all internal drawable objects.
             */
            {
                queuedCommands.forEach((d) -> {
                    queuedCommandsCopy.add(d.cloneDrawable());
                });
            }

            @Override
            public void draw(Graphics2D g2d, TurtleState turtleState) {
                //since this list can be written to, do not swith to for-each
                for (int ii = 0; ii < queuedCommandsCopy.size(); ii++) {
                    queuedCommandsCopy.get(ii).draw(g2d, turtleState);
                    listenersCopy.fire().drawn(this);
                }
            }

            @Override
            public void addListener(DrawListener listener) {
                listenersCopy.addListener(listener);
            }

            @Override
            public void removeListener(DrawListener listener) {
                listenersCopy.removeListener(listener);
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        return d;
    }

    @Override
    public void pause(final int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
            org.tros.utils.logging.Logging.getLogFactory().getLogger(LogoPanel.class).fatal(null, ex);
        }
    }

    @Override
    public void forward(final double distance) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                double newx = turtleState.penX + (distance * Math.cos(turtleState.angle));
                double newy = turtleState.penY + (distance * Math.sin(turtleState.angle));

                if (!turtleState.penup) {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.draw(new Line2D.Double(turtleState.penX, turtleState.penY, newx, newy));
                }

                turtleState.penX = newx;
                turtleState.penY = newy;
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    private void submitCommand(Drawable command) {
        queuedCommands.add(command);
    }

    @Override
    public void backward(final double distance) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                double newx = turtleState.penX - (distance * Math.cos(turtleState.angle));
                double newy = turtleState.penY - (distance * Math.sin(turtleState.angle));

                if (!turtleState.penup) {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.draw(new Line2D.Double(turtleState.penX, turtleState.penY, newx, newy));
                }

                turtleState.penX = newx;
                turtleState.penY = newy;
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void left(final double angle) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                turtleState.angle -= Math.PI * angle / 180.0;
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void right(final double angle) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                turtleState.angle += Math.PI * angle / 180.0;
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void setXY(final double x, final double y) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                double x2 = (turtleState.width > 0 ? turtleState.width : getWidth()) / 2.0 + x;
                double y2 = (turtleState.height > 0 ? turtleState.height : getHeight()) / 2.0 + y;

                if (!turtleState.penup) {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.draw(new Line2D.Double(turtleState.penX, turtleState.penY, x2, y2));
                }
                turtleState.penX = x2;
                turtleState.penY = y2;
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void penUp() {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                turtleState.penup = true;
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void penDown() {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                turtleState.penup = false;
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void clear() {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                try {
                    g2.setColor(Color.white);
                    g2.fillRect(0, 0,
                        turtleState.width > 0 ? (int) turtleState.width : getWidth(),
                        turtleState.height > 0 ? (int) turtleState.height : getHeight());
                    turtleState.penColor = Color.black;
                    g2.setColor(turtleState.penColor);

                    turtleState.font = new Font(null, 0, 12);

                    g2.setFont(turtleState.font);
                } catch (Exception ex) {

                }
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void home() {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                turtleState.penX = turtleState.width > 0 ? turtleState.width / 2.0 : getWidth() / 2.0;
                turtleState.penY = turtleState.height > 0 ? turtleState.height / 2.0 : getHeight() / 2.0;
                turtleState.angle = -1.0 * (Math.PI / 2.0);
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void canvascolor(int red, int green, int blue) {
        red = Math.min(255, Math.max(0, red));
        green = Math.min(255, Math.max(0, green));
        blue = Math.min(255, Math.max(0, blue));

        Color canvasColor = new Color(red, green, blue);
        canvascolor(canvasColor);
    }

    @Override
    public void canvascolor(final String color) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                Color canvasColor = getColorByName(color);

                g2.setColor(canvasColor);
                g2.fillRect(0, 0,
                        turtleState.width > 0 ? (int) turtleState.width : getWidth(),
                        turtleState.height > 0 ? (int) turtleState.height : getHeight());
                g2.setColor(turtleState.penColor);
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    private void canvascolor(final Color color) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                g2.setColor(color);
                g2.fillRect(0, 0,
                        turtleState.width > 0 ? (int) turtleState.width : getWidth(),
                        turtleState.height > 0 ? (int) turtleState.height : getHeight());
                g2.setColor(turtleState.penColor);
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void pencolor(int red, int green, int blue, int alpha) {
        red = Math.min(255, Math.max(0, red));
        green = Math.min(255, Math.max(0, green));
        blue = Math.min(255, Math.max(0, blue));

        Color canvasColor = new Color(red, green, blue, alpha);
        pencolor(canvasColor);
    }

    private void pencolor(final Color color) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                turtleState.penColor = color;
                g2.setColor(turtleState.penColor);
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void pencolor(final String color) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                turtleState.penColor = getColorByName(color);
                g2.setColor(turtleState.penColor);
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    private Color getColorByName(String color) {
        color = color.toLowerCase();
        Color ret = Color.black;

        try {
            Field field = Color.class.getField(color);
            return (Color) field.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
        }
        if (null != color) {
            switch (color) {
                case "darkgray":
                    ret = Color.darkGray;
                    break;
                case "lightgray":
                    ret = Color.lightGray;
                    break;
                default:
                    if (!color.startsWith("#") || !color.startsWith(color)) {
                        color = "#" + color;
                    }
                    Color c = java.awt.Color.decode(color);
                    ret = c == null ? Color.black : c;
                    break;
            }
        }
        return ret;
    }

    @Override
    public void drawString(final String message) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                if (!turtleState.penup) {
                    AffineTransform saveXform = g2.getTransform();
                    //double offsetAngle = (Math.PI / 2.0);
                    double offsetAngle = 0;
                    g2.setTransform(AffineTransform.getRotateInstance(turtleState.angle + offsetAngle, turtleState.penX, turtleState.penY));
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                    g2.drawString(message, (int) turtleState.penX, (int) turtleState.penY);
                    g2.setTransform(saveXform);
                }
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void fontSize(final int size) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                String fontName = turtleState.font.getFontName();
                int style = turtleState.font.getStyle();

                turtleState.font = new Font(fontName, style, size);

                g2.setFont(turtleState.font);
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void fontName(final String fontFace) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                int style = turtleState.font.getStyle();
                int size = turtleState.font.getSize();

                turtleState.font = new Font(fontFace, style, size);

                g2.setFont(turtleState.font);
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void fontStyle(final int style) {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                String fontName = turtleState.font.getFontName();
                int size = turtleState.font.getSize();

                turtleState.font = new Font(fontName, style, size);

                g2.setFont(turtleState.font);
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public BufferedImage getBufferedImage() {
        BufferedImage buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) buffer.createGraphics();
        draw(g2d, turtleState);
        return buffer;
    }

    @Override
    public void hideTurtle() {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                turtleState.showTurtle = false;
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void showTurtle() {
        Drawable command = new Drawable() {

            @Override
            public void draw(Graphics2D g2, TurtleState turtleState) {
                turtleState.showTurtle = true;
            }

            @Override
            public void addListener(DrawListener listener) {
            }

            @Override
            public void removeListener(DrawListener listener) {
            }

            @Override
            public Drawable cloneDrawable() {
                return this;
            }
        };
        submitCommand(command);
    }

    @Override
    public void message(String message) {
        message = message.trim();
        console.appendToOutputTextArea(message + System.getProperty("line.separator"));
    }

    @Override
    public void warning(String message) {
        message = message.trim();
        console.appendToOutputTextArea(">> " + message + System.getProperty("line.separator"));
    }

    @Override
    public double getTurtleX() {
        return turtleState.penX;
    }

    @Override
    public double getTurtleY() {
        return turtleState.penY;
    }

    @Override
    public double getTurtleAngle() {
        return turtleState.angle;
    }

    @Override
    public final void reset() {
        turtleState.penup = false;
        turtleState.showTurtle = true;
        queuedCommands.clear();
        commands.clear();
        clear();
        home();
        repaint();
    }

    @Override
    public void repaint() {
        if (SwingUtilities.isEventDispatchThread()) {
            LogoPanel.super.repaint();
        } else {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(LogoMenuBar.class);
            if (prefs.getBoolean(LogoMenuBar.WAIT_FOR_REPAINT, true)) {
                try {
                    SwingUtilities.invokeAndWait(LogoPanel.super::repaint);
                } catch (InterruptedException | InvocationTargetException ex) {
                    org.tros.utils.logging.Logging.getLogFactory().getLogger(LogoPanel.class).fatal(null, ex);
                }
            } else {
                SwingUtilities.invokeLater(LogoPanel.super::repaint);
            }
        }
    }

    @Override
    public Component getComponent() {
        return this;
    }
}

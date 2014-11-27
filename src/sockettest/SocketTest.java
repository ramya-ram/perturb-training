package sockettest;

import java.awt.*;
import java.applet.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

/**
 *
 * @author Akshathkumar Shetty
 */
public class SocketTest extends JFrame {
    private ClassLoader cl = getClass().getClassLoader();
    public ImageIcon logo = new ImageIcon(
            cl.getResource("logo.gif"));
    public ImageIcon ball = new ImageIcon(
            cl.getResource("ball.gif"));
    private JTabbedPane tabbedPane;
    public SocketTestClient client;
    public SocketTestServer server;
    
    /** Creates a new instance of SocketTest */
    public SocketTest() {
        Container cp = getContentPane();
        
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        client = new SocketTestClient(this);
        server = new SocketTestServer(this);
        SocketTestUdp udp = new SocketTestUdp(this);
        About about = new About();
        
        tabbedPane.addTab("Computer Simulation Game", ball, server, "Test any client");
        //tabbedPane.addTab("Client", ball, (Component)client, "Test any server");
        //tabbedPane.addTab("Udp", ball, udp, "Test any UDP Client or Server");
        //tabbedPane.addTab("About", ball, about, "About SocketTest");
        
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        cp.add(tabbedPane);
    }
    
    /**
     * @param args the command line arguments
     */
    public static SocketTest startSocketTest() {
        try {
            UIManager.setLookAndFeel("net.sourceforge.mlf.metouia.MetouiaLookAndFeel");
        } catch(Exception e) {
            //e.printStackTrace();
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch(Exception ee) {
                System.out.println("Error setting native LAF: " + ee);
            }
        }
		
		boolean toSplash = true;
		
		SplashScreen splash = null;
		if(toSplash) splash = new SplashScreen();
        
        SocketTest st = new SocketTest();
        st.setTitle("SocketTest v 3.0.0");
        st.setSize(600,500);
        Util.centerWindow(st);
        st.setDefaultCloseOperation(EXIT_ON_CLOSE);
        st.setIconImage(st.logo.getImage());
        if(toSplash) splash.kill();
        st.setVisible(true);
        
        return st;
    }
    
}

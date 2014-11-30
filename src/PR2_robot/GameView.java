package PR2_robot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import code.State;
	
public class GameView extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel centerPanel;
	private JLabel timeLabel;
	private JLabel titleLabel;
	private JTextPane textPane;
	private static final int NUM_FIRES = 5;
	private String fileBase = "C:\\Users\\julie\\Pictures\\";
	private String fireIntensityFile = fileBase+"fireIntensity";
	private String fireNameFile = fileBase+"fireName";
	private ImageIcon[] intensityImages;
	private JPanel stateView;
	private JButton nextButton;
	private boolean nextClicked;
	private JButton startRound;
	private boolean startRoundClicked;
	private JTextPane teammate;
	public boolean titleView = true;
    public GameView() {
    	titleLabel = new JLabel();
        nextButton = new JButton("Next");
        startRound = new JButton("Start Round!");
        initTitleGUI("start");
        
        addMouseListener(new MouseAdapter(){
		    public void mousePressed(MouseEvent e){
		    	System.out.println("event "+e);
		    	if(e.getButton() == 3){
		    		System.out.println("button 3 clicked");
		    		if(nextClicked == false && nextButton.isEnabled() && !titleView){
		    			nextClicked = true;
		    			System.out.println("setting nextClicked to true");
		    		}
		    		if(startRoundClicked == false && startRound.isEnabled() && titleView){
		    			startRoundClicked = true;
		    			System.out.println("setting startRoundClicked to true");
		    		}
		    	}
		    }
        });
    }
    
    public void initTitleGUI(String title){
    	titleView = true;
    	getContentPane().removeAll();
    	getContentPane().repaint();
    	setLayout(new BorderLayout());
    	
    	JPanel topPanel = new JPanel();
    	topPanel.setPreferredSize(new Dimension(120,150));
    	
    	JPanel panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    	panel.setAlignmentY(Component.CENTER_ALIGNMENT);
    	
    	JLabel icon = new JLabel("", SwingConstants.CENTER);
    	ImageIcon pic = null;
    	if(title.equals("start"))
    		pic = new ImageIcon(fileBase+"start.jpg");
    	else if(title.equals("congrats"))
    		pic = new ImageIcon(fileBase+"congrats.jpg");
    	else if(title.equals("end"))
    		pic = new ImageIcon(fileBase+"end.jpg");
    	icon.setIcon(pic);  	
    	icon.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    	icon.setAlignmentY(JLabel.CENTER_ALIGNMENT);
        panel.add(icon);
        
        JPanel inBetween = new JPanel();
        inBetween.setPreferredSize(new Dimension(50,100));
        panel.add(inBetween);
        
        startRound.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        startRound.setAlignmentX(Component.CENTER_ALIGNMENT);
        startRound.setAlignmentY(Component.CENTER_ALIGNMENT);
        startRound.setEnabled(false);
        //startRound.addActionListener(new StartRoundListener());
        panel.add(startRound);
        
        JPanel bottomPanel = new JPanel();
    	bottomPanel.setPreferredSize(new Dimension(120,150));
    	
        add(topPanel, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        setSize(1800, 900);
        add(bottomPanel, BorderLayout.SOUTH);
        setTitle("Coordinated Fire Extinguishing");
        setVisible(true);
        
    }

    public void initGUI() {
    	titleView = false;
    	setLayout(new BorderLayout());
    	JPanel topPanel = new JPanel();
    	topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
    	//titleLabel.setText(Main.title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 50));
        titleLabel.setForeground(Color.BLUE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(titleLabel);

        timeLabel = new JLabel("Time Left: ", SwingConstants.CENTER);
        timeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        //centerPanel.add(timeLabel);//, BorderLayout.NORTH);
        topPanel.add(timeLabel);
        
    	add(topPanel, BorderLayout.NORTH);

        centerPanel = new JPanel();
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));     
        centerPanel.setSize(70, 70);
        add(centerPanel, BorderLayout.CENTER);
        
        intensityImages = new ImageIcon[NUM_FIRES];
        for(int i=0; i < NUM_FIRES; i++){
        	//System.out.println(fireIntensityFile+i+".png");
        	intensityImages[i] = new ImageIcon(fireIntensityFile+i+".png");
        }
  
        JLabel firesLabel = new JLabel("Fire Intensities:", SwingConstants.CENTER);
        firesLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 35));
        //firesLabel.setForeground(Color.RED);
        firesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(firesLabel);
        
        stateView = new JPanel();
        //stateView.setSize(50, 50);
        updateState(new State(new int[]{0,0,0,0,0}));    
        centerPanel.add(stateView);//, BorderLayout.CENTER);
        
        JPanel fireNamesPanel = new JPanel();
        fireNamesPanel.setBackground(Color.WHITE);
        //fireNames.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 50));
        //fireNames.setForeground(Color.RED);
        //fireNames.setAlignmentX(Component.CENTER_ALIGNMENT);
        for(int i=0; i < NUM_FIRES; i++){
        	JLabel label = new JLabel();
        	label.setIcon(new ImageIcon(fireNameFile+i+".png"));
        	fireNamesPanel.add(label);
        }
        centerPanel.add(fireNamesPanel);
        
        teammate = new JTextPane();
        teammate.setContentType("text/plain");
        teammate.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        teammate.setEditable(false);
        teammate.setPreferredSize(new Dimension(1800,130));
        
        StyledDocument doc = teammate.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        teammate.setAlignmentX(CENTER_ALIGNMENT);
        centerPanel.add(teammate);
        
        //burnedDownMessage = new JLabel("", SwingConstants.CENTER);
        //burnedDownMessage.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        //burnedDownMessage.setForeground(Color.RED);
        //burnedDownMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        //centerPanel.add(burnedDownMessage); 
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        //textLabel = new JLabel("", SwingConstants.CENTER);
        textPane = new JTextPane();
        textPane.setContentType("text/plain");
        textPane.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        textPane.setEditable(false);
        
        doc = textPane.getStyledDocument();
        center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        
        //textPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomPanel.add(textPane);//, BorderLayout.SOUTH);
        
        nextButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        nextButton.setAlignmentX(CENTER_ALIGNMENT);
        nextButton.setEnabled(false);
        //nextButton.addActionListener(new NextButtonListener());
        bottomPanel.add(nextButton);

        add(bottomPanel, BorderLayout.SOUTH);
        
        setSize(1800, 900);
        setTitle("Coordinated Fire Extinguishing");
        //setResizable(false);
        //setLocationRelativeTo(null);
        //setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }
    
    public void waitingForTeammate(boolean waiting){
    	if(waiting)
    		teammate.setText("Waiting for teammate...");
    	else
    		teammate.setText("");
    }
    
    public void setTeammateText(String text){
    	teammate.setText(""+text);
    }
    
    public void addTeammateText(String text){
    	teammate.setText(teammate.getText()+"\n"+text);
    }
    
    public void setNextEnable(boolean enable){
    	nextButton.setEnabled(enable);
    }
    
    public void setStartRoundEnable(boolean enable){
    	startRound.setEnabled(enable);
    }
    
    public void waitForNextClick() {
    	while(!nextClicked){
			System.out.print("");
        }
		nextButton.setEnabled(false);
		nextClicked = false;
		System.out.println("nextclicked now false");
    }
    
    public void waitForStartRoundClick() {
    	while(!startRoundClicked){
			System.out.print("");
        }
    	System.out.println("startround clicked!");
		startRound.setEnabled(false);
		startRoundClicked = false;
		getContentPane().removeAll();
    	getContentPane().repaint();
		initGUI();
    }

    public void setTitleLabel(String title){
    	titleLabel.setText(""+title);
    }
    
    public void setTime(int timeLeft){
    	if(timeLeft < 0)
    		timeLabel.setText("Time Left: ");
    	else
    		timeLabel.setText("Time Left: "+timeLeft);	
    }
    
    public void updateState(State state) {
    	stateView.removeAll();
        for(int i=0; i < state.stateOfFires.length; i++){
        	JLabel label = new JLabel();
        	label.setIcon(intensityImages[state.stateOfFires[i]]);
        	stateView.add(label);
        }
        stateView.setBackground(Color.WHITE);
    }
    
    public void setAnnouncements(String text){
    	textPane.setText(""+text);
    }
    
    /*public void setBurnedDownMessage(String text){
    	burnedDownMessage.setText(text);
    }*/
    
    /*public class StartRoundListener implements ActionListener { 	
	    public void actionPerformed(ActionEvent e) {
	        //startRoundClicked = true;	        
	    }
    }
    
    public class NextButtonListener implements ActionListener { 	
	    public void actionPerformed(ActionEvent e) {
	        //nextClicked = true;	        
	    }
    }*/
}

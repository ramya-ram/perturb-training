package PR2_robot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import code.Main;
import code.State;
	
public class GameView extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel centerPanel;
	private JLabel timeLabel;
	private JLabel titleLabel;
	private JTextPane textPane;
	private JTextField textField;
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
	public int typeOfExecution;
	
	public String humanMessage;
	
    public GameView(int typeOfExecution) {
    	this.typeOfExecution = typeOfExecution;
    	titleLabel = new JLabel();
        nextButton = new JButton("Next");
        startRound = new JButton("Start Round!");
        initTitleGUI("start");
        
    	if(Main.CURRENT_EXECUTION == Main.SIMULATION_HUMAN){
    		nextButton.addActionListener(new NextButtonListener());
    		startRound.addActionListener(new StartRoundListener());
    	} else if(Main.CURRENT_EXECUTION == Main.ROBOT_HUMAN){  	
    		addMouseListener(new MouseAdapter(){
			    public void mousePressed(MouseEvent e){
			    	System.out.println("event "+e);
			    	int buttonToPress = 3;
			    	if(e.getButton() == buttonToPress){
			    		System.out.println("button "+buttonToPress+" clicked");
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
        
        startRound.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        startRound.setAlignmentX(Component.CENTER_ALIGNMENT);
        startRound.setAlignmentY(Component.CENTER_ALIGNMENT);
        startRound.setEnabled(false);
        //startRound.addActionListener(new StartRoundListener());
        panel.add(startRound);
        
        JPanel bottomPanel = new JPanel();
    	bottomPanel.setPreferredSize(new Dimension(120,150));
    	
        add(topPanel, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        setSize(1600, 850);
        add(bottomPanel, BorderLayout.SOUTH);
        setTitle("Coordinated Fire Extinguishing");
        setVisible(true);
        
    }

    public void initGUI() {
    	titleView = false;
    	getContentPane().removeAll();
    	getContentPane().repaint();
    	setLayout(new BorderLayout());
    	JPanel topPanel = new JPanel();
    	topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
    	//titleLabel.setText(Main.title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        titleLabel.setForeground(Color.BLUE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(titleLabel);

        timeLabel = new JLabel("Time Left: ", SwingConstants.CENTER);
        timeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
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
        firesLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
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
        teammate.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
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
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        //textLabel = new JLabel("", SwingConstants.CENTER);
        textPane = new JTextPane();
        textPane.setContentType("text/plain");
        textPane.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        textPane.setEditable(false);
        
        doc = textPane.getStyledDocument();
        center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        
        //textPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomPanel.add(textPane, BorderLayout.NORTH);//, BorderLayout.SOUTH);
        
        JPanel leftGlue = new JPanel();
        leftGlue.setPreferredSize(new Dimension(50,100));
        bottomPanel.add(leftGlue, BorderLayout.WEST);
        
        JPanel rightGlue = new JPanel();
        rightGlue.setPreferredSize(new Dimension(50,100));
        bottomPanel.add(rightGlue, BorderLayout.EAST);
        
        textField = new JTextField();
        textField.setEnabled(true);
        textField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        textField.setPreferredSize(new Dimension(100,50));
        textField.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	System.out.println("e: "+e+" source: "+e.getSource());
            	String text = textField.getText();
            	if(text.length() > 0){
            		humanMessage = text;
            		textField.setText("");
            	}
            }
        });
        bottomPanel.add(textField, BorderLayout.CENTER);
        
        nextButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        nextButton.setAlignmentX(CENTER_ALIGNMENT);
        nextButton.setEnabled(false);
        //nextButton.addActionListener(new NextButtonListener());
        bottomPanel.add(nextButton, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
        
        setSize(1600, 850);
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
    
    public String getTeammateText(){
    	return teammate.getText();
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
		focusNextButton();

    	while(!nextClicked){
			System.out.print("");
        }
		nextButton.setEnabled(false);
		nextClicked = false;
		System.out.println("nextclicked now false");
    }
    
    public void waitForStartRoundClick() {
		focusStartRound();

    	while(!startRoundClicked){
			System.out.print("");
        }
    	System.out.println("startround clicked!");
		startRound.setEnabled(false);
		startRoundClicked = false;
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
    
    public void focusTextField(){
    	textField.requestFocus();
    }
    
    private void focusNextButton(){
    	nextButton.requestFocus();
    }
    
    private void focusStartRound(){
    	startRound.requestFocus();
    }
    
    /*public void setBurnedDownMessage(String text){
    	burnedDownMessage.setText(text);
    }*/
    
    public class StartRoundListener implements ActionListener { 	
	    public void actionPerformed(ActionEvent e) {
	        startRoundClicked = true;	
	        System.out.println("setting startRound to true");
	    }
    }
    
    public class NextButtonListener implements ActionListener { 	
	    public void actionPerformed(ActionEvent e) {
	        nextClicked = true;	 
	        System.out.println("setting nextClicked to true");
	    }
    }
}

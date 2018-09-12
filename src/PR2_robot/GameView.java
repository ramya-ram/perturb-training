package PR2_robot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import code.State;
import code.MyWorld;
	
/**
 * Java swing GUI of fire extinguishing game
 * Shows state of the fires, communication between teammates, and other task info
 */
public class GameView extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel centerPanel;
	private JLabel timeLabel;
	private JLabel titleLabel;
	private JTextPane announcements;
	private JTextField textField;
	private static final int NUM_FIRES = 5;
	private String fileBase = "data/";
	private String fireIntensityFile = fileBase+"fireIntensity";
	private String fireNameFile = fileBase+"fireName";
	public ImageIcon[] intensityImages;
	public JPanel stateView;
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
        setResizable(false);
        
		nextButton.addActionListener(new NextButtonListener());
		startRound.addActionListener(new StartRoundListener());
    }
    
    /**
     * Creates a title GUI screen with a message specified by the associated jpg image in data/
     */
    public void initTitleGUI(String title){
    	titleView = true;
    	getContentPane().removeAll();
    	getContentPane().repaint();
    	setLayout(new BorderLayout());
    	
    	JPanel topPanel = new JPanel();
    	topPanel.setPreferredSize(new Dimension(120,150));
    	
    	titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(titleLabel);
    	
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
    	else if(title.equals("roundUp"))
    		pic = new ImageIcon(fileBase+"roundUp.jpg");
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

    /**
     * Initializes the main GUI screen that participants view to interact with the simulated robot
     * The particular session, the time, the state of the fires, the actions of both team members, and a text field for input are displayed to allow for interaction
     */
    public void initGUI() {
    	titleView = false;
    	getContentPane().removeAll();
    	getContentPane().repaint();
    	setLayout(new BorderLayout());
    	JPanel topPanel = new JPanel();
    	topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(titleLabel);
        timeLabel = new JLabel("Time Left: ", SwingConstants.CENTER);
        timeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(timeLabel);
        
    	add(topPanel, BorderLayout.NORTH);

        centerPanel = new JPanel();
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));     
        centerPanel.setSize(70, 70);
        add(centerPanel, BorderLayout.CENTER);
        
        intensityImages = new ImageIcon[NUM_FIRES];
        for(int i=0; i < NUM_FIRES; i++){
        	intensityImages[i] = new ImageIcon(fireIntensityFile+i+".png");
        }
  
        JLabel firesLabel = new JLabel("Fire Intensities:", SwingConstants.CENTER);
        firesLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        firesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(firesLabel);
        
        stateView = new JPanel();
        MyWorld.updateState(new State());    
        centerPanel.add(stateView);
        
        JPanel fireNamesPanel = new JPanel();
        fireNamesPanel.setBackground(Color.WHITE);
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
        
        StyledDocument doc = teammate.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        teammate.setAlignmentX(CENTER_ALIGNMENT);
        JScrollPane scrollPaneTeammate = new JScrollPane(teammate);
        scrollPaneTeammate.setPreferredSize(new Dimension(1800,400));
        scrollPaneTeammate.setBorder(null);
        centerPanel.add(scrollPaneTeammate);
      
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        announcements = new JTextPane();
        announcements.setContentType("text/plain");
        announcements.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        announcements.setEditable(false);
        
        doc = announcements.getStyledDocument();
        center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        
        JScrollPane scrollPaneAnnouncements = new JScrollPane(announcements);
        scrollPaneAnnouncements.setPreferredSize(new Dimension(1800,150));
        scrollPaneAnnouncements.setBorder(null);
        bottomPanel.add(scrollPaneAnnouncements, BorderLayout.NORTH);
        
        JPanel leftGlue = new JPanel();
        leftGlue.setPreferredSize(new Dimension(50,100));
        
        JPanel rightGlue = new JPanel();
        rightGlue.setPreferredSize(new Dimension(50,100));
        
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
        bottomPanel.add(nextButton, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
        
        setSize(1600, 850);
        setTitle("Coordinated Fire Extinguishing");
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
    
    public void setTextFieldEnable(boolean enable){
    	textField.setEnabled(enable);
    }
    
    public void setNextEnable(boolean enable){
    	nextButton.setEnabled(enable);
    }
    
    public void setStartRoundEnable(boolean enable){
    	startRound.setEnabled(enable);
    }
    
    /**
     * Wait for the next button to be clicked before moving on to the next time step
     */
    public void waitForNextClick() {
		focusNextButton();

    	while(!nextClicked){
			System.out.print("");
        }
		nextButton.setEnabled(false);
		nextClicked = false;
		System.out.println("nextclicked now false");
    }
    
    /**
     * Wait for the start round button to be clicked before moving on to the next interaction round
     */
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

    public void setTitleAndRoundLabel(String title, int roundNum, Color color){
    	if(title.length() > 0)
    		titleLabel.setText(""+title+" (Round "+roundNum+")");
    	else
    		titleLabel.setText("");
    	titleLabel.setForeground(color);
    }
    
    public void setTime(int timeLeft){
    	if(timeLeft < 0)
    		timeLabel.setText("Time Left: ");
    	else
    		timeLabel.setText("Time Left: "+timeLeft);	
    }
    
    public void setAnnouncements(String text){
    	announcements.setText(""+text);
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

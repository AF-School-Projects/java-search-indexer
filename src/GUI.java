import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.FontUIResource;

/**
 * 
 * GUI for the search engine simulator back-end {@link Indexer}
 * 
 * @author Alex Feaser 2019
 *
 */
public class GUI {

	private JFrame frmHello;
	private JTextField textField;
	private Indexer indexer;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frmHello.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		indexer = new Indexer();
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmHello = new JFrame();
		frmHello.setTitle("Java Search Engine");
		frmHello.setBounds(100, 100, 720, 500);
		frmHello.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmHello.getContentPane().setLayout(new BorderLayout(0, 0));
		frmHello.setLocationRelativeTo(null);

		UIManager.put("TextField.font", new FontUIResource(new Font("Arial", Font.BOLD, 12)));
		
		JPanel panel = new JPanel();
		JButton btnSearch = new JButton("Search");
		
		textField = new JTextField();
		textField.addActionListener(e -> btnSearch.doClick());
		textField.setColumns(25);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		JEditorPane textPane = new JEditorPane();
		textPane.setContentType("text/html");
		textPane.setEditable(false);
		textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		scrollPane.setViewportView(textPane);
		frmHello.getContentPane().add(panel, BorderLayout.NORTH);
		frmHello.getContentPane().add(scrollPane, BorderLayout.CENTER);
		panel.add(textField);
		panel.add(btnSearch);
		
		btnSearch.addActionListener(e -> {
			StringBuilder sb = new StringBuilder();
			List<Pair<Document, Double>> res = indexer.queryCorpus(textField.getText());
			if (!res.isEmpty())
				sb.append(res.size()).append(" results found.<br><br>");
			res.forEach(pair -> sb.append("<a href=\"").append(pair.first.path).append("\"><b>")
					.append(new File(pair.first.path).getName()).append("</b></a>  (").append(String.format("%.2f%% relevance", pair.second * 100)).append(")<br>")
					.append("\"").append(indexer.snippet(pair.first, textField.getText().trim(), 100)).append("\"<br><br>"));
			textPane.setText(center(res.isEmpty() ? "No results found" : sb.toString()));
			textPane.setCaretPosition(0);
		});
		
		textPane.addHyperlinkListener(he -> {
	        if(he.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
	        	try {
					Desktop.getDesktop().edit(new File(he.getDescription()));
				} catch (IOException e) { e.printStackTrace(); }
		});
	}
	
	private String center(String s) { return "<center>" + s + "</center>"; }
}

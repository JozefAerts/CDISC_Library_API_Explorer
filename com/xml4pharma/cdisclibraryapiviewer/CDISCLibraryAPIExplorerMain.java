package com.xml4pharma.cdisclibraryapiviewer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.io.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.json.JSONObject;

/** Main class for the CDISC Library API Viewer  */
public class CDISCLibraryAPIExplorerMain {
	
	private String baseURL = "https://api.library.cdisc.org/api";
	private String apiKey = null;
	private String startRequest = "/mdr/products";
	private int textFieldWidth = 80;
	// GUI
	private JPanel panel;
	private JLabel baseLabel;
	private GridBagConstraints gbc;
	private JLabel baseAPILabel;
	private JLabel apiKeyLabel;
	private JTextField apiKeyTextField;
	private JLabel requestStringLabel;
	private JTextField requestStringField;
	private JButton priorRequestButton;
	private JButton nextRequestButton;
	private JLabel responseTypeLabel;
	private JPanel responseTypePanel;
	private JRadioButton jsonRadioButton;
	private JRadioButton xmlRadioButton;
	private JLabel responseLabel;
	private JLabel explainClickLabel;
	private JTextPane responseTextPane;
	//private JButton searchButton;
	private JButton resetButton;
	private JButton goButton;
	private JScrollPane sp;
	private JLabel searchLabel;
	private SearchPanel searchPanel;
	//private JLabel copyPasteLabel;
	
	private int stackIndex = 0;
	
	// extension 2025-05-20
	private HashMap<String,String> apiChoices;
	private JList<String> apiChoicesList;
	
	// TODO: have a stack of last used queries
	private ArrayList<String> stack;
	
	public CDISCLibraryAPIExplorerMain() {
		// first look whether an API key can be found in the "apikey.dat" file
		apiKey = readAPIKeyFromFile();
		System.out.println("API key = " + apiKey);
		setupApiChoicesHashMap();
		Vector<String> choiceNames = new Vector<String>();
		for (Entry<String, String> entry : apiChoices.entrySet()) {
		    String key = entry.getKey();
		    String value = entry.getValue();  // will be used later
		    choiceNames.add(key);
		}
		System.out.println("choiceNames = " + choiceNames);
		apiChoicesList = new JList<String>(choiceNames);
		apiChoicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		apiChoicesList.setSelectedIndex(0);
		// set up a stack for keeping history
		stack = new ArrayList<String>();
		// GUI 
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;  // 2025-09-29
		//
		gbc.insets = new Insets(5,5,5,5);
		gbc.fill = GridBagConstraints.BOTH;
		// first row
		baseAPILabel = new JLabel("Base: ");
		baseAPILabel.setForeground(Color.blue);
		panel.add(baseAPILabel, gbc);
		gbc.gridx++;
		baseLabel = new JLabel(baseURL);
		panel.add(baseLabel, gbc);
		// second row
		gbc.gridx = 0; gbc.gridy++;
		apiKeyLabel = new JLabel("API-key: ");
		apiKeyLabel.setForeground(Color.blue);
		panel.add(apiKeyLabel, gbc);
		if(apiKey == null) {
			gbc.gridx++;
			apiKeyTextField = new JTextField(textFieldWidth);
			panel.add(apiKeyTextField, gbc);
		} else {
			gbc.gridx++;
			JLabel apiKeyReadFromFileLabel = new JLabel("API key was read from the file apikey.dat");
			panel.add(apiKeyReadFromFileLabel, gbc);
		}
		// 2025-06-20
		gbc.gridx = 0; gbc.gridy++;
		JLabel endPointLabel = new JLabel("Standard for API");
		panel.add(endPointLabel, gbc);
		gbc.gridx++;
		//String[] data = {"one", "two", "three", "four"};
		//apiChoicesList = new JList<String>(data);
		//JList<String> testList = new JList<String>(data);
		JScrollPane sp = new JScrollPane(apiChoicesList);
		panel.add(apiChoicesList, gbc); 
		apiChoicesList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				selectedStandardChanged();
			}			
		});
		// next row
		gbc.gridx = 0; gbc.gridy++;
		requestStringLabel = new JLabel("Request string: ");
		requestStringLabel.setForeground(Color.blue);
		panel.add(requestStringLabel, gbc);
		gbc.gridx++;
		requestStringField = new JTextField(textFieldWidth);
		panel.add(requestStringField, gbc);
		requestStringField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// on "Enter" start the process
				executeQuery();
			}
		});
		// 2025-06-20: for pasting text
		AbstractDocument document = (AbstractDocument) requestStringField.getDocument();
		document.setDocumentFilter(new DocumentFilter() {
			public void replace(FilterBypass fb, int offs, int length,
                    String str, AttributeSet a) throws BadLocationException {
                String text = fb.getDocument().getText(0, fb.getDocument().getLength());
                System.out.println("replace pasted text = " + text);
                if(str.trim().startsWith("/mdr/specializations")) {
                	str = "/cosmos/v2" + str.trim();
                } else if(str.trim().startsWith("mdr/specializations")) {
                	str = "/cosmos/v2/" + str.trim();
                } else if(str.trim().startsWith("/mdr/bc")) {
                	str = "/cosmos/v2" + str.trim();
                } else if(str.trim().startsWith("mdr/bc")) {
                	str = "/cosmos/v2/" + str.trim();
                }
                //System.out.println("replacement string = " + str);
                super.replace(fb, offs, length, str, a);
			}
			public void insertString(FilterBypass fb, int offs, String str,
                    AttributeSet a) throws BadLocationException {
				/* 
                String text = fb.getDocument().getText(0, fb.getDocument().getLength());
                System.out.println("insert pasted text = " + text); */
			}
            
		});
		// set an initial request string
		requestStringField.setText(startRequest);
		// 2022-12-08
		JPanel emptyPanel = new JPanel();
		gbc.gridx = 0; gbc.gridy++;
		panel.add(emptyPanel, gbc);
		JPanel priorNextQueryPanel = new JPanel();
		priorRequestButton = new JButton("<");
		priorRequestButton.setToolTipText("Previous API request");
		priorNextQueryPanel.add(priorRequestButton);
		nextRequestButton = new JButton(">");
		nextRequestButton.setToolTipText("Next API request");
		priorNextQueryPanel.add(nextRequestButton);
		gbc.gridx++;
		panel.add(priorNextQueryPanel, gbc);
		priorRequestButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String priorRequestString = getPriorRequest();
				if(priorRequestString != null) {
					// reset the text
					requestStringField.setText(priorRequestString);
					// execute the request
					executeQuery();
				}
			}
		});
		nextRequestButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String nextrRequestString = getNextRequest();
				if(nextrRequestString != null) {
					// reset the text
					requestStringField.setText(nextrRequestString);
					// execute the query
					executeQuery();
				}
			}
		});
		// choice between JSON and XML
		gbc.gridx = 0; gbc.gridy++;
		responseTypeLabel = new JLabel("<html>Response<br/>Type:</html> ");
		responseTypeLabel.setForeground(Color.blue);
		panel.add(responseTypeLabel, gbc);
		responseTypePanel = new JPanel();
		jsonRadioButton = new JRadioButton("JSON          ");
		xmlRadioButton = new JRadioButton("XML");
		responseTypePanel.add(jsonRadioButton);
		responseTypePanel.add(xmlRadioButton);
		gbc.gridx++;
		panel.add(responseTypePanel, gbc);
		// button grouping
		jsonRadioButton.setSelected(true);
		ButtonGroup bg = new ButtonGroup();
		bg.add(jsonRadioButton); bg.add(xmlRadioButton);
		// 
		// reset button
		gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.BOTH;
		resetButton = new JButton("Reset");
		resetButton.setBackground(Color.green);
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resetSearch();
			}
		});
		panel.add(resetButton, gbc);
		// execution button
		gbc.gridx++;
		goButton = new JButton("Go!");
		goButton.setBackground(Color.CYAN);
		panel.add(goButton, gbc);
		// listener
		goButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				executeQuery();
			}
		});
		String explainClickText = "<html>In the response, <font color=\"blue\">left-click</font> a line with a <font color=\"red\">reference</font> to copy the reference value to the textfield, <font color=\"blue\">right-click</font> it to execute it immediately.<br/>"
				+ "Alternatively, you can <font color=\"blue\">copy-paste</font> (using CTRL-C, CTRL-V) the value of an <font color=\"red\">href</font> into the <font color=\"blue\">Request string</font> textfield,<br/>" 
						+ "in order to further navigate through the CDISC Library using the API and obtain new information and links.";
		explainClickLabel = new JLabel(explainClickText);
		gbc.gridx = 0; gbc.gridy++; 
		panel.add(new JLabel(), gbc);  // empty place in grid
		gbc.gridx++;
		panel.add(explainClickLabel, gbc);
		// The response 
		gbc.gridx = 0; gbc.gridy++;
		// Using a separate JPanel
		responseLabel = new JLabel("Response: ");
		responseLabel.setForeground(Color.blue);
		responseTextPane = new JTextPane();
		responseTextPane.setEditable(false);
		responseTextPane.setMinimumSize(new Dimension(300,500));
		responseTextPane.setPreferredSize(new Dimension(300,500));
		// 2026-08-22: allow fast navigation by mouse-click
		responseTextPane.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		    	System.out.println("MouseEvent e = " + e);
		        int pos = responseTextPane.viewToModel(e.getPoint());
		        try {
		            Document doc = responseTextPane.getDocument();
		            String text = doc.getText(0, doc.getLength());
		            int lineStart = text.lastIndexOf('\n', pos - 1) + 1;

		            int lineEnd = text.indexOf('\n', pos);
		            if (lineEnd == -1) {
		                lineEnd = text.length();
		            }
		            String lineText = text.substring(lineStart, lineEnd);
		            lineText = lineText.trim();
		            System.out.println("trimmed line: " + lineText);
		            String reference = null;
		            // remove opening and closing quotes, and comma at the end, when present
		            if(lineText.startsWith("\"href\": ") == false) return;  // do nothing when it is not an "href" line
		            reference = lineText.substring(7).trim();
		            if(reference.startsWith("\"")) reference = reference.substring(1);
		            if(reference.endsWith(",")) reference = reference.substring(0,reference.length()-1);
		            if(reference.endsWith("\"")) reference = reference.substring(0,reference.length()-1);
		            System.out.println("reference = " + reference);
		            // left click: copy to the textfield
		            // right-click: copy to the textfield and execute immediately
		            if (SwingUtilities.isLeftMouseButton(e)) {
		            	requestStringField.setText(reference);
		            } else if (SwingUtilities.isRightMouseButton(e)) {
		            	requestStringField.setText(reference);
		            	goButton.doClick();  // programmically click the button
		            }
		        }
		        catch (BadLocationException ex) {
		            ex.printStackTrace();
		        }
		    }
		});
		//
		sp = new JScrollPane(responseTextPane);
		JPanel responsePanel = new JPanel();
		responsePanel.setLayout(new BorderLayout());
		responsePanel.add(responseLabel, BorderLayout.WEST);
		responsePanel.add(sp, BorderLayout.CENTER);
		gbc.gridwidth = 2; 
		// 2025-09-29: take care that response panel is never allocated a minimal size
		gbc.weightx = 1; gbc.weighty = 1;
		panel.add(responsePanel, gbc);
		// 2022-10-11
		/* gbc.gridx = 1; gbc.gridy++; gbc.fill = GridBagConstraints.CENTER;
		searchButton = new JButton("Search in Response");
		searchButton.setBackground(Color.orange);
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchInResponse();
			}
		});
		panel.add(searchButton, gbc); */
		gbc.weightx = 0; gbc.weighty = 0;  // reset weights
		// 2022-10-12
		
		searchLabel = new JLabel("<html>Search in<br/> Response: </html>");
		searchLabel.setForeground(Color.blue);
		gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		panel.add(searchLabel, gbc);
		searchPanel = new SearchPanel(responseTextPane, xmlRadioButton);
		gbc.gridx++; 
		panel.add(searchPanel, gbc);
		// 
		/* copyPasteLabel = new JLabel("<html>From the response, you can <font color=\"blue\">copy-paste</font> (using CTRL-C, CTRL-V) the value of an <font color=\"red\">href</font> into the <font color=\"blue\">Request string</font> textfield,<br/>" 
				+ "in order to further navigate through the CDISC Library using the API and obtain new information and links."
				+ "</html>");
		gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
		panel.add(copyPasteLabel, gbc); */
		// Putting everything together
		JFrame f = new JFrame();
		f.setTitle("CDISC Library API Explorer");
		f.add(panel);
		f.pack();
		f.setResizable(true);  // 2025-09-29: set to false
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	private String getPriorRequest() {
		String priorRequestString = null;
		if(stackIndex > 0) {
			priorRequestString = stack.get(stackIndex-1);
			stackIndex--;
			printStackIndex();
		}
		return priorRequestString;
	}
	
	private String getNextRequest() {
		String nextRequestString = null;
		if(stackIndex < stack.size()-1) {
			nextRequestString = stack.get(stackIndex+1);
			stackIndex++;
			printStackIndex();
		}
		return nextRequestString;
	}
	
	/** Action for when the user changed the standard to be used for the API */
	private void selectedStandardChanged() {
		String selectedStandard = apiChoicesList.getSelectedValue();
		String requestString = apiChoices.get(selectedStandard);
		if(requestString != null) requestStringField.setText(requestString);
		// 2025-06-21: XML is only available for the "CDISC Standards",
		// Not for Biomedical Concepts or Dataset Specializations
		if(selectedStandard.equals("CDISC Standards") == false) jsonRadioButton.setSelected(true);
	}
	
	private void executeQuery() {
		String response = "";
		responseTextPane.setText("Executing ...");
		responseTextPane.revalidate();
		BasicCDISCLibraryClient client = new BasicCDISCLibraryClient(apiKey);
		String requestString = requestStringField.getText().trim();
		stack.add(requestString);  //stackIndex++; // 2022-12-08
		stackIndex = stack.size() - 1; // put on top
		printStackIndex();
		System.out.println("Executing request = " + requestString);
		if(jsonRadioButton.isSelected()) {
			response = client.getCDISCLibraryJSON(requestString);
			if(client.getResponseStatus() == 200) {
				// format the JSON
				JSONObject json = new JSONObject(response); // Convert text to object
				//System.out.println(json.toString(4)); // Print it with specified indentation
				response = json.toString(4);
			} else if(client.getResponseStatus() == 401) {
				response = "Invalid API key or no API key provided";
			} else {
				response = "HTTP Response = " + response;
			}
		} else {
			response = client.getCDISCLibraryXML(requestString);
			if(client.getResponseStatus() == 200) {
				// pretty-print
				response = prettyPrintByTransformer(response,4,false);
			} else if(client.getResponseStatus() == 401) {
				response = "Invalid API key or no API key provided";
			} else {
				System.out.println("Failure response = " + response);
				response = "HTTP Response Status = " + client.getResponseStatus();
			}
		}
		responseTextPane.setText(response);
		// HighLighting
		if(client.getResponseStatus() == 200) {
			if(jsonRadioButton.isSelected()) {  // highlighter for JSON
				highlightJSON(responseTextPane);
			} else {
				highlightXML(responseTextPane);  // highlighter for XML
			}
		}
		responseTextPane.setCaretPosition(0);
	}
	
	// for testing only
	private void printStackIndex() {
		System.out.println("stackIndex = " + stackIndex);
	}
	
	/** Reset */
	private void resetSearch() {
		requestStringField.setText("/mdr/products");
		responseTextPane.setText("");
	}
	
	private void highlightJSON(JTextComponent component) {
		MyHighlightPainter highlighter = new MyHighlightPainter(Color.yellow);
		String pattern = "\"href\"";
	    try {
	    	Document doc = component.getDocument();
	    	// Get the documents highlighter and remove any existing highlighting
	    	Highlighter high = responseTextPane.getHighlighter();
	    	high.removeAllHighlights();
	        //String text = component.getText(0, doc.getLength());
	        // int pos = component.getCaretPosition();
	    	int pos = 0;
	        //boolean found = false;
	        int findLength = pattern.length();
	        // Rest the search position if we're at the end of the document
	        //if (pos + findLength > doc.getLength()) {
	        //    pos = 0;
	        //}
	        while (pos + findLength <= doc.getLength()) {
	            // Extract the text from the docuemnt
	            String match = doc.getText(pos, findLength).toLowerCase();
	            // Check to see if it matches or request
	            if (match.equals(pattern)) {
	                //found = true;
	                //break;
	            	high.addHighlight(pos, pos+pattern.length(), highlighter);
	            	// We will NOT highlight the reference string, as this makes it harder to copy-paste
	            	// now find the next start of "/mdr"
	            	/* pos = pos + 6;
	            	boolean foundReference = false;
	            	String reference = "";
	            	String searchString = "/mdr";
	            	while(foundReference == false) {
	            		String s = doc.getText(pos, searchString.length());
	            		if(s.equals(searchString)) {
	            			foundReference = true;
	            		} else {
	            			pos++;
	            		}	
	            	}
	            	System.out.println("foundReference = " + foundReference + " - position = " + pos);
	            	if(foundReference == true) {
	            		high.addHighlight(pos, pos+searchString.length(), highlighter);
	            	} */
	            }
	            pos++;
	        }        
	    } catch (BadLocationException e) {
	    	e.printStackTrace();
	    }
	}
	
	private void highlightXML(JTextComponent component) {
		MyHighlightPainter highlighter = new MyHighlightPainter(Color.yellow);
		String pattern = "<href>";
	    try {
	    	Document doc = component.getDocument();
	    	// Get the documents highlighter and remove any existing highlighting
	    	Highlighter high = responseTextPane.getHighlighter();
	    	high.removeAllHighlights();
	        //String text = component.getText(0, doc.getLength());
	        // int pos = component.getCaretPosition();
	    	int pos = 0;
	        //boolean found = false;
	        int findLength = pattern.length();
	        // Rest the search position if we're at the end of the document
	        //if (pos + findLength > doc.getLength()) {
	        //    pos = 0;
	        //}
	        while (pos + findLength <= doc.getLength()) {
	            // Extract the text from the document
	            String match = doc.getText(pos, findLength).toLowerCase();
	            // Check to see if it matches or request
	            if (match.equals(pattern)) {
	                //found = true;
	                //break;
	            	high.addHighlight(pos, pos+pattern.length(), highlighter);
	            	// We will NOT highlight the reference string, as this makes it harder to copy-paste
	            	// now find the next start of "/mdr"
	            	/* pos = pos + 6;
	            	boolean foundReference = false;
	            	String reference = "";
	            	String searchString = "/mdr";
	            	while(foundReference == false) {
	            		String s = doc.getText(pos, searchString.length());
	            		if(s.equals(searchString)) {
	            			foundReference = true;
	            		} else {
	            			pos++;
	            		}	
	            	}
	            	System.out.println("foundReference = " + foundReference + " - position = " + pos);
	            	if(foundReference == true) {
	            		high.addHighlight(pos, pos+searchString.length(), highlighter);
	            	} */
	            }
	            pos++;
	        }        
	    } catch (BadLocationException e) {
	    	e.printStackTrace();
	    }
	}
	
	/** XML formatter - see https://www.baeldung.com/java-pretty-print-xml */ 
	public static String prettyPrintByTransformer(String xmlString, int indent, boolean ignoreDeclaration) {

	    try {
	        //InputSource src = new InputSource(new StringReader(xmlString));
	        //Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);
	    	// Avoiding to use a DOM ...
	    	Source src = new StreamSource(new StringReader(xmlString));     
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        transformerFactory.setAttribute("indent-number", indent);
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

	        Writer out = new StringWriter();
	        //transformer.transform(new DOMSource(document), new StreamResult(out));
	        transformer.transform(src, new StreamResult(out));
	        return out.toString();
	    } catch (Exception e) {
	        throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
	    }
	}
	
	

	/** Reads the CDISC Library API-key from the file apikey.dat  */
	private String readAPIKeyFromFile() {
		System.out.println("Trying to read the API key from the file apikey.dat");
		//BufferedReader br = null; 
		String apiKey = null;
		try {
	         File file = new File("apikey.dat");
	         FileReader fr = new FileReader(file);
	         BufferedReader br = new BufferedReader(fr);
	         String line = "";
	         //String[] tempArr;
	         while((line = br.readLine()) != null) {
	            if(line.trim().startsWith("#") == false) {  // skip comment lines that have a # in it
	            	// only take the first part
	            	String[] parts = line.split(" ");
	            	if(parts.length > 0 ) return parts[0];
	            }
	         }
	         br.close();
	     } catch(IOException ioe) {
	            ioe.printStackTrace();
	     }
		 return apiKey;
	}
	
	// 2025-06-20
	/** Set up a HashMap with choices which base to use  */
	private void setupApiChoicesHashMap() {
		apiChoices = new HashMap<String,String>();
		apiChoices.put("CDISC Standards", "/mdr/products");
		apiChoices.put("CDISC Biomedical Concepts", "/cosmos/v2/mdr/bc/biomedicalconcepts");
		apiChoices.put("CDISC SDTM Dataset Specializations", "/cosmos/v2/mdr/specializations/datasetspecializations");
	}
	
	/** HighLighter for texts */
	protected class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
		public MyHighlightPainter(Color color) {
		    super(color);
		}
	}
	
	
	
	/** Pasting in the textfield with the request */
	private class MyDocumentListener implements DocumentListener {
	    public void changedUpdate(DocumentEvent e) {
	    	// do nothing
	    }

	    public void insertUpdate(DocumentEvent e) {
	        Document document = e.getDocument();
	        try {
	            String s = document.getText(0, document.getLength());
	            // we will try to correct typical paste errors
	            if(s.startsWith("/mdr/specializations")) {
	            	//requestStringField.setText("/cosmos/v2" + s);
	            	//document.remove(0, document.getLength());
	            	removeUpdate(e);
	            } else if(s.startsWith("mdr/specializations")) {
	            	requestStringField.setText("/cosmos/v2/" + s);
	            }

	        } catch (BadLocationException e1) {
	            e1.printStackTrace();
	            return;
	        }

	    }

	    public void removeUpdate(DocumentEvent e) {
	    	// do nothing
	    }
	}
	
	
	
	public static void main(String[] args) {
		CDISCLibraryAPIExplorerMain explorer = new CDISCLibraryAPIExplorerMain();

	}

}

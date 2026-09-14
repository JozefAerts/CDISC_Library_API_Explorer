package com.xml4pharma.cdisclibraryapiviewer;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import java.awt.*;
import java.awt.event.*;
import java.util.*;


/**
 * A search panel for searching in an HTML document
 * @author Jozef Aerts
 *
 */

public class SearchPanel extends JPanel {
		
	private GridBagLayout gbl;
	private GridBagConstraints gbc;
	private JLabel searchLabel;
	private JTextField searchField;
	private JButton clearButton;
	private JButton nextButton;
	private JCheckBox caseSensitiveCheckBox;
	private JCheckBox wholeWordCheckBox;
	//private JPanel parentPanel;
	private javax.swing.text.Document doc;  // The document from the JTextPane
	private String searchText = null;
	//private String previousSearchText = null;
	private JTextPane textPane;
	// we need to have access to the "XML radiobutton"
	// searching
	private String priorText = null;
	private int lastPos = 0;
	private boolean isCaseSensitive = false;
	private boolean isWholeWord = false;
	private JRadioButton xmlRadioButton;
	private boolean usesXML = false;  // whether XML is used instead of JSON
	
	public SearchPanel(JTextPane textPane, JRadioButton xmlRadioButton) {
		this.textPane = textPane;
		this.xmlRadioButton = xmlRadioButton;
		//pr = new DOMPrinter();
		gbl = new GridBagLayout();
		gbc = new GridBagConstraints();
		this.setLayout(gbl);
		// add elements
		searchLabel = new JLabel("Search: ");
		// todo : BOLD
		gbc.gridx = 0; gbc.gridy = 0;
		this.add(searchLabel, gbc);
		//
		searchField = new JTextField(20);
		gbc.gridx++;
		this.add(searchField,gbc);
		//		 
		nextButton = new JButton("Next");
		gbc.gridx++;
		this.add(nextButton, gbc);
		nextButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				searchAction(e);
			}
		});
		//
		clearButton = new JButton("Clear");
		gbc.gridx++;
		this.add(clearButton, gbc);
		// actions of "next" and "previous" button
		clearButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				clearAction(e);
			}
		});
		// caseSensitiveCheckBox
		caseSensitiveCheckBox = new JCheckBox("Case Sensitive");
		// 2022-10-12: reset upon check/uncheck
		caseSensitiveCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetSearch();
			}
		});
		gbc.gridx++;
		this.add(caseSensitiveCheckBox, gbc);
		wholeWordCheckBox = new JCheckBox("Whole word");
		// 2022-10-12: reset upon check/uncheck
		wholeWordCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetSearch();
			}
		});
		gbc.gridx++;
		this.add(wholeWordCheckBox, gbc);
	}
	
	/** Reset the search action to start from the top */
	private void resetSearch() {
		textPane.setCaretPosition(0);
	}
	
	// Action of the "Search" button
	private void searchAction(ActionEvent e) {	
		// TODO: Next vs. previous
		searchText = searchField.getText().trim();
		System.out.println("Starting searching for text = " + searchText);
		isCaseSensitive = caseSensitiveCheckBox.isSelected();
		isWholeWord = wholeWordCheckBox.isSelected();
		usesXML = xmlRadioButton.isSelected();
		// new search text
		//System.out.println("searchText = " + searchText + " - priorText = " + priorText);
		if(priorText != null && searchText.equalsIgnoreCase(priorText) == false) {
			System.out.println("Resetting caret position");
			textPane.setCaretPosition(0);
			priorText = searchText;
		} else {
			priorText = searchText;
		}
		//
		doc = textPane.getDocument();
		// now look for the text, starting from the current position
		highLight(textPane, searchText);
		// remember the last search string
		//priorText = searchText;
	}
	
	public void highLight(JTextComponent component, String pattern) {
	    try {
	        Document doc = component.getDocument();
	        //String text = component.getText(0, doc.getLength());
	        int pos = component.getCaretPosition();
	        //System.out.println("caret position = " + pos);
	        boolean found = false;
	        int findLength = pattern.length();
	        // Rest the search position if we're at the end of the document
	        if (pos + findLength > doc.getLength()) {
	            pos = 0;
	        }
	        while (pos + findLength <= doc.getLength()) {
	        	//System.out.println("Searching from position = " + pos);
	            // Extract the text from teh docuemnt
	            //String match = doc.getText(pos, findLength).toLowerCase();
	            String match = doc.getText(pos, findLength);
	            // Check to see if it matches or request
	            if(isCaseSensitive == true) {
	            	if(isWholeWord == true) {
	            		found = searchWholeWord(pos, pattern);
	            		if(found == true) {
	            			pos++;
	            			break;
	            		}
	            	} else {
			            if (match.equals(pattern)) {
			                found = true;
			                break;
			            }
	            	}
	            } else {   // case-INSENSITIVE
	            	if(isWholeWord == true) {
	            		found = searchWholeWord(pos, pattern);
	            		if(found == true) {
	            			pos++;  // skip first character, e.g. the blank
	            			break;
	            		}        
	            	} else {
	            		if(match.equalsIgnoreCase(pattern)) {
		            		found = true;
		            		break;
		            	}
	            	}
	            	
	            }
	            pos++;
	        }  // end of "while"
	        // System.out.println("found = " + found + " caret position = " + textPane.getCaretPosition());
	        if (found == true) {
	            component.setSelectionStart(pos);
	            component.setSelectionEnd(pos + pattern.length());
	            component.getCaret().setSelectionVisible(true);
	        } else if(textPane.getCaretPosition() == 0) {
	        	String message = "Nothing found";
	        	JOptionPane.showMessageDialog(new JFrame(), message);
	        } else {
	        	if(pos == lastPos) {  // nothing new found
	        		String message = "Nothing more found";
	        		JOptionPane.showMessageDialog(new JFrame(), message);
	        		textPane.setCaretPosition(0);
	        	}
	        }
	        lastPos = pos;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	/** Searches for whole word.
	 * This is dependent on whether in JSON is searched or whether in XML is searched */
	private boolean searchWholeWord(int pos, String pattern) {
		//System.out.println("Looking for whole word = " + searchText);
		boolean isCorrectStart = false;
		boolean isCorrectEnd = false;
		int findLength = pattern.length() + 2;
		String match = null;
		try {
			match = doc.getText(pos, findLength);
		} catch (BadLocationException e) {
			return false;
		}
		// exclude words that contain JSON or XML tags
		if(usesXML == true) {
			//if(match.contains("<") || match.contains(">")) return false;
		} else {
			if(match.contains("{") || match.contains("}")) return false;
		}
		// case of a word in the middle of a sentence
		if(match.startsWith(" ") && 
				(match.endsWith(" ") || match.endsWith(",") || match.endsWith("."))  // MAYBE TODO: further extend
				) {
			// case sensitiveness
			//System.out.println("whole word found - text substring = " + match.substring(1,match.length()-1));
			if(isCaseSensitive) {
				if(match.substring(1,match.length()-1).equals(searchText) == true) return true;
			} else {
				if(match.substring(1,match.length()-1).equalsIgnoreCase(searchText) == true) return true;
			}
		}
		if(usesXML == true) {  // case XML - Remark that the API Response does not use XML attributes
			if(match.startsWith(">") || match.startsWith(" ")) isCorrectStart = true;
			if(match.endsWith("<") || match.endsWith(" ")) isCorrectEnd = true;
		} else {  // case JSON
			if(match.startsWith("\"") || match.startsWith(" ")) isCorrectStart = true;
			if(match.endsWith("\"") || match.endsWith(" ") || match.endsWith(",") || match.endsWith(".")) isCorrectEnd = true;
		} 
		
		if(isCorrectStart == true && isCorrectEnd == true) {
			// empty hit
			if(match.trim().length() == 0) return false;
			System.out.println("correct start and end - match word = " + match + " - isCaseSensitive = " + isCaseSensitive + " - usesXML = " + usesXML);
			if(isCaseSensitive) {
				if(match.substring(1,match.length()-1).equals(searchText) == true) return true;
			} else {
				if(match.substring(1,match.length()-1).equalsIgnoreCase(searchText) == true) return true;
			}
		} else {  // not an allowed start or end, so not a whole word
			return false;
		}
		// nothing found
		return false;
	}
	
	// Action of the "clear" button
	private void clearAction(ActionEvent e) {
		searchField.setText("");
		searchAction(null);
	}

}

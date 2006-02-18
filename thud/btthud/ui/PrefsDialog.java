//
//  PrefsDialog.java
//  Thud
//
//  Created by Anthony Parker on April 10, 2002, 2:26 AM.
//  Copyright (c) 2001-2006 Anthony Parker & the THUD team. 
//  All rights reserved. See LICENSE.TXT for more information.
//
package btthud.ui;

import btthud.data.*;
import btthud.util.*;
import java.awt.*;
import javax.swing.*;
import java.util.*;

public class PrefsDialog extends javax.swing.JDialog {
    
    private MUPrefs     prefs = null;
    private Thud		thudClass = null;

    private javax.swing.JTabbedPane 	TabbedPane;

    private javax.swing.JPanel 			GeneralOptionsTab;
    private javax.swing.JCheckBox 		echoCheckBox;
    private javax.swing.JCheckBox		antiAliasTextCheckBox;
    private javax.swing.JCheckBox 		highlightMyHexCheckBox;
    private javax.swing.JCheckBox		overwriteWithUnknownCheckBox;
    private javax.swing.JLabel 			speedLengthLabel;
    private javax.swing.JComboBox 		speedLengthBox;
    private javax.swing.JLabel 			scrollbackSizeLabel;
    private javax.swing.JComboBox 		scrollbackSizeBox;

    private javax.swing.JPanel 			MapColorsTab;
    private javax.swing.JButton 		bTerrainColors[] = new javax.swing.JButton[MUHex.TOTAL_TERRAIN];

    private javax.swing.JPanel 			FontTab;
    private javax.swing.JLabel 			mainLabel;
    private javax.swing.JComboBox 		mainSizeBox;
    private javax.swing.JLabel 			contactsLabel;
    private javax.swing.JComboBox 		contactsSizeBox;
    private javax.swing.JLabel 			contactsOnMapLabel;
    private javax.swing.JComboBox 		contactsOnMapSizeBox;
    private javax.swing.JLabel 			elevationsLabel;
    private javax.swing.JComboBox 		elevationsSizeBox;
    private javax.swing.JLabel 			hexNumbersLabel;
    private javax.swing.JComboBox 		hexNumberSizeBox;

    private javax.swing.JButton 		CancelButton;
    private javax.swing.JButton 		SaveButton;

    private javax.swing.JLabel			nullLabel;
    
    /** Creates new form PrefsDialog */
    public PrefsDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);

        thudClass = (Thud) parent;
        
        if (thudClass.prefs == null)
            prefs = new MUPrefs();

        try {
            this.prefs = (MUPrefs) ObjectCloner.deepCopy(thudClass.prefs);
        } catch (Exception e) {
            System.out.println("Error: prefsDialog: " + e);
        }
        
        initComponents();
        setMapColorIcons();
    }

    private void initComponents() {
        TabbedPane = new javax.swing.JTabbedPane();
        
        GeneralOptionsTab = new javax.swing.JPanel();
        speedLengthLabel = new javax.swing.JLabel();
        speedLengthBox = new javax.swing.JComboBox();
        scrollbackSizeLabel = new javax.swing.JLabel();
        scrollbackSizeBox = new javax.swing.JComboBox();

        MapColorsTab = new javax.swing.JPanel();
        for (int i = 0; i < MUHex.TOTAL_TERRAIN; i++)
            bTerrainColors[i] = new javax.swing.JButton();
        
        FontTab = new javax.swing.JPanel();
        mainLabel = new javax.swing.JLabel();
        mainSizeBox = new javax.swing.JComboBox();
        contactsLabel = new javax.swing.JLabel();
        contactsSizeBox = new javax.swing.JComboBox();
        contactsOnMapLabel = new javax.swing.JLabel();
        contactsOnMapSizeBox = new javax.swing.JComboBox();
        elevationsLabel = new javax.swing.JLabel();
        elevationsSizeBox = new javax.swing.JComboBox();
        hexNumbersLabel = new javax.swing.JLabel();
        hexNumberSizeBox = new javax.swing.JComboBox();

        nullLabel = new javax.swing.JLabel();
        
        CancelButton = new javax.swing.JButton();
        SaveButton = new javax.swing.JButton();

        // Set the content pane to a null layout
        getContentPane().setLayout(null);

        // Set the size of our dialog box, and some other stuff
        setSize(405, 325);
        setResizable(false);
        setTitle("Thud Preferences");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Thud Preferences");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        
        // --- GENERAL OPTIONS ---
        GeneralOptionsTab.setLayout(new java.awt.GridLayout(5, 2));
        
        echoCheckBox = new javax.swing.JCheckBox("Echo Commands", null, prefs.echoCommands);
        echoCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                echoCheckBoxActionPerformed(evt);
            }
        });
        GeneralOptionsTab.add(echoCheckBox);

        // Does this work? No idea...
        antiAliasTextCheckBox = new javax.swing.JCheckBox("Antialias Text", null, prefs.antiAliasText);
        antiAliasTextCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                antiAliasTextCheckBoxActionPerformed(evt);
            }
        });
        GeneralOptionsTab.add(antiAliasTextCheckBox);
        
        highlightMyHexCheckBox = new javax.swing.JCheckBox("Highlight My Hex", null, prefs.highlightMyHex);
        highlightMyHexCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                highlightMyHexCheckBoxActionPerformed(evt);
            }
        });
        GeneralOptionsTab.add(highlightMyHexCheckBox);

        overwriteWithUnknownCheckBox = new javax.swing.JCheckBox("Erase Unknown Terrain", null, prefs.overwriteWithUnknown);
        overwriteWithUnknownCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                overwriteWithUnknownCheckBoxActionPerformed(evt);
            }
        });
        GeneralOptionsTab.add(overwriteWithUnknownCheckBox);

        speedLengthLabel.setText("Speed Indicator Divisor");
        GeneralOptionsTab.add(speedLengthLabel);
        speedLengthBox.addItem(new Float(1.0));
        speedLengthBox.addItem(new Float(1.5));
        speedLengthBox.addItem(new Float(2.0));
        speedLengthBox.addItem(new Float(2.5));
        speedLengthBox.addItem(new Float(3.0));
        speedLengthBox.addItem(new Float(3.5));
        speedLengthBox.addItem(new Float(4.0));
        speedLengthBox.setSelectedItem(new Float(prefs.speedIndicatorLength));
        GeneralOptionsTab.add(speedLengthBox);

        scrollbackSizeLabel.setText("Lines of Text in Scrollback");
        GeneralOptionsTab.add(scrollbackSizeLabel);
        scrollbackSizeBox.addItem(new Integer(500));
        scrollbackSizeBox.addItem(new Integer(1000));
        scrollbackSizeBox.addItem(new Integer(2000));
        scrollbackSizeBox.addItem(new Integer(5000));
        scrollbackSizeBox.addItem(new Integer(7500));
        scrollbackSizeBox.addItem(new Integer(10000));
        scrollbackSizeBox.addItem(new Integer(20000));
        scrollbackSizeBox.setSelectedItem(new Integer(prefs.maxScrollbackSize));
        GeneralOptionsTab.add(scrollbackSizeBox);
        
        TabbedPane.addTab("General", GeneralOptionsTab);

        // --- MAP COLOR OPTIONS ---
        MapColorsTab.setLayout(new java.awt.GridLayout(0, 2));

        for (int i = 0; i < MUHex.TOTAL_TERRAIN; i++)
        {
            bTerrainColors[i].setText(MUHex.terrainForId(i) + " " + MUHex.nameForId(i));
            bTerrainColors[i].addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    bTerrainColorActionPerformed(evt);
                }
            });

            MapColorsTab.add(bTerrainColors[i]);
        }
                                                
        TabbedPane.addTab("Map Colors", MapColorsTab);

        // --- FONT OPTIONS ---
        FontTab.setLayout(new GridLayout(5, 2));
        
        mainLabel.setText("Main Window Font Size");
        FontTab.add(mainLabel);
        addFontSizeMenus(mainSizeBox);
        mainSizeBox.setSelectedItem(new Integer(prefs.mainFontSize));
        FontTab.add(mainSizeBox);
        
        contactsLabel.setText("Contacts Window Font Size");
        FontTab.add(contactsLabel);
        addFontSizeMenus(contactsSizeBox);
        contactsSizeBox.setSelectedItem(new Integer(prefs.contactFontSize));
        FontTab.add(contactsSizeBox);
        
        contactsOnMapLabel.setText("Contacts on Map Font Size");
        FontTab.add(contactsOnMapLabel);
        addFontSizeMenus(contactsOnMapSizeBox);
        contactsOnMapSizeBox.setSelectedItem(new Integer(prefs.infoFontSize));
        FontTab.add(contactsOnMapSizeBox);
        
        elevationsLabel.setText("Elevations on Map Size");
        FontTab.add(elevationsLabel);
        addFontSizeMenus(elevationsSizeBox);
        elevationsSizeBox.setSelectedItem(new Integer(prefs.elevationFontSize));
        FontTab.add(elevationsSizeBox);

        hexNumbersLabel.setText("Hex Numbers on Map Size");
        FontTab.add(hexNumbersLabel);
        addFontSizeMenus(hexNumberSizeBox);
        hexNumberSizeBox.setSelectedItem(new Integer(prefs.hexNumberFontSize));
        FontTab.add(hexNumberSizeBox);
        
        TabbedPane.addTab("Fonts", FontTab);
        
        getContentPane().add(TabbedPane);
        TabbedPane.setBounds(10, 10, 390, 250);
        
        CancelButton.setText("Cancel");
        CancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CancelButtonActionPerformed(evt);
            }
        });
        
        getContentPane().add(CancelButton);
        CancelButton.setBounds(190, 270, 100, 23);
        
        SaveButton.setText("Save");
        SaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveButtonActionPerformed(evt);
            }
        });
        
        getContentPane().add(SaveButton);
        SaveButton.setBounds(297, 270, 100, 23);
    }

    private void SaveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // They've clicked the save button
        // Get the values of some of the items
        prefs.mainFontSize = ((Integer) mainSizeBox.getSelectedItem()).intValue();
        prefs.contactFontSize = ((Integer) contactsSizeBox.getSelectedItem()).intValue();
        prefs.infoFontSize = ((Integer) contactsOnMapSizeBox.getSelectedItem()).intValue();
        prefs.elevationFontSize = ((Integer) elevationsSizeBox.getSelectedItem()).intValue();
        prefs.hexNumberFontSize = ((Integer) hexNumberSizeBox.getSelectedItem()).intValue();

        prefs.speedIndicatorLength = ((Float) speedLengthBox.getSelectedItem()).floatValue();
        prefs.maxScrollbackSize = ((Integer) scrollbackSizeBox.getSelectedItem()).intValue();
        
        thudClass.prefs = prefs;
        
        closeDialog(null);
    }

    // -----------------------
    // These are the action handlers for the map colors

    private void bTerrainColorActionPerformed(java.awt.event.ActionEvent evt) {

        StringTokenizer	st = new StringTokenizer(evt.getActionCommand());
        int whichTerrain = MUHex.idForTerrain(st.nextToken().charAt(0));
        
        Color	newColor = JColorChooser.showDialog(this, "Unknown Hex Color", prefs.terrainColors[whichTerrain]);

        if (newColor != null)
        {
            prefs.terrainColors[whichTerrain] = newColor;
            bTerrainColors[whichTerrain].setIcon(new ColorWellIcon(prefs.terrainColors[whichTerrain]));
        }
    }
    
    // -----------------------
    
    private void CancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // They've basically canceled
        closeDialog(null);
    }

    // -----------------------
    
    private void echoCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
        prefs.echoCommands = echoCheckBox.isSelected();
    }

    private void highlightMyHexCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
        prefs.highlightMyHex = highlightMyHexCheckBox.isSelected();
    }

    private void antiAliasTextCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
        prefs.antiAliasText = antiAliasTextCheckBox.isSelected();
    }

    private void overwriteWithUnknownCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
        prefs.overwriteWithUnknown = overwriteWithUnknownCheckBox.isSelected();
    }
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {
        setVisible(false);
        dispose();
    }

    private void setMapColorIcons() {
        for (int i = 0; i < MUHex.TOTAL_TERRAIN; i++)
            bTerrainColors[i].setIcon(new ColorWellIcon(prefs.terrainColors[i]));
    }

    /** Add standard font size menu items to a combo box */
    private void addFontSizeMenus(JComboBox theBox)
    {
        theBox.addItem(new Integer(9));
        theBox.addItem(new Integer(10));
        theBox.addItem(new Integer(11));
        theBox.addItem(new Integer(12));
        theBox.addItem(new Integer(14));
        theBox.addItem(new Integer(16));
        theBox.addItem(new Integer(18));
        theBox.addItem(new Integer(20));
        theBox.addItem(new Integer(24));
        theBox.addItem(new Integer(32));
    }
}

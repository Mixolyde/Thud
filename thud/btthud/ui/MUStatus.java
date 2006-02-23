//
//  MUStatus.java
//  Thud
//
//  Copyright (c) 2001-2006 Anthony Parker & the THUD team. 
//  All rights reserved. See LICENSE.TXT for more information.
//
package btthud.ui;

import btthud.data.*;
import btthud.engine.*;
import btthud.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;

import java.util.*;
import java.text.*;

/**
 * Implements a status report window that displays heading, speed, heat, and weapon information
 * very similar to the MUX's 'status'.
 * @author tkrajcar
 */
public class MUStatus extends JFrame
                           implements Runnable,
                                      ActionListener
{
    MUConnection		conn;
    MUData				data;
    MUPrefs				prefs;

    Font				mFont;
    
    JTextPane			statusPane;
    Thread				thread = null;
    SimpleAttributeSet	conRegular, conIrregular;
    ArrayList			elements;
    
    boolean				go = true;

    
    public MUStatus(MUConnection conn, MUData data, MUPrefs prefs)
    {
        super("Status Report");
        
        this.data = data;
        this.conn = conn;
        this.prefs = prefs;

        mFont = new Font("Monospaced", Font.PLAIN, prefs.statusFontSize);
        
        elements = new ArrayList();
        BulkStyledDocument	bsd = new BulkStyledDocument(prefs.statusFontSize, 50, mFont);
        statusPane = new JTextPane(bsd);
        statusPane.setBackground(Color.black);
        statusPane.setEditable(false);
        statusPane.setDoubleBuffered(true);

        statusPane.setFont(mFont);
 
        initAttributeSets();
        JScrollPane scrollPane = new JScrollPane(statusPane,
                                                 JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setDoubleBuffered(true);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(scrollPane);
        contentPane.setDoubleBuffered(true);
        
        setContentPane(contentPane);

        setSize(prefs.statusSizeX, prefs.statusSizeY);
        setLocation(prefs.statusLoc);
        
        this.setAlwaysOnTop(prefs.statusAlwaysOnTop);
        
        // Show the window now
        this.setVisible(true);
        
        start();
    }


    
    public void newPreferences(MUPrefs prefs)
    {
        this.prefs = prefs;
        mFont = new Font("Monospaced", Font.PLAIN, prefs.statusFontSize);
        statusPane.setFont(mFont);
        this.setAlwaysOnTop(prefs.statusAlwaysOnTop);
        initAttributeSets();
    }
    
    // --------------------
    public void start()
    {
        if (thread == null)
        {
            thread = new Thread(this, "MUStatusReport");
            thread.start();
        }
    }

    protected void initAttributeSets()
    {
        conRegular = new SimpleAttributeSet();
        StyleConstants.setFontSize(conRegular, prefs.statusFontSize);
        StyleConstants.setForeground(conRegular, Color.white);

        conIrregular = new SimpleAttributeSet();
        StyleConstants.setFontSize(conIrregular, prefs.statusFontSize);
        StyleConstants.setForeground(conIrregular, Color.white);
        StyleConstants.setBold(conIrregular,true);
        
    }
    
    public void run()
    {
        MUMyInfo				mydata;
        BulkStyledDocument 		doc = (BulkStyledDocument) statusPane.getDocument();                
        
        while (go)
        {
            try
            {
                if (data.hudRunning)
                {
                	elements.clear();
                    mydata = this.data.myUnit;
                                                          
                    String s = "";
                    
                    // Move/heat block
                    s = mydata.leftJust(mydata.name,14,true) +
	                	"[" +
	                	mydata.id +
	                	"]" +
	                	"  XYZ:" +
	                	mydata.rightJust(String.valueOf(mydata.x), 4, false) +
	                	"," +
	                	mydata.rightJust(String.valueOf(mydata.y), 4, false) +
	                	"," +
	                	mydata.rightJust(String.valueOf(mydata.z), 4, false) +
	                	"  " +	                	
                        "Heat Prod:  " + 
                    	mydata.rightJust(String.valueOf(mydata.heat),3,false) +
                    	" deg C.";
                    addString(s, conRegular);
                    addBlankLine();
                    
                    NumberFormat speedFormatter = new DecimalFormat("##0.0");
                                      
                    s = "Speed:  " + 
                    	mydata.rightJust(speedFormatter.format(mydata.speed),6,false) +
                    	" KPH  Heading:" +
                    	mydata.rightJust(String.valueOf(mydata.heading),6,false)
                    	+ " deg  Heat Sinks: " +
                    	mydata.rightJust(String.valueOf(mydata.heatSinks),3,false);
                    addString(s, conRegular);
                    addBlankLine();
                    
                    s = "Des.Spd:" +
                    	mydata.rightJust(speedFormatter.format(mydata.desiredSpeed),6,false) +
                    	" KPH  Des.Hdg:" +
                    	mydata.rightJust(String.valueOf(mydata.desiredHeading),6,false) +
                    	" deg  Heat Dissp: " +
                    	mydata.rightJust(String.valueOf(mydata.heatDissipation),3,false) +
                    	" deg C.";
                    addString(s, conRegular);
                    addBlankLine();
                    
                    s = "";
                    if(mydata.maxVerticalSpeed != 0) {
                    	s = s +
	                    	"Vrt Spd:" +
	                		mydata.rightJust(speedFormatter.format(mydata.verticalSpeed),6,false) +
	                		" KPH  " +                   	
                    		"Des.VSp:" +
                    		mydata.rightJust(speedFormatter.format(mydata.desiredVerticalSpeed),6,false) +
                    		" KPH  ";                    		
                    }
                    if(mydata.maxFuel != 0) {
                    	s = s +
                    		"Fuel: " +
                    		mydata.rightJust(String.valueOf(mydata.fuel),4,false) +
                    		"/" +
                    		mydata.rightJust(String.valueOf(mydata.maxFuel),4,false) +
                    		" (" +
                    		speedFormatter.format(mydata.percentFuelLeft()) + 
                    		"%)";                    		
                    }
                    
                    if(s.length() > 0)
                    	addString(s,conRegular);
                    addBlankLine();                    
                    
                    if(mydata.canHaveTurret()) {
                    	s = "Turret Hdg: " +
                    		mydata.rightJust(String.valueOf((mydata.turretHeading + mydata.heading + 180) % 360),5,false) +
                    		" deg";
                    		addString(s,conRegular);
                    		addBlankLine();                    	
                    }
                    	                    
                    if(mydata.status.length() > 0 && mydata.status.equals("-") == false) {             
                       	for(char sc : mydata.status.toCharArray()) { // loop through mydata.status
                    		switch(sc) {
	                			case 'B': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,0,0));
	                				addString("BURNING",conIrregular);
	                				break;
	                			}
	                			case 'C': {
	                				addString("CARRYING CLUB",conRegular);
	                				break;
	                			}
	                			case 'D': {
	                				addString("DUG IN",conRegular);
	                				break;
	                			}
	                			case 'e': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("EMITTING ECCM",conIrregular);
	                				break;
	                			}
	                			case 'E': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("EMITTING ECM",conIrregular);
	                				break;
	                			}
	                			case 'f': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("STANDING UP",conIrregular);
	                				break;
	                			}	                			
	                			case 'F': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,0,0));
	                				addString("FALLEN",conIrregular);
	                				break;
	                			}	                			
	                			case 'h': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("GOING HULL DOWN",conIrregular);
	                				break;
	                			}	                			
	                			case 'H': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("HULL DOWN",conIrregular);
	                				break;
	                			}	                			
	                			case 'I': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,0,0));
	                				addString("ON FIRE",conIrregular);
	                				break;
	                			}	                			
	                			case 'J': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("JUMPING",conIrregular);
	                				break;
	                			}	                			
	                			case 'l': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("ILLUMINATED",conIrregular);
	                				break;
	                			}	                			
	                			case 'L': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("ILLUMINATING",conIrregular);
	                				break;
	                			}
	                			case 'M': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("SPRINTING",conIrregular);
	                				break;
	                			}
	                			case 'm': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("EVADING",conIrregular);
	                				break;
	                			}
	                			case 'n': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("ENEMY NARC ATTACHED",conIrregular);
	                				break;
	                			}	                			
	                			case 'N': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("FRIENDLY NARC ATTACHED",conIrregular);
	                				break;
	                			}	                			
	                			case '+': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("OVERHEATING",conIrregular);
	                				break;
	                			}	                			
	                			case 'O': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("ORBITAL DROPPING",conIrregular);
	                				break;
	                			}	                			
	                			case 'p': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("PROTECTED BY ECM",conIrregular);
	                				break;
	                			}
	                			case 'P': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));	                				
	                				addString("PROTECTED BY ECCM",conIrregular);
	                				break;
	                			}
	                			case 's': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("STARTING UP",conIrregular);
	                				break;
	                			}	      	                			
	                			case 'S': {
	                        		StyleConstants.setForeground(conIrregular,new Color(255,0,0));
	                    			addString("SHUTDOWN",conIrregular);
	                    			break;
	                			}
	                			case 'T': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("BEING TOWED",conIrregular);
	                				break;
	                			}
	                			case 't': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("TOWING",conIrregular);
	                				break;
	                			}
	                			case 'W': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("SWARMING",conIrregular);
	                				break;
	                			}
	                			case 'X': {
	                    			StyleConstants.setForeground(conIrregular,new Color(255,255,0));
	                				addString("SPINNING",conIrregular);
	                				break;
	                			}	      
	                		}
                    		addString("  ",conRegular);
                    	}
                    } else {
                    	// Using addBlankLine() here won't work because it won't render two starttag-endtag pairs in a row - it eats one
                    	addString("\n",conRegular);
                    }
                	addBlankLine();
                	
                    s = "------- Weapon ------- [##] Loc - Status || --- Ammo Type --- Rds";
                    addString(s, conRegular);
                    addBlankLine();
                    
                   	MUUnitWeapon weapons[] = mydata.unitWeapons;
                   	MUUnitAmmo ammo[] = mydata.unitAmmo;
                   	
                    for(int i = 0; i < weapons.length; i++) {
                    	if (weapons[i] != null) {
	                    	MUUnitWeapon weapon = weapons[i];
	                    	MUWeapon weapontype = MUUnitInfo.getWeapon(weapon.typeNumber);

	                    	String weapname = weapontype.name;

	                    	weapname = weapname.replaceAll("IS\\.","");
	                    	weapname = weapname.replaceAll("Clan\\.","");
	                    	
	                    	
	                    	s = " " + 
	                    		mydata.leftJust(weapname,19,true);
	                    	if(weapon.fireMode.equals("-")) {
	                    		s = s + " ";
	                    	} else {
	                    		s = s + weapon.fireMode;
	                    	}	                    	
	                    	if(weapon.ammoType.equals("-")) {
	                    		s = s + " ";
	                    	} else {
	                    		s = s + weapon.ammoType;
	                    	}
	                    	s = s +
	                    		" [" + 
	                    		mydata.rightJust(String.valueOf(weapon.number),2,false) + 
	                    		"] " +
	                    		mydata.rightJust(weapon.loc,3,false) + 	                    		 
	                    		"   ";
	                    	addString(s, conRegular);
	                    	
                    		if (weapon.status.equals("R")) {
                    			StyleConstants.setForeground(conIrregular, Color.green);
                    			addString(" Ready", conIrregular);                    			                    	
                    		} else if(weapon.status.equals("*")) {
                    			StyleConstants.setForeground(conIrregular,new Color(128,128,128));
                    			addString(" *****", conIrregular);
                    		} else if(weapon.status.equals("A") || weapon.status.equals("a") || weapon.status.equals("J")) {
                    			StyleConstants.setForeground(conIrregular,new Color(160,0,0));
                    			addString("JAMMED", conIrregular);
                    		} else if(weapon.status.equals("D")) {
                    			StyleConstants.setForeground(conIrregular,new Color(128,128,128));
                    			addString("DISBLD", conIrregular);
                    		} else if(weapon.status.equals("S")) {
                    			StyleConstants.setForeground(conIrregular,new Color(160,0,0));
                    			addString("SHORTD", conIrregular);                    			
                    		} else {
                    			addString(mydata.rightJust(weapon.status,6,false), conRegular);                    			                    		
                    		}                     		

                    		s = " || ";
                    		addString(s, conRegular);	                                            			
                    	} else {
                    		addString("                               ", conRegular);                    		
                    	}

                    	/* end of weapon bar - now do ammo */
                    	if(ammo[i] != null) {
                    		MUUnitAmmo thisAmmo = ammo[i];
                    		MUWeapon thisWeapon = MUUnitInfo.getWeapon(thisAmmo.weaponTypeNumber);
	                    	String weapname = thisWeapon.name;

	                    	String mode = thisAmmo.ammoMode;	                    	
	                    	if(mode.equals("-")) 
	                    		mode = " ";
	                    	
	                    	weapname = weapname.replaceAll("IS\\.","");
	                    	weapname = weapname.replaceAll("Clan\\.","");
	                    		
                    		s = " " +
                    			mydata.leftJust(weapname,15,false) +
                    			mode + " ";
                    		addString(s, conRegular);
                    		
                    		s =	mydata.rightJust(String.valueOf(thisAmmo.roundsRemaining),3,false);
                    		StyleConstants.setForeground(conIrregular, MUUnitInfo.colorForPercent(mydata.percentAmmoLeft(thisAmmo)));
                    		addString(s, conIrregular);                    		                    
                    	}
                       
                    	/* end of ammo bar - blank line */
                    	addBlankLine();
                    }
                    	
                }
                
            	doc.clearAndInsertParsedString(elements);
                
                // This should probably sleep until notified or something
                Thread.sleep(1000);

            }
            catch (InterruptedException e)
            {
                // no big deal
            }
            catch (Exception e)
            {
                System.out.println("Error: status refresh: " + e);
            }
        }
    }
    
    public void pleaseStop()
    {
        go = false;
        this.dispose();
    }

    /* ---------------------- */

    public void actionPerformed(ActionEvent newEvent)
    {

    }	
    
    /** Adds a blank line to ArrayList elements and sends it back. Used to eliminate code duplication
     * 
     * @param elements ArrayList to append blank line elements to
     */
    private void addBlankLine() {
        elements.add(new DefaultStyledDocument.ElementSpec(conRegular, DefaultStyledDocument.ElementSpec.EndTagType));
        elements.add(new DefaultStyledDocument.ElementSpec(conRegular, DefaultStyledDocument.ElementSpec.StartTagType));            	
    }
    
    /** Adds a given line to ArrayList elements and sends it back. Used to eliminate code duplication
     * 
     * @param elements ArrayList to append blank line elements to
     * @param s String of text to append
     * @param attrs Attributes to use when adding string
     */
    private void addString(String s, MutableAttributeSet attrs) {
        elements.add(new DefaultStyledDocument.ElementSpec(new SimpleAttributeSet(attrs),
                DefaultStyledDocument.ElementSpec.ContentType,
                s.toCharArray(),
                0,
                s.length()));
    }       
}

//
//  OptionTextPane.java
//  Thud
//
//  Created by Anthony Parker on Wed Oct 16 2002.
//  Copyright (c) 2002 Anthony Parker. All rights reserved.
//

package btthud.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class OptionTextPane extends JTextPane {

    boolean			antiAliased;

    public OptionTextPane(StyledDocument d, boolean antiAliased)
    {  
        super(d);
        this.antiAliased = antiAliased;
    }

    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            this.antiAliased ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        
        super.paintComponent(g2);
    }
}

//
//  MUXMapComponent.java
//  Thud
//
//  Created by Anthony Parker on Thu Dec 27 2001.
//  Copyright (c) 2001-2002 Anthony Parker. All rights reserved.
//  Please see LICENSE.TXT for more information.
//
//
package btthud.ui;

import btthud.data.*;
import btthud.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.text.*;

import java.lang.*;
import java.util.*;

import java.awt.print.*;

/* Notes:

This is the class that does probably the most interesting work in all of Thud. It draws the map and the components inside the map.

It tries to draw as much as possible beforehand (in the changeHeight() function) so as to do as little processing as possible when it is drawing a lot of hexes across and down. This includes filling, drawing lines, drawing terrain types, and drawing elevations. It stores all of this in an array then copies it while running. It is probably possible to do further optimizations here, and that would be a good idea.

Also needed; a better way to determine exactly what hexes to draw or not. Right now it just guestimates then adds on some on both sides. We could avoid drawing a lot of hexes if we had a more accurate algorithm.

- asp, 7/7/2002

    */

public class MUXMapComponent extends JComponent implements Scrollable, Printable
{
    MUXMap					map;
    MPrefs					prefs;

    Font					hexNumberFont;
    Font					terrainFont;
    Font					elevFont;

    FontRenderContext		frc;
    
    int						elevWidth[] = new int[10]; 	// Stores width of each elevation number glyph, 0 - 9
    BufferedImage			hexImages[][] = new BufferedImage[MUXHex.TOTAL_TERRAIN][10];			// One for each hex type and elevation
    HexShape				hexPoly;

    static final float		tan60 = (float) Math.tan(toRadians(60.0f));
    static final float		sin60 = (float) Math.sin(toRadians(60.0f));

    int						h = 40;
    float					w = h / (2 * sin60);
    float					l = h / (2 * tan60);

    RenderingHints			rHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Rectangle				bounds;
    
    public MUXMapComponent(MUXMap map, MPrefs prefs, int hexHeight)
    {
        super();

        this.map = map;
        this.prefs = prefs;

        setupFonts();

        setDoubleBuffered(true);
        
        // Do some initial setup
        changeHeight(hexHeight);
        precalculateNumbers();

        bounds = getBounds();
    
        rHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                   prefs.antiAliasText ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        
    }

    // ---------------

    public Dimension getPreferredScrollableViewportSize()
    {
        return getPreferredSize();
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return getScrollableUnitIncrement(visibleRect, orientation, direction) * 10;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        int				skip;
        
        if (h <= 5)
            skip = 5;
        else if (h <= 10)
            skip = 3;
        else if (h <= 20)
            skip = 2;
        else
            skip = 1;
        
        if (orientation == SwingConstants.VERTICAL)
            return h * skip;
        else
            return (int)(w + l) * skip;
    }
    
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }

    public boolean getScrollableTracksViewportWidth()
    {
        return false;
    }
    /* ---------------------- */

    /**
      * The purpose of this function is to take some CPU-intensive stuff that mostly stays the same and move it outside
      * of the main drawing loops, for efficiency. We store the values in private class variables. If any font sizes change,
      * this function should be called again.
      */
    private void precalculateNumbers()
    {
        frc = new FontRenderContext(new AffineTransform(), true, prefs.antiAliasText);
        
        Rectangle2D	elevRect;
        
        for (int i = 0; i < 10; i++)
        {
            elevRect = elevFont.getStringBounds(Integer.toString(i), frc);
            elevWidth[i] = (int) elevRect.getWidth();
        }
    }

    /**
      * This function is for outside callers who want to make sure that we redraw in case of preference color or font change.
      */
    public void newPreferences(MPrefs prefs, int hexHeight)
    {
        this.prefs = prefs;

        setupFonts();
        changeHeight(hexHeight);
        
        repaint();
    }

    /**
      * Returns the total width of the map, in pixels
      */
    public int getTotalWidth()
    {
        return (int) (l + ((w + l) * map.getSizeX()));
    }

    /**
      * Returns the total height of the map, in pixels
      */
    public int getTotalHeight()
    {
        // This would be the total number of hexes in y times height
        // Also adjust 1/2 hex
        return (int) ((h * ((float) map.getSizeY() + 0.5)));
    }

    
    /**
      * The purpose of this function is to take some CPU-intensive stuff that mostly stays the same and move it outside
      * of the main drawing loops, for efficiency. This one deals with saving the Images for each color of hex, to avoid 'fill' costs.
      * It should be called any time that the height changes.
      */
    private void changeHeight(int newHeight)
    {
        // Set some variables
        if (newHeight < 5)
            h = 5;
        else
            h = newHeight;

        w = -h / (2 * sin60);
        l = h / (2 * tan60);

        terrainFont = new Font("Monospaced", Font.PLAIN, h/2 - 2);

        hexPoly = new HexShape(h);

        // Now draw our images      
        for (int i = 0; i < MUXHex.TOTAL_TERRAIN; i++)
        {
            for (int j = 0; j < 10; j++)
            {
                BufferedImage	newImage = new BufferedImage(hexPoly.getBounds().width, hexPoly.getBounds().height, BufferedImage.TYPE_INT_ARGB);

                // Get the graphics context for this BufferedImage
                Graphics2D		g = (Graphics2D) newImage.getGraphics();

                // Setup the color
                if (prefs.tacDarkenElev)
                    g.setColor(MUXHex.colorForElevation(colorForTerrain(i), j, prefs.elevationColorMultiplier));
                else
                    g.setColor(colorForTerrain(i));
                
                // Fill the hex
                g.fill(hexPoly);

                // Draw the line around the hex
                g.setColor(Color.gray);
                g.draw(hexPoly);                    

                // Draw the elevation number (lower right corner)
                if (prefs.tacShowTerrainElev && h >= 20)
                {
                    // Draw the elevation
                    g.setColor(Color.black);
                    g.setFont(elevFont);

                    if (j != 0)			// We don't draw zero elevation numbers
                    {
                        String		hexElev = Integer.toString(j);
                        int			width;

                        if (j < 0 && Math.abs(j) <= 9)
                            width = 2 * elevWidth[Math.abs(j)];
                        else if (j > 0 && j <= 9)
                            width = elevWidth[j];
                        else
                            width = elevWidth[0];

                        g.drawString(hexElev,
                                     (float) (hexPoly.getX(0) + w - width),
                                     (float) (hexPoly.getY(0) + h - 2));
                    }
                }

                // Draw the terrain type (upper left corner)
                if (prefs.tacShowTerrainChar && h >= 20)
                {
                    if (i != MUXHex.PLAIN)			// we don't draw plain types
                    {
                        g.setFont(terrainFont);
                        g.drawString(String.valueOf(MUXHex.terrainForId(i)), (float) hexPoly.getX(0), (float) (hexPoly.getY(0) + h/2));
                    }
                }
                
                hexImages[i][j] = newImage;                
            }
        }
    }

    /**
      * This function sets up the fonts, according to the sizes specified in the preferences. It should be called each time the font size
      * changes.
      */
    private void setupFonts()
    {
        hexNumberFont = new Font("Monospaced", Font.BOLD, prefs.hexNumberFontSize);
        terrainFont = new Font("Monospaced", Font.PLAIN, 10);		// this changes dynamically anyway. based on size of hex
        elevFont = new Font("Monospaced", Font.PLAIN, prefs.elevationFontSize);

        rHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                   prefs.antiAliasText ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        
        // need to recalculate font widths
        precalculateNumbers();
    }
    
    /* ---------------------- */

    /**
      * Support for printing! How exciting...
      */
    public int print(Graphics gfx, PageFormat pf, int pi) throws PrinterException
    {
        Graphics2D			g = (Graphics2D) gfx;
        AffineTransform		trans = g.getTransform();
        AffineTransform		oldTrans = g.getTransform();


        trans.translate(pf.getImageableX(), pf.getImageableY());
        g.setTransform(trans);

        
        paint2d(g);

        g.setTransform(oldTrans);
        
        return Printable.PAGE_EXISTS;
    }

    public void paint(Graphics gfx)
    {
        paint2d((Graphics2D) gfx);
    }
    
    /**
     * Paints the whole map, in steps.
     */
    public void paint2d(Graphics2D g)
    {
        // -----------
        
        //getBounds(bounds);
        Dimension		dim = getPreferredSize();
        bounds = new Rectangle(0, 0, dim.width, dim.height);
        
        AffineTransform		oldTrans = g.getTransform();

        g.addRenderingHints(rHints);
        
        // ----

        g.setColor(Color.white);
        g.fill(bounds);

        // Paint the terrain
        paintTerrainGraphics(g);
        
        // ----
        
        // Reset the transform
        g.setTransform(oldTrans);
    }

    /**
      * Do the dirty work of drawing terrain into a graphics object.
      * @param g The graphics context into which we are drawing.
      */
    public void paintTerrainGraphics(Graphics2D g)
    {        
        // Now we go into a loop to draw the proper colors or textures for each terrain
        for (int x = 0; x < map.getSizeX(); x++)
        {
            for (int y = 0; y < map.getSizeY(); y++)
                paintOneHex(g, x, y);
        }
    }

    /**
      * Allows the frame to tell us when a hex has changed, so we can repaint it
      */
    public void changeHex(int x, int y)
    {
        paintOneHex((Graphics2D) getGraphics(), x, y);
    }

    public void changeHex(Point h)
    {
        changeHex((int) h.getX(), (int) h.getY());
    }

    public void changeHexes(LinkedList hexes)
    {
        Graphics2D				g = (Graphics2D) getGraphics();
        ListIterator			it = hexes.listIterator(0);
        Point					p;
        
        while (it.hasNext())
        {
            p = (Point) it.next();
            paintOneHex(g, (int) p.getX(), (int) p.getY());
        }
    }
    
    /**
      * Paint an individual hex
      */
    protected void paintOneHex(Graphics2D g, int x, int y)
    {
        AffineTransform			oldTrans = g.getTransform();
        AffineTransform			trans = new AffineTransform(oldTrans);
        Point2D					realHex = new Point2D.Float();				// drawing coordinates
        
        hexPoly.hexToReal(x, y, false, realHex);

        // Make sure that this hex is actually viewable
        if (!g.hitClip((int) realHex.getX(), (int) realHex.getY(), (int) (w + 2 * l), h))
            return;
        
        trans.translate(realHex.getX() -  l, realHex.getY());
        g.setTransform(trans);
        g.drawImage(imageForTerrain(map.getHexTerrain(x, y),
                                    map.getHexAbsoluteElevation(x, y)),
                    null,
                    null);
        
        // Optional stuff -----------------------

        if (prefs.tacShowHexNumbers)
        {
            AffineTransform			beforeNumberRot = g.getTransform();
            AffineTransform			baseTrans2 = g.getTransform();
            String					hexString = x + "," + y;
            baseTrans2.rotate(-Math.PI / 2, hexPoly.getX(2), hexPoly.getY(2));
            g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.25f));
            g.setFont(hexNumberFont);
            g.setTransform(baseTrans2);
            g.drawString(hexString,
                         (float) (hexPoly.getX(2) + 2),
                         (float) (hexPoly.getY(2) + (hexNumberFont.getLineMetrics(hexString, frc)).getAscent()));
            g.setTransform(beforeNumberRot);
        }

        if (prefs.tacShowCliffs)
        {
            Stroke		saveStroke = g.getStroke();
            int			thisElevation = map.getHexElevation(x, y);
            g.setColor(Color.red);
            g.setStroke(new BasicStroke(2.0f));		// Make the red line wider

            if (x % 2 == 0)
            {
                // Even X
                if (Math.abs(map.getHexElevation(x + 0, y - 1) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(0), (int) hexPoly.getY(0), (int) hexPoly.getX(5), (int) hexPoly.getY(5));
                if (Math.abs(map.getHexElevation(x - 1, y + 0) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(0), (int) hexPoly.getY(0), (int) hexPoly.getX(1), (int) hexPoly.getY(1));
                if (Math.abs(map.getHexElevation(x - 1, y + 1) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(1), (int) hexPoly.getY(1), (int) hexPoly.getX(2), (int) hexPoly.getY(2));
                if (Math.abs(map.getHexElevation(x + 0, y + 1) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(2), (int) hexPoly.getY(2), (int) hexPoly.getX(3), (int) hexPoly.getY(3));
                if (Math.abs(map.getHexElevation(x + 1, y + 1) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(3), (int) hexPoly.getY(3), (int) hexPoly.getX(4), (int) hexPoly.getY(4));
                if (Math.abs(map.getHexElevation(x + 1, y + 0) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(4), (int) hexPoly.getY(4), (int) hexPoly.getX(5), (int) hexPoly.getY(5));
            }
            else
            {
                // Odd X
                if (Math.abs(map.getHexElevation(x + 0, y - 1) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(0), (int) hexPoly.getY(0), (int) hexPoly.getX(5), (int) hexPoly.getY(5));
                if (Math.abs(map.getHexElevation(x - 1, y - 1) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(0), (int) hexPoly.getY(0), (int) hexPoly.getX(1), (int) hexPoly.getY(1));
                if (Math.abs(map.getHexElevation(x - 1, y + 0) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(1), (int) hexPoly.getY(1), (int) hexPoly.getX(2), (int) hexPoly.getY(2));
                if (Math.abs(map.getHexElevation(x + 0, y + 1) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(2), (int) hexPoly.getY(2), (int) hexPoly.getX(3), (int) hexPoly.getY(3));
                if (Math.abs(map.getHexElevation(x + 1, y + 0) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(3), (int) hexPoly.getY(3), (int) hexPoly.getX(4), (int) hexPoly.getY(4));
                if (Math.abs(map.getHexElevation(x + 1, y - 1) - thisElevation) > prefs.cliffDiff)
                    g.drawLine((int) hexPoly.getX(4), (int) hexPoly.getY(4), (int) hexPoly.getX(5), (int) hexPoly.getY(5));
            }

            g.setStroke(saveStroke);				// Restore the old stroke
        }

        // Draw the line if it's selected
        if (map.getHexSelected(x, y))
        {
            // We draw a black line on edges with an unselected adjacent hex
            Stroke		saveStroke = g.getStroke();
            g.setColor(Color.black);
            g.setStroke(new BasicStroke(3.0f));		// Make the black line wider

            if (x % 2 == 0)
            {
                // Even X
                if (!map.getHexSelected(x + 0, y - 1))
                    g.drawLine((int) hexPoly.getX(0), (int) hexPoly.getY(0), (int) hexPoly.getX(5), (int) hexPoly.getY(5));
                if (!map.getHexSelected(x - 1, y + 0))
                    g.drawLine((int) hexPoly.getX(0), (int) hexPoly.getY(0), (int) hexPoly.getX(1), (int) hexPoly.getY(1));
                if (!map.getHexSelected(x - 1, y + 1))
                    g.drawLine((int) hexPoly.getX(1), (int) hexPoly.getY(1), (int) hexPoly.getX(2), (int) hexPoly.getY(2));
                if (!map.getHexSelected(x + 0, y + 1))
                    g.drawLine((int) hexPoly.getX(2), (int) hexPoly.getY(2), (int) hexPoly.getX(3), (int) hexPoly.getY(3));
                if (!map.getHexSelected(x + 1, y + 1))
                    g.drawLine((int) hexPoly.getX(3), (int) hexPoly.getY(3), (int) hexPoly.getX(4), (int) hexPoly.getY(4));
                if (!map.getHexSelected(x + 1, y + 0))
                    g.drawLine((int) hexPoly.getX(4), (int) hexPoly.getY(4), (int) hexPoly.getX(5), (int) hexPoly.getY(5));
            }
            else
            {
                // Odd X
                if (!map.getHexSelected(x + 0, y - 1))
                    g.drawLine((int) hexPoly.getX(0), (int) hexPoly.getY(0), (int) hexPoly.getX(5), (int) hexPoly.getY(5));
                if (!map.getHexSelected(x - 1, y - 1))
                    g.drawLine((int) hexPoly.getX(0), (int) hexPoly.getY(0), (int) hexPoly.getX(1), (int) hexPoly.getY(1));
                if (!map.getHexSelected(x - 1, y + 0))
                    g.drawLine((int) hexPoly.getX(1), (int) hexPoly.getY(1), (int) hexPoly.getX(2), (int) hexPoly.getY(2));
                if (!map.getHexSelected(x + 0, y + 1))
                    g.drawLine((int) hexPoly.getX(2), (int) hexPoly.getY(2), (int) hexPoly.getX(3), (int) hexPoly.getY(3));
                if (!map.getHexSelected(x + 1, y + 0))
                    g.drawLine((int) hexPoly.getX(3), (int) hexPoly.getY(3), (int) hexPoly.getX(4), (int) hexPoly.getY(4));
                if (!map.getHexSelected(x + 1, y - 1))
                    g.drawLine((int) hexPoly.getX(4), (int) hexPoly.getY(4), (int) hexPoly.getX(5), (int) hexPoly.getY(5));
            }

            g.setStroke(saveStroke);				// Restore the old stroke
        }

        g.setTransform(oldTrans);
    }
    
    /**
      * Get the proper color to describe a terrain character.
      */
    public Color colorForTerrain(int terrain)
    {
        return prefs.terrainColors[terrain];
    }

    /**
      * Get the Image that we want to copy for a given terrain character.
      */
    
    protected BufferedImage imageForTerrain(int terrain, int elevation)
    {
        return hexImages[terrain][elevation];
    }

    // ----------------------
    // Helper functions
    // ----------------------

    public Point realToHex(int rX, int rY)
    {
        return hexPoly.realToHex(rX, rY);
    }

    static public float toRadians(float a)
    {
        return (float) ((a / 180.0f) * Math.PI + Math.PI);
    }
}

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

    */

public class MUXMapComponent extends JComponent implements Scrollable, Printable
{
    MUXMap			map;
    MPrefs			prefs;

    Font			hexNumberFont;
    Font			terrainFont;
    Font			elevFont;

    FontRenderContext		frc;
    
    int				elevWidth[] = new int[10]; 	// Stores width of each elevation number glyph, 0 - 9
    BufferedImage		hexImages[][] = new BufferedImage[MUXHex.TOTAL_TERRAIN][10];			// One for each hex type and elevation
    HexShape			hexPoly;

    static final float		tan60 = (float) Math.tan(toRadians(60.0f));
    static final float		sin60 = (float) Math.sin(toRadians(60.0f));

    int				h = 40;
    float			w = h / (2 * sin60);
    float			l = h / (2 * tan60);
    
    RenderingHints              rHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Rectangle			bounds;

    boolean			drawOverview = false;

    public MUXMapComponent(MUXMap map, MPrefs prefs, int hexHeight)
    {
        super();

        this.map = map;
        this.prefs = prefs;

        rHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                   prefs.antiAliasText ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        rHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        rHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rHints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        rHints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        rHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        setupFonts();

        setDoubleBuffered(true);
        
        // Do some initial setup
        changeHeight(hexHeight);
        precalculateNumbers();

        bounds = getBounds();
    }

    // ---------------

    public Dimension getPreferredScrollableViewportSize()
    {
        return getPreferredSize();
    }
    
    public Dimension getPreferredSize()
    {
        return new Dimension(getTotalWidth(), getTotalHeight());
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
	
	/**
	  * Set the drawing mode to 'overview' (1 pixel per hex).
	  */
	public void setDrawOverview(boolean overview) {
		drawOverview = overview;
	}
	
	public boolean getDrawOverview() {
		return drawOverview;
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
        
        //repaint();
    }

    /**
      * Returns the total width of the map, in pixels
      */
    public int getTotalWidth()
    {
		if (drawOverview) {
			return map.getSizeX();
		} else {
			return (int) (l + ((w + l) * map.getSizeX()) + 10);
		}
    }

    /**
      * Returns the total height of the map, in pixels
      */
    public int getTotalHeight()
    {
		if (drawOverview) {
			return map.getSizeY();
		} else {
			// This would be the total number of hexes in y times height
			// Also adjust 1/2 hex
			return (int) ((h * ((float) map.getSizeY() + 0.5)) + 10);
		}
    }

    
    /**
      * The purpose of this function is to take some CPU-intensive stuff that mostly stays the same and move it outside
      * of the main drawing loops, for efficiency. This one deals with saving the Images for each color of hex, to avoid 'fill' costs.
      * It should be called any time that the height changes.
      */
    private void changeHeight(int newHeight)
    {
        //Color		alphaColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
        
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
                hexImages[i][j] = null;
            }
        }
    }

    /**
      * Pre-render one particular type of hex (one elevation, one terrain)
      */
    private void setupOneHex(int t, int e)
    {
        BufferedImage	newImage = new BufferedImage(hexPoly.getBounds().width, hexPoly.getBounds().height, BufferedImage.TYPE_INT_ARGB);
        
        // Get the graphics context for this BufferedImage
        Graphics2D		g = (Graphics2D) newImage.getGraphics();
        
        g.addRenderingHints(rHints);
        
        if (prefs.tacDarkenElev)
            g.setColor(MUXHex.colorForElevation(colorForTerrain(t), e, prefs.elevationColorMultiplier));
        else
            g.setColor(colorForTerrain(t));
        
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
            
            if (e != 0)			// We don't draw zero elevation numbers
            {
                String		hexElev = Integer.toString(e);
                int             width;
                
                if (e < 0 && Math.abs(e) <= 9)
                    width = 2 * elevWidth[Math.abs(e)];
                else if (e > 0 && e <= 9)
                    width = elevWidth[e];
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
            if (t != MUXHex.PLAIN)			// we don't draw plain types
            {
                g.setFont(terrainFont);
                g.drawString(String.valueOf(MUXHex.terrainForId(t)), (float) hexPoly.getX(0), (float) (hexPoly.getY(0) + h/2));
            }
        }
        
        hexImages[t][e] = newImage;
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
        Graphics2D              g = (Graphics2D) gfx;
        AffineTransform		trans = g.getTransform();
        AffineTransform		oldTrans = g.getTransform();


        trans.translate(pf.getImageableX(), pf.getImageableY());
        g.setTransform(trans);
        
        paint(g);

        g.setTransform(oldTrans);
        
        return Printable.PAGE_EXISTS;
    }

    public void repaint(Rectangle2D r)
    {
        Rectangle		intRectangle = new Rectangle((int) Math.floor(r.getX()),
                                                (int) Math.floor(r.getY()),
                                                (int) (Math.ceil(r.getWidth() + r.getX()) - Math.floor(r.getX())),
                                                (int) (Math.ceil(r.getHeight() + r.getY()) - Math.floor(r.getY())));
        super.repaint(intRectangle);
    }
    
    public void paint(Graphics gfx)
    {
        Graphics2D          g = (Graphics2D) gfx;
        paint2d(g);
    }
    
    public void paint2d(Graphics2D g)
    {
        AffineTransform     oldTrans = g.getTransform();
        
        g.addRenderingHints(rHints);
        
        // Paint the terrain
        paintTerrainGraphics(g);
        
        // Reset the transform
        g.setTransform(oldTrans);
    }

    /**
      * Do the dirty work of drawing terrain into a graphics object.
      * @param g The graphics context into which we are drawing.
      */
    public void paintTerrainGraphics(Graphics2D g)
    {
        Rectangle   clipRect = g.getClipBounds();
        Point       startCoord, endCoord;
        
		if (drawOverview) {
			BufferedImage	image = new BufferedImage(map.getSizeX(), map.getSizeY(), BufferedImage.TYPE_INT_ARGB);
			
			for (int x = 0; x < map.getSizeX(); x++) {
				for (int y = 0; y < map.getSizeY(); y++) {
					image.setRGB(x, y, colorForTerrain(map.getHexTerrain(x, y)).getRGB());
				}
			}
			
			g.drawImage(image, null, 0, 0);
			
		} else {
			if (clipRect == null)
			{
				startCoord = new Point(0, 0);
				endCoord = new Point(map.getSizeX(), map.getSizeY());
			}
			else
			{
				startCoord = hexPoly.realToHex((int) clipRect.getX(), (int) clipRect.getY());
				endCoord = hexPoly.realToHex((int) clipRect.getX() + (int) clipRect.getWidth(), (int) clipRect.getY() + (int) clipRect.getHeight());            
			}
			
			// Make sure we get the edges of other hexes
			startCoord.setLocation(startCoord.getX() - 1, startCoord.getY() - 1);
			endCoord.setLocation(endCoord.getX() + 2, endCoord.getY() + 2);
			
			// Assumes map is square
			clipPoint(startCoord, 0, map.getSizeX());
			clipPoint(endCoord, 0, map.getSizeX());
			
			// Now we go into a loop to draw the proper colors or textures for each terrain
			for (int x = (int) startCoord.getX(); x < (int) endCoord.getX(); x++)
			{
				for (int y = (int) startCoord.getY(); y < (int) endCoord.getY(); y++)
				{
					paintOneHex(g, x, y);
				}
			}
			
			// Draw any items that go on top of the hexes (currently, only selection indicators)
			paintMarkups(g);
		}
    }
    
    private void clipPoint(Point p, int min, int max)
    {
        if (p.getX() < min)
            p.setLocation(min, p.getY());
        if (p.getY() < min)
            p.setLocation(p.getX(), min);
        if (p.getX() > max)
            p.setLocation(max, p.getY());
        if (p.getY() > max)
            p.setLocation(p.getX(), max);
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
        Point2D                         realHex = new Point2D.Float();				// drawing coordinates
        
        hexPoly.hexToReal(x, y, HexShape.HEX_UPPER_LEFT, realHex);
        
        trans.translate(realHex.getX() - l, realHex.getY());
        g.setTransform(trans);
        
        g.drawImage(imageForTerrain(map.getHexTerrain(x, y),
                                    map.getHexAbsoluteElevation(x, y)),
                    null,
                    null);
        
        // Optional stuff -----------------------
        
        /*
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
        */

        g.setTransform(oldTrans);
    }
    
    private void paintMarkups(Graphics2D g)
    {
        AffineTransform			oldTrans = g.getTransform();
        AffineTransform			trans = new AffineTransform(oldTrans);
        Point2D                         realHex = new Point2D.Float();				// drawing coordinates
        Point                           h;
        int                             x, y;
        ListIterator                    it = map.selectedHexesIterator();
        float                           dash[] = {w / 4f};
        float                           dashOffset = w / 4f;
        Rectangle                       clipBounds = g.getClipBounds();
        Rectangle2D                     hexBounds = new Rectangle2D.Double();
        
        while (it.hasNext())
        {
            h = (Point) it.next();
            expandedRectForHex(h, hexBounds);
            
            if (clipBounds.intersects(hexBounds))
            {
                x = (int) h.getX();
                y = (int) h.getY();
            
                trans.setTransform(oldTrans);
                hexPoly.hexToReal(x, y, HexShape.HEX_UPPER_LEFT, realHex);
                trans.translate(realHex.getX() - l, realHex.getY());
                g.setTransform(trans);
                
                // We draw a black line on edges with an unselected adjacent hex
                Stroke		saveStroke = g.getStroke();
                
                for (int i = 0; i < 2; i++)
                {
                    if (i == 0)
                    {
                        g.setColor(Color.black);
                        g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1.0f, dash, 0.0f));
                    }
                    else
                    {
                        g.setColor(Color.red);
                        g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1.0f, dash, dashOffset));
                    }
                    
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
                }
                
                g.setStroke(saveStroke);				// Restore the old stroke
            }
            
            g.setTransform(oldTrans);
        }
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
        if (hexImages[terrain][elevation] == null)
            setupOneHex(terrain, elevation);
        
        return hexImages[terrain][elevation];            
    }

    // ----------------------
    // Helper functions
    // ----------------------

    public Point realToHex(int rX, int rY)
    {
		if (drawOverview) {
			return new Point(rX, rY);
		} else {
			return hexPoly.realToHex(rX, rY);
		}
    }
    
    public boolean hexInRect(int hX, int hY, Rectangle r)
    {
		if (drawOverview) {
			return r.contains(hX, hY);
		} else {
			return hexPoly.hexIntersectsClipRect(hX, hY, r);
		}
    }

    public Rectangle2D rectForHex(Point h)
    {
		if (drawOverview) {
			return new Rectangle2D.Double(h.getX(), h.getY(), 1, 1);
		} else {
			return hexPoly.hexToRect((int) h.getX(), (int) h.getY());
		}
    }
    
    public Point2D centerOfHex(int hX, int hY)
    {
		if (drawOverview) {
			return new Point2D.Float(hX, hY);
		} else {
			return hexPoly.hexToReal(hX, hY, HexShape.HEX_CENTER);
		}
    }
    
    public void expandedRectForHex(Point h, Rectangle2D r)
    {
		if (drawOverview) {
			r.setRect(h.getX(), h.getY(), 1, 1);
		} else {
			hexPoly.hexToExpandedRect((int) h.getX(), (int) h.getY(), r);
		}
    }
    
    static public float toRadians(float a)
    {
        return (float) ((a / 180.0f) * Math.PI + Math.PI);
    }
}

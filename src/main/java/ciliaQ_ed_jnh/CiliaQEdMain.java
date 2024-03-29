package ciliaQ_ed_jnh;
/** ===============================================================================
* CiliaQ_Editor Version 0.0.4
* 
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*  
* See the GNU General Public License for more details.
*  
* Copyright (C) @author Jan Niklas Hansen
* Date: May 16, 2020 (This Version: January 08, 2023)
*   
* For any questions please feel free to contact me (jan.hansen@uni-bonn.de).
* =============================================================================== */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.*;
import ij.text.*;

public class CiliaQEdMain implements PlugIn, Measurements {
	//Name variables
	static final String PLUGINNAME = "CiliaQ Editor";
	static final String PLUGINVERSION = "0.0.4";
	
	//Fix fonts
	static final Font SuperHeadingFont = new Font("Sansserif", Font.BOLD, 16);
	static final Font HeadingFont = new Font("Sansserif", Font.BOLD, 14);
	static final Font BoldFont = new Font("Sansserif", Font.BOLD, 12);
	static final Font TextFont = new Font("Sansserif", Font.PLAIN, 12);
	static final Font InstructionsFont = new Font("Sansserif", 2, 12);
	static final Font RoiFont = new Font("Sansserif", Font.PLAIN, 12);
	
	DecimalFormat df6 = new DecimalFormat("#0.000000");
	DecimalFormat df3 = new DecimalFormat("#0.000");
	DecimalFormat df0 = new DecimalFormat("#0");
	
	static SimpleDateFormat NameDateFormatter = new SimpleDateFormat("yyMMdd_HHmmss");
	static SimpleDateFormat FullDateFormatter = new SimpleDateFormat("yyyy-MM-dd	HH:mm:ss");
	static SimpleDateFormat FullDateFormatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//Progress Dialog
	private EditingDialog EdDialog;
	
	//-----------------define params for Dialog-----------------
	int channelMask = 2, channelForCopying = 3;
	boolean copyZeroPixels = false;
	static final String[] outputVariant = {"save as filename + suffix '_ed'", "save as filename + suffix 'ed' + date", "overwrite input file"};
	String chosenOutputName = outputVariant[0];
	//-----------------define params for Dialog-----------------
	
public void run(String arg) {
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
//-------------------------GenericDialog--------------------------------------
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	
	GenericDialog gd = new GenericDialog(PLUGINNAME + " - set parameters");	
	//show Dialog-----------------------------------------------------------------
	//.setInsets(top, left, bottom)
	gd.setInsets(0,0,0);	gd.addMessage(PLUGINNAME + ", Version " + PLUGINVERSION + ", \u00a9 2019-2020 JN Hansen", SuperHeadingFont);
	gd.setInsets(0,0,0);	gd.addMessage("Input images: a multichannel stack containing a segmented (semibinarized or binarized) channel", InstructionsFont);
	gd.setInsets(-5,0,0);	gd.addMessage("and an unsegmented copy of the same channel (as can be generated by CiliaQ Preparator) is required.", InstructionsFont);
	
	gd.setInsets(10,0,0);	gd.addMessage("SETTINGS:", HeadingFont);	
	gd.setInsets(0,0,0);	gd.addNumericField("Channel Nr of channel that is segmented / semi-binarized / binarized", channelMask, 0);
	gd.setInsets(0,20,0);	gd.addMessage("(This is the channel where selected data is pasted into, Channel Nr: >= 1 & <= number of channels)", InstructionsFont);
	
	gd.setInsets(10,0,0);	gd.addNumericField("Channel Nr of an unmodified/unsegmented copy of the same channel", channelForCopying, 0);
	gd.setInsets(0,20,0);	gd.addMessage("(This is the channel where selected data is copied from, Channel Nr: >= 1 & <= number of channels)", InstructionsFont);
	
	gd.setInsets(10,0,0);	gd.addCheckbox("Add pixels to mask even if intensity is 0.0 in the unsegmented copy of the channel", copyZeroPixels);
	gd.setInsets(0,20,0);	gd.addMessage("(Caution: When using this option with semi-binary images, the pixels will receive an intensity of 1.0", InstructionsFont);
	gd.setInsets(0,20,0);	gd.addMessage("in the semi-binarized channel even though their actual intensity was 0.0. Thus, in this case, do not", InstructionsFont);
	gd.setInsets(0,20,0);	gd.addMessage("make use of 'reconstruction-channel'-intensity-parameters determined by CiliaQ later since they may", InstructionsFont);
	gd.setInsets(0,20,0);	gd.addMessage("be minimally falsified.", InstructionsFont);
		
	gd.setInsets(10,0,0);	gd.addMessage("SAVING:", HeadingFont);	
	gd.setInsets(5,0,0);	gd.addChoice("Output image name: ", outputVariant, chosenOutputName);
	gd.setInsets(0,20,0);	gd.addMessage("(Caution: If a file with the output filename already exists, the plugin will overwrite the file!", InstructionsFont);
	gd.setInsets(-5,20,0);	gd.addMessage("Including the date in the name of the output file prevents overwriting.)", InstructionsFont);
		
	gd.showDialog();
	//show Dialog-----------------------------------------------------------------

	//read and process variables--------------------------------------------------	
	channelMask = (int) gd.getNextNumber();
	channelForCopying = (int) gd.getNextNumber();
	copyZeroPixels = gd.getNextBoolean();
	chosenOutputName = gd.getNextChoice();	
		
	//read and process variables--------------------------------------------------
	if (gd.wasCanceled()) return;
	
	/*
	 * Test whether input error
	 * */
	boolean passSameChannelTest = true;
	if(channelMask == channelForCopying){
		passSameChannelTest = false;
	}
	if(!passSameChannelTest){
		new WaitForUserDialog("Select different channels for the segmented and the unsegmented channel.").show();
		return;
	}
		
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
//---------------------end-GenericDialog-end----------------------------------
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
	ImagePlus imp, impCopy;
	try{
		imp = WindowManager.getCurrentImage();
		impCopy = imp.duplicate();
	}catch(Exception e){
		IJ.error("No image open.");
		return;
	}
	
	//TODO Retrieve the LUTs and store them, change color of the channels for best overlap visualization, revert colors when finished.
		
	//Do editing
	EdDialog = new EditingDialog(imp, impCopy, channelMask, channelForCopying, chosenOutputName, copyZeroPixels);
	EdDialog.setLocation(0,0);
	EdDialog.setVisible(true);
	EdDialog.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(WindowEvent winEvt){
        	EdDialog.cancel();
        }
	});
	
	//prohibit closing of ImagePlus imp		
	imp.getWindow().addWindowListener(new java.awt.event.WindowAdapter(){
		@Override
		public void windowClosing(WindowEvent winEvt){
			EdDialog.closedWindow();
        }
	});	
	
	while(EdDialog.running == true){
		if(EdDialog.needWindowListener){
			EdDialog.getImp().getWindow().addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(WindowEvent winEvt){
					EdDialog.closedWindow();
		        }
			});	
			EdDialog.needWindowListener = false;
		}
		try{
			Thread.currentThread().sleep(50);
	    }catch(Exception e){
	    }
	}	
	EdDialog.dispose();
	EdDialog = null;
	System.gc();
}

public static void addFooter(TextPanel tp, Date currentDate){
	tp.append("");
	tp.append("Datafile was generated on " + FullDateFormatter2.format(currentDate) + " by '"
			+PLUGINNAME+"', an ImageJ plug-in by Jan Niklas Hansen (jan.hansen@uni-bonn.de, https://github.com/hansenjn/CiliaQ_Editor).");
	tp.append("The plug-in '"+PLUGINNAME+"' is distributed in the hope that it will be useful,"
			+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
			+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.");
	tp.append("Plug-in version:	V"+PLUGINVERSION);	
}

public static String getOneRowFooter(Date currentDate){
	return  "Datafile was generated on " + FullDateFormatter2.format(currentDate) + " by '"+PLUGINNAME
			+"', an ImageJ plug-in by Jan Niklas Hansen (jan.hansen@uni-bonn.de(jan.hansen@uni-bonn.de, https://github.com/hansenjn/CiliaQ_Editor))."
			+ "	The plug-in '"+PLUGINNAME+"' is distributed in the hope that it will be useful,"
				+ " but WITHOUT ANY WARRANTY; without even the implied warranty of"
				+ " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE."
			+"	Plug-in version:	V"+PLUGINVERSION;	
}
}//end main class
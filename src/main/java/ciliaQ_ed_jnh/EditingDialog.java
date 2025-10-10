package ciliaQ_ed_jnh;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

//For the sliders
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.TitledBorder;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.gui.YesNoCancelDialog;
import ij.io.FileInfo;
import ij.io.RoiEncoder;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.text.TextPanel;

public class EditingDialog extends javax.swing.JFrame implements ActionListener {
	/** ===============================================================================
	* Part of CiliaQ_Editor Version 0.0.4
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
	* Date: May 16, 2020 (This Version: October 10, 2025)
	*   
	* For any questions please feel free to contact me (jan.hansen@uni-bonn.de).
	* =============================================================================== */
	
	private static final long serialVersionUID = 1L;
	String dataLeft [], dataRight[], notifications [];
	public boolean running = true;
	int task, tasks;
	
	static final int ERROR = 0, NOTIFICATION = 1, LOG = 2;;
	JPanel bgPanel;
	JLabel editings;
	JButton addButton, removeButton, addAllButton, removeAllButton, finishButton, cancelButton, undoButton;
			
	private ImagePlus imp;
	private LUT imageLUTs []; // To store the original LUTs to which later can be reverted. 
	private ImagePlus impCopy;
	private String dir, name, outputPath;
	private Date startDate;
	private int mask, template;
	private boolean binary = false;
	private boolean copyZeroPx = false;
	private LinkedList<Roi> rois;
	private LinkedList<Boolean> added;
	private LinkedList<Integer> slices;
	private LinkedList<Integer> frames;
	private LinkedList<Integer> addedTogether;	// This variable is marks how many consecutive ROIs were applied together (e.g. in different slices whihc is important for doing a multi-slice undo operation
	private KeyListener keyLis;

	public boolean needWindowListener = false;
	
	//For the sliders
//	private JSlider maskMinSlider;
	private JSlider maskMaxSlider, templateMinSlider, templateMaxSlider;
	private final int SLIDER_MIN = 0;
	private final int SLIDER_MAX = 100;
	private int sliderStart;
	
	public EditingDialog (ImagePlus image, ImagePlus imageCopy, int maskChannel, int templateChannel, String savingOption, boolean copyZeroPixels) {
		super();
		imp = image;			
		impCopy = imageCopy;
				
		FileInfo info = imp.getOriginalFileInfo();
		name = info.fileName;
		dir = info.directory;
		startDate = new Date();
		outputPath = this.outputPath(savingOption);
				imp.show();
		mask = maskChannel;
		template = templateChannel;
		rois = new LinkedList<Roi>();
		added = new LinkedList<Boolean>();
		slices = new LinkedList<Integer>();
		frames = new LinkedList<Integer>();
		addedTogether = new LinkedList<Integer>();
				
		//Test if binary
		checkBinary();
		
		//If not binary but pixels not identical, throw error
		if(!binary)	checkIdentical();
		copyZeroPx = copyZeroPixels;
		
		initKeyListener();
		imp.getWindow().getCanvas().addKeyListener(keyLis);
		imp.getWindow().addKeyListener(keyLis);
		
		//Retrieve the LUTs and store them, change color of the channels for best overlap visualization, revert colors when finished.
		imp.setDisplayMode(IJ.COMPOSITE);
		imageLUTs = imp.getLuts();
		changeLUTsForVisualization();
		
		initGUI();	
	}
	
	private void initGUI() {
		int prefXSize = 400, prefYSize = 510;
		this.setMinimumSize(new java.awt.Dimension(280, prefYSize));
		this.setSize(prefXSize, prefYSize);
		this.setTitle(CiliaQEdMain.PLUGINNAME + " version " + CiliaQEdMain.PLUGINVERSION);
		getContentPane().setSize(prefXSize,prefYSize);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		int locHeight = 30;
		bgPanel = new JPanel();
		bgPanel.setLayout(new BoxLayout(bgPanel, BoxLayout.Y_AXIS));
		bgPanel.setVisible(true);
		bgPanel.setMinimumSize(new java.awt.Dimension(getContentPane().getSize().width,getContentPane().getSize().height));
		bgPanel.setAlignmentX(CENTER_ALIGNMENT);
		bgPanel.setAlignmentY(CENTER_ALIGNMENT);
		getContentPane().add(bgPanel);
		{
			JScrollPane scroll = new JScrollPane();
			scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scroll.setVisible(true);
			{
				JTextPane reference = new JTextPane();
				reference.setText("**Info**\nCiliaQ Editor is an ImageJ plugin by Jan N. Hansen (\u00a9 2025). "
					    + "How to cite and use: https://github.com/hansenjn/CiliaQ_Editor\n"
					    + "\n**Manual**\n"					    
					    + "Draw a selection/ROI where you want to correct the mask and use one of the following options:\n"					
					    + "- Press 'add selection' (F1) - Add pixels in the selection to the mask\n"					
					    + "- Press 'remove selection' (F2) - Remove pixels in the selection from the mask\n"					
					    + "- Press 'add sel. to all slices' (F3) - Add selection to mask across all Z-slices\n"					
					    + "- Press 'remove sel. from all slices' (F4) - Remove selection from mask across all Z-slices. "					    
					    + "  WARNING: Make sure that you do not accidentally remove or connect cilia in other Z-slices if you add or remove selections in the whole stack.\n"				
					    + "- Press 'undo last editing' (Ctrl+Z) - Revert the last edit made\n\n"	
					
					    + "**Display Settings**\n"
					    + "For better visualization, the mask channel is shown in Magenta and the template in Green.\n"
					    + "The mask display range is set to 0-5 and the template to 0-99.9th percentile.\n"
					    + "When you save your editing or abort editing, the colors will be reverted to the original colors.\n"
					    + "You can use the sliders below to adjust the intensity display range for your convenience.\n\n"
					    
					    + "When finished, press 'finish analysis & save editings' to save your work;\n"								
					    + "In turn, the plugin will create a new file with ending _Ed and metadata about the changes you applied.\n"		
					    + "To discard all changes, press 'abort analysis & discard editings'.\n");
				
				reference.setFont(CiliaQEdMain.TextFont);
				reference.setVisible(true);
				reference.setEditable(false);
				scroll.add(reference);
				scroll.setViewportView(reference);
			}
			bgPanel.add(scroll);
		}	
		{
			JPanel top = new JPanel();
			top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
			bgPanel.add(top);
			
			top.add(new JPanel());
			editings = new JLabel("Editings performed: " + rois.size());
			editings.setMinimumSize(new java.awt.Dimension(bgPanel.getWidth(),30));
			editings.setFont(CiliaQEdMain.TextFont);
			editings.setVisible(true);
			editings.setHorizontalAlignment(SwingConstants.CENTER);
			top.add(editings);
			top.add(new JPanel());
		}
		{
			{
				addButton = new JButton();
				addButton.addActionListener(this);
				addButton.setText("add selection (F1)");
				addButton.setFont(CiliaQEdMain.TextFont);
				addButton.setMinimumSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				addButton.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				addButton.setAlignmentX(CENTER_ALIGNMENT);
				addButton.setAlignmentY(CENTER_ALIGNMENT);
				addButton.setHorizontalAlignment(SwingConstants.CENTER);
				addButton.setVerticalAlignment(SwingConstants.CENTER);
				addButton.setVisible(true);
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BorderLayout());
				buttonPanel.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				buttonPanel.add(addButton);
				bgPanel.add(buttonPanel);
			}
			{
				removeButton = new JButton();
				removeButton.addActionListener(this);
				removeButton.setText("remove selection (F2)");
				removeButton.setFont(CiliaQEdMain.TextFont);
				removeButton.setMinimumSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				removeButton.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				removeButton.setAlignmentX(CENTER_ALIGNMENT);
				removeButton.setAlignmentY(CENTER_ALIGNMENT);
				removeButton.setHorizontalAlignment(SwingConstants.CENTER);
				removeButton.setVerticalAlignment(SwingConstants.CENTER);
				removeButton.setVisible(true);
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BorderLayout());
				buttonPanel.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				buttonPanel.add(removeButton);
				bgPanel.add(buttonPanel);
			}
			{
				addAllButton = new JButton();
				addAllButton.addActionListener(this);
				addAllButton.setText("add sel. to all slices (F3)");
				addAllButton.setFont(CiliaQEdMain.TextFont);
				addAllButton.setMinimumSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				addAllButton.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				addAllButton.setAlignmentX(CENTER_ALIGNMENT);
				addAllButton.setAlignmentY(CENTER_ALIGNMENT);
				addAllButton.setHorizontalAlignment(SwingConstants.CENTER);
				addAllButton.setVerticalAlignment(SwingConstants.CENTER);
				addAllButton.setVisible(true);
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BorderLayout());
				buttonPanel.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				buttonPanel.add(addAllButton);
				bgPanel.add(buttonPanel);
			}
			{
				removeAllButton = new JButton();
				removeAllButton.addActionListener(this);
				removeAllButton.setText("remove sel. from all slices (F4)");
				removeAllButton.setFont(CiliaQEdMain.TextFont);
				removeAllButton.setMinimumSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				removeAllButton.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				removeAllButton.setAlignmentX(CENTER_ALIGNMENT);
				removeAllButton.setAlignmentY(CENTER_ALIGNMENT);
				removeAllButton.setHorizontalAlignment(SwingConstants.CENTER);
				removeAllButton.setVerticalAlignment(SwingConstants.CENTER);
				removeAllButton.setVisible(true);
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BorderLayout());
				buttonPanel.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				buttonPanel.add(removeAllButton);
				bgPanel.add(buttonPanel);
			}
			{
				undoButton = new JButton();
				undoButton.addActionListener(this);
				undoButton.setText("undo last editing (Ctrl+Z)");
				undoButton.setFont(CiliaQEdMain.TextFont);
				undoButton.setMinimumSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				undoButton.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				undoButton.setAlignmentX(CENTER_ALIGNMENT);
				undoButton.setAlignmentY(CENTER_ALIGNMENT);
				undoButton.setHorizontalAlignment(SwingConstants.CENTER);
				undoButton.setVerticalAlignment(SwingConstants.CENTER);
				undoButton.setVisible(true);
				undoButton.setEnabled(false);
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BorderLayout());
				buttonPanel.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				buttonPanel.add(undoButton);
				bgPanel.add(buttonPanel);
			}
		}
		
		{
			JPanel spacer = new JPanel();
			spacer.setMaximumSize(new java.awt.Dimension(prefXSize,10));
			spacer.setVisible(true);
			bgPanel.add(spacer);
		}
		
		//Sliders
		{
			{
			    // Mask channel intensity sliders
			    JPanel maskSliderPanel = new JPanel();
			    maskSliderPanel.setLayout(new BoxLayout(maskSliderPanel, BoxLayout.Y_AXIS));
			    maskSliderPanel.setBorder(new TitledBorder("Segmented channel (ID=" + mask + ") - Display Range (Min fixed to 0)"));
			    	
//			    JPanel maskMinPanel = new JPanel(new BorderLayout());
//			    maskMinPanel.add(new JLabel("Min: "), BorderLayout.WEST);
//			    maskMinSlider = new JSlider(JSlider.HORIZONTAL, SLIDER_MIN, SLIDER_MAX, SLIDER_MIN);
//			    maskMinSlider.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth() - 60, 25));
//			    maskMinSlider.addChangeListener(new ChangeListener() {
//			        public void stateChanged(ChangeEvent e) {
//			            updateMaskDisplayRange();
//			        }
//			    });	
//			    maskMinPanel.add(maskMinSlider, BorderLayout.CENTER);
//			    maskSliderPanel.add(maskMinPanel);
		
			    JPanel maskMaxPanel = new JPanel(new BorderLayout());	
			    maskMaxPanel.add(new JLabel("Max: "), BorderLayout.WEST);	
			    maskMaxSlider = new JSlider(JSlider.HORIZONTAL, SLIDER_MIN, SLIDER_MAX, 5);	
			    maskMaxSlider.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth() - 60, 25));	
			    maskMaxSlider.addChangeListener(new ChangeListener() {	
			        public void stateChanged(ChangeEvent e) {	
			            updateMaskDisplayRange();	
			        }	
			    });	
			    maskMaxPanel.add(maskMaxSlider, BorderLayout.CENTER);	
			    maskSliderPanel.add(maskMaxPanel);			    
		
			    bgPanel.add(maskSliderPanel);	
			}		
			{	
			    // Template channel intensity sliders	
			    JPanel templateSliderPanel = new JPanel();	
			    templateSliderPanel.setLayout(new BoxLayout(templateSliderPanel, BoxLayout.Y_AXIS));	
			    templateSliderPanel.setBorder(new TitledBorder("Unsegmented channel (ID=" + template + ") - Display Range"));	
			    	
			    JPanel templateMinPanel = new JPanel(new BorderLayout());	
			    templateMinPanel.add(new JLabel("Min: "), BorderLayout.WEST);	
			    templateMinSlider = new JSlider(JSlider.HORIZONTAL, SLIDER_MIN, SLIDER_MAX, SLIDER_MIN);	
			    templateMinSlider.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth() - 60, 25));	
			    templateMinSlider.addChangeListener(new ChangeListener() {	
			        public void stateChanged(ChangeEvent e) {	
			            updateTemplateDisplayRange();	
			        }	
			    });	
			    templateMinPanel.add(templateMinSlider, BorderLayout.CENTER);	
			    templateSliderPanel.add(templateMinPanel);		    
		
			    JPanel templateMaxPanel = new JPanel(new BorderLayout());	
			    templateMaxPanel.add(new JLabel("Max: "), BorderLayout.WEST);
			    imp.setC(template);
			    templateMaxSlider = new JSlider(JSlider.HORIZONTAL, SLIDER_MIN, SLIDER_MAX, sliderStart);	
			    templateMaxSlider.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth() - 60, 25));	
			    templateMaxSlider.addChangeListener(new ChangeListener() {	
			        public void stateChanged(ChangeEvent e) {	
			            updateTemplateDisplayRange();	
			        }	
			    });	
			    templateMaxPanel.add(templateMaxSlider, BorderLayout.CENTER);	
			    templateSliderPanel.add(templateMaxPanel);
		  
			    bgPanel.add(templateSliderPanel);	
			}
		}
		
		{
			JPanel spacer = new JPanel();
			spacer.setMaximumSize(new java.awt.Dimension(prefXSize,10));
			spacer.setVisible(true);
			bgPanel.add(spacer);
		}
		{
			{
				finishButton = new JButton();
				finishButton.addActionListener(this);
				finishButton.setText("finish analysis & save editings");
				finishButton.setFont(CiliaQEdMain.BoldFont);
				finishButton.setMinimumSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				finishButton.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				finishButton.setAlignmentX(CENTER_ALIGNMENT);
				finishButton.setAlignmentY(CENTER_ALIGNMENT);
				finishButton.setHorizontalAlignment(SwingConstants.CENTER);
				finishButton.setVerticalAlignment(SwingConstants.CENTER);
				finishButton.setVisible(true);
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BorderLayout());
				buttonPanel.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				buttonPanel.add(finishButton);
				bgPanel.add(buttonPanel);
			}
			{
				cancelButton = new JButton();
				cancelButton.addActionListener(this);
				cancelButton.setText("abort analysis & discard editings");
				cancelButton.setFont(CiliaQEdMain.BoldFont);
				cancelButton.setMinimumSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				cancelButton.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				cancelButton.setAlignmentX(CENTER_ALIGNMENT);
				cancelButton.setAlignmentY(CENTER_ALIGNMENT);
				cancelButton.setHorizontalAlignment(SwingConstants.CENTER);
				cancelButton.setVerticalAlignment(SwingConstants.CENTER);
				cancelButton.setVisible(true);
				cancelButton.setForeground(Color.red);
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BorderLayout());
				buttonPanel.setPreferredSize(new java.awt.Dimension(bgPanel.getWidth(), locHeight));
				buttonPanel.add(cancelButton);
				bgPanel.add(buttonPanel);
			}
		}
	}

	private void initKeyListener(){
		keyLis = new KeyListener(){
			@Override
			public void keyTyped(KeyEvent e) {
				
			}

			@Override
			public void keyPressed(KeyEvent e) {
				
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_Z && e.isControlDown()){
					undo();
				}else if(e.getKeyCode() == KeyEvent.VK_F1){
					addRoi();
				}else if(e.getKeyCode() == KeyEvent.VK_F2){
					removeRoi();
				}else if(e.getKeyCode() == KeyEvent.VK_F3){
					addRoiAll();
				}else if(e.getKeyCode() == KeyEvent.VK_F4){
					removeRoiAll();
				}
				imp.updateAndRepaintWindow();
				editings.setText("Editings performed: " + rois.size());
			}
		};
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		Object eventSource = ae.getSource();
//	    IJ.log("Action event received from: " + eventSource);		
	    try {
	    	if (eventSource == addButton){
				addRoi();
			}else if (eventSource == removeButton){
				removeRoi();
			}else if (eventSource == addAllButton){
				addRoiAll();
			}else if (eventSource == removeAllButton){
				removeRoiAll();
			}else if (eventSource == undoButton){
				undo();
			}else if (eventSource == finishButton){
				YesNoCancelDialog ync = new YesNoCancelDialog(this,"CiliaQ Editor - finish?","Do you want to finish editing and save the results?");
				if(ync.yesPressed()){
					restoreOriginalLUTs(); // switch back changed color schemes
					saveAndClose();
				}			
			}else if (eventSource == cancelButton){
				this.restoreOriginalLUTs(); // switch back color schemes
				cancel();				
			}else {
				IJ.log("Unknown source: " + eventSource);
			}
	    	
		    
			imp.updateAndRepaintWindow();
			imp.updateAndDraw();
			editings.setText("Editings performed: " + rois.size());
			
			bgPanel.revalidate();
			bgPanel.repaint();
	    } catch (Exception ex) {
	        IJ.log("Exception in button action - Message: " + ex.getMessage());
	        IJ.log("Exception in button action - Localized Message: " + ex.getLocalizedMessage());
	        String out = "";
			for(int err = 0; err < ex.getStackTrace().length; err++){
				out += " \n " + ex.getStackTrace()[err].toString();
			}			
			IJ.log("Exception in button action. Stack trace:\n" + out);
	    }		
	}
	
	private void undo(){
		if(rois.size()>0){
			//First we fill all pixels that were subjected to changes by adding or removing with their original intensities, retrieved from a copy of the image
			for(int i = 0; i < rois.size(); i++){
				Roi roi = rois.get(i);
				int s = slices.get(i);
				int t = frames.get(i);
				int indexCopy = impCopy.getStackIndex(mask, s, t)-1;
				int indexPaste = imp.getStackIndex(mask, s, t)-1;
				
				Rectangle r = roi.getBounds();
		   		for(int x = r.x; x <= r.x+r.width && x < imp.getWidth(); x++){
		   			if(x < 0) x = 0;
		   			for(int y = r.y; y <= r.y+r.height && y < imp.getHeight(); y++){
		   				if(y < 0) y = 0;
		   				if(roi.contains(x, y)){
		   						imp.getStack().setVoxel(x, y, indexPaste,
		   	   						impCopy.getStack().getVoxel(x, y, indexCopy));
		   				}
					}	
				}
			}
			
			//Now, we remove the ROI(s) from the list that need to be "undown"
			removeLastRois();
			
			//Now we apply all copy paste steps again
			for(int i = 0; i < rois.size(); i++){
				if(added.get(i)){
					copy(rois.get(i), slices.get(i), frames.get(i));
				}else{
					remove(rois.get(i), slices.get(i), frames.get(i));
				}
			}
			
			if(rois.size()==0){
				undoButton.setEnabled(false);
			}
		}
	}
	
	private void addRoi(){
		Roi roi = imp.getRoi();
		if(roi != null){
	   		saveStep(roi, true, imp.getSlice(), imp.getFrame(), 1);
			copy(roi, imp.getSlice(), imp.getFrame());
		}
		if(!undoButton.isEnabled()){
			undoButton.setEnabled(true);
		}
	}
	
	private void addRoiAll(){
		Roi roi = imp.getRoi();
		if(roi != null){
	   		saveStepToMultipleSlices(roi, true, 1, imp.getNSlices(), imp.getFrame());
	   		for(int s = 1; s <= imp.getNSlices(); s++) {
	   			copy(roi, s, imp.getFrame());
	   		}
		}
		if(!undoButton.isEnabled()){
			undoButton.setEnabled(true);
		}
	}
	
	private void removeRoi(){
		Roi roi = imp.getRoi();
		if(roi != null){
	   		saveStep(roi, false, imp.getSlice(), imp.getFrame(), 1);
			remove(roi, imp.getSlice(), imp.getFrame());
		}
		if(!undoButton.isEnabled()){
			undoButton.setEnabled(true);
		}
	}
	
	private void removeRoiAll(){
		Roi roi = imp.getRoi();
		if(roi != null){
	   		saveStepToMultipleSlices(roi, false, 1, imp.getNSlices(), imp.getFrame());
			for(int s = 1; s <= imp.getNSlices(); s++) {
				remove(roi, s, imp.getFrame());				
			}
		}
		if(!undoButton.isEnabled()){
			undoButton.setEnabled(true);
		}
	}
	
	private void saveAndClose(){
		RoiEncoder re;
		IJ.saveAsTiff(imp, outputPath);
		saveSettings(outputPath);
		try{
			new File(outputPath).mkdirs();
			for(int i = 0; i < rois.size(); i++){
				if(added.get(i)){
					re = new RoiEncoder(outputPath + System.getProperty("file.separator") + (i+1) 
							+ "_s" + slices.get(i) + "_t" + frames.get(i) + "_add.roi");
				}else{
					re = new RoiEncoder(outputPath + System.getProperty("file.separator") + (i+1) 
							+ "_s" + slices.get(i) + "_t" + frames.get(i) + "_rem.roi");					
				}
				re.write(rois.get(i));				
			}
		}catch(Exception e){
			JOptionPane.showMessageDialog(this, "Failed to correctly save a folder with the rois!");
		}
		running = false;
	}	

	public void cancel(){
		if(rois.size()!=0){
			YesNoCancelDialog ync = new YesNoCancelDialog(this,"CiliaQ Editor - abort?","Do you really wish to stop editing without saving changes?");
			if(ync.yesPressed()){
				running = false;
			}
		}else{
			running = false;
		}
	}
	
	public void closedWindow(){
		if(rois.size()==0){
			running = false;
			return;
		}
		YesNoCancelDialog ync = new YesNoCancelDialog(this,"CiliaQ Editor - abort?","You closed the image.\nRestore the image with editings to continue editing (Yes, Cancel)?\nOr stop editing without saving editings (No)?");
		if(!ync.yesPressed() && !ync.cancelPressed()){
			running = false;
		}else{
			imp = impCopy.duplicate();
			for(int i = 0; i < rois.size(); i++){
				if(added.get(i)){
					copy(rois.get(i), slices.get(i), frames.get(i));
				}else{
					remove(rois.get(i), slices.get(i), frames.get(i));
				}
			}
			imp.show();
			imp.setTitle(imp.getTitle().replace("DUP_", ""));
			needWindowListener = true;
		}
	}
	
	public ImagePlus getImp(){
		return imp;
	}
	
	private void copy (Roi roi, int s, int t){
		int indexCopy = imp.getStackIndex(template, s, t)-1;
		int indexPaste = imp.getStackIndex(mask, s, t)-1;
   		Rectangle r = roi.getBounds();
		double max = Math.pow(2.0, imp.getBitDepth())-1;
   		for(int x = r.x; x <= r.x+r.width && x < imp.getWidth(); x++){
   			if(x < 0) x = 0;
   			for(int y = r.y; y <= r.y+r.height && y < imp.getHeight(); y++){
   				if(y < 0) y = 0;
   				if(roi.contains(x, y)){
   					if(binary){
   						if(imp.getStack().getVoxel(x, y, indexCopy) != 0.0 || copyZeroPx){
   	   	   					imp.getStack().setVoxel(x, y, indexPaste, max);
   						}
   					}else{
   						if(imp.getStack().getVoxel(x, y, indexCopy) == 0.0 && copyZeroPx){
   							imp.getStack().setVoxel(x, y, indexPaste, 1.0);
   						}else {
   							imp.getStack().setVoxel(x, y, indexPaste,
   									imp.getStack().getVoxel(x, y, indexCopy));
   						}
   					}
   				}
			}	
		}
	}
	
	private void remove (Roi roi, int s, int t){
		int indexPaste = imp.getStackIndex(mask, s, t)-1;
   		Rectangle r = roi.getBounds();
   		for(int x = r.x; x <= r.x+r.width && x < imp.getWidth(); x++){
   			if(x < 0) x = 0;
   			for(int y = r.y; y <= r.y+r.height && y < imp.getHeight(); y++){
   				if(y < 0) y = 0;
   				if(roi.contains(x, y)){
   					imp.getStack().setVoxel(x, y, indexPaste,0.0);
   				}
			}	
		}
	}
	
	private void saveStep(Roi roi, boolean add, int slice, int frame, int addTogether){
		rois.add((Roi)((Object)roi.clone()));
		added.add(add);
		slices.add(slice);
		frames.add(frame);
		addedTogether.add(addTogether);
	}
	
	private void saveStepToMultipleSlices(Roi roi, boolean add, int sliceMin, int sliceMax, int frame) {
		for(int s = sliceMin; s <= sliceMax; s++) {
			saveStep(roi,add,s,frame, sliceMax-sliceMin+1);
		}
	}
	
	private void removeLastRois(){
		int at = 1;
		if(addedTogether.get(addedTogether.size()-1) <= 0){
			IJ.error("Error when applying undo action! Report error to developer please!");
		}else if(addedTogether.get(addedTogether.size()-1) > 1){
			at = addedTogether.get(addedTogether.size()-1);
			
		}
		for(int i = 0; i < at; i++) {
			rois.remove(rois.size()-1);
			added.remove(added.size()-1);
			slices.remove(slices.size()-1);
			frames.remove(frames.size()-1);
			addedTogether.remove(addedTogether.size()-1);				
		}
	}
	
	private String outputPath(String chosenOutputName){
		//Create output filename
	   String filePrefix;
		if(name.contains(".")){
			filePrefix = name.substring(0,name.lastIndexOf("."));
		}else{
			filePrefix = name;
		}
		
		if(chosenOutputName.equals(CiliaQEdMain.outputVariant[1])){
			//saveDate
			filePrefix += "_ed_" + CiliaQEdMain.NameDateFormatter.format(startDate);
		}else if(chosenOutputName.equals(CiliaQEdMain.outputVariant[0])){
			filePrefix += "_ed";
		}
		
		filePrefix = dir + filePrefix;
		
		return filePrefix;
	}
	
	private void checkBinary(){
		binary = true;
		double max = Math.pow(2.0, imp.getBitDepth())-1;
		for(int s = 0; s < imp.getNSlices(); s++){
			for(int f = 0; f < imp.getNFrames(); f++){
				for(int x = 0; x < imp.getWidth(); x++){
					for(int y = 0; y < imp.getHeight(); y++){
						if(imp.getStack().getVoxel(x, y, imp.getStackIndex(mask, s+1, f+1)-1) != max
								&& imp.getStack().getVoxel(x, y, imp.getStackIndex(mask, s+1, f+1)-1) != 0.0){
							binary = false;
							return;
						}
					}
				}
			}
		}
	}
	

	private void checkIdentical() {
		for(int s = 0; s < imp.getNSlices(); s++){
			for(int f = 0; f < imp.getNFrames(); f++){
				for(int x = 0; x < imp.getWidth(); x++){
					for(int y = 0; y < imp.getHeight(); y++){
						if(imp.getStack().getVoxel(x, y, imp.getStackIndex(mask, s+1, f+1)-1) != 0.0
								&& imp.getStack().getVoxel(x, y, imp.getStackIndex(mask, s+1, f+1)-1) !=
								imp.getStack().getVoxel(x, y, imp.getStackIndex(template, s+1, f+1)-1)){
							JOptionPane.showMessageDialog(this, "Plugin canceled because editing prohibited:\npixel intensities in the segmented "
									+ "channel do not match\nthe intensities in the unsegmented "
									+ "channel.");
							running = false;
							return;
						}
					}
				}
			}
		}
	}
	
	private void saveSettings(String path){
		TextPanel tp = new TextPanel("metadata");
		tp.append("Saving date:	" + CiliaQEdMain.FullDateFormatter.format(startDate));
		tp.append("Image name:	" + name);
		tp.append("Channel Nr of channel that is segmented / semi-binarized / binarized (>= 1 & <= nr of channels):	" + mask);
		tp.append("Channel Nr of an unmodified/unsegmented copy of the same channel (>= 1 & <= nr of channels):	" + template);
		tp.append("Number of edits:	" +  rois.size());
		tp.append("Individual edits:	Roi nr	added/removed	slice	frame");
		String appText;
		for(int i = 0; i < rois.size(); i++){
			appText = ("	" + (i+1) + "	");
			if(added.get(i)){
				appText += "added";
			}else{
				appText += "removed";
			}
			appText +=	"	" + slices.get(i) + "	" + frames.get(i);
			tp.append(appText);
		}
		CiliaQEdMain.addFooter(tp, startDate);		
		tp.saveAs(path + ".txt");
	}
	

	// Change LUTs for specific channels (mask to Magenta, template to Green)
	private void changeLUTsForVisualization() {
	    // Check if image is composite
	    CompositeImage ci = null;
	    if (imp.isComposite()) {
	        ci = (CompositeImage) imp;
	    }else {
	    	new WaitForUserDialog("Could not switch colors automatically since no composite image.").show();
	    	return;
	    }

	    // Create Magenta LUT for mask channel
	    byte[] rMask = new byte[256];
	    byte[] gMask = new byte[256];
	    byte[] bMask = new byte[256];
	    for (int i=0; i<256; i++) {
	        rMask[i] = (byte)i;
	        gMask[i] = 0;
	        bMask[i] = (byte)i;
	    }
	    LUT maskLut = new LUT(rMask, gMask, bMask);

	    // Create Green LUT for template channel
	    byte[] rTemplate = new byte[256];
	    byte[] gTemplate = new byte[256];
	    byte[] bTemplate = new byte[256];

	    for (int i=0; i<256; i++) {
	        rTemplate[i] = 0;
	        gTemplate[i] = (byte)i;
	        bTemplate[i] = 0;
	    }
	    LUT templateLut = new LUT(rTemplate, gTemplate, bTemplate);
	    
	    // Apply LUTs
	    if (ci != null) {
	        ci.setChannelLut(maskLut, mask);
	        ci.setChannelLut(templateLut, template);
	        ci.updateAndDraw();
	    }
	    
	    // Optimize display range for mask channel
	    if (ci != null) {
	    	 // Store current position
	        int currentChannel = ci.getChannel();
	        int currentSlice = ci.getSlice();
	        int currentFrame = ci.getFrame();
	        
	        // Set to mask channel
	        ci.setPosition(mask, currentSlice, currentFrame);	        

	        // Set display range to 0-1
	        ci.setDisplayRange(0.0, 5.0);
	        ci.updateAndDraw();

	        // Set to template channel
	        ci.setPosition(template, currentSlice, currentFrame);	        

	        // Calculate and store the percentile value for template slider scaling
	        double percValue = calculatePercentileValue(ci.getProcessor(), 99.9);
	        sliderStart = (int) (percValue / (Math.pow(2.0, imp.getBitDepth())-1) * 100.0);
	        ci.setDisplayRange(0.0, percValue);
	        ci.updateAndDraw();
	        
	        // Restore original position
	        ci.setPosition(currentChannel, currentSlice, currentFrame);
	    }        
	}

	// Restore original LUTs before saving
	private void restoreOriginalLUTs() {
	    if (imp.isComposite()) {
	        CompositeImage ci = (CompositeImage) imp;
	        for (int c = 0; c < imageLUTs.length; c++) {
	            ci.setChannelLut(imageLUTs[c], c+1);
	        }
	        ci.updateAndDraw();
	    } else if (imageLUTs.length > 0) {
	        // For single channel images (unlikely to happen but lets keep this metehod general)
	        imp.setLut(imageLUTs[0]);
	    }
	}
	
	/**
	 * Calculates the pixel value at the specified percentile in an image
	 *
	 * @param ip The ImageProcessor containing the pixel data
	 * @param percentile The percentile to find (0-100)
	 * @return The pixel value at the specified percentile
	 */
	private static double calculatePercentileValue(ImageProcessor ip, double percentile) {
	    // Get image histogram
	    int[] histogram = ip.getHistogram();
	    int histogramLength = histogram.length;	    

	    // Calculate the total number of pixels
	    long totalPixels = 0;
	    for (int i = 0; i < histogramLength; i++) {
	        totalPixels += histogram[i];
	    }	    

	    // Calculate the number of pixels below the percentile
	    long pixelsBelow = (long)(totalPixels * percentile / 100.0);	    

	    // Find the pixel value at the percentile
	    long count = 0;
	    int percentileValue = 0;
	    for (int i = 0; i < histogramLength; i++) {
	        count += histogram[i];
	        if (count >= pixelsBelow) {
	            percentileValue = i;
	            break;
	        }
	    }

	    return percentileValue;
	}
	
	private void updateMaskDisplayRange() {
	    if (imp.isComposite()) {
	        CompositeImage ci = (CompositeImage) imp;
	        
	        // Store current position
	        int currentChannel = ci.getChannel();
	        int currentSlice = ci.getSlice();
	        int currentFrame = ci.getFrame();

	        // Calculate min and max values
//	        double min = maskMinSlider.getValue();  // 0-5 scale
	        double min = 0.0;
	        double max = maskMaxSlider.getValue();  // 0-5 scale

//	        if (min >= max) {
//	        	min = max;
//	            maskMinSlider.setValue((int)max); // Update slider without triggering listener
//	        }
	        
//	        min *= (Math.pow(2.0, imp.getBitDepth())-1)/100.0;
	        max *= (Math.pow(2.0, imp.getBitDepth())-1)/100.0;
	        
	        // Do not allow max < 1 to avoid that mask disappears
	        if(max < 1.0) max = 1.0;

	        // Set to mask channel
	        ci.setPosition(mask, currentSlice, currentFrame);

	        // Set display range
	        ci.setDisplayRange(min, max);
	        ci.updateAndDraw();
	        
	        // Restore original position
	        ci.setPosition(currentChannel, currentSlice, currentFrame);
	    }
	}

	private void updateTemplateDisplayRange() {
	    if (imp.isComposite()) {
	        CompositeImage ci = (CompositeImage) imp;

	        // Store current position
	        int currentChannel = ci.getChannel();
	        int currentSlice = ci.getSlice();
	        int currentFrame = ci.getFrame();
	        

	        // Calculate min and max values (0-templateMaxValue)
	        double min = templateMinSlider.getValue();
	        double max = templateMaxSlider.getValue(); 

	        if (min >= max) {
	            min = max;
	            templateMinSlider.setValue((int)(max));
	        }
	        
	        min *= (Math.pow(2.0, imp.getBitDepth())-1)/100.0;
	        max *= (Math.pow(2.0, imp.getBitDepth())-1)/100.0;
       
	        // Set to template channel
	        ci.setPosition(template, currentSlice, currentFrame);

	        // Set display range
	        ci.setDisplayRange(min, max);
	        ci.updateAndDraw();

	        // Restore original position
	        ci.setPosition(currentChannel, currentSlice, currentFrame);
	    }

	}
}
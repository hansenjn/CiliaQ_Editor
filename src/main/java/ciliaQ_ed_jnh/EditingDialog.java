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

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.YesNoCancelDialog;
import ij.io.FileInfo;
import ij.io.RoiEncoder;
import ij.text.TextPanel;

public class EditingDialog extends javax.swing.JFrame implements ActionListener {
	/** ===============================================================================
	* Part of CiliaQ_Editor Version 0.0.2
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
	* Date: May 16, 2020 (This Version: May 22, 2020)
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
	JButton addButton, removeButton, finishButton, cancelButton, undoButton;
			
	private ImagePlus imp;
	private ImagePlus impCopy;
	private String dir, name, outputPath;
	private Date startDate;
	private int mask, template;
	private boolean binary = false;
	private LinkedList<Roi> rois;
	private LinkedList<Boolean> added;
	private LinkedList<Integer> slices;
	private LinkedList<Integer> frames;
	private KeyListener keyLis;

	public boolean needWindowListener = false;
	
	public EditingDialog (ImagePlus image, ImagePlus imageCopy, int maskChannel, int templateChannel, String savingOption) {
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
				
		//Test if binary
		checkBinary();
		
		//If not binary but pixels not identical, throw error
		if(!binary)	checkIdentical();

		initKeyListener();
		imp.getWindow().getCanvas().addKeyListener(keyLis);
		imp.getWindow().addKeyListener(keyLis);
		initGUI();	
	}
	
	private void initGUI() {
		int prefXSize = 280, prefYSize = 350;
		this.setMinimumSize(new java.awt.Dimension(prefXSize, prefYSize));
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
				reference.setText("**Info**\nCiliaQ Editor is an ImageJ plugin by Jan N. Hansen (\u00a9 2020). "
						+ "How to cite and use: https://github.com/hansenjn/CiliaQ_Editor\n"
						+ "\n**Manual**\nDraw a selection/Roi where you want to correct the mask and press add selection or F1 to add the pixels in the"
						+ " selection to the mask or press remove selection or F2 to remove the pixels in the selection from the mask.");
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
				}
				imp.updateAndRepaintWindow();
				editings.setText("Editings performed: " + rois.size());
			}
		};
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		Object eventQuelle = ae.getSource();
		if (eventQuelle == addButton){
			addRoi();
		}else if (eventQuelle == removeButton){
			removeRoi();
		}else if (eventQuelle == undoButton){
			undo();
		}else if (eventQuelle == finishButton){
			YesNoCancelDialog ync = new YesNoCancelDialog(this,"CiliaQ Editor - finish?","Do you want to finish editing and save the results?");
			if(ync.yesPressed()){
				saveAndClose();
			}			
		}else if (eventQuelle == cancelButton){
			cancel();				
		}
		imp.updateAndRepaintWindow();
		editings.setText("Editings performed: " + rois.size());
	}
	
	private void undo(){
		if(rois.size()>0){
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
			
			removeLastRoi();			
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
		if(!roi.equals(null)){
	   		saveStep(roi, true, imp.getSlice(), imp.getFrame());
			copy(roi, imp.getSlice(), imp.getFrame());
		}
		if(!undoButton.isEnabled()){
			undoButton.setEnabled(true);
		}
	}
	
	private void removeRoi(){
		Roi roi = imp.getRoi();
		if(!roi.equals(null)){
	   		saveStep(roi, false, imp.getSlice(), imp.getFrame());
			remove(roi, imp.getSlice(), imp.getFrame());
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
				re = new RoiEncoder(outputPath + System.getProperty("file.separator") + (i+1) + ".roi");
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
   						if(imp.getStack().getVoxel(x, y, indexCopy) != 0.0){
   	   	   					imp.getStack().setVoxel(x, y, indexPaste, max);   							
   						}   						
   					}else{
   	   					imp.getStack().setVoxel(x, y, indexPaste,
   	   						imp.getStack().getVoxel(x, y, indexCopy));
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
	
	private void saveStep(Roi roi, boolean add, int slice, int frame){
		rois.add((Roi)((Object)roi.clone()));
		added.add(add);
		slices.add(slice);
		frames.add(frame);
	}
	
	private void removeLastRoi(){
		rois.remove(rois.size()-1);
		added.remove(added.size()-1);
		slices.remove(slices.size()-1);
		frames.remove(frames.size()-1);
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
}
package functionality.backend;

import static util.FunctionalityUtil.intToLetter;
import static util.FunctionalityUtil.setupIntToLetterMap;
import static util.GUIUtil.LIGHT_GREY;
import functionality.KoraSteps;
import functionality.StatusDistributionManager;
import functionality.listeners.AnalyseButtonListener;
import functionality.listeners.AnalyseSimonButtonListener;
import functionality.listeners.ChangeEpochButtonListener;
import functionality.listeners.PlotAllButtonListener;
import functionality.listeners.PlotRelevantButtonListener;
import functionality.listeners.SelectionAdapterBase;
import functionality.listeners.WearingTimeButtonListener;
import gui.MainControls;

import java.io.File;
import java.util.ArrayList;

import log.Logger;
import log.MessageType;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import util.CsvFileLoader;

/**Functionality of {@link MainControls} is implemented here.
 * 
 * @author Johannes
 *
 */
public class MainControlsBackend extends MainControls {

	/** possible devices */
	private String[] devices = {"ActiGraph (gt3x, gt3x+)","GENEActive"};
	/** possible data measurements*/
	private String[] data = {"count/epoch measurement", "raw data measurement"};
	/** The currently used csv File loader*/
	private CsvFileLoader csvFile;
	/** current File name*/
	public String currentFile;
	/** current File path*/
	public String currentPath;
	/** The plot button Listener*/
	private PlotAllButtonListener plotAllListener;
	/** Listener for epoch changing*/
	private ChangeEpochButtonListener changeEpochButtonListener;

	
	/**Constructor */
	public MainControlsBackend(Composite parent, int style) {
		super(parent, style);
		setupFunctionality();
	}

	/** Sets up functionality of all gui elements*/
	private void setupFunctionality() {
		// ----------------------------------------------------------------------------
		// device selection
		deviceCombo.setItems(devices);
		deviceCombo.select(0);
		// will be anabled in future releases
		deviceCombo.setEnabled(false);
		// ----------------------------------------------------------------------------
		// data type selection
		dataCombo.setItems(data);
		dataCombo.select(0);
		// will be enabled in future releases
		dataCombo.setEnabled(false);
		// ----------------------------------------------------------------------------
		// epoch periode
		epochScale.addSelectionListener(new EpochScaleSelectionListener());
		// ----------------------------------------------------------------------------
		// create distribution manager
		StatusDistributionManager distributionManager = new StatusDistributionManager(browseButton);
		// ----------------------------------------------------------------------------
		// browse functionality
		BrowseButtonListener browseListener = new BrowseButtonListener();
		browseButton.addSelectionListener(browseListener);
		distributionManager.register(browseListener, null);
		// ----------------------------------------------------------------------------
		// Plot all data + cut functionality
		plotAllListener = new PlotAllButtonListener(currentPath, currentFile, btnPlotAllData);
		distributionManager.register(plotAllListener, browseListener);
		btnPlotAllData.addSelectionListener(plotAllListener);
		// ----------------------------------------------------------------------------
		// Plot relevant data
		PlotRelevantButtonListener plotRelevantListener = new PlotRelevantButtonListener(btnPlotRelevantData);
		btnPlotRelevantData.addSelectionListener(plotRelevantListener);
		distributionManager.register(plotRelevantListener, plotAllListener);
		// ----------------------------------------------------------------------------
		// Change Epoch functionality
		changeEpochButtonListener = new ChangeEpochButtonListener(btnChangeEpoch);
		btnChangeEpoch.addSelectionListener(changeEpochButtonListener);
		distributionManager.register(changeEpochButtonListener, plotRelevantListener);
		// ----------------------------------------------------------------------------
		// wearing time functionality
		WearingTimeButtonListener wearingTimeButtonListener = new WearingTimeButtonListener(btnWearingTime);
		btnWearingTime.addSelectionListener(wearingTimeButtonListener);
		distributionManager.register(wearingTimeButtonListener, plotRelevantListener);
		// ----------------------------------------------------------------------------
		// Analyse btn functionality (andre's algorithms)
		AnalyseButtonListener analyseListener = new AnalyseButtonListener(btnAnalyseData);
		btnAnalyseData.addSelectionListener(analyseListener);
		distributionManager.register(analyseListener, plotRelevantListener);
		// ----------------------------------------------------------------------------
		// Analyse btn functionality (simon's algorithms)
		AnalyseSimonButtonListener analyseSimonButtonListener = new AnalyseSimonButtonListener(btnAnalyseDataSimon);
		btnAnalyseDataSimon.addSelectionListener(analyseSimonButtonListener);
		distributionManager.register(analyseSimonButtonListener, wearingTimeButtonListener);
		// ----------------------------------------------------------------------------
		// csv preview
		// set up util map
		setupIntToLetterMap();
		// ----------------------------------------------------------------------------
	
		final Thread distributionThread = new Thread(distributionManager);
		distributionThread.setName("DistributionManager");
		distributionThread.start();
		
		addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				distributionThread.interrupt();
				KoraSteps.dispose();
			}
		});
	}
	

	/** Selection Listener for epoch scale*/
	private class EpochScaleSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Scale s = (Scale)e.getSource();
			int current = s.getSelection();
			epochText.setText(current + "");
			s.setToolTipText(current + "");
			changeEpochButtonListener.setEpochTime(current);
			
			int currentEpoch = KoraSteps.getInitialEpochPeriode();
			epochScale.setIncrement(currentEpoch);
			epochScale.setPageIncrement(currentEpoch);
			
		}
	}
	
	/** Selection listener for browse button */
	private class BrowseButtonListener extends SelectionAdapterBase{
		
		/** Constructor */
		public BrowseButtonListener() {
			this.isStepFinished = false;
		}
		
		@Override
		public void widgetSelected(SelectionEvent event) {
			// open FileDialog; select only csv files; display them in browse combo
			FileDialog dialog = new FileDialog(new Shell());
			String[] filter = {"*.csv"};
			dialog.setFilterExtensions(filter);
			dialog.open();
			currentPath = dialog.getFilterPath()+"\\";
			currentFile = dialog.getFileName();
			String absolutePath = currentPath+currentFile;
			
			// check selected file
			if(absolutePath.length()	< 5 || !absolutePath.contains(".csv")) {
				Logger.log(MessageType.WARNING, "No file was selected!");
				currentPath = "";
				currentFile = "";
				return;
			}
			
			// add file to combo box
			browseCombo.add(absolutePath,0);
			browseCombo.select(0);
			
			// load file
			csvFile = new CsvFileLoader(new File(absolutePath));
			boolean success = csvFile.load();
			setupCsvTable(csvFile);
			// update plot listener if file loading was successful
			if(success){
				plotAllListener.updatePathAndFile(currentPath, currentFile);
			}
			
			isStepFinished = KoraSteps.KoraStep1(currentPath, currentFile);
			
			// update Epoch periode
			int currentEpoch = KoraSteps.getInitialEpochPeriode();
			epochScale.setMinimum(currentEpoch);
			// maximum epoch time is 60s
			int maxEpoch = 60;
			epochScale.setMaximum(maxEpoch);
			epochScale.setIncrement(currentEpoch);
			epochScale.setPageIncrement(currentEpoch);
			epochText.setText(currentEpoch + "");
			changeEpochButtonListener.setEpochTime(currentEpoch);
			
		}
		
		
		@Override
		public boolean finished() {
			return isStepFinished;
		}

		@Override
		public void notifyWaiting() {
			// nothing to do
			// initial function
		}

		@Override
		public void reset() {
			isStepFinished = false;
			
		}
		
	
	}

	/** Displays the csv file */
	private void setupCsvTable(CsvFileLoader csvFile){
		table.removeAll();
		
		// first column for lineNumber
		TableColumn firstCol = new TableColumn(table, SWT.NONE);
		firstCol.setWidth(30);
		firstCol.setText("   ");

		// set up other columns
		for(int i=0; i<csvFile.getMaxLineSize(); ++i){
			TableColumn col = new TableColumn(table, SWT.NONE);
			col.setWidth(75);
			col.setText(intToLetter.get(i));
		}
		
		int lineNr = 0;
		for(ArrayList<String> line : csvFile.getCsvTable()) {
			
			
			TableItem item = new TableItem(table, SWT.READ_ONLY);
			for(int i=0; i<line.size(); ++i) {
				// set line Number
				if(i==0){
					item.setText(i, lineNr+++"");
					item.setBackground(i, LIGHT_GREY);
				}
				
				// i+1 because first column is for line number
				item.setText(i+1, line.get(i));
				
			}
			
		}
		
		// set label
		csvPreviewLabel.setText("Preview of " + csvFile.getFile().getName() + "  (line 0 - " + CsvFileLoader.MAX_LINES_TO_READ +")");
		table.redraw();
		

		
	}
}
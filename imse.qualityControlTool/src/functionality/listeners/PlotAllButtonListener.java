package functionality.listeners;

import static util.GUIUtil.APPLICABLE;
import static util.GUIUtil.NOT_APPLICABLE;
import static util.GUIUtil.setButtonColor;
import log.Logger;
import log.MessageType;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import functionality.KoraSteps;
import functionality.backend.MainControlsBackend;

/**SelectionAdapter for plotting all initial data 
 * 
 * @author Johannes
 *
 */
public class PlotAllButtonListener extends SelectionAdapterBase {

	/** Path of the csv-file */
	private String path;
	/** Name of csv-file */
	private String file;
	/** Plot all button */
	private Button thisButton;
	
	private String currentData;
	
	/** Constructor */
	public PlotAllButtonListener(String path, String file, Button thisButton, String currentData){
		this.path = path;
		this.file = file;
		this.thisButton = thisButton;
		this.isWaiting = true;
		this.isStepFinished = false;
		this.currentData = currentData;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if(isWaiting) {
			Logger.log(MessageType.WARNING, "Please select a file first!");
			return;
		}
		
		//TODO: Check if ok
		// plot the csv file
			isStepFinished = KoraSteps.KoraStep2(path,file,currentData);
			if(!isStepFinished)
				return;
		
		
	}
	

	/** updates the current path and file*/
	public void updatePathAndFile(String path, String file){
		this.path = path;
		this.file = file;
	}

	@Override
	public boolean finished() {
		return isStepFinished;
	}

	@Override
	public void notifyWaiting() {
		isWaiting = false;
		setButtonColor(thisButton, APPLICABLE);
	}

	@Override
	public void reset() {
		isStepFinished = false;
		isWaiting = true;
		setButtonColor(thisButton, NOT_APPLICABLE);
	}
	
}

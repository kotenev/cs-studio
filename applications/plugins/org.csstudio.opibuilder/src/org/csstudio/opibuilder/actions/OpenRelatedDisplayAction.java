package org.csstudio.opibuilder.actions;

import org.csstudio.opibuilder.widgetActions.OpenDisplayAction;
import org.eclipse.jface.action.Action;

/** The action open default related display in different target.
 * 
 * @author Xihui Chen
 *
 */
public class OpenRelatedDisplayAction extends Action {

	public enum OPEN_DISPLAY_TARGET {
		DEFAULT,
		TAB,
		NEW_WINDOW
	}
	
	private OpenDisplayAction openDisplayAction;
	
	private OPEN_DISPLAY_TARGET  target;

	public OpenRelatedDisplayAction(OpenDisplayAction openDisplayAction,
			OPEN_DISPLAY_TARGET target) {
		super();
		this.openDisplayAction = openDisplayAction;
		this.target = target;
		switch (target) {
		case TAB:
			setText("Open in New Tab");
			break;
		case NEW_WINDOW:
			setText("Open in New Window");
			break;
		default:
			setText("Open");
			break;
		}
	}
	
	@Override
	public void run() {
		openDisplayAction.setCtrlPressed(false);
		openDisplayAction.setShiftPressed(false);
		
		switch (target) {
		case TAB:
			openDisplayAction.setCtrlPressed(true);
			break;
		case NEW_WINDOW:
			openDisplayAction.setShiftPressed(true);
			break;
		default:
			break;
		}											
		openDisplayAction.run();	
		
		
	}
	
}
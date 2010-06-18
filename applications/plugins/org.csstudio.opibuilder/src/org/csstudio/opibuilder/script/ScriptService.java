package org.csstudio.opibuilder.script;

import org.csstudio.opibuilder.editparts.AbstractBaseEditPart;
import org.csstudio.opibuilder.util.ConsoleService;
import org.csstudio.opibuilder.util.UIBundlingThread;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.utility.pv.PV;
import org.eclipse.osgi.util.NLS;
import org.mozilla.javascript.Context;

/**The center service for script execution.
 * @author Xihui Chen
 *
 */
public class ScriptService {
	
	public static final String PV_ARRAY = "pvArray";

	public static final String WIDGET_CONTROLLER = "widgetController";

	private static ScriptService instance;
	
	private Context scriptContext;
	
	/** Private constructor to prevent instantiation
	 *  @see #getInstance()
	 */
	private ScriptService() {
		
		UIBundlingThread.getInstance().addRunnable(new Runnable() {
			
			public void run() {
				scriptContext = Context.enter();
			}
		});
	}
	
	public synchronized static ScriptService getInstance() {
		if(instance == null)
			instance = new ScriptService();
		return instance;
	}
	
	
	public Context getScriptContext() {
		return scriptContext;
	}
	
	/**Register the script in the script service, so that it could be executed afterwards.
	 * @param scriptData
	 * @param editpart
	 * @param pvArray
	 * @throws Exception
	 */
	public void registerScript(final ScriptData scriptData, final AbstractBaseEditPart editpart, final PV[] pvArray){		
		UIBundlingThread.getInstance().addRunnable(new Runnable(){
			public void run() {
				try {					
					new RhinoScriptStore(scriptData, editpart, pvArray);
				}catch (Exception e) {
					String name = scriptData instanceof RuleScriptData ? 
							((RuleScriptData)scriptData).getRuleData().getName() : scriptData.getPath().toString();
					String errorInfo = NLS.bind("Failed to register {0}. \n{1}",
							name, e.getMessage());
					ConsoleService.getInstance().writeError(errorInfo);
					CentralLogger.getInstance().error(this, errorInfo, e);
				} 				
			}
		});
		
	}
	
	
	public void exit(){
		UIBundlingThread.getInstance().addRunnable(new Runnable(){
			public void run() {
				Context.exit();
			}			
		});
		
		instance =  null;
	}
	
}

/*************************************************************************
 *
 * $RCSfile: UnoTypeProvider.java,v $
 *
 * $Revision: 1.3 $
 *
 * last change: $Author: cedricbosdo $ $Date: 2006/06/09 06:14:02 $
 *
 * The Contents of this file are made available subject to the terms of
 * either of the GNU Lesser General Public License Version 2.1
 *
 * Sun Microsystems Inc., October, 2000
 *
 *
 * GNU Lesser General Public License Version 2.1
 * =============================================
 * Copyright 2000 by Sun Microsystems, Inc.
 * 901 San Antonio Road, Palo Alto, CA 94303, USA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 * 
 * The Initial Developer of the Original Code is: Sun Microsystems, Inc..
 *
 * Copyright: 2002 by Sun Microsystems, Inc.
 *
 * All Rights Reserved.
 *
 * Contributor(s): Cedric Bosdonnat
 *
 *
 ************************************************************************/
package org.openoffice.ide.eclipse.core.unotypebrowser;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.openoffice.ide.eclipse.core.OOEclipsePlugin;
import org.openoffice.ide.eclipse.core.PluginLogger;
import org.openoffice.ide.eclipse.core.i18n.I18nConstants;
import org.openoffice.ide.eclipse.core.internal.model.OOo;
import org.openoffice.ide.eclipse.core.model.IUnoidlProject;
import org.openoffice.ide.eclipse.core.preferences.IOOo;

/**
 * Class providing UNO types from an OpenOffice.org instance and optionally
 * from a UNO project.
 * 
 * @author cbosdonnat
 *
 */
public class UnoTypeProvider {

	public final static int MODULE = 1;
	
	public final static int INTERFACE = 2;
	
	public final static int SERVICE = 4;
	
	public final static int STRUCT = 8;
	
	public final static int ENUM = 16;
	
	public final static int EXCEPTION = 32;
	
	public final static int TYPEDEF = 64;
	
	public final static int CONSTANT = 128;
	
	public final static int CONSTANTS = 256;
	
	public final static int SINGLETON = 512;
	
	/**
	 * Creates a UNO type provider from a UNO project.
	 * 
	 * @param aProject the UNO project to query (with its OOo parameter)
	 * @param aTypes the types to get
	 */
	public UnoTypeProvider(IUnoidlProject aProject, int aTypes) {
		setTypes(aTypes);
		setProject(aProject);
	}
	
	/**
	 * Creates a UNO type provider from an OpenOffice.org instance
	 * 
	 * @param aOOoInstance the OOo instance to query
	 * @param aTypes the types to get
	 */
	public UnoTypeProvider(IOOo aOOoInstance, int aTypes) {
		setTypes(aTypes);
		setOOoInstance(aOOoInstance);
	}
	
	/**
	 * Dispose the type provider
	 */
	public void dispose(){
		removeAllTypes();
		
		internalTypes = null;
		oooInstance = null;
		pathToRegister = null;
		
		if (null != getTypesJob){
			if (!getTypesJob.cancel()) { // Not sure it stops when running
				process.destroy();
			}
			getTypesJob = null;
			process = null;
		}
	}
	
	//---------------------------------------------------------- Type managment
	
	private int types = 1023;
	
	private static int[] allowedTypes = {
		MODULE,
		INTERFACE,
		SERVICE,
		STRUCT,
		ENUM,
		EXCEPTION,
		TYPEDEF,
		CONSTANT,
		CONSTANTS,
		SINGLETON
	};
	
	/**
	 * Method changing all the '1' into '0' and the '0' into '1' but only
	 * on the interesting bytes for the types.
	 * 
	 * @param aType
	 * @return
	 */
	static int invertTypeBits(int aType){
		int result = 0;
		
		String sInv = Integer.toBinaryString(aType);
		int length = allowedTypes.length - sInv.length();
		
		if (length <= 10){
			
			for (int i=0; i<length; i++) {
				sInv = '0' + sInv;
			}
			
			sInv = sInv.replace('0', '2').replace('1', '0');
			sInv = sInv.replace('2', '1');
			result = Integer.parseInt(sInv, 2);
		}
		
		return result;
	}
	
	/**
	 * Set one or more types. To specify more than one types give the bit or
	 * of all the types, eg <code>INTERFACE | SERVICE</code>
	 * 
	 * @param aTypes the bit or of the types
	 */
	public void setTypes(int aTypes) {
		
		// Only 10 bits available
		if (aTypes >= 0 && aTypes < 1024) {
			types = aTypes;
		}
	}
	
	/**
	 * Get the types set as an integer. The types field is a bit or of all the
	 * types set.
	 */
	public int getTypes(){
		return types;
	}
	
	/**
	 * Checks if the given type will be queried
	 * 
	 * @param type the type to match
	 * @return <code>true</code> if the type is one of the types set
	 */
	public boolean isTypeSet(int type){
		return (getTypes() & type) == type;
	}
	
	/**
	 * Checks whether the list contains the given type name
	 * 
	 * @param scopedName the type name to match
	 * @return <code>true</code> if the list contains a type with this name
	 */
	public boolean contains(String scopedName) {
		
		boolean result = false;
		scopedName = scopedName.replaceAll("::", ".");
		
		if (isInitialized()) {
			Iterator iter = internalTypes.iterator();
			while (iter.hasNext() && !result) {
				InternalUnoType type = (InternalUnoType)iter.next();
				if (type.getFullName().equals(scopedName)) {
					result = true;
				}
			}
		}
		
		return result;
	}
	
	//------------------------------------------------------- Project managment

	private IOOo	oooInstance;
	private String pathToRegister;
	
	private boolean initialized = false;
	
	/**
	 * Set the UNO projet for which to get the UNO types. This project's
	 * <code>types.rdb</code> registry will be used as external registry
	 * for the types query.
	 * 
	 * @param aProject the project for which to launch the type query 
	 */
	public void setProject(IUnoidlProject aProject){
		
		if (null != aProject) {
			if (null != getTypesJob) {
				getTypesJob.cancel();
			}
			
			oooInstance = aProject.getOOo();
			pathToRegister = (aProject.getFile(
					aProject.getTypesPath()).getLocation()).toOSString();
			
			initialized = false;
			askUnoTypes();
		}
	}
	
	/**
	 * Sets the OOo if the new one is different from the old one.
	 * 
	 *  @param aOOoInstance OpenOffice.org instance to bootstrap
	 */
	public void setOOoInstance(IOOo aOOoInstance) {
		
		if (!(null != oooInstance && oooInstance.equals(aOOoInstance))) {
			
			// Stops the current job if there is already one.
			if (null != getTypesJob) {
				getTypesJob.cancel();
			}
			
			oooInstance = aOOoInstance;
			
			initialized = false;
			askUnoTypes();
		}
	}
	
	/**
	 * Return whether the type provider has been initialized
	 */
	public boolean isInitialized(){
		return initialized;
	}
	
	//---------------------------------------------------- TypeGetter launching
	
	private String computeGetterCommand() throws IOException {
		String command = null;
		
		if (null != oooInstance) {
			// Defines OS specific constants
			String pathSeparator = System.getProperty("path.separator");
			String fileSeparator = System.getProperty("file.separator");
			
			// Constitute the classpath for OOo Boostrapping
			String classpath = "-cp ";
			if (Platform.getOS().equals(Platform.OS_WIN32)) {
				classpath = classpath + "\"";
			}
			
			String oooClassesPath = oooInstance.getClassesPath();
			File oooClasses = new File(oooClassesPath);
			String[] content = oooClasses.list();
			
			for (int i=0, length=content.length; i<length; i++){
				String contenti = content[i];
				if (contenti.endsWith(".jar")) {
					classpath = classpath + oooClassesPath + fileSeparator + 
									contenti + pathSeparator;
				}
			}
			
			// Add the UnoTypeGetter jar to the classpath
			URL pluginURL = Platform.find(
					OOEclipsePlugin.getDefault().getBundle(), 
					new Path("/."));
			
			String path = Platform.asLocalURL(pluginURL).getFile();
			path = path + "UnoTypesGetter.jar";
			
			if (Platform.getOS().equals(Platform.WS_WIN32)){
				path = path.substring(1).replaceAll("/", "\\\\");
			}
			
			classpath = classpath + path;
			if (Platform.getOS().equals(Platform.OS_WIN32)) {
				classpath = classpath + "\"";
			}
			classpath = classpath + " ";
			
			// Compute the types mask argument
			String typesMask = "-T" + types;
			
			// Get the OOo types.rdb registry path as external registry
			String typesPath = oooInstance.getTypesPath();
			typesPath = "-Efile:///" + typesPath.replace(" ", "%20");
			
			// Add the local registry path
			String localRegistryPath = "";
			// If the path to the registry isn't set, don't take
			// it into account in the command build
			if (null != pathToRegister) {
				localRegistryPath = " -Lfile:///" + 
					pathToRegister.replace(" ", "%20");
			}
			
			// Computes the command to execute if oooInstance isn't the URE
			if (oooInstance instanceof OOo) {
				command = "java " + classpath + 
						"org.openoffice.ide.eclipse.core.unotypebrowser.UnoTypesGetter " + 
						typesPath + " " + localRegistryPath + " " + typesMask;
			} else {
				
				// compute the arguments array
				String[] args = new String[] {
						typesPath,
						localRegistryPath,
						typesMask
				};
				
				command = oooInstance.createUnoCommand(
						"org.openoffice.ide.eclipse.core.unotypebrowser.UnoTypesGetter", 
						"file:///"+path, new String[]{}, args);
			}
		}
		return command;
	}
	
	private Job getTypesJob;
	private Process process;
	
	/**
	 * Launches the UNO type query process
	 */
	public void askUnoTypes() {
		
		if (null == getTypesJob || Job.RUNNING != getTypesJob.getState()) {
			getTypesJob = new Job(OOEclipsePlugin.getTranslationString(
					I18nConstants.FETCHING_TYPES)) {
				
				protected IStatus run(IProgressMonitor monitor) {
					
					IStatus status = new Status(IStatus.OK,
							OOEclipsePlugin.OOECLIPSE_PLUGIN_ID,
							IStatus.OK,
							"",
							null);
					
					try {
						monitor.beginTask(
								OOEclipsePlugin.getTranslationString(
										I18nConstants.FETCHING_TYPES),
								2);
						removeAllTypes();
						
						String command = computeGetterCommand();
						
						// Computes the environment variables
						
						process = Runtime.getRuntime().exec(command);
						
						// Reads the types and add them to the list
						LineNumberReader reader = new LineNumberReader(
									new InputStreamReader(
											process.getInputStream()));
						
						String line = reader.readLine();
						monitor.worked(1);
						
						while (null != line) {
							InternalUnoType internalType = new InternalUnoType(line);
							addType(internalType);
							line = reader.readLine();
						}
						
						monitor.worked(1);
						setInitialized();
						
					} catch (IOException e) {
						monitor.worked(0);
						status = new Status(IStatus.ERROR,
								OOEclipsePlugin.OOECLIPSE_PLUGIN_ID,
								IStatus.ERROR,
								"",
								e);
						
						PluginLogger.getInstance().debug(e.getMessage());
					} catch (Exception e) {
						PluginLogger.getInstance().error(e.getMessage(), e);
						monitor.worked(0);
						cancel();
					}
					
					return status;
				}
			};
			
			getTypesJob.setSystem(true);
			getTypesJob.setPriority(Job.INTERACTIVE);
			
			// Execute the job asynchronously
			getTypesJob.schedule();
		}
	}
	
	private Vector listeners = new Vector(); 
	
	/**
	 * Register the given listener
	 */
	public void addInitListener(IInitListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Makes the given initialization listener stop listening
	 */
	public void removeInitListener(IInitListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Propagate the news to the listeners that it has been initialized.
	 */
	private void setInitialized(){
		initialized = true;
		
		for (int i=0, length=listeners.size(); i<length; i++) {
			((IInitListener)listeners.get(i)).initialized();
		}
	}

	//---------------------------------------------------- Collection managment
	
	private Vector internalTypes = new Vector();
	
	/**
	 * Get a type from its path
	 * 
	 * @param typePath the type path
	 * 
	 * @return the corresponding complete type description
	 */
	public InternalUnoType getType(String typePath) {
		
		Iterator iter = internalTypes.iterator();
		InternalUnoType result = null;
		
		while (null == result && iter.hasNext()) {
			InternalUnoType type = (InternalUnoType)iter.next();
			if (type.getFullName().equals(typePath)) {
				result = type;
			}
		}
		return result;
		
	}
	
	/**
	 * Returns the types list as an array
	 */
	protected Object[] toArray() {
		return internalTypes.toArray();
	}
	
	/**
	 * Add a type to the list
	 */
	protected void addType(InternalUnoType internalType) {
		internalTypes.add(internalType);
	}

	/**
	 * purge the types list
	 */
	protected void removeAllTypes() {
		if (internalTypes != null) {
			internalTypes.clear();
		}
	}
}
package com.opencms.defaults;

/*
 *
 * Copyright (C) 2000  The OpenCms Group
 *
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 *
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.opencms.template.*;
import com.opencms.workplace.*;
import com.opencms.file.*;
import com.opencms.core.*;
//import com.opencms.util.*;

import java.util.*;
//import java.io.*;

import java.lang.reflect.*;
//import java.lang.String;


/**
 * Abstract class for generic backoffice display. It automatically
 * generates the <ul><li>head section with filters and buttons,</li>
 * 					<li>body section with the table data,</li>
 * 					<li>lock states of the entries. if there are any,</li>
 *					<li>delete dialog,</li>
 *					<li>lock dialog.</li></ul>
 * calls the <ul><li>edit dialog of the calling backoffice class</li>
 *				<li>new dialog of the calling backoffice class</li></ul>
 * using the content definition class defined by the getContentDefinition method.
 * The methods and data provided by the content definition class
 * is accessed by reflection. This way it is possible to re-use
 * this class for any content definition class, that just has
 * to extend the A_CmsContentDefinition class!
 * Creation date: (27.10.00 10:04:42)
 * author: Michael Knoll
 * version 1.0
 */
public abstract class A_CmsBackoffice extends CmsWorkplaceDefault {

private static int C_NOT_LOCKED = -1;

/**
 * gets the backoffice url of the module by using the cms object
 * @returns a string with the backoffice url
 */

public abstract String getBackofficeUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception;
/**
 * Gets the content of a given template file.
 * This method displays any content provided by a content definition
 * class on the template. The used backoffice class does not need to use a
 * special getContent method. It just has to extend the methods of this class!
 * Using reflection, this method creates the table headline and table content
 * with the layout provided by the template automatically!
 * @param cms A_CmsObject Object for accessing system resources
 * @param templateFile Filename of the template file
 * @param elementName <em>not used here</em>.
 * @param parameters <em>not used here</em>.
 * @param templateSelector template section that should be processed.
 * @return Processed content of the given template file.
 * @exception CmsException
 */

public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

	//return var
	byte[] returnProcess = null;

	// session will be created or fetched
	I_CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
	//create new workplace templatefile object
	CmsXmlWpTemplateFile template = new CmsXmlWpTemplateFile(cms, templateFile);
	//get parameters
	String selectBox = (String) parameters.get("selectbox");
	String filterParam = (String) parameters.get("filterparameter");
	String id = (String) parameters.get("id");
	String idlock = (String) parameters.get("idlock");
	String iddelete = (String) parameters.get("iddelete");
	String idedit = (String) parameters.get("idedit");
  String idview = (String) parameters.get("idview");
	String action = (String) parameters.get("action");
  String parentId = (String) parameters.get("parentId");
	String ok = (String) parameters.get("ok");
	String setaction = (String) parameters.get("setaction");

	String hasFilterParam = (String) session.getValue("filterparameter");
	template.setData("filternumber","0");

	//change filter
	if ((hasFilterParam == null) && (filterParam == null) && (setaction == null)) {
				if (selectBox != null) {
					session.putValue("filter", selectBox);
					template.setData("filternumber",selectBox);
				}
	} else {
		template.setData("filternumber", (String)session.getValue("filter"));
	}

	//move id values to id, remove old markers
	if (idlock != null) {
		id = idlock;
		session.putValue("idlock", idlock);
		session.removeValue("idedit");
		session.removeValue("idnew");
		session.removeValue("iddelete");
	}
	if (idedit != null) {
		id = idedit;
		session.putValue("idedit", idedit);
		session.removeValue("idlock");
		session.removeValue("idnew");
		session.removeValue("iddelete");
	}
	if (iddelete != null) {
		id = iddelete;
		session.putValue("iddelete", iddelete);
		session.removeValue("idedit");
		session.removeValue("idnew");
		session.removeValue("idlock");
	}
	if ((id != null) && (id.equals("new"))) {
		session.putValue("idnew", id);
		session.removeValue("idedit");
		session.removeValue("idnew");
		session.removeValue("iddelete");
		session.removeValue("idlock");
	}

	//get marker id from session
	String idsave = (String) session.getValue("idsave");
	if (ok == null)
		idsave = null;

  if(parentId != null) {
    session.putValue("parentId", parentId);
  }

	//get marker for accessing the new dialog
	String idnewsave = (String) session.getValue("idnew");
	//access to new dialog
	if ((id != null) && (id.equals("new")) || ((idsave != null) && (idsave.equals("new")))) {
    if (idsave != null) {
			parameters.put("id", idsave);
      //session.removeValue("idsave");
    }
		if (id != null) {
			parameters.put("id", id);
      session.putValue("idsave", id);
    }
		//process the "new entry" form
		returnProcess = getContentNew(cms, template, elementName, parameters, templateSelector);
		//finally retrun processed data
		return returnProcess;
	}

	//go to the appropriate getContent methods
	if ((id == null) && (idsave == null) && (action == null) && (idlock==null) && (iddelete == null) && (idedit == null))  {
  	//process the head frame containing the filter
		returnProcess = getContentHead(cms, template, elementName, parameters, templateSelector);
		//finally return processed data
		return returnProcess;
	} else {
		//process the body frame containing the table
    if(action == null) action = "";
		if (action.equalsIgnoreCase("list")){
			//process the list output
      // clear "idsave" here in case user verification of data failed and input has to be shown again ...
      session.removeValue("idsave");
			returnProcess = getContentList(cms, template, elementName, parameters, templateSelector);
			//finally return processed data
			return returnProcess;
		} else {
			//get marker for accessing the edit dialog
			String ideditsave = (String) session.getValue("idedit");

			//go to the edit dialog
			if ((idedit != null) || (ideditsave != null)) {
				//store id parameters for edit dialog
				if (idsave != null) {
					parameters.put("id", idsave);
          //session.removeValue("idsave");
        }
				if (id != null) {
					parameters.put("id", id);
          session.putValue("idsave", id);
        }
				returnProcess = getContentEdit(cms, template, elementName, parameters, templateSelector);
				//finally return processed data
				return returnProcess;
			} else {
				//store id parameters for delete and lock
				if (idsave != null) {
					parameters.put("id", idsave);
          session.removeValue("idsave");
				} else {
					parameters.put("id", id);
          session.putValue("idsave", id);
				}
				//get marker for accessing the delete dialog
				String iddeletesave = (String) session.getValue("iddelete");
				//access delete dialog
				if (((iddelete != null) || (iddeletesave != null)) && (idlock == null)) {
					returnProcess = getContentDelete(cms, template, elementName, parameters, templateSelector);
					return returnProcess;
				} else {
					//access lock dialog
					returnProcess = getContentLock(cms, template, elementName, parameters, templateSelector);
					//finally return processed data
					return returnProcess;
				}
			}
		}
	}
}
/**
 * gets the content definition class
 * @returns class content definition class
 * Must be implemented in the extending backoffice class!
 */

public abstract Class getContentDefinitionClass() ;
/**
 * gets the content definition class method constructor
 * @returns content definition object
 */

private Object getContentDefinition(CmsObject cms, Class cdClass, Integer id) {

	Object o = null;
	try {
		Constructor c = cdClass.getConstructor(new Class[] {CmsObject.class, Integer.class});
		o = c.newInstance(new Object[] {cms, id});
	} catch (InvocationTargetException ite) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Invocation target exception!");
		}
	} catch (NoSuchMethodException nsm) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Requested method was not found!");
		}
	} catch (InstantiationException ie) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: the reflected class is abstract!");
		}
	} catch (Exception e) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Other exception!");
		}
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
        }
	}
	return o;
}
/**
 * gets the content definition class method constructor
 * @returns content definition object
 */

private Object getContentDefinition(CmsObject cms, Class cdClass, String id) {

	Object o = null;
	try {
		Constructor c = cdClass.getConstructor(new Class[] {CmsObject.class, String.class});
		o = c.newInstance(new Object[] {cms, id});
	} catch (InvocationTargetException ite) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Invocation target exception!");
		}
	} catch (NoSuchMethodException nsm) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Requested method was not found!");
		}
	} catch (InstantiationException ie) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: the reflected class is abstract!");
		}
	} catch (Exception e) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice contentDefinitionConstructor: Other exception!");
		}
        if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
            A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
        }
	}
	return o;
}
/**
 * Gets the content of a given template file.
 * <P>
 * While processing the template file the table entry
 * <code>entryTitle<code> will be displayed in the delete dialog
 *
 * @param cms A_CmsObject Object for accessing system resources
 * @param templateFile Filename of the template file
 * @param elementName not used here
 * @param parameters get the parameters action for the button activity
 * 					 and id for the used content definition instance object
 * @param templateSelector template section that should be processed.
 * @return Processed content of the given template file.
 * @exception CmsException
 */

private byte[] getContentDelete(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

	//return var
	byte[] processResult = null;

	// session will be created or fetched
	I_CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
	//get the class of the content definition
	Class cdClass = getContentDefinitionClass();

	//get (stored) id parameter
	String id = (String) parameters.get("id");
	if (id == null)
		id = "";
	/*if (id != "") {
		session.putValue("idsave", id);
	} else {
		String idsave = (String) session.getValue("idsave");
		if (idsave == null)
			idsave = "";
		id = idsave;
		session.removeValue("idsave");
	}*/

	// get value of hidden input field action
	String action = (String) parameters.get("action");

	//no button pressed, go to the default section!
	//delete dialog, displays the title of the entry to be deleted
	if (action == null || action.equals("")) {
		if (id != "") {
			//set template section
			templateSelector = "delete";

			//create appropriate class name with underscores for labels
			String moduleName = "";
			moduleName = (String) getClass().toString(); //get name
			moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
			moduleName = moduleName.trim();
			moduleName = moduleName.replace('.', '_'); //replace dots with underscores

			//create new language file object
			CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);

			//get the dialog from the langauge file and set it in the template
			template.setData("deletetitle", lang.getLanguageValue("messagebox.title.delete"));
			template.setData("deletedialog", lang.getLanguageValue("messagebox.message1.delete"));
			template.setData("newsentry", id);
			template.setData("setaction", "default");
		}
		// confirmation button pressed, process data!
	} else {

		//set template section
		templateSelector = "done";
		//remove marker
		session.removeValue("idsave");

		//delete the content definition instance
		Integer idInteger = null;
		try {
			idInteger = Integer.valueOf(id);
		} catch (Exception e) {
			//access content definition constructor by reflection
			Object o = null;
			o = getContentDefinition(cms, cdClass, id);
			//get delete method and delete content definition instance
			try {
       ((A_CmsContentDefinition) o).delete(cms);
				//Method deleteMethod = (Method) cdClass.getMethod("delete", new Class[] {CmsObject.class});
				//deleteMethod.invoke(o, new Object[] {cms});
			} catch (Exception e1) {
				if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
					A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: delete method throwed an exception!");
				}
				templateSelector = "deleteerror";
				template.setData("deleteerror", e1.getMessage());
			}
			//finally start the processing
			processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
			return processResult;
		}

		//access content definition constructor by reflection
		Object o = null;
		o = getContentDefinition(cms, cdClass, idInteger);
		//get delete method and delete content definition instance
		try {
     ((A_CmsContentDefinition) o).delete(cms);

     }catch (Exception e) {
       templateSelector = "deleteerror";
       template.setData("deleteerror", e.getMessage());
		}
	}

	//finally start the processing
	processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
	return processResult;
}

/**
 * gets the content of a edited entry form.
 * Has to be overwritten in your backoffice class!
 */
public abstract byte[] getContentEdit(CmsObject cms,CmsXmlWpTemplateFile templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException;
/**
 * Gets the content of a given template file.
 * <P>
 *
 * @param cms A_CmsObject Object for accessing system resources
 * @param templateFile Filename of the template file
 * @param elementName not used here
 * @param parameters get the parameters action for the button activity
 * 					 and id for the used content definition instance object
 *					 and the author, title, text content for setting the new/changed data
 * @param templateSelector template section that should be processed.
 * @return Processed content of the given template file.
 * @exception CmsException
 */

private byte[] getContentHead(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

	//return var
	byte[] processResult = null;

	//get the class of the content definition
	Class cdClass = getContentDefinitionClass();

	//init vars
	String singleSelection = "";
	String allSelections = "";

	//create new or fetch existing session
	CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
    String uri = cms.getRequestContext().getUri();
    String sessionSelectBoxValue = uri+"selectBoxValue";
	//get filter method from session
	//String selectBoxValue = (String) session.getValue("filter");
    String selectBoxValue = (String) parameters.get("selectbox");
    if(selectBoxValue == null) {
        // set default value
        if((String)session.getValue(sessionSelectBoxValue) != null) {
            // came back from edit or something ... redisplay last filter
            selectBoxValue = (String)session.getValue(sessionSelectBoxValue);
        } else {
            // the very first time here...
            selectBoxValue = "0";
        }
    }
    boolean filterChanged = true;
    if( selectBoxValue.equals((String)session.getValue(sessionSelectBoxValue)) ) {
        filterChanged = false;
    }else {
        filterChanged = true;
    }

    //get vector of filter names from the content definition
	Vector filterMethods = getFilterMethods(cms);

    if( Integer.parseInt(selectBoxValue) >=  filterMethods.size() ) {
        // the stored seclectBoxValue is does not exist any more, ...
        selectBoxValue = "0";
    }
    session.putValue(sessionSelectBoxValue, selectBoxValue); // store in session for Selectbox!
    session.putValue("filter",selectBoxValue);  // store filter in session for getContentList!

	String filterParam = (String) parameters.get("filterparameter");
    String action = (String) parameters.get("action");
    String setaction = (String) parameters.get("setaction");
    // create the key for the filterparameter in the session ... should be unique to avoid problems...

    String sessionFilterParam = uri+selectBoxValue+"filterparameter";
	//store filterparameter in the session, new enty for every filter of every url ...
	if (filterParam != null) {
		session.putValue(sessionFilterParam, filterParam);
    }

	//create appropriate class name with underscores for labels
	String moduleName = "";
	moduleName = (String) getClass().toString(); //get name
	moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
	moduleName = moduleName.trim();
	moduleName = moduleName.replace('.', '_'); //replace dots with underscores

	//create new language file object
	CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);
	//set labels in the template
	template.setData("filterlabel", lang.getLanguageValue(moduleName + ".label.filter"));
	template.setData("filterparameterlabel", lang.getLanguageValue(moduleName + ".label.filterparameter"));

	//no filter selected so far, store a default filter in the session
	CmsFilterMethod filterMethod = null;
	if (selectBoxValue == null) {
		CmsFilterMethod defaultFilter = (CmsFilterMethod) filterMethods.firstElement();
		session.putValue("selectbox", defaultFilter.getFilterName());
	}/*
	if (filterParam != null) {
		parameters.put("filterparameter", filterParam);
    }*/

    // show param box ?
    CmsFilterMethod currentFilter = (CmsFilterMethod) filterMethods.elementAt(Integer.parseInt(selectBoxValue));
    if(currentFilter.hasUserParameter()) {
        if(filterChanged) {
            template.setData("filterparameter", currentFilter.getDefaultFilterParam());
            // access default in getContentList() ....
            session.putValue(sessionFilterParam, currentFilter.getDefaultFilterParam());
        } else if(filterParam!= null) {
            template.setData("filterparameter", filterParam);
        } else {
            // redisplay after edit or something like this ...
            template.setData("filterparameter", (String)session.getValue(sessionFilterParam));
        }
        template.setData("insertFilter", template.getProcessedDataValue("selectboxWithParam", this, parameters));
        template.setData("setfocus", template.getDataValue("focus"));
    }else{
        template.setData("insertFilter", template.getProcessedDataValue("singleSelectbox", this, parameters));
    }

	//if getCreateUrl equals null, the "create new entry" button
	//will not be displayed in the template
	String createButton = null;
	try {
		createButton = (String) getCreateUrl(cms, null, null, null);
	} catch (Exception e) {
	}
	if (createButton == null) {
		String cb = template.getDataValue("nowand");
		template.setData("createbutton", cb);
	} else {
		String cb = template.getProcessedDataValue("wand", this, parameters);
		template.setData("createbutton", cb);
	}

	//finally start the processing
	processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
	return processResult;
}
/**
 * Gets the content of a given template file.
 * This method displays any content provided by a content definition
 * class on the template. The used backoffice class does not need to use a
 * special getContent method. It just has to extend the methods of this class!
 * Using reflection, this method creates the table headline and table content
 * with the layout provided by the template automatically!
 * @param cms A_CmsObject Object for accessing system resources
 * @param templateFile Filename of the template file
 * @param elementName <em>not used here</em>.
 * @param parameters <em>not used here</em>.
 * @param templateSelector template section that should be processed.
 * @return Processed content of the given template file.
 * @exception CmsException
 */
private byte[] getContentList(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

	//return var
	byte[] processResult = null;
	// session will be created or fetched
	I_CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
	//get the class of the content definition
	Class cdClass = getContentDefinitionClass();

  String action = (String) parameters.get("action");

	//read value of the selected filter
	String filterMethodName = (String) session.getValue("filter");
	if (filterMethodName == null) {
		filterMethodName = "0";
	}

  String uri = cms.getRequestContext().getUri();
  String sessionFilterParam = uri+filterMethodName+"filterparameter";
	//read value of the inputfield filterparameter
	String filterParam = (String) session.getValue(sessionFilterParam);
	if (filterParam == "") filterParam = null;

	//change template to list section for data list output
	templateSelector = "list";

	//init vars
	String tableHead = "";
	String singleRow = "";
	String allEntrys = "";
	String entry = "";
  String url = "";
	int columns = 0;

	// get number of columns
	Vector columnsVector = new Vector();
	String fieldNamesMethod = "getFieldNames";
	Class paramClasses[] = {CmsObject.class};
	Object params[] = {cms};
	columnsVector = (Vector) getContentMethodObject(cms, cdClass, fieldNamesMethod, paramClasses, params);
	columns = columnsVector.size();

	//create appropriate class name with underscores for labels
	String moduleName = "";
	moduleName = (String) getClass().toString(); //get name
	moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
	moduleName = moduleName.trim();
	moduleName = moduleName.replace('.', '_'); //replace dots with underscores

	//create new language file object
	CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);

	//create tableheadline
	for (int i = 0; i < columns; i++) {
		tableHead += (template.getDataValue("tabledatabegin"))
		+ lang.getLanguageValue(moduleName + ".label." + columnsVector.elementAt(i).toString().toLowerCase().trim())
		+ (template.getDataValue("tabledataend"));
	}
	//set template data for table headline content
	template.setData("tableheadline", tableHead);

	//get vector of filterMethods
	//& select the appropriate filter method,
	//  if no filter is appropriate, select a default filter
	//& get number of rows for output
	Vector tableContent = new Vector();
	try {
		Vector filterMethods = (Vector) cdClass.getMethod("getFilterMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
		CmsFilterMethod filterMethod = null;
		CmsFilterMethod filterName = (CmsFilterMethod) filterMethods.elementAt(Integer.parseInt(filterMethodName));
		filterMethodName = filterName.getFilterName();
		//loop trough the filter methods and set the chosen one
		for (int i = 0; i < filterMethods.size(); i++) {
			CmsFilterMethod currentFilter = (CmsFilterMethod) filterMethods.elementAt(i);
			if (currentFilter.getFilterName().equals(filterMethodName)) {
				filterMethod = currentFilter;
				break;
			}
		}
		// the chosen filter does not exist, use the first one!
		if (filterMethod == null) {
			filterMethod = (CmsFilterMethod) filterMethods.firstElement();
		}
		// now apply the filter with the cms object, the filter method and additional user parameters
		tableContent = (Vector) cdClass.getMethod("applyFilter", new Class[] {CmsObject.class, CmsFilterMethod.class, String.class}).invoke(null, new Object[] {cms, filterMethod, filterParam});
	} catch (InvocationTargetException ite) {
		//error occured while applying the filter
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: apply filter throwed an InvocationTargetException!");
		}
		templateSelector = "error";
		template.setData("filtername", filterMethodName);
    while(ite.getTargetException() instanceof InvocationTargetException) {
      ite = ((InvocationTargetException) ite.getTargetException());
    }
		template.setData("filtererror", ite.getTargetException().getMessage());
    session.removeValue(sessionFilterParam);
		//session.removeValue("filter");
	} catch (NoSuchMethodException nsm) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: apply filter method was not found!");
		}
		templateSelector = "error";
		template.setData("filtername", filterMethodName);
		template.setData("filtererror", nsm.getMessage());
		session.removeValue(sessionFilterParam);
    //session.removeValue("filterparameter");
	} catch (Exception e) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: apply filter: Other Exception!");
		}
		templateSelector = "error";
		template.setData("filtername", filterMethodName);
		template.setData("filtererror", e.getMessage());
    session.removeValue(sessionFilterParam);
		//session.removeValue("filterparameter");
	}
	//get the number of rows
	int rows = tableContent.size();

	//
	// get the field methods from the content definition
	//
	Vector fieldMethods = new Vector();
	try {
		fieldMethods = (Vector) cdClass.getMethod("getFieldMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
	} catch (Exception exc) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentList: getFieldMethods throwed an exception");
		}
		templateSelector = "error";
		template.setData("filtername", filterMethodName);
		template.setData("filtererror", exc.getMessage());
	}

	// create output from the table data
	String fieldEntry = "";
	String id = "";
	for (int i = 0; i < rows; i++) {
		//init
		entry = "";
		singleRow = "";
		Object entryObject = new Object();
		entryObject = tableContent.elementAt(i); //cd object in row #i

		//set data of single row
		for (int j = 0; j < columns; j++) {
			// call the field methods
			Method getMethod = null;
			try {
				getMethod = (Method) fieldMethods.elementAt(j);
			} catch (Exception e) {
				if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
					A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Could not get field method - check for correct spelling!");
				}
			}
			try {
				//apply methods on content definition object
				Object fieldEntryObject = null;
				fieldEntryObject = getMethod.invoke(entryObject, new Object[0]);
				if (fieldEntryObject != null) fieldEntry = fieldEntryObject.toString();
				else fieldEntry = null;
			} catch (InvocationTargetException ite) {
				if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
					A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice content definition object throwed an InvocationTargetException!");
				}
			} catch (Exception e) {
				if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
					A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice content definition object: Other exception!");
				}
			}

		try {
            id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
		} catch (Exception e) {
			if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
				A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: getUniqueId throwed an Exception!");
			}
		}

		//insert unique id in contextmenue
		if (id != null)
			template.setData("uniqueid", id);
			//insert table entry
			if (fieldEntry != null) {
        try{
          url = getUrl(cms, null, null, null);
        }catch (Exception e) {
          url = "";
        }
        if(!url.equals("")) {
          // enable url
				  entry += (template.getDataValue("tabledatabegin")) + (template.getProcessedDataValue("url", this, parameters)) + fieldEntry + (template.getDataValue("tabledataend"));
        }else {
          // disable url
          entry += (template.getDataValue("tabledatabegin")) + fieldEntry + (template.getDataValue("tabledataend"));
        }
			} else {
				entry += (template.getDataValue("tabledatabegin"))  + "" + (template.getDataValue("tabledataend"));
			}
		}
		//get the unique id belonging to an entry
		try {
     id = ((A_CmsContentDefinition)entryObject).getUniqueId(cms);
		} catch (Exception e) {
			if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
				A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice: getUniqueId throwed an Exception!");
			}
		}

		//insert unique id in contextmenue
		if (id != null)
			template.setData("uniqueid", id);

		//set the lockstates for the actual entry
		setLockstates(cms, template, cdClass, entryObject, parameters);

		//insert single table row in template
		template.setData("entry", entry);

		// processed row from template
		singleRow = template.getProcessedDataValue("singlerow", this, parameters);
		allEntrys += (template.getDataValue("tablerowbegin")) + singleRow + (template.getDataValue("tablerowend"));
	}

	//insert tablecontent in template
	template.setData("tablecontent", "" + allEntrys);

	//save select box value into session
	session.putValue("selectbox", filterMethodName);

	//finally start the processing
	processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
	return processResult;
}
/**
 * Gets the content of a given template file.
 * <P>
 * While processing the template file the table entry
 * <code>entryTitle<code> will be displayed in the delete dialog
 *
 * @param cms A_CmsObject Object for accessing system resources
 * @param templateFile Filename of the template file
 * @param elementName not used here
 * @param parameters get the parameters action for the button activity
 * 					 and id for the used content definition instance object
 * @param templateSelector template section that should be processed.
 * @return Processed content of the given template file.
 * @exception CmsException
 */

private byte[] getContentLock(CmsObject cms, CmsXmlWpTemplateFile template, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

	//return var
	byte[] processResult = null;

	// session will be created or fetched
	I_CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
	//get the class of the content definition
	Class cdClass = getContentDefinitionClass();
  int actUserId = cms.getRequestContext().currentUser().getId();

	//get (stored) id parameter
	String id = (String) parameters.get("id");
	if (id == null)
		id = "";
	/*if (id != "") {
		session.putValue("idsave", id);
	} else {
		String idsave = (String) session.getValue("idsave");
		if (idsave == null)
			idsave = "";
		id = idsave;
		session.removeValue("idsave");
	} */
	parameters.put("idlock", id);

	// get value of hidden input field action
	String action = (String) parameters.get("action");

	//no button pressed, go to the default section!
	if (action == null || action.equals("")) {
		//lock dialog, displays the title of the entry to be changed in lockstate
		templateSelector = "lock";
		Integer idInteger = null;
		int ls = -1;
		try {
			idInteger = Integer.valueOf(id);
		} catch (Exception e) {
			ls = -1;

			//access content definition object specified by id through reflection
			String title = "no title";
			Object o = null;
			o = getContentDefinition(cms, cdClass, id);
			try {
       ls = ((A_CmsContentDefinition) o).getLockstate();
				/*Method getLockstateMethod = (Method) cdClass.getMethod("getLockstate", new Class[] {});
				ls = (int) getLockstateMethod.invoke(o, new Object[0]); */
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

		//access content definition object specified by id through reflection
		String title = "no title";
		Object o = null;
		if (idInteger != null) {
			o = getContentDefinition(cms, cdClass, idInteger);
			try {
        ls = ((A_CmsContentDefinition) o).getLockstate();
			} catch (Exception e) {
                 if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
                }
			}
		} else {
			o = getContentDefinition(cms, cdClass, id);
			try {
                ls = ((A_CmsContentDefinition) o).getLockstate();
			} catch (Exception e) {
				 if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
                }
			}
		}
		//create appropriate class name with underscores for labels
		String moduleName = "";
		moduleName = (String) getClass().toString(); //get name
		moduleName = moduleName.substring(5); //remove 'class' substring at the beginning
		moduleName = moduleName.trim();
		moduleName = moduleName.replace('.', '_'); //replace dots with underscores
		//create new language file object
		CmsXmlLanguageFile lang = new CmsXmlLanguageFile(cms);

		//get the dialog from the langauge file and set it in the template
		if (ls != C_NOT_LOCKED && ls != actUserId) {
      // "lock"
			template.setData("locktitle", lang.getLanguageValue("messagebox.title.lockchange"));
			template.setData("lockstate", lang.getLanguageValue("messagebox.message1.lockchange"));
		}
		if (ls == C_NOT_LOCKED) {
      // "nolock"
			template.setData("locktitle", lang.getLanguageValue("messagebox.title.lock"));
			template.setData("lockstate", lang.getLanguageValue("messagebox.message1.lock"));
		}
		if (ls == actUserId) {
			template.setData("locktitle", lang.getLanguageValue("messagebox.title.unlock"));
			template.setData("lockstate", lang.getLanguageValue("messagebox.message1.unlock"));
		}

		//set the title of the selected entry
		template.setData("newsentry", id);

		//go to default template section
		template.setData("setaction", "default");
		parameters.put("action", "done");

		// confirmation button pressed, process data!
	} else {
		templateSelector = "done";
		// session.removeValue("idsave");

		//access content definition constructor by reflection
		Integer idInteger = null;
		int ls = C_NOT_LOCKED;
		try {
			idInteger = Integer.valueOf(id);
		} catch (Exception e) {
        ls = C_NOT_LOCKED;

			//access content definition object specified by id through reflection
			String title = "no title";
			Object o = null;
			o = getContentDefinition(cms, cdClass, id);
			try {
                ls = ((A_CmsContentDefinition) o).getLockstate();
			} catch (Exception ex) {
				 if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, ex.getMessage() );
                }
			}
		}

		//call the appropriate content definition constructor
		Object o = null;
		if (idInteger != null) {
			o = getContentDefinition(cms, cdClass, idInteger);
			try {
                ls = ((A_CmsContentDefinition) o).getLockstate();
			} catch (Exception e) {
				 if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
                }
			}
		} else {
			o = getContentDefinition(cms, cdClass, id);
			try {
                ls = ((A_CmsContentDefinition) o).getLockstate();
			} catch (Exception e) {
				 if(I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
                    A_OpenCms.log(C_OPENCMS_INFO, e.getMessage() );
                }
			}
		}
		try {
            ls = ((A_CmsContentDefinition) o).getLockstate();
		} catch (Exception e) {
			if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
				A_OpenCms.log(C_OPENCMS_INFO, getClassName() + " Backoffice getContentLock: Method getLockstate throwed an exception!");
			}
		}

		//show the possible cases of a lockstate in the template
		//and change lockstate in content definition (and in DB or VFS)
		if (ls == actUserId) {
			//steal lock (userlock -> nolock)
			try {
                ((A_CmsContentDefinition) o).setLockstate(C_NOT_LOCKED);
			} catch (Exception e) {
				if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
					A_OpenCms.log(C_OPENCMS_INFO, getClassName() + " Backoffice getContentLock: Method setLockstate throwed an exception!");
				}
			}
			//write to DB
			try {
        ((A_CmsContentDefinition) o).write(cms);   // reflection is not neccessary!
			} catch (Exception e) {
				if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
					A_OpenCms.log(C_OPENCMS_INFO, getClassName() + " Backoffice getContentLock: Method write throwed an exception!");
				}
			}
			templateSelector = "done";
		} else {
			if ((ls != C_NOT_LOCKED) && (ls != actUserId)) {
				//unlock (lock -> userlock)
				try {
          ((A_CmsContentDefinition) o).setLockstate(actUserId);
				} catch (Exception e) {
					if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
						A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentLock: Could not set lockstate!");
					}
				}
				//write to DB
				try {
          ((A_CmsContentDefinition) o).write(cms);
				} catch (Exception e) {
					if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
						A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentLock: Could not set lockstate!");
					}
				}
				templateSelector = "done";
			} else {
				//lock (nolock -> userlock)
				try {
          ((A_CmsContentDefinition) o).setLockstate(actUserId);
				} catch (Exception e) {
					if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
						A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentLock: Could not set lockstate!");
					}
				}
				//write to DB/VFS
				try {
          ((A_CmsContentDefinition) o).write(cms);
				} catch (Exception e) {
					if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
						A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentLock: Could not write to content definition!");
					}
				}
			}
		}
	}
	//finally start the processing
	processResult = startProcessing(cms, template, elementName, parameters, templateSelector);
	return processResult;
}
/**
 * gets the content definition class method object
 * @returns object content definition class method object
 */

private Object getContentMethodObject(CmsObject cms, Class cdClass, String method, Class paramClasses[], Object params[]) {

	//return value
	Object retObject = null;
	if (method != "") {
		try {
			retObject = cdClass.getMethod(method, paramClasses).invoke(null, params);
		} catch (InvocationTargetException ite) {
			if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
				A_OpenCms.log(C_OPENCMS_INFO, getClassName() + method + " throwed an InvocationTargetException!");
			}
			ite.getTargetException().printStackTrace();
		} catch (NoSuchMethodException nsm) {
			if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
				A_OpenCms.log(C_OPENCMS_INFO, getClassName() + method + ": Requested method was not found!");
			}
		} catch (Exception e) {
			if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
				A_OpenCms.log(C_OPENCMS_INFO, getClassName() + method + ": Other Exception!");
			}
		}
	}
	return retObject;
}
/**
 * gets the content of a new entry form.
 * Has to be overwritten in your backoffice class!
 */

public byte[] getContentNew(CmsObject cms, CmsXmlWpTemplateFile templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {

	parameters.put("id", "new");
	return getContentEdit(cms, templateFile, elementName, parameters, templateSelector);
}

public String getUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception {
  return "";
}
/**
 * gets the create url by using the cms object
 * @returns a string with the create url
 */

public abstract String getCreateUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception;
/**
 * gets the edit url by using the cms object
 * @returns a string with the edit url
 */

public abstract String getEditUrl(CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObject) throws Exception;
	/**
	 * Used for filling the values of a checkbox.
	 * <P>
	 * Gets the resources displayed in the Checkbox group on the new resource dialog.
	 * @param cms The CmsObject.
	 * @param lang The langauge definitions.
	 * @param names The names of the new rescources (used for optional images).
	 * @param values The links that are connected with each resource.
	 * @param parameters Hashtable of parameters.
	 * @returns The vectors names and values are filled with the information found in the
	 * workplace.ini.
	 * @exception Throws CmsException if something goes wrong.
	 */
	public Integer setCheckbox(CmsObject cms, Vector names, Vector values, Hashtable parameters)
		throws CmsException {
			int returnValue = 0;
			CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
			String checkboxValue = (String) session.getValue("checkselect");
			if (checkboxValue == null){
				checkboxValue = "";
			}
			// add values for the checkbox
			values.addElement("contents");
			values.addElement("navigation");
			values.addElement("design");
			values.addElement("other");
			// add corresponding names for the checkboxvalues
			names.addElement("contents");
			names.addElement("navigation");
			names.addElement("design");
			names.addElement("other");
			// set the return values
		    if (checkboxValue.equals("contents")) {
			    returnValue = 0;
			}
			if (checkboxValue.equals("navigation")) {
				returnValue = 1;
			}
			if (checkboxValue.equals("design")) {
				returnValue = 2;
			}
			if (checkboxValue.equals("other")) {
				returnValue = 3;
			}
		return new Integer (returnValue);
	}
/**
 *set the lockstates in the list output
 */

private void setLockstates(CmsObject cms, CmsXmlWpTemplateFile template, Class cdClass, Object entryObject, Hashtable parameters) {

	//init lock state vars
	String la = "false";
	Object laObject = new Object();
	int ls = -1;
  String lockString = null;
  int actUserId = cms.getRequestContext().currentUser().getId();
  String isLockedBy = null;

	//is the content definition object (i.e. the table entry) lockable?
	try {
		//get the method
		Method laMethod = cdClass.getMethod("isLockable", new Class[] {});
		//get the returned object
		laObject = laMethod.invoke(null, null);
	} catch (InvocationTargetException ite) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates: Method isLockable throwed an Invocation target exception!");
		}
	} catch (NoSuchMethodException nsm) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates: Requested method isLockable was not found!");
		}
	} catch (Exception e) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates: Method isLockable throwed an exception!");
		}
	}

	//cast the returned object to a string
	la = (String) laObject.toString();
	if (la.equals("false")) {
		try{
			//the entry is not lockable: use standard contextmenue
			template.setData("backofficecontextmenue", "backofficeedit");
			template.setData("lockedby", template.getDataValue("nolock"));
		} catch  (Exception e) {
			if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
				A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates:'not lockable' section hrowed an exception!");
			}
		}
	} else {
		//...get the lockstate of an entry
		try {
			//get the method lockstate
      ls = ((A_CmsContentDefinition) entryObject).getLockstate();
		} catch (Exception e) {
			if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
				A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates: Method getLockstate throwed an exception!");
			}
		}
		try {
			//show the possible cases of a lockstate in the template
			if (ls == actUserId) {
        // lockuser
        isLockedBy = cms.getRequestContext().currentUser().getName();
        template.setData("isLockedBy", isLockedBy);   // set current users name in the template
				lockString = template.getProcessedDataValue("lockuser", this, parameters);
				template.setData("lockedby", lockString);
				template.setData("backofficecontextmenue", "backofficelockuser");
			} else {
				if (ls != C_NOT_LOCKED) {
          // lock
          // set the name of the user who locked the file in the template ...
          isLockedBy = cms.readUser(ls).getName();
          template.setData("isLockedBy", isLockedBy);
					lockString = template.getProcessedDataValue("lock", this, parameters);
					template.setData("lockedby", lockString);
					template.setData("backofficecontextmenue", "backofficelock");
				} else {
          // nolock
					lockString = template.getProcessedDataValue("nolock", this, parameters);
					template.setData("lockedby", lockString);
					template.setData("backofficecontextmenue", "backofficenolock");
				}
			}
		} catch (Exception e) {
			if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
				A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice setLockstates throwed an exception!");
			}
		}
	}
}

/**
 * This method creates the selectbox in the head-frame
 * @author Tilo Kellermeier
 */
public Integer getFilter(CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters)
	throws CmsException {
  CmsSession session = (CmsSession) cms.getRequestContext().getSession(true);
  int returnValue = 0;
  String uri = cms.getRequestContext().getUri();
  String sessionSelectBoxValue = uri+"selectBoxValue";
  Vector filterMethods = getFilterMethods(cms);
  //String tmp = (String) session.getValue("selectBoxValue");
  String tmp = (String) session.getValue(sessionSelectBoxValue);
  if(tmp != null)
    returnValue = Integer.parseInt(tmp);

  for (int i = 0; i < filterMethods.size(); i++) {
		CmsFilterMethod currentFilter = (CmsFilterMethod) filterMethods.elementAt(i);
		//insert filter in the template selectbox
		names.addElement(currentFilter.getFilterName());
		values.addElement(""+ i);
  }
  return new Integer(returnValue);
}

/**
 * get the filterMethods
 * @author Tilo Kellermeier
 */
private Vector getFilterMethods(CmsObject cms) {
  Vector filterMethods = new Vector();
  Class cdClass = getContentDefinitionClass();
	try {
		filterMethods = (Vector) cdClass.getMethod("getFilterMethods", new Class[] {CmsObject.class}).invoke(null, new Object[] {cms});
	} catch (InvocationTargetException ite) {
		//error occured while applying the filter
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentHead: InvocationTargetException!");
		}
	} catch (NoSuchMethodException nsm) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentHead: Requested method was not found!");
		}
	} catch (Exception e) {
		if (I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING && A_OpenCms.isLogging() ) {
			A_OpenCms.log(C_OPENCMS_INFO, getClassName() + ": Backoffice getContentHead: Problem occured with your filter methods!");
		}
	}
  return filterMethods;
}
}

/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ui.dialogs;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResource.CmsResourceUndoMode;
import org.opencms.lock.CmsLockActionRecord;
import org.opencms.lock.CmsLockActionRecord.LockChange;
import org.opencms.lock.CmsLockException;
import org.opencms.lock.CmsLockUtil;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.ui.A_CmsUI;
import org.opencms.ui.CmsVaadinUtils;
import org.opencms.ui.I_CmsDialogContext;
import org.opencms.ui.components.CmsBasicDialog;

import org.apache.commons.logging.Log;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;

/**
 * Dialog used to change resource modification times.<p>
 */
public class CmsUndoDialog extends CmsBasicDialog {

    /** Logger instance for this class. */
    private static final Log LOG = CmsLog.getLog(CmsUndoDialog.class);

    /** Serial version id. */
    private static final long serialVersionUID = 1L;

    /** The dialog context. */
    protected I_CmsDialogContext m_context;

    /** The Cancel button. */
    private Button m_cancelButton;

    /** Check box to enable/disable modification of children. */
    private CheckBox m_modifySubresourcesField;

    /** The OK  button. */
    private Button m_okButton;

    /** The date selection field. */
    private CheckBox m_undoMoveField;

    /**
     * Creates a new instance.<p>
     *
     * @param context the dialog context
     */
    public CmsUndoDialog(I_CmsDialogContext context) {
        m_context = context;
        CmsVaadinUtils.readAndLocalizeDesign(
            this,
            OpenCms.getWorkplaceManager().getMessages(A_CmsUI.get().getLocale()),
            null);

        boolean hasFolders = false;
        for (CmsResource resource : context.getResources()) {
            if (resource.isFolder()) {
                hasFolders = true;
                break;
            }
        }
        m_modifySubresourcesField.setVisible(hasFolders);
        m_cancelButton.addClickListener(new ClickListener() {

            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {

                m_context.finish(null);
            }

        });

        m_okButton.addClickListener(new ClickListener() {

            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {

                try {
                    undo();
                    m_context.finish(null);
                } catch (Exception e) {
                    m_context.error(e);
                }
            }
        });
        displayResourceInfo(m_context.getResources());
    }

    /**
     * Touches the selected files.<p>
     *
     * @throws CmsException if something goes wrong
     */
    protected void undo() throws CmsException {

        boolean recursive = m_modifySubresourcesField.getValue().booleanValue();
        boolean undoMove = m_undoMoveField.getValue().booleanValue();
        CmsObject cms = m_context.getCms();
        for (CmsResource resource : m_context.getResources()) {
            CmsLockActionRecord actionRecord = null;
            try {
                actionRecord = CmsLockUtil.ensureLock(m_context.getCms(), resource);
                CmsResourceUndoMode mode = CmsResourceUndoMode.getUndoMode(undoMove, recursive);
                cms.undoChanges(cms.getSitePath(resource), mode);
            } finally {
                if ((actionRecord != null) && (actionRecord.getChange() == LockChange.locked)) {
                    try {
                        m_context.getCms().unlockResource(resource);
                    } catch (CmsLockException e) {
                        LOG.warn(e.getLocalizedMessage(), e);
                    }
                }

            }

        }

    }

}

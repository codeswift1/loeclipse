/*************************************************************************
 *
 * $RCSfile: NewUreAppWizard.java,v $
 *
 * $Revision: 1.5 $
 *
 * last change: $Author: cedricbosdo $ $Date: 2007/11/25 20:32:29 $
 *
 * The Contents of this file are made available subject to the terms of
 * the GNU Lesser General Public License Version 2.1
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
package org.openoffice.ide.eclipse.core.wizards;

import org.openoffice.ide.eclipse.core.OOEclipsePlugin;
import org.openoffice.ide.eclipse.core.i18n.ImagesConstants;

/**
 * The new URE application wizard is simply a new UNO project wizard, with
 * the inherited interface forced to <code>com::sun::star::lang::XMain</code>.
 *
 * @author cedricbosdo
 *
 */
public class NewUreAppWizard extends NewUnoProjectWizard {

    /**
     * Constructor.
     */
    public NewUreAppWizard() {
        setDisableServicePage("com::sun::star::lang::XMain"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        super.addPages();

        mMainPage.setDescription(Messages.getString("NewUreAppWizard.Description")); //$NON-NLS-1$
        mMainPage.setTitle(Messages.getString("NewUreAppWizard.Title")); //$NON-NLS-1$
        mMainPage.setImageDescriptor(OOEclipsePlugin.getImageDescriptor(
                        ImagesConstants.URE_APP_WIZ));
    }
}

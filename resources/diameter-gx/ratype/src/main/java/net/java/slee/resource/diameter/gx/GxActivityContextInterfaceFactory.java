/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package net.java.slee.resource.diameter.gx;

import javax.slee.ActivityContextInterface;
import javax.slee.UnrecognizedActivityException;

/**
 * Declares the methods to obtain an ActivityContextInterface for Gx activities.
 *
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 * @author <a href="mailto:carl-magnus.bjorkell@emblacom.com"> Carl-Magnus Björkell </a>
 */
public interface GxActivityContextInterfaceFactory {

    /**
     * Method for obtaining ActivityContextInterface for a Gx client activity.
     *
     * @param cSession the Gx client activity
     * @return the ActivityContextInterface
     * @throws UnrecognizedActivityException
     */
    public ActivityContextInterface getActivityContextInterface(GxClientSessionActivity cSession) throws UnrecognizedActivityException;

    /**
     * Method for obtaining ActivityContextInterface for a Gx server activity.
     *
     * @param sSession the Gx server activity
     * @return the ActivityContextInterface
     * @throws UnrecognizedActivityException
     */
    public ActivityContextInterface getActivityContextInterface(GxServerSessionActivity sSession) throws UnrecognizedActivityException;
}

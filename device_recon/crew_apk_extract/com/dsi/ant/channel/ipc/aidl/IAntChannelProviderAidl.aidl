/*
 * This software is subject to the license described in the License.txt file
 * included with this software distribution. You may not use this file except in compliance
 * with this license.
 *
 * Copyright (c) Dynastream Innovations Inc. 2013
 * All rights reserved.
 */

package com.dsi.ant.channel.ipc.aidl;

import android.os.Message;

import com.dsi.ant.channel.ipc.aidl.AntIpcResult;
import com.dsi.ant.channel.ipc.aidl.IAntChannelAidl;
import com.dsi.ant.channel.Capabilities;

interface IAntChannelProviderAidl {

    /**
     * If the returned aidl is null the Bundle "error" will contain a
     * {@link com.dsi.ant.channel.ChannelNotAvailableException} under the key "error"
     */
    IAntChannelAidl acquireChannel(int whichNetwork, in Capabilities requiredCapabilities, 
            in Capabilities desiredCapabilities, out Bundle error);

    /**
     * DO NOT REMOVE. This method is not used but must still exist in the aidl to ensure backwards
     * compatibility. Since it still exists, ANT Radio Service will be required to implement it.
     * Acquiring channel on a private network is being implemented within handleMessage().
     */
    IAntChannelAidl acquireChannelKey(in byte[] networkKey, in Capabilities requiredCapabilities,
            in Capabilities desiredCapabilities, out Bundle error);

    /**
     * Returns the number of channels with the requested capabilities available at the time of the query
     */
    int getNumChannelsAvailable(in Capabilities requiredCapabilities);
    
    /**
     * Returns true if the legacy interface is in use at the time of the query
     */
    boolean isLegacyInterfaceInUse();

    ///////////////////////////////////////////////////////////////VERSION 1 ENDS HERE

    /**
     * Passes the message with parameter data to the service and
     * returns a general result object. Allows adding new functionality
     * without aidl version issues.
     */
    AntIpcResult handleMessage(in Message command, out Bundle error);
}
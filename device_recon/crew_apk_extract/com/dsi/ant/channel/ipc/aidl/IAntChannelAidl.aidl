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
import android.os.Messenger;
import com.dsi.ant.channel.Capabilities;
import com.dsi.ant.message.ipc.AntMessageParcel;

interface IAntChannelAidl
{
    // ANT messages
    void writeMessage(in AntMessageParcel message, out Bundle error);
    AntMessageParcel requestResponse(in AntMessageParcel message, out Bundle error);

    // Broadcast data
    void setBroadcastData(in byte[] payload, out Bundle error);

    // Non-blocking transfers
    void startAcknowledgedTransfer(in byte[] payload, out Bundle error);

    // Blocking transfers
    void acknowledgedTransfer(in byte[] payload, out Bundle error);
    void burstTransfer(in byte[] data, out Bundle error);
    void cancelTransfer(out Bundle error);

    // Used to detect remote process crashes.
    void addDeathNotifier(in IBinder ref);
    void removeDeathNotifier(in IBinder ref);

    // Setup
    boolean addEventReceiver(in Messenger eventReceiver);
    boolean removeEventReceiver(in Messenger eventReceiver);
    
    // State
    Capabilities getCapabilities();
    
    void releaseChannel();
    
    // Allows adding new functionality without aidl version issues
    Bundle handleMessage(in Message command, out Bundle error);

    ///////////////////////////////////////////////////////////////VERSION 1 ENDS HERE
}
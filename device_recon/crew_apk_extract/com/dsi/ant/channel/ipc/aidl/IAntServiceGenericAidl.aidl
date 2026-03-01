/*
 * This software is subject to the license described in the License.txt file
 * included with this software distribution. You may not use this file except in compliance
 * with this license.
 *
 * Copyright (c) Dynastream Innovations Inc. 2014
 * All rights reserved.
 */

package com.dsi.ant.channel.ipc.aidl;

import android.os.Message;

import com.dsi.ant.channel.ipc.aidl.AntIpcResult;

interface IAntServiceGenericAidl {
    /**
     * Passes the message with parameter data to the service and
     * returns a general result object. Allows adding new functionality
     * without aidl version issues.
     */
    AntIpcResult sendCommand_adapterProvider(in Message command, out Bundle error);
}

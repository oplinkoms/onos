/*
 * Copyright 2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.drivers.oplink;

import org.onlab.util.Frequency;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;

/**
 * Quick and dirty device abstraction for oplink roadm devices.
 *
 * TODO: Refactor once device is finished
 */
public final class OplinkRoadmDevice {

    private static final int MAX_REPETITIONS = 100;      // Only 88 directed ports on our devices

    public static final GridType GRID_TYPE = GridType.DWDM;
    public static final ChannelSpacing CHANNEL_SPACING = ChannelSpacing.CHL_50GHZ;
    public static final Frequency START_CENTER_FREQ = Frequency.ofGHz(191_350);
    public static final Frequency END_CENTER_FREQ = Frequency.ofGHz(196_100);

    // Oplink SDN ROADM has shifted channel plan.
    // Channel 1 corresponds to ITU-T center frequency, which has spacing multiplier 0.
    public static final int MULTIPLIER_SHIFT = 1;
    private OplinkRoadmDevice() {
    }
}

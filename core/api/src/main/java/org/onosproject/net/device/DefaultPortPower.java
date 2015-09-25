/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.device;

import org.onosproject.net.DeviceId;

/**
 * Default implementation of immutable port statistics.
 */
public final class DefaultPortPower implements PortPower {

    private final DeviceId deviceId;
    private final int  port;
    private final int  power;

    private DefaultPortPower(DeviceId deviceId,
                                  int port,
                                  int power) {
        this.deviceId = deviceId;
        this.port = port;
        this.power = power;
    }

    // Constructor for serializer
    private DefaultPortPower() {
        this.deviceId = null;
        this.port = 0;
        this.power = -60;
    }

    /**
     * Creates a builder for DefaultPortPower object.
     *
     * @return builder object for DefaultPortPower object
     */
    public static DefaultPortPower.Builder builder() {
        return new Builder();
    }

    @Override
    public int port() {
        return this.port;
    }

    @Override
    public int power() {
        return this.power;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("device: " + deviceId + ", ");

        sb.append("port: " + this.port + ", ");
        sb.append("power: " + this.power);

        return sb.toString();
    }

    public static final class Builder {

        private DeviceId deviceId;
        private int port;
        private int  power;

        private Builder() {

        }

        /**
         * Sets port number.
         *
         * @param port port number
         * @return builder object
         */
        public Builder setPort(int port) {
            this.port = port;

            return this;
        }

        /**
         * Sets the device identifier.
         *
         * @param deviceId device identifier
         * @return builder object
         */
        public Builder setDeviceId(DeviceId deviceId) {
            this.deviceId = deviceId;

            return this;
        }

        /**
         * Sets port power.
         *
         * @param power port power
         * @return builder object
         */
        public Builder setPower(int power) {
            this.power = power;

            return this;
        }

        /**
         * Creates a PortPower object.
         *
        * @return DefaultPortPower object
         */
        public DefaultPortPower build() {
            return new DefaultPortPower(
                    deviceId,
                    port,
                    power);
        }

    }
}

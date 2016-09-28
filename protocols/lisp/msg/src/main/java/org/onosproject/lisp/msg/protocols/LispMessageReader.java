/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.msg.protocols;

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;

/**
 * An interface for de-serializing LISP control message.
 */
public interface LispMessageReader<T> {

    /**
     * Reads from byte buffer and de-serialize the LISP control message.
     *
     * @param byteBuf byte buffer
     * @return LISP message instance
     * @throws LispParseError LISP control message parse error
     */
    T readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException;
}

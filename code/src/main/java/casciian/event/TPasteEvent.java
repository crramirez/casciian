/*
 * Casciian - Java Text User Interface
 *
 * Copyright 2025 Carlos Rafael Ramirez
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
 */
package casciian.event;

import casciian.backend.Backend;

/**
 * This class carries text pasted through a terminal input protocol.
 */
public class TPasteEvent extends TInputEvent {

    /**
     * Pasted text.
     */
    private final String text;

    /**
     * Public constructor.
     *
     * @param backend the backend that generated this event
     * @param text pasted text
     */
    public TPasteEvent(final Backend backend, final String text) {
        super(backend);
        this.text = text;
    }

    /**
     * Get the pasted text.
     *
     * @return pasted text
     */
    public String getText() {
        return text;
    }
}

/*
 * Casciian Document Parsers add-on
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

/**
 * Markdown {@link casciian.document.parsers.DocumentParser} that converts
 * CommonMark / GitHub Flavored Markdown into {@link casciian.bits.RichText}.
 *
 * <p>The parser is registered as a {@link java.util.ServiceLoader} provider,
 * so putting the add-on on the classpath / module path makes it available
 * through {@link casciian.document.parsers.DocumentParserRegistry}.</p>
 */
package casciian.documentparsers.markdown;

/*
 	Copyright (c) 2021 TOSHIBA Digital Solutions Corporation.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.toshiba.mwcloud.gs.tools.shell.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method is a sub-command method.
 *
 * <p>This annotation defines some properties for a sub-command:
 *
 * <ul>
 *   <li>{@code name}: name of the sub-command, default value is '' (empty string)
 *   <li>{@code multiline}: whether a sub-command supports multiple-line input, default value is
 *       {@code false}
 *   <li>{@code assignall}: whether to assign all the remaining arguments to the last argument of
 *       the sub-command without splitting them with white space, default value is {@code false}
 *   <li>{@code hidden}: whether a sub-command is a hidden command, default value is {@code false}
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GSCommand {

  /** Name method with String (default is empty). */
  String name() default "";

  /** Set multiple line number (default is false). */
  boolean multiline() default false;

  /** Set assign all (default is false). */
  boolean assignall() default false;

  /** Set hidden command (default is false). */
  boolean hidden() default false;
}

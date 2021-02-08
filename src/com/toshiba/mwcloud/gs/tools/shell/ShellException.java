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

package com.toshiba.mwcloud.gs.tools.shell;

/** Exception for gs shell. */
public class ShellException extends RuntimeException {

  /** Constructor for 
   * {@code ShellException}. */
  public ShellException() {}

  /**
   * Constructor for {@code ShellException}. Constructor with only parameter message of the
   * exception.
   *
   * @param message message for the exception
   */
  public ShellException(String message) {
    super(message);
  }

  /**
   * Constructor for {@code ShellException}. Constructor with only parameter cause of the exception.
   *
   * @param cause cause of the exception
   */
  public ShellException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for {@code ShellException}. Constructor with two parameter message of the exception
   * and cause of the exception.
   *
   * @param message message for the exception
   * @param cause cause of the exception
   */
  public ShellException(String message, Throwable cause) {
    super(message, cause);
  }
}

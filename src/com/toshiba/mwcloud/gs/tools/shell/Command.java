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

import com.toshiba.mwcloud.gs.tools.shell.annotation.GSCommand;
import com.toshiba.mwcloud.gs.tools.shell.annotation.GSNullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.script.ScriptContext;




/** Class for parsing arguments and executing sub-commands using reflection mechanism. */
public class Command {
  private final String name;
  private final AbstractCommandClass instance;
  private final Method method;
  private final GSCommand annotation;

  private static final ResourceBundle bundle =
      ResourceBundle.getBundle("com.toshiba.mwcloud.gs.tools.shell.GridStoreShellMessages");

  /**
   * Constructor for {@code Command} class.
   *
   * @param instance an {@code AbstractCommandClass} object
   * @param method information about, and access to, a single method on a sub-command class
   */
  public Command(AbstractCommandClass instance, Method method) {
    GSCommand anon = method.getAnnotation(GSCommand.class);
    String name = anon.name().isEmpty() ? method.getName() : anon.name();

    this.name = name.toLowerCase();
    this.instance = instance;
    this.method = method;
    this.annotation = anon;
  }

  /**
   * Get name of the command.
   *
   * @return name of the command
   */
  public String getName() {
    return name;
  }

  /**
   * Get {@code AbstractCommandClass} of the {@code Command}.
   *
   * @return an {@code AbstractCommandClass} object
   */
  public AbstractCommandClass getInstance() {
    return instance;
  }

  /**
   * Get method of the {@code Command}.
   *
   * @return method of the {@code Command}
   */
  public Method getMethod() {
    return method;
  }

  /**
   * Check whether the command is a multiple-line command.
   *
   * @return {@code true} if the command is a multiple-line command, otherwise {@code false}
   */
  public boolean isMultiLine() {
    return annotation.multiline();
  }

  /**
   * Check whether all the remaining arguments are assigned to the last argument of the sub-command.
   *
   * @return {@code true} if all the remaining arguments are assigned to the last argument of the
   *     sub-command without splitting them with white space , otherwise {@code false}
   */
  public boolean isAssignAll() {
    return annotation.assignall();
  }

  /**
   * Check whether the command is a hidden command.
   *
   * @return {@code true} if it is a hidden command, otherwise {code false}
   */
  public boolean isHidden() {
    return annotation.hidden();
  }

  /**
   * Check whether the return type of the command is void.
   *
   * @return {@code false} if the return type is void, otherwise {@code false}}
   */
  public boolean hasResult() {
    return method.getReturnType() != void.class;
  }

  /**
   * Invoke the method represented by {@code method} with specified parameters.
   *
   * @param context the script context that holds a set of key-value attribute that can be set or
   *     retrieved by using {@code ScriptContext} methods
   * @param parameters parameters of the sub-command
   * @return the result of dispatching the method represented by this {@code instance} with {@code
   *     parameters}
   * @throws ShellException if there is an error when invoking the method
   */
  public Object invoke(ScriptContext context, String parameters) {
    Object[] parsedParams = parseArguments(context, parameters);
    try {
      return method.invoke(instance, parsedParams);
    } catch (IllegalAccessException e) {
      throw new ShellException(bundle.getString("error.internalError"), e);
    } catch (IllegalArgumentException e) {
      throw new ShellException(bundle.getString("error.illegalArgument"), e);
    } catch (InvocationTargetException e) {
      throw new ShellException(e.getCause().getMessage(), e);
    }
  }

  /**
   * Parsed a string to an array of arguments.
   *
   * @param context the script context that holds a set of key-value attribute that can be set or
   *     retrieved by using {@code ScriptContext} methods
   * @param arguments a string which represents arguments
   * @return an array of arguments
   * @throws ShellException if there are too many arguments or the variable is not defined or the
   *     required argument is not set
   */
  public Object[] parseArguments(ScriptContext context, String arguments) {
    Class<?>[] paramTypes = method.getParameterTypes();
    Annotation[][] paramAnnos = method.getParameterAnnotations();

    int paramMax = (isMultiLine() || isAssignAll()) ? paramTypes.length : Integer.MAX_VALUE;
    String[] tokens = arguments.isEmpty() ? new String[0] : arguments.split("\\s+", paramMax);

    Object[] args = new Object[tokens.length];
    for (int i = 0; i < tokens.length; ++i) {
      if (tokens[i].startsWith("$") && !tokens[i].contains(" ") ) { // Variables (If the variable name contains spaces, consider it part of the SQL statement and exclude it).
        // For the getval, set, and show subcommands, the first argument (variable name) is not expanded as a variable.
        if(i==0 && (this.name.equals("getval") || this.name.equals("set") || this.name.equals("show"))){
          args[i] = tokens[i];
          continue;
        }
        String varName = tokens[i].substring(1);

        args[i] = context.getAttribute(varName);
        if (args[i] == null) {
          throw new ShellException(
              MessageFormat.format(bundle.getString("error.varNotDefined"), varName));
        }

        if (args[i].getClass() == ShellCluster.class) {
          args[i] = instance.updateClusterNodes((ShellCluster) args[i]);
        }

        /*}*/
      } else {
        args[i] = tokens[i];
      }
    }

    Object[] result = new Object[paramTypes.length];
    int count = 0;
    for (int i = 0; i < paramTypes.length; ++i) {
      if (args.length <= i) {
        if (getAnnotation(paramAnnos[i], GSNullable.class) != null) {
          result[i] = null;

        } else if (i == paramTypes.length - 1 && method.isVarArgs()) {
          Class<?> componentType = paramTypes[i].getComponentType();
          result[i] = Array.newInstance(componentType, 0);

        } else {
          throw new ShellException(bundle.getString("error.missingArgument"));
        }

      } else if (paramTypes[i].isAssignableFrom(args[i].getClass())) {
        result[i] = args[i];

      } else if (paramTypes[i] == ShellNode[].class && args[i].getClass() == ShellCluster.class) {
        result[i] = ((ShellCluster) args[i]).getNodes().toArray(new ShellNode[0]);

      } else if (i == paramTypes.length - 1 && method.isVarArgs()) {
        Class<?> componentType = paramTypes[i].getComponentType();
        int arrayLength = args.length - i;
        Object array = Array.newInstance(componentType, arrayLength);
        for (int k = 0; k < arrayLength; ++k) {
          Array.set(array, k, convertArgument(args[i + k], componentType));
        }
        count += (arrayLength - 1);
        result[i] = array;

      } else if (paramTypes[i].isArray()) {
        if (args[i].getClass().isArray()) {
          Object[] arg = (Object[]) args[i];
          Class<?> componentType = paramTypes[i].getComponentType();
          Object array = Array.newInstance(componentType, arg.length);
          for (int k = 0; k < arg.length; ++k) {
            Array.set(array, k, convertArgument(arg[k], componentType));
          }
          count += (arg.length - 1);
          result[i] = array;

        } else {
          Class<?> componentType = paramTypes[i].getComponentType();
          Object array = Array.newInstance(componentType, 1);
          Array.set(array, 0, convertArgument(args[i], componentType));
          result[i] = array;
        }

      } else {
        result[i] = convertArgument(args[i], paramTypes[i]);
      }
      count++;
    }

    if (count < args.length) {
      throw new ShellException(bundle.getString("error.tooMuchArg"));
    }

    return result;
  }

  private static <T extends Annotation> T getAnnotation(Annotation[] annotations, Class<T> clazz) {
    for (Annotation anon : annotations) {
      if (anon.annotationType() == clazz) {
        return clazz.cast(anon);
      }
    }
    return null;
  }

  private Object convertArgument(Object arg, Class<?> clazz) {
    try {
      if (clazz == String.class) {
        return arg.toString();

      } else if (clazz == long.class || clazz == Long.class) {
        return arg.getClass() == Long.class ? arg : Long.parseLong(arg.toString());
      } else if (clazz == int.class || clazz == Integer.class) {
        return arg.getClass() == Integer.class ? arg : Integer.parseInt(arg.toString());
      } else if (clazz == double.class || clazz == Double.class) {
        return arg.getClass() == Double.class ? arg : Double.parseDouble(arg.toString());
      } else if (clazz == short.class || clazz == Short.class) {
        return arg.getClass() == Short.class ? arg : Short.parseShort(arg.toString());
      } else if (clazz == byte.class || clazz == Byte.class) {
        return arg.getClass() == Byte.class ? arg : Byte.parseByte(arg.toString());
      } else if (clazz == char.class || clazz == Character.class) {
        return arg.getClass() == Character.class ? arg : arg.toString().charAt(0);

      } else if (clazz == boolean.class || clazz == Boolean.class) {
        if (arg.getClass() == Boolean.class) {
          return arg;
        }
        String tmp = arg.toString().toLowerCase();
        if (tmp.equals("yes") || tmp.equals("on") || tmp.equals("true") || tmp.equals("1")) {
          return true;
        } else if (tmp.equals("no")
            || tmp.equals("off")
            || tmp.equals("false")
            || tmp.equals("0")) {
          return false;
        } else {
          throw new ShellException(
              bundle.getString("error.illegalBoolean") + ": arg=[" + tmp + "]");
        }

      } else if (clazz.isEnum()) {
        String tmp = arg.toString().toLowerCase();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<? extends Enum> enumClass = (Class<? extends Enum>) clazz;

        Enum<?>[] enumConstants = enumClass.getEnumConstants();
        for (Enum<?> e : enumConstants) {
          if (tmp.equals(e.name().toLowerCase())) {
            return e;
          }
        }

        throw new ShellException(
            MessageFormat.format(
                bundle.getString("error.illegalEnum"), arg, Arrays.asList(enumConstants)));

      } else if (clazz == ShellNode.class && arg.getClass() != ShellNode.class) {
        if (arg.getClass() == ShellCluster.class) {
          throw new ShellException(
              bundle.getString("error.classNotNode")
                  + ": arg=[$"
                  + ((ShellCluster) arg).getClusterVariableName()
                  + "]");
        } else {
          throw new ShellException(bundle.getString("error.classNotNode") + ": arg=[" + arg + "]");
        }

      } else {
        return arg;
      }
    } catch (IllegalArgumentException e) {
      String message = bundle.getString("error.argTypeInvalid") + ": arg=[";
      if (arg.getClass() == ShellNode.class) {
        message += "$" + ((ShellNode) arg).getName();
      } else if (arg.getClass() == ShellCluster.class) {
        message += "$" + ((ShellCluster) arg).getClusterVariableName();
      } else {
        message += arg;
      }
      throw new ShellException(message + "] expectedType=[" + clazz.getSimpleName() + "]", e);
    }
  }

  private String buildSyntax() {
    Class<?>[] paramTypes = method.getParameterTypes();
    Annotation[][] paramAnnos = method.getParameterAnnotations();

    StringBuilder result = new StringBuilder(name);
    for (int i = 0; i < paramTypes.length; ++i) {
      boolean nullable = getAnnotation(paramAnnos[i], GSNullable.class) != null;
      String name = paramTypes[i].getSimpleName().toUpperCase();
      result.append(" ");
      if (nullable) {
        result.append("[");
      }
      result.append("<").append(name).append(">");
      if (nullable) {
        result.append("]");
      }
    }

    return result.toString();
  }

  /**
   * Get syntax of the command.
   *
   * @return syntax of the command
   */
  public String getSyntax() {
    try {
      return name + " " + instance.getMessage(name + ".parameter");
    } catch (MissingResourceException e) {
      return buildSyntax();
    }
  }

  /**
   * Get description of the command.
   *
   * @return description of the command or an empty string if there is no resource for description
   */
  public String getDescription() {
    try {
      return instance.getMessage(name + ".description");
    } catch (MissingResourceException e) {
      return "";
    }
  }

  /**
   * Get detail description of the command.
   *
   * @return detail description of the command or an empty string if there is no resource for
   *     description
   */
  public String getDetailDescription() {
    try {
      return instance.getMessage(name + ".detail");
    } catch (MissingResourceException e) {
      return "";
    }
  }
}

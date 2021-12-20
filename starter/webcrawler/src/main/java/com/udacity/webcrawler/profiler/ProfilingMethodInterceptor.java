package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 * The lessons called this an invocation handler, not a method interceptor.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ProfilingState profilingState;
  private final Object delegate;


  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock,
                             Object delegate, //The wrap method called this delegate
                             ProfilingState  profilingState) {
    this.clock = Objects.requireNonNull(clock);
    this.profilingState = profilingState;
    this.delegate = delegate;
  }

  /*
  These verbose comments are for me (the student).  I am struggling to fully
  understand proxy objects.

  When a method in a proxy object is called, this is the function that handles
  that method invocation.  The method that was called is passed in 'method' and
  the arguments for that method live in the array called 'args'.
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.

    Object result = null;

    if (method.isAnnotationPresent(Profiled.class)) {

      final Instant startTime = clock.instant();

  
      try {
        result = method.invoke(delegate, args);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      } catch (Exception e) { 
        throw e;  
      } finally {

        profilingState.record(delegate.getClass(), method,
                Duration.between(startTime, clock.instant()));
      }


    } else result = method.invoke(delegate, args);
    return result;
  }
}

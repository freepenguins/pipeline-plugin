/*
 * The MIT License
 *
 * Copyright (c) 2013-2014, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.workflow.test.steps;

import hudson.Extension;
import java.util.Collections;
import java.util.HashMap;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Step that blocks until signaled.
 */
public final class SemaphoreStep extends Step {

    private static final Map<String,Integer> iota = new HashMap<String,Integer>();
    private static final Map<String,StepContext> contexts = new HashMap<String,StepContext>();
    private static final Map<String,Object> returnValues = new HashMap<String,Object>();
    private static final Map<String,Throwable> errors = new HashMap<String,Throwable>();

    public final String k;

    public SemaphoreStep() {
        this(UUID.randomUUID().toString());
    }

    public SemaphoreStep(String id) {
        Integer old = iota.get(id);
        if (old == null) {
            old = 0;
        }
        int number = old + 1;
        iota.put(id, number);
        k = id + "/" + number;
    }

    /** Starts running and waits for {@link #success} or {@link #failure} to be called, if they have not been already. */
    @Override public boolean start(StepContext context) throws Exception {
        if (returnValues.containsKey(k)) {
            context.onSuccess(returnValues.get(k));
            return true;
        } else if (errors.containsKey(k)) {
            context.onFailure(errors.get(k));
            return true;
        } else {
            contexts.put(k, context);
            return false;
        }
    }

    /** Marks the step as having successfully completed; or, if not yet started, makes it do so synchronously when started. */
    public void success(Object returnValue) {
        success(k, returnValue);
    }

    public static void success(String k, Object returnValue) {
        if (contexts.containsKey(k)) {
            contexts.get(k).onSuccess(returnValue);
        } else {
            returnValues.put(k, returnValue);
        }
    }

    /** Marks the step as having failed; or, if not yet started, makes it do so synchronously when started. */
    public void failure(Throwable error) {
        failure(k, error);
    }

    public static void failure(String k, Throwable error) {
        if (contexts.containsKey(k)) {
            contexts.get(k).onFailure(error);
        } else {
            errors.put(k, error);
        }
    }
    
    public StepContext getContext() {
        return contexts.get(k);
    }

    @Override public StepDescriptor getDescriptor() {
        return new DescriptorImpl();
    }

    @Extension public static final class DescriptorImpl extends StepDescriptor {

        @Override public Set<Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        @Override public String getFunctionName() {
            return "semaphore";
        }

        @Override public Step newInstance(Map<String,Object> arguments) {
            return new SemaphoreStep((String) arguments.get("value"));
        }

        @Override public String getDisplayName() {
            return "Test step";
        }

    }

}

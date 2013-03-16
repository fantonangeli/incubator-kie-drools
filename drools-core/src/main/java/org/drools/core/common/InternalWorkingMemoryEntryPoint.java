/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.common;

import org.drools.FactException;
import org.drools.FactHandle;
import org.drools.RuleBase;
import org.drools.reteoo.EntryPointNode;
import org.drools.core.rule.EntryPoint;
import org.drools.core.rule.Rule;


import org.drools.core.spi.Activation;
import org.kie.runtime.rule.SessionEntryPoint;

public interface InternalWorkingMemoryEntryPoint extends SessionEntryPoint {
    ObjectTypeConfigurationRegistry getObjectTypeConfigurationRegistry();
    RuleBase getRuleBase();
    public void delete(final FactHandle factHandle,
                        final Rule rule,
                        final Activation activation) throws FactException;
    public void update(org.kie.runtime.rule.FactHandle handle,
                       Object object,
                       long mask,
                       Activation activation) throws FactException;

    public EntryPoint getEntryPoint();
    public InternalWorkingMemory getInternalWorkingMemory();

    public FactHandle getFactHandleByIdentity(final Object object);
    
    void reset();
    
    ObjectStore getObjectStore();
    
    EntryPointNode getEntryPointNode();
}

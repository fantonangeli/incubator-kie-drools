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

package org.drools.reteoo.compiled;

import org.drools.core.base.ClassFieldReader;
import org.drools.core.util.Iterator;
import org.drools.core.util.ObjectHashMap;
import org.drools.reteoo.*;
import org.drools.core.rule.constraint.MvelConstraint;
import org.drools.core.spi.AlphaNodeFieldConstraint;

/**
 * This class is used for reading an {@link ObjectTypeNode} using callbacks.
 * <p/>
 * The user defines a number of callback methods in a {@link NetworkHandler} that will be called when events occur
 * during parsing. The events include :
 * <li>ObjectTypeNode</li>
 * <li>Non-hashed and hashed AlphaNodes</li>
 * <li>BetaNodes</li>
 * <li>LeftInputAdapterNodes</li>
 * <p/>
 * Events are fired when each of these network features are encountered, and again when the end of them is encountered.
 * OTN parsing is unidirectional; previously parsed data cannot be re-read without starting the parsing operation again.
 */
public class ObjectTypeNodeParser {

    /**
     * OTN we are parsing/traversing
     */
    private final ObjectTypeNode objectTypeNode;

    /**
     * Creates a new parser for the specified ObjectTypeNode
     *
     * @param objectTypeNode otn to parse
     */
    public ObjectTypeNodeParser(ObjectTypeNode objectTypeNode) {
        this.objectTypeNode = objectTypeNode;
    }

    /**
     * Parse the {@link #objectTypeNode}.
     * <p/>
     * <p>The application can use this method to instruct the OTN parser to begin parsing an {@link ObjectTypeNode}.</p>
     * <p/>
     * Once a parse is complete, an application may reuse the same Parser object, possibly with a different
     * {@link NetworkHandler}.</p>
     *
     * @param handler handler that will receieve the events generated by this parser
     * @see NetworkHandler
     */
    public void accept(NetworkHandler handler) {
        ObjectSinkPropagator propagator = objectTypeNode.getSinkPropagator();

        handler.startObjectTypeNode(objectTypeNode);
        traversePropagator(propagator, handler);
        handler.endObjectTypeNode(objectTypeNode);
    }

    private void traversePropagator(ObjectSinkPropagator propagator, NetworkHandler handler) {
        if (propagator instanceof SingleObjectSinkAdapter) {
            // we know there is only a single child sink for this propagator
            ObjectSink sink = propagator.getSinks()[0];

            traverseSink(sink, handler);
        } else if (propagator instanceof CompositeObjectSinkAdapter) {
            CompositeObjectSinkAdapter composite = (CompositeObjectSinkAdapter) propagator;

            traverseSinkLisk(composite.getHashableSinks(), handler);
            traverseSinkLisk(composite.getOthers(), handler);
            traverseHashedAlphaNodes(composite.getHashedSinkMap(), handler);
        }
    }

    private void traversePropagator(LeftTupleSinkPropagator propagator, NetworkHandler handler) {
        if (propagator instanceof SingleLeftTupleSinkAdapter) {
            // we know there is only a single child sink for this propagator
            LeftTupleSink sink = propagator.getSinks()[0];

            traverseSink(sink, handler);
        } else if (propagator instanceof CompositeLeftTupleSinkAdapter) {
            CompositeLeftTupleSinkAdapter composite = (CompositeLeftTupleSinkAdapter) propagator;

            LeftTupleSink[] sinks = composite.getSinks();
            traverseSinkLisk(sinks, handler);
        }
    }

    private void traverseSinkLisk(ObjectSinkNodeList sinks, NetworkHandler handler) {
        if (sinks != null) {
            for (ObjectSinkNode sink = sinks.getFirst(); sink != null; sink = sink.getNextObjectSinkNode()) {

                traverseSink(sink, handler);
            }
        }
    }

    private void traverseSinkLisk(LeftTupleSink[] sinks, NetworkHandler handler) {
        if (sinks != null) {
            for (int sinkIndex = 0; sinkIndex < sinks.length; ++sinkIndex) {
                traverseSink(sinks[sinkIndex], handler);
            }
        }
    }

    private void traverseHashedAlphaNodes(ObjectHashMap hashedAlphaNodes, NetworkHandler handler) {
        if (hashedAlphaNodes != null && hashedAlphaNodes.size() > 0) {
            AlphaNode firstAlpha = getFirstAlphaNode(hashedAlphaNodes);
            ClassFieldReader hashedFieldReader = getClassFieldReaderForHashedAlpha(firstAlpha);

            // start the hashed alphas
            handler.startHashedAlphaNodes(hashedFieldReader);

            Iterator iter = hashedAlphaNodes.iterator();
            for (ObjectHashMap.ObjectEntry entry = (ObjectHashMap.ObjectEntry) iter.next(); entry != null; entry = (ObjectHashMap.ObjectEntry) iter.next()) {
                CompositeObjectSinkAdapter.HashKey hashKey = (CompositeObjectSinkAdapter.HashKey) entry.getKey();
                AlphaNode alphaNode = (AlphaNode) entry.getValue();

                handler.startHashedAlphaNode(alphaNode, hashKey.getObjectValue());
                // traverse the propagator for each alpha
                traversePropagator(alphaNode.getSinkPropagator(), handler);

                handler.endHashedAlphaNode(alphaNode, hashKey.getObjectValue());
            }

            // end of the hashed alphas
            handler.endHashedAlphaNodes(hashedFieldReader);
        }
    }

    private void traverseSink(ObjectSink sink, NetworkHandler handler) {
        if (sink.getType() == NodeTypeEnums.AlphaNode) {
            AlphaNode alphaNode = (AlphaNode) sink;

            handler.startNonHashedAlphaNode(alphaNode);

            traversePropagator(alphaNode.getSinkPropagator(), handler);

            handler.endNonHashedAlphaNode(alphaNode);
        } else if (NodeTypeEnums.isBetaNode( sink ) ) {
            BetaNode betaNode = (BetaNode) sink;

            handler.startBetaNode(betaNode);
            // todo traverse beta
            handler.endBetaNode(betaNode);
        } else if (sink.getType() == NodeTypeEnums.LeftInputAdapterNode) {
            LeftInputAdapterNode leftInputAdapterNode = (LeftInputAdapterNode) sink;

            handler.startLeftInputAdapterNode(leftInputAdapterNode);
            // todo traverse lia
            handler.endLeftInputAdapterNode(leftInputAdapterNode);
        }
    }

    private void traverseSink(LeftTupleSink sink, NetworkHandler handler) {
        // todo traverse sink's propagator
    }

    /**
     * Returns the first {@link org.kie.reteoo.AlphaNode} from the specified {@link ObjectHashMap}.
     *
     * @param hashedAlphaNodes map of hashed AlphaNodes
     * @return first alpha from the specified map
     * @throws IllegalArgumentException thrown if the map doesn't contain any alpha nodes
     */
    private AlphaNode getFirstAlphaNode(final ObjectHashMap hashedAlphaNodes) throws IllegalArgumentException {
        AlphaNode firstAlphaNode;

        final Iterator iter = hashedAlphaNodes.iterator();
        final ObjectHashMap.ObjectEntry entry = (ObjectHashMap.ObjectEntry) iter.next();

        if (entry != null) {
            firstAlphaNode = (AlphaNode) entry.getValue();
        } else {
            throw new IllegalArgumentException("ObjectHashMap does not contain any hashed AlphaNodes!");
        }

        return firstAlphaNode;
    }

    /**
     * Returns the {@link ClassFieldReader} for the hashed AlphaNode. The AlphaNode's constraint has to be a
     * MvelConstraint. This is the only type of hashed alpha currently supported.
     *
     * @param alphaNode hashed alpha to get reader for
     * @return ClassFieldReader
     * @throws IllegalArgumentException thrown if the AlphaNode's {@link org.kie.spi.AlphaNodeFieldConstraint} is not a
     *                                  {@link MvelConstraint}.
     */
    private ClassFieldReader getClassFieldReaderForHashedAlpha(final AlphaNode alphaNode) throws IllegalArgumentException {
        final AlphaNodeFieldConstraint fieldConstraint = alphaNode.getConstraint();

        if (!(fieldConstraint instanceof MvelConstraint)) {
            throw new IllegalArgumentException("Only support MvelConstraint hashed AlphaNodes, not " + fieldConstraint.getClass());
        }
        // we need to get the first alpha in the map to get the attribute name that be use for the prefix of the
        // generated variable name
        return (ClassFieldReader) ((MvelConstraint)fieldConstraint).getFieldExtractor();
    }
}

package com.jonnyutah12.thaumcraftclassic.nodes;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;

/**
 * Vis node contract (content-mapping.csv node consumers: tilewandpedestal).
 *
 * <p>Original: {@code thaumcraft.api.nodes.INode} (extends IAspectContainer,
 * plus node type/modifier accessors). The node type/modifier classes and their
 * registry belong to TC-M5-02; this file carries only the aspect-container
 * surface that existing consumers (the wand recharge pedestal, TC-M6-04d)
 * need, so TC-M5-02's TileNode implements it when it lands. Do not add
 * type/modifier methods here — they belong to the M5 node classes.</p>
 */
public interface INode {

    /** Current node aspects (original IAspectContainer.getAspects). */
    AspectList getAspects();

    /** Remove up to {@code amount} of the aspect from the node (original takeFromContainer). */
    void takeFromContainer(Aspect aspect, int amount);
}

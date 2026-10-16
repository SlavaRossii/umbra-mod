package dev.umbra.util;

import java.util.UUID;

/** A creature that wears a player's own face: the client draws it with that player's skin. */
public interface Mirrored {
    /** Whose skin, or null for the plain look. */
    UUID mirrorOf();

    /** What kind of copy: 0 a shadow, 1 a freed shadow (an ally), 2 the Doppelganger. */
    int mirrorKind();
}

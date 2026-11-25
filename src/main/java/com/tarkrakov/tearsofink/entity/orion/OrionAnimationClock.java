package com.tarkrakov.tearsofink.entity.orion;

/** A visual playhead sampled on client ticks and interpolated by the renderer. */
final class OrionAnimationClock {
    private float previous;
    private float current;

    void reset(float serverTicks) {
        previous = current = serverTicks;
    }

    void advance(float serverTicks) {
        previous = current;
        // Spread packet jitter over subsequent frames without adding a fixed delay to contact poses.
        float next = current + 1;
        float correction = Math.clamp((serverTicks - next) * .5f, -1, .5f);
        // A long stall can stop the playhead but cannot run it backwards or far ahead.
        current = Math.max(previous, Math.min(next + correction, serverTicks + 1));
    }

    float previous() { return previous; }
    float current() { return current; }
}

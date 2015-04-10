package com.greenyetilab.race;

/**
 * An adapter for the GameObject interface
 */
public abstract class GameObjectAdapter implements GameObject {
    private boolean mIsFinished = false;

    @Override
    public boolean isFinished() {
        return mIsFinished;
    }

    protected void setFinished(boolean value) {
        mIsFinished = value;
    }
}

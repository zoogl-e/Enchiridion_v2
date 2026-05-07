package net.zoogle.enchiridion.api;

public final class BookContentSession {
    private BookContentMode mode = BookContentMode.READING;

    public BookContentMode mode() {
        return mode;
    }

    public void setMode(BookContentMode mode) {
        this.mode = mode != null ? mode : BookContentMode.READING;
    }

    public boolean isBountyDocument() {
        return mode == BookContentMode.BOUNTY;
    }
}

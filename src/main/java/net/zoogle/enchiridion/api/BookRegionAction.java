package net.zoogle.enchiridion.api;

@FunctionalInterface
public interface BookRegionAction {
    boolean onClick(BookContext context, int spreadIndex, int mouseButton);
}

package net.zoogle.enchiridion.api;

public interface BookPageProvider {
    BookSpread getSpread(BookContext context, int spreadIndex);

    default int spreadCount(BookContext context) {
        return 1;
    }
}

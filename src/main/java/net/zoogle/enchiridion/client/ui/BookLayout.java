package net.zoogle.enchiridion.client.ui;

public record BookLayout(
        int bookX,
        int bookY,
        int bookWidth,
        int bookHeight,
        int gutterWidth,
        int pageInset,
        int pageWidth,
        int pageHeight,
        int leftPageX,
        int leftPageY,
        int rightPageX,
        int rightPageY
) {
    public static BookLayout fromScreen(int screenWidth, int screenHeight) {
        int bookWidth = Math.min(320, screenWidth - 40);
        int bookHeight = Math.min(190, screenHeight - 40);
        int bookX = (screenWidth - bookWidth) / 2;
        int bookY = (screenHeight - bookHeight) / 2;
        int gutter = 12;
        int inset = 14;
        int pageWidth = (bookWidth - gutter) / 2 - inset * 2;
        int pageHeight = bookHeight - inset * 2;
        int leftX = bookX + inset;
        int leftY = bookY + inset;
        int rightX = bookX + bookWidth / 2 + gutter / 2 + inset;
        int rightY = leftY;
        return new BookLayout(bookX, bookY, bookWidth, bookHeight, gutter, inset, pageWidth, pageHeight, leftX, leftY, rightX, rightY);
    }
}

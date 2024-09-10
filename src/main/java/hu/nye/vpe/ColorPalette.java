package hu.nye.vpe;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Color palette.
 */
public class ColorPalette {

    private static final ColorPalette gameColorPalette = new ColorPalette();
    private final int palettesNumber = 20;
    private static int currentPaletteNumber;
    private static Color[] currentPalette;

    private final Color[] palette00 = {
            Color.decode("#FFEFE2"), Color.decode("#FFACAC"), Color.decode("#FF88B6"), Color.decode("#D55EBC"),
            Color.decode("#8E69B5"), Color.decode("#2F2F78"), Color.decode("#4A7DC4"), Color.decode("#72C2F5"),
            Color.decode("#B1FFFF"), Color.decode("#E7FFE2")
    }; // Seasons

    private final Color[] palette01 = {
            Color.decode("#F40D0D"), Color.decode("#F73005"), Color.decode("#F75B05"), Color.decode("#F9894B"),
            Color.decode("#F3E697"), Color.decode("#F8D91B"), Color.decode("#DDBF06"), Color.decode("#D98DA8"),
            Color.decode("#E00D58"), Color.decode("#E53A4D")
    }; // Sunset

    private final Color[] palette02 = {
            Color.decode("#092942"), Color.decode("#23444D"), Color.decode("#2E6958"), Color.decode("#2C8274"),
            Color.decode("#61BFBC"), Color.decode("#F0A8A1"), Color.decode("#E08689"), Color.decode("#BD5E6B"),
            Color.decode("#A63A62"), Color.decode("#FFFFFF")
    }; // Turquoise wawes

    private final Color[] palette03 = {
            Color.decode("#A45F6F"), Color.decode("#EB899F"), Color.decode("#B65165"), Color.decode("#6B313C"),
            Color.decode("#EEC1AD"), Color.decode("#F38596"), Color.decode("#8B4E82"), Color.decode("#A571A5"),
            Color.decode("#C299C2"), Color.decode("#EBBFC4")
    }; // Ashite

    private final Color[] palette04 = {
            Color.decode("#6A4C93"), Color.decode("#8D6A9F"), Color.decode("#CC76A1"), Color.decode("#FAD4C0"),
            Color.decode("#ADD7F6"), Color.decode("#B7F0AD"), Color.decode("#CDE7B0"), Color.decode("#ECE4B7"),
            Color.decode("#E6C79C"), Color.decode("#F25F5C")
    }; // Bright

    private final Color[] palette05 = {
            Color.decode("#374570"), Color.decode("#11678F"), Color.decode("#08B7D6"), Color.decode("#89F1F5"),
            Color.decode("#FFFFFF"), Color.decode("#FFDCC1"), Color.decode("#F9A361"), Color.decode("#FB7292"),
            Color.decode("#CC4778"), Color.decode("#7C2749")
    }; // Transitions

    private final Color[] palette06 = {
            Color.decode("#F1F5E7"), Color.decode("#DBD78A"), Color.decode("#CFA688"), Color.decode("#A36278"),
            Color.decode("#70518A"), Color.decode("#523957"), Color.decode("#1E1438"), Color.decode("#5D7AA3"),
            Color.decode("#6CA698"), Color.decode("#9BD0E0")
    }; // Shrubbery

    private final Color[] palette07 = {
            Color.decode("#355676"), Color.decode("#213955"), Color.decode("#7A6B82"), Color.decode("#FE6553"),
            Color.decode("#D8677C"), Color.decode("#FF7F8C"), Color.decode("#FE9A73"), Color.decode("#FEC987"),
            Color.decode("#B9EBE2"), Color.decode("#EFF0C7")
    }; // Early Sunset

    private final Color[] palette08 = {
            Color.decode("#FCA93F"), Color.decode("#FED766"), Color.decode("#4DBBAF"), Color.decode("#009FB7"),
            Color.decode("#325E97"), Color.decode("#631D76"), Color.decode("#813273"), Color.decode("#9E4770"),
            Color.decode("#CB4E80"), Color.decode("#F75590")
    }; // Cool

    private final Color[] palette09 = {
            Color.decode("#DF0606"), Color.decode("#FF9300"), Color.decode("#FFED00"), Color.decode("#00FF12"),
            Color.decode("#00FFF6"), Color.decode("#0064FF"), Color.decode("#5200FF"), Color.decode("#BF00FF"),
            Color.decode("#FF00BF"), Color.decode("#FF0080")
    }; // Rainbow

    private final Color[] palette10 = {
            Color.decode("#1D27CA"), Color.decode("#5057C4"), Color.decode("#8D92CA"), Color.decode("#00C7F2"),
            Color.decode("#12A7C6"), Color.decode("#106071"), Color.decode("#3F8D9E"), Color.decode("#81DDF1"),
            Color.decode("#90CAD6"), Color.decode("#000FFF")
    }; // Ocean

    private final Color[] palette11 = {
            Color.decode("#E6965C"), Color.decode("#CC4747"), Color.decode("#3D97CC"), Color.decode("#358899"),
            Color.decode("#0E7070"), Color.decode("#E6ACE6"), Color.decode("#C462C4"), Color.decode("#B34BB3"),
            Color.decode("#993599"), Color.decode("#3D003D")
    }; // Pants

    private final Color[] palette12 = {
            Color.decode("#C7AC69"), Color.decode("#B5785E"), Color.decode("#9C5050"), Color.decode("#733F66"),
            Color.decode("#4B335C"), Color.decode("#81AB7D"), Color.decode("#67998F"), Color.decode("#4A7380"),
            Color.decode("#3F5978"), Color.decode("#33385C")
    }; // Discover

    private final Color[] palette13 = {
            Color.decode("#FFD7F9"), Color.decode("#FFB3D8"), Color.decode("#F58484"), Color.decode("#D6965D"),
            Color.decode("#C8B341"), Color.decode("#7B8F52"), Color.decode("#3E7A80"), Color.decode("#274962"),
            Color.decode("#3F0037"), Color.decode("#6C0202")
    }; // Ancient Paints

    private final Color[] palette14 = {
            Color.decode("#F7ECA7"), Color.decode("#D9DF9B"), Color.decode("#B9D28D"), Color.decode("#91C17B"),
            Color.decode("#81A988"), Color.decode("#6077A3"), Color.decode("#5C4670"), Color.decode("#5A2651"),
            Color.decode("#560831"), Color.decode("#321209")
    }; // Nuclear Waste

    private final Color[] palette15 = {
            Color.decode("#970036"), Color.decode("#BF2B55"), Color.decode("#E36391"), Color.decode("#EC6BF4"),
            Color.decode("#B26FFA"), Color.decode("#5964B2"), Color.decode("#5C8CDD"), Color.decode("#B7FFFD"),
            Color.decode("#53C0E9"), Color.decode("#007C97")
    }; // Termite

    private final Color[] palette16 = {
            Color.decode("#FFACAC"), Color.decode("#A7FF98"), Color.decode("#CB98FF"), Color.decode("#66FFAD"),
            Color.decode("#FFCD78"), Color.decode("#96FFEE"), Color.decode("#B1FF74"), Color.decode("#EA83FF"),
            Color.decode("#8ABCFF"), Color.decode("#FFB08A")
    }; //Springs Day

    private final Color[] palette17 = {
            Color.decode("#AC00FF"), Color.decode("#008BFF"), Color.decode("#F9F871"), Color.decode("#FFC540"),
            Color.decode("#FF8449"), Color.decode("#FF1777"), Color.decode("#FF00B9"), Color.decode("#00A18D"),
            Color.decode("#009FDC"), Color.decode("#0099FF")
    }; //Beach vibes

    private final Color[] palette18 = {
            Color.decode("#3E2F78"), Color.decode("#203D87"), Color.decode("#43649C"), Color.decode("#2683B8"),
            Color.decode("#EFD8F8"), Color.decode("#D9A1CD"), Color.decode("#EFACA6"), Color.decode("#D9808F"),
            Color.decode("#B04650"), Color.decode("#A93550")
    }; //Magnolias

    private final Color[] palette19 = {
            Color.decode("#8A88B3"), Color.decode("#B7C2CC"), Color.decode("#D9F7F7"), Color.decode("#1E7B87"),
            Color.decode("#254A5C"), Color.decode("#00102E"), Color.decode("#2F003B"), Color.decode("#57024D"),
            Color.decode("#9C037D"), Color.decode("#FFC27D")
    }; //Space guy

    private final Color[] palette20 = {
            Color.decode("#FFBAEE"), Color.decode("#FFBADF"), Color.decode("#FFBCBA"), Color.decode("#FFE4BA"),
            Color.decode("#F8FFBA"), Color.decode("#BFFFBA"), Color.decode("#BAFFDF"), Color.decode("#BAFDFF"),
            Color.decode("#BADFFF"), Color.decode("#BFBAFF")
    }; //Pastel

    private final ArrayList<Color[]> paletteArray = new ArrayList<Color[]>();

    private ColorPalette() {
        init();
    }

    public static ColorPalette getInstance() {
        return gameColorPalette;
    }

    private void init() {
        paletteArray.add(palette00);
        paletteArray.add(palette01);
        paletteArray.add(palette02);
        paletteArray.add(palette03);
        paletteArray.add(palette04);
        paletteArray.add(palette05);
        paletteArray.add(palette06);
        paletteArray.add(palette07);
        paletteArray.add(palette08);
        paletteArray.add(palette09);
        paletteArray.add(palette10);
        paletteArray.add(palette11);
        paletteArray.add(palette12);
        paletteArray.add(palette13);
        paletteArray.add(palette14);
        paletteArray.add(palette15);
        paletteArray.add(palette16);
        paletteArray.add(palette17);
        paletteArray.add(palette18);
        paletteArray.add(palette19);
        paletteArray.add(palette20);
        setRandomPalette();
    }

    /**
     * Set random palette.
     */
    public void setRandomPalette() {
        Random r = new Random();
        this.currentPaletteNumber = r.nextInt(palettesNumber);
        this.currentPalette = paletteArray.get(currentPaletteNumber);
        Collections.shuffle(Collections.singletonList(this.currentPalette));
    }

    public Color[] getCurrentPalette() {
        return currentPalette;
    }

}

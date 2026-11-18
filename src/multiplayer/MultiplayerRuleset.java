package multiplayer;

/** Multiplayer-safe rulesets implemented for independent player snakes. */
public enum MultiplayerRuleset {
    CLASSIC("Classic Snake"),
    COMBO("Combo"),
    SPEED("Progressive Speed"),
    WRAPAROUND("Wraparound");

    private final String displayName;
    MultiplayerRuleset(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    /** Resolves a stable wire identifier without accepting arbitrary class names. */
    public static MultiplayerRuleset fromWire(String value) {
        try { return valueOf(value); }
        catch (RuntimeException exception) { throw new MultiplayerException("Unsupported multiplayer ruleset: " + value); }
    }
}
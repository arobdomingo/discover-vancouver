package com.discovervancouver.importer;

public enum VancouverDataset {
    PARKS("parks", "park", false, true),
    CULTURAL_SPACES("cultural-spaces", "culture", true, false),
    LIBRARIES("libraries", "study", true, true),
    COMMUNITY_CENTRES("community-centres", "recreation", true, false),
    FOOD_VENDORS("food-vendors", "restaurant", false, false),
    FREE_LOW_COST_FOOD(
        "free-and-low-cost-food-programs",
        "restaurant",
        true,
        true
    );

    private final String id;
    private final String category;
    private final boolean indoor;
    private final boolean priceKnown;

    VancouverDataset(
        String id,
        String category,
        boolean indoor,
        boolean priceKnown
    ) {
        this.id = id;
        this.category = category;
        this.indoor = indoor;
        this.priceKnown = priceKnown;
    }

    public String id() { return id; }
    public String category() { return category; }
    public boolean indoor() { return indoor; }
    public boolean priceKnown() { return priceKnown; }
}

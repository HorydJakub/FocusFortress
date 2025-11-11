package com.focusfortress.model;

import lombok.Getter;

import java.util.List;

@Getter
public enum InterestCategory {
        PHILOSOPHY_MINDFULNESS(
            "Philosophy & Mindfulness",
            List.of(
                    new CategoryStructure("Mind & Body", "🧠", List.of(
                            new SubcategoryStructure("Meditation", "🧘‍♂️"),
                            new SubcategoryStructure("Stoicism", "⚖️"),
                            new SubcategoryStructure("Buddhism", "☸️"),
                            new SubcategoryStructure("Consciousness", "💭")
                    )),
                    new CategoryStructure("Personal Growth", "🌱", List.of(
                            new SubcategoryStructure("Self-Reflection", "🪞"),
                            new SubcategoryStructure("Journaling", "📔"),
                            new SubcategoryStructure("Inner Peace", "☮️")
                    ))
            )
    ),

    SCIENCE_TECHNOLOGY(
            "Science & Technology",
            List.of(
                    new CategoryStructure("Natural Sciences", "🌌", List.of(
                            new SubcategoryStructure("Biology", "🧬"),
                            new SubcategoryStructure("Physics", "⚛️"),
                            new SubcategoryStructure("Chemistry", "⚗️"),
                            new SubcategoryStructure("Astronomy", "🔭")
                    )),
                    new CategoryStructure("Tech & AI", "🤖", List.of(
                            new SubcategoryStructure("Programming", "💻"),
                            new SubcategoryStructure("Artificial Intelligence", "🧠"),
                            new SubcategoryStructure("Data Science", "📊")
                    ))
            )
    ),

    HEALTH_FITNESS(
            "Health & Fitness",
            List.of(
                    new CategoryStructure("Physical Health", "🏃", List.of(
                            new SubcategoryStructure("Exercise", "🏋️"),
                            new SubcategoryStructure("Nutrition", "🥗"),
                            new SubcategoryStructure("Sleep", "😴"),
                            new SubcategoryStructure("Recovery", "🧘")
                    )),
                    new CategoryStructure("Mental Health", "🧠", List.of(
                            new SubcategoryStructure("Stress Management", "😌"),
                            new SubcategoryStructure("Mental Wellness", "💆"),
                            new SubcategoryStructure("Therapy", "🗣️")
                    ))
            )
    ),

    PERSONAL_DEVELOPMENT(
            "Personal Development",
            List.of(
                    new CategoryStructure("Skills & Learning", "📚", List.of(
                            new SubcategoryStructure("Reading", "📖"),
                            new SubcategoryStructure("Learning", "🎓"),
                            new SubcategoryStructure("Critical Thinking", "🤔")
                    )),
                    new CategoryStructure("Habits & Productivity", "⚡", List.of(
                            new SubcategoryStructure("Time Management", "⏰"),
                            new SubcategoryStructure("Focus", "🎯"),
                            new SubcategoryStructure("Goal Setting", "🏆")
                    ))
            )
    ),

    FINANCE_WEALTH(
            "Finance & Wealth",
            List.of(
                    new CategoryStructure("Personal Finance", "💵", List.of(
                            new SubcategoryStructure("Budgeting", "📊"),
                            new SubcategoryStructure("Saving", "🏦"),
                            new SubcategoryStructure("Investing", "📈"),
                            new SubcategoryStructure("Debt Management", "💳")
                    )),
                    new CategoryStructure("Career & Income", "💼", List.of(
                            new SubcategoryStructure("Career Growth", "📈"),
                            new SubcategoryStructure("Entrepreneurship", "🚀"),
                            new SubcategoryStructure("Side Hustles", "💡")
                    ))
            )
    ),

    CREATIVITY_ARTS(
            "Creativity & Arts",
            List.of(
                    new CategoryStructure("Creative Expression", "🖌️", List.of(
                            new SubcategoryStructure("Writing", "✍️"),
                            new SubcategoryStructure("Drawing", "🎨"),
                            new SubcategoryStructure("Music", "🎵"),
                            new SubcategoryStructure("Design", "🖼️")
                    )),
                    new CategoryStructure("Hobbies & Crafts", "🛠️", List.of(
                            new SubcategoryStructure("DIY", "🔨"),
                            new SubcategoryStructure("Photography", "📷"),
                            new SubcategoryStructure("Cooking", "👨‍🍳")
                    ))
            )
    ),

    RELATIONSHIPS_SOCIAL(
            "Relationships & Social",
            List.of(
                    new CategoryStructure("Connections", "💞", List.of(
                            new SubcategoryStructure("Communication", "💬"),
                            new SubcategoryStructure("Empathy", "❤️"),
                            new SubcategoryStructure("Boundaries", "🚧")
                    )),
                    new CategoryStructure("Community", "🤝", List.of(
                            new SubcategoryStructure("Networking", "🌐"),
                            new SubcategoryStructure("Social Skills", "🗣️"),
                            new SubcategoryStructure("Public Speaking", "🎤")
                    ))
            )
    ),

    NATURE_ENVIRONMENT(
            "Nature & Environment",
            List.of(
                    new CategoryStructure("Sustainability", "♻️", List.of(
                            new SubcategoryStructure("Eco-Living", "🌿"),
                            new SubcategoryStructure("Climate Action", "🌡️"),
                            new SubcategoryStructure("Minimalism", "📦")
                    )),
                    new CategoryStructure("Outdoor Activities", "🏞️", List.of(
                            new SubcategoryStructure("Hiking", "🥾"),
                            new SubcategoryStructure("Gardening", "🌻"),
                            new SubcategoryStructure("Wildlife", "🦌")
                    ))
            )
    ),

    HISTORY_CULTURE(
            "History & Culture",
            List.of(
                    new CategoryStructure("Human History", "🏛️", List.of(
                            new SubcategoryStructure("Ancient Civilizations", "🏺"),
                            new SubcategoryStructure("Modern History", "📰"),
                            new SubcategoryStructure("Anthropology", "🗿")
                    )),
                    new CategoryStructure("Cultural Studies", "🌏", List.of(
                            new SubcategoryStructure("Languages", "🗣️"),
                            new SubcategoryStructure("Traditions", "🎭"),
                            new SubcategoryStructure("World Cultures", "🌐")
                    ))
            )
    ),

    SPIRITUALITY(
            "Spirituality",
            List.of(
                    new CategoryStructure("Inner Journey", "🔮", List.of(
                            new SubcategoryStructure("Spiritual Practices", "🕉️"),
                            new SubcategoryStructure("Mindfulness", "🧘"),
                            new SubcategoryStructure("Purpose", "🎯")
                    )),
                    new CategoryStructure("Belief Systems", "🙏", List.of(
                            new SubcategoryStructure("Religion", "⛪"),
                            new SubcategoryStructure("Philosophy of Life", "💭"),
                            new SubcategoryStructure("Ethics", "⚖️")
                    ))
            )
    );

    private final String displayName;
    private final List<CategoryStructure> categories;

    InterestCategory(String displayName, List<CategoryStructure> categories) {
        this.displayName = displayName;
        this.categories = categories;
    }

    public static InterestCategory fromString(String value) {
        for (InterestCategory ic : values()) {
            if (ic.name().equals(value) || ic.displayName.equals(value)) {
                return ic;
            }
        }
        throw new IllegalArgumentException("Invalid interest category: " + value);
    }
}
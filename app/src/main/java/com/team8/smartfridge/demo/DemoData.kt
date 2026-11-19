package com.team8.smartfridge.demo

import com.team8.smartfridge.model.DailyLog
import com.team8.smartfridge.model.PantryItem
import com.team8.smartfridge.model.SavedRecipe
import com.team8.smartfridge.model.User
import com.team8.smartfridge.network.AnalyzedInstruction
import com.team8.smartfridge.network.ExtendedIngredient
import com.team8.smartfridge.network.IngredientItem
import com.team8.smartfridge.network.IngredientSuggestion
import com.team8.smartfridge.network.InstructionStep
import com.team8.smartfridge.network.Nutrient
import com.team8.smartfridge.network.NutritionInfo
import com.team8.smartfridge.network.RecipeDetail
import com.team8.smartfridge.network.RecipeMatchResult
import com.team8.smartfridge.network.RecipeSearchItem
import com.team8.smartfridge.network.SubstituteResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Sample data and canned answers for the DEMO build (BuildConfig.DEMO).
 *
 * The demo build has no accounts, no API keys and no network: every screen is
 * fed from here, so anyone can install the APK and explore the whole app. The
 * normal build never touches this file.
 */
object DemoData {

    private const val PKG = "com.team8.smartfridge.demo"
    private fun image(id: Int) = "android.resource://$PKG/drawable/demo_recipe_$id"

    // ── Recipes ───────────────────────────────────────────────────────────────

    private class Recipe(
        val id: Int,
        val title: String,
        val meal: String,                 // breakfast | lunch | dinner | snack
        val minutes: Int,
        val servings: Int,
        val cal: Float, val protein: Float, val carbs: Float, val fat: Float,
        val tags: Set<String>,            // vegetarian, vegan, keto, gluten-free
        val avoid: Set<String>,           // allergens it contains
        val ingredients: List<Pair<String, String>>,   // name to "amount, prep"
        val steps: List<String>
    )

    private val recipes = listOf(
        Recipe(2001, "Veggie Omelette", "breakfast", 15, 2, 320f, 22f, 8f, 24f,
            setOf("vegetarian", "keto", "gluten-free"), setOf("egg", "dairy"),
            listOf("eggs" to "3 large eggs", "spinach" to "1 cup fresh spinach", "tomato" to "1 tomato, diced",
                "cheese" to "1/4 cup shredded cheese", "olive oil" to "1 tbsp olive oil"),
            listOf("Whisk the eggs with a pinch of salt and pepper.",
                "Warm the olive oil in a pan and soften the spinach and tomato for two minutes.",
                "Pour in the eggs, scatter the cheese and cook until just set.",
                "Fold in half and slide onto a warm plate.")),
        Recipe(2002, "Banana Oat Pancakes", "breakfast", 20, 3, 410f, 11f, 66f, 12f,
            setOf("vegetarian"), setOf("egg", "dairy", "gluten"),
            listOf("banana" to "2 ripe bananas, mashed", "oats" to "1 cup rolled oats", "eggs" to "2 eggs",
                "milk" to "1/2 cup milk", "maple syrup" to "2 tbsp maple syrup, to serve"),
            listOf("Blend the oats into a coarse flour.",
                "Mix with the mashed bananas, eggs and milk into a thick batter.",
                "Cook spoonfuls on a lightly oiled pan until golden on both sides.",
                "Serve with maple syrup and sliced banana.")),
        Recipe(2003, "Berry Overnight Oats", "breakfast", 10, 1, 360f, 14f, 58f, 9f,
            setOf("vegetarian"), setOf("dairy", "gluten"),
            listOf("oats" to "1/2 cup rolled oats", "milk" to "3/4 cup milk", "yogurt" to "1/2 cup yogurt",
                "mixed berries" to "1/2 cup mixed berries", "honey" to "1 tsp honey"),
            listOf("Stir the oats, milk and yogurt together in a jar.",
                "Fold in half of the berries and the honey.",
                "Cover and chill overnight.",
                "Top with the remaining berries and eat cold.")),
        Recipe(2004, "Avocado Toast with Egg", "breakfast", 10, 1, 390f, 16f, 32f, 23f,
            setOf("vegetarian"), setOf("egg", "gluten"),
            listOf("bread" to "2 slices whole-grain bread", "avocado" to "1 ripe avocado", "eggs" to "1 egg",
                "lemon" to "1/2 lemon, juiced", "chili flakes" to "a pinch of chili flakes"),
            listOf("Toast the bread until crisp.",
                "Mash the avocado with lemon juice, salt and pepper.",
                "Fry or poach the egg.",
                "Pile the avocado on the toast, top with the egg and chili flakes.")),
        Recipe(2005, "Greek Salad", "lunch", 10, 2, 260f, 9f, 14f, 19f,
            setOf("vegetarian", "keto", "gluten-free"), setOf("dairy"),
            listOf("cucumber" to "1 cucumber, chunked", "tomato" to "3 tomatoes, wedged", "red onion" to "1/2 red onion, sliced",
                "feta cheese" to "1/2 cup feta", "olives" to "1/3 cup olives", "olive oil" to "2 tbsp olive oil"),
            listOf("Chop the cucumber and tomatoes into bite-size pieces.",
                "Add the onion, olives and crumbled feta.",
                "Dress with olive oil, oregano and a squeeze of lemon.",
                "Toss gently and serve straight away.")),
        Recipe(2006, "Chicken Caesar Wrap", "lunch", 15, 2, 480f, 36f, 38f, 20f,
            emptySet(), setOf("gluten", "dairy", "egg", "fish"),
            listOf("chicken breast" to "2 cooked chicken breasts, sliced", "tortillas" to "2 large tortillas",
                "romaine lettuce" to "2 handfuls, shredded", "parmesan cheese" to "2 tbsp grated", "caesar dressing" to "3 tbsp"),
            listOf("Toss the chicken, lettuce and parmesan with the dressing.",
                "Warm the tortillas for ten seconds.",
                "Spoon the filling down the middle, fold in the sides and roll tightly.",
                "Slice in half and serve.")),
        Recipe(2007, "Hearty Lentil Soup", "lunch", 40, 4, 350f, 20f, 52f, 6f,
            setOf("vegetarian", "vegan", "gluten-free"), emptySet(),
            listOf("lentils" to "1 cup red lentils", "carrot" to "2 carrots, diced", "onion" to "1 onion, diced",
                "garlic" to "3 cloves garlic", "vegetable stock" to "4 cups vegetable stock", "cumin" to "1 tsp ground cumin"),
            listOf("Soften the onion, carrot and garlic in a large pot.",
                "Stir in the cumin, then the lentils and stock.",
                "Simmer for 30 minutes until the lentils are soft.",
                "Blend half of it for a creamy texture and season to taste.")),
        Recipe(2008, "Quinoa Buddha Bowl", "lunch", 25, 2, 430f, 16f, 58f, 15f,
            setOf("vegetarian", "vegan", "gluten-free"), setOf("sesame"),
            listOf("quinoa" to "1 cup cooked quinoa", "chickpeas" to "1 can chickpeas, drained", "avocado" to "1 avocado, sliced",
                "spinach" to "2 handfuls spinach", "cucumber" to "1/2 cucumber", "lemon" to "1 lemon", "tahini" to "2 tbsp tahini"),
            listOf("Roast the chickpeas with a little oil until crunchy.",
                "Arrange quinoa, spinach, cucumber and avocado in a bowl.",
                "Top with the chickpeas.",
                "Whisk the tahini with lemon juice and water, then drizzle over.")),
        Recipe(2009, "Tomato Basil Pasta", "dinner", 25, 4, 540f, 18f, 82f, 14f,
            setOf("vegetarian", "vegan"), setOf("gluten"),
            listOf("pasta" to "400 g pasta", "tomato" to "6 ripe tomatoes, chopped", "garlic" to "3 cloves garlic",
                "olive oil" to "3 tbsp olive oil", "basil" to "a handful of fresh basil"),
            listOf("Boil the pasta in salted water until al dente.",
                "Gently fry the garlic in olive oil, add the tomatoes and simmer for ten minutes.",
                "Toss the pasta through the sauce with a splash of pasta water.",
                "Finish with torn basil.")),
        Recipe(2010, "Garlic Chicken Stir Fry", "dinner", 30, 3, 480f, 38f, 30f, 20f,
            emptySet(), setOf("soy"),
            listOf("chicken breast" to "2 chicken breasts, sliced", "broccoli" to "1 head broccoli, in florets", "garlic" to "4 cloves garlic",
                "soy sauce" to "3 tbsp soy sauce", "rice" to "1 1/2 cups cooked rice"),
            listOf("Sear the chicken in a very hot pan until golden.",
                "Add the broccoli and garlic and stir fry for three minutes.",
                "Pour in the soy sauce and a splash of water and toss to coat.",
                "Serve over steamed rice.")),
        Recipe(2011, "Lemon Herb Salmon", "dinner", 25, 2, 520f, 40f, 12f, 34f,
            setOf("keto", "gluten-free"), setOf("fish"),
            listOf("salmon" to "2 salmon fillets", "lemon" to "1 lemon", "garlic" to "2 cloves garlic, minced",
                "olive oil" to "2 tbsp olive oil", "asparagus" to "1 bunch asparagus"),
            listOf("Heat the oven to 200 C.",
                "Rub the salmon with olive oil, garlic, lemon zest and herbs.",
                "Lay it on a tray with the asparagus and lemon slices.",
                "Roast for 12 to 15 minutes until the salmon flakes.")),
        Recipe(2012, "Mushroom Risotto", "dinner", 45, 4, 460f, 12f, 70f, 14f,
            setOf("vegetarian", "gluten-free"), setOf("dairy"),
            listOf("rice" to "1 1/2 cups arborio rice", "mushrooms" to "300 g mushrooms, sliced", "onion" to "1 onion, finely diced",
                "garlic" to "2 cloves garlic", "vegetable stock" to "4 cups hot vegetable stock", "parmesan cheese" to "1/2 cup grated"),
            listOf("Brown the mushrooms in a wide pan and set them aside.",
                "Soften the onion and garlic, then toast the rice for a minute.",
                "Add the hot stock a ladle at a time, stirring, until the rice is creamy, about 25 minutes.",
                "Stir in the mushrooms and parmesan and rest for two minutes.")),
        Recipe(2013, "Hummus and Veggie Sticks", "snack", 10, 2, 190f, 7f, 22f, 9f,
            setOf("vegetarian", "vegan", "gluten-free"), setOf("sesame"),
            listOf("hummus" to "1/2 cup hummus", "carrot" to "2 carrots, in sticks", "cucumber" to "1 cucumber, in sticks",
                "bell pepper" to "1 bell pepper, in strips"),
            listOf("Cut the vegetables into sticks.",
                "Spoon the hummus into a small bowl.",
                "Serve together and dip away.")),
        Recipe(2014, "Apple Almond Butter Bites", "snack", 5, 2, 220f, 5f, 28f, 11f,
            setOf("vegetarian", "vegan", "gluten-free"), setOf("nuts"),
            listOf("apple" to "1 large apple, sliced", "almond butter" to "2 tbsp almond butter", "cinnamon" to "a pinch of cinnamon"),
            listOf("Slice the apple into rounds and remove the core.",
                "Spread each with almond butter.",
                "Dust with cinnamon.")),
        Recipe(2015, "Greek Yogurt Parfait", "snack", 5, 1, 240f, 17f, 30f, 6f,
            setOf("vegetarian"), setOf("dairy", "gluten"),
            listOf("yogurt" to "3/4 cup Greek yogurt", "mixed berries" to "1/2 cup mixed berries", "granola" to "2 tbsp granola",
                "honey" to "1 tsp honey"),
            listOf("Layer yogurt and berries in a glass.",
                "Top with granola and a drizzle of honey.")),
        Recipe(2016, "Cauliflower Fried Rice", "dinner", 25, 3, 310f, 14f, 22f, 17f,
            setOf("vegetarian", "keto"), setOf("egg", "soy"),
            listOf("cauliflower" to "1 head cauliflower, riced", "carrot" to "1 carrot, diced", "eggs" to "2 eggs",
                "soy sauce" to "2 tbsp soy sauce", "garlic" to "2 cloves garlic", "green onion" to "2 green onions"),
            listOf("Pulse the cauliflower into rice-sized grains.",
                "Fry the garlic and carrot, then push to the side and scramble the eggs.",
                "Add the cauliflower and soy sauce and stir fry for five minutes.",
                "Top with sliced green onion.")),
    )

    private fun nutrition(r: Recipe) = NutritionInfo(
        listOf(
            Nutrient("Calories", r.cal, "kcal"),
            Nutrient("Protein", r.protein, "g"),
            Nutrient("Carbohydrates", r.carbs, "g"),
            Nutrient("Fat", r.fat, "g")
        )
    )

    private val genericTerms = setOf("", "healthy", "recipe", "recipes", "food", "meal", "easy", "quick")

    private fun avoidTokens(intolerances: String?): Set<String> {
        val raw = intolerances?.lowercase().orEmpty()
        val out = mutableSetOf<String>()
        if (raw.contains("nut")) out += "nuts"
        if (raw.contains("dairy") || raw.contains("lactose") || raw.contains("milk")) out += "dairy"
        if (raw.contains("egg")) out += "egg"
        if (raw.contains("gluten") || raw.contains("wheat")) out += "gluten"
        if (raw.contains("shellfish")) out += "shellfish"
        if (raw.contains("fish") || raw.contains("seafood")) out += "fish"
        if (raw.contains("soy")) out += "soy"
        if (raw.contains("sesame")) out += "sesame"
        return out
    }

    /** Mirrors Spoonacular's complexSearch closely enough for the app's filters. */
    fun searchRecipes(query: String, intolerances: String?, diet: String?, type: String?): List<RecipeSearchItem> {
        val term = query.trim().lowercase()
        val dietKey = diet?.lowercase()?.takeIf { it.isNotBlank() && it != "none" }
        val avoid = avoidTokens(intolerances)
        return recipes.asSequence()
            .filter { type.isNullOrBlank() || it.meal == type.lowercase() }
            .filter { r ->
                when (dietKey) {
                    null -> true
                    "vegetarian" -> "vegetarian" in r.tags || "vegan" in r.tags
                    "vegan", "keto", "gluten-free", "gluten free" -> dietKey.replace(" ", "-") in r.tags
                    else -> true
                }
            }
            .filter { r -> r.avoid.none { it in avoid } }
            .filter { r ->
                term in genericTerms || r.title.lowercase().contains(term) ||
                    r.ingredients.any { it.first.contains(term) } || r.meal == term
            }
            .map {
                RecipeSearchItem(
                    id = it.id, title = it.title, image = image(it.id), imageType = "jpg",
                    readyInMinutes = it.minutes, servings = it.servings, nutrition = nutrition(it)
                )
            }
            .toList()
    }

    /** "What can I make?": recipes ranked by how much of your pantry they use. */
    fun findByIngredients(pantry: List<String>): List<RecipeMatchResult> {
        val have = pantry.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        fun owned(name: String) = have.any { name.contains(it) || it.contains(name) }
        return recipes.mapNotNull { r ->
            val used = r.ingredients.filter { owned(it.first) }
            if (used.isEmpty()) return@mapNotNull null
            val missed = r.ingredients.filterNot { owned(it.first) }
            fun item(p: Pair<String, String>) = IngredientItem(id = 0, name = p.first, amount = 1f, unit = "", image = null)
            RecipeMatchResult(
                id = r.id, title = r.title, image = image(r.id),
                usedIngredientCount = used.size, missedIngredientCount = missed.size,
                usedIngredients = used.map(::item), missedIngredients = missed.map(::item)
            )
        }.sortedWith(compareBy({ it.missedIngredientCount }, { -it.usedIngredientCount }))
    }

    fun recipeById(id: Int): RecipeDetail? {
        val r = recipes.firstOrNull { it.id == id } ?: return null
        return RecipeDetail(
            id = r.id, title = r.title, image = image(r.id), readyInMinutes = r.minutes, servings = r.servings,
            summary = "<b>${r.title}</b> is a ${r.meal} that takes about ${r.minutes} minutes and serves ${r.servings}. " +
                "A sample recipe in the SmartFridge demo.",
            nutrition = nutrition(r),
            extendedIngredients = r.ingredients.mapIndexed { i, (name, original) ->
                ExtendedIngredient(id = i + 1, name = name, original = original, amount = 1f, unit = "")
            },
            analyzedInstructions = listOf(
                AnalyzedInstruction("", r.steps.mapIndexed { i, s -> InstructionStep(i + 1, s, null) })
            )
        )
    }

    private val ingredientNames = listOf(
        "almond butter", "apple", "apricot", "asparagus", "avocado", "bacon", "banana", "basil", "bell pepper", "black beans",
        "blueberries", "bread", "broccoli", "butter", "cabbage", "carrot", "cauliflower", "celery", "cheese", "cheddar cheese",
        "chicken breast", "chicken thigh", "chickpeas", "cinnamon", "coconut milk", "cucumber", "cumin", "eggs", "feta cheese",
        "flour", "garlic", "ginger", "green beans", "ground beef", "honey", "hummus", "kale", "lemon", "lentils", "lettuce",
        "lime", "maple syrup", "milk", "mushrooms", "oats", "olive oil", "olives", "onion", "orange", "oregano", "pasta",
        "peanut butter", "peas", "pork chop", "potato", "quinoa", "rice", "salmon", "shrimp", "soy sauce", "spinach",
        "strawberries", "sweet potato", "tahini", "tofu", "tomato", "tortillas", "tuna", "turkey", "yogurt", "zucchini"
    )

    fun autocomplete(query: String): List<IngredientSuggestion> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return ingredientNames.filter { it.contains(q) }.take(15)
            .mapIndexed { i, n -> IngredientSuggestion(id = i + 1, name = n, image = null) }
    }

    private val substitutes = mapOf(
        "butter" to listOf("olive oil", "coconut oil", "unsweetened applesauce"),
        "milk" to listOf("oat milk", "almond milk", "soy milk"),
        "eggs" to listOf("flax egg", "mashed banana", "silken tofu"),
        "egg" to listOf("flax egg", "mashed banana", "silken tofu"),
        "cheese" to listOf("nutritional yeast", "cashew cream", "vegan cheese"),
        "feta cheese" to listOf("goat cheese", "ricotta salata", "firm tofu with lemon"),
        "parmesan cheese" to listOf("nutritional yeast", "pecorino", "grana padano"),
        "yogurt" to listOf("coconut yogurt", "sour cream", "silken tofu blended smooth"),
        "chicken breast" to listOf("turkey breast", "firm tofu", "chickpeas"),
        "pasta" to listOf("rice noodles", "zucchini noodles", "gluten-free pasta"),
        "rice" to listOf("quinoa", "cauliflower rice", "couscous"),
        "flour" to listOf("oat flour", "almond flour", "gluten-free blend"),
        "soy sauce" to listOf("tamari", "coconut aminos", "worcestershire sauce"),
        "honey" to listOf("maple syrup", "agave", "date syrup"),
        "bread" to listOf("whole-grain wrap", "gluten-free bread", "rice cakes"),
        "salmon" to listOf("trout", "arctic char", "firm tofu with nori"),
        "tahini" to listOf("sunflower seed butter", "almond butter", "peanut butter"),
        "garlic" to listOf("garlic powder", "shallot", "chives"),
        "lemon" to listOf("lime", "white wine vinegar", "orange juice"),
        "olive oil" to listOf("avocado oil", "sunflower oil", "melted butter"),
        "maple syrup" to listOf("honey", "agave syrup", "date syrup"),
        "mixed berries" to listOf("frozen berries", "sliced strawberries", "chopped apple"),
        "basil" to listOf("parsley", "oregano", "baby spinach"),
        "cucumber" to listOf("zucchini", "celery", "bell pepper"),
        "red onion" to listOf("shallot", "white onion", "spring onion"),
        "olives" to listOf("capers", "pickled gherkins", "sun-dried tomatoes"),
        "tortillas" to listOf("pita bread", "flatbread", "large lettuce leaves"),
        "romaine lettuce" to listOf("baby gem lettuce", "spinach", "kale"),
        "caesar dressing" to listOf("yogurt with lemon and garlic", "ranch dressing", "olive oil and lemon juice"),
        "cumin" to listOf("ground coriander", "paprika", "curry powder"),
        "quinoa" to listOf("couscous", "brown rice", "bulgur wheat"),
        "chickpeas" to listOf("white beans", "lentils", "cubed tofu"),
        "vegetable stock" to listOf("chicken stock", "miso broth", "water and a pinch of salt"),
        "asparagus" to listOf("green beans", "broccoli", "zucchini"),
        "mushrooms" to listOf("zucchini", "eggplant", "firm tofu"),
        "hummus" to listOf("guacamole", "white bean dip", "tzatziki"),
        "bell pepper" to listOf("carrot sticks", "celery", "snap peas"),
        "almond butter" to listOf("peanut butter", "sunflower seed butter", "tahini"),
        "cinnamon" to listOf("nutmeg", "allspice", "vanilla extract"),
        "apple" to listOf("pear", "banana", "peach"),
        "granola" to listOf("crushed nuts", "rolled oats", "muesli"),
        "cauliflower" to listOf("broccoli", "riced zucchini", "cooked rice"),
        "green onion" to listOf("chives", "white onion", "leek"),
        "broccoli" to listOf("green beans", "cauliflower", "asparagus"),
        "chili flakes" to listOf("black pepper", "paprika", "hot sauce"),
        "spinach" to listOf("kale", "chard", "arugula"),
        "tomato" to listOf("canned tomatoes", "red bell pepper", "sun-dried tomatoes"),
        "banana" to listOf("unsweetened applesauce", "mashed pear", "mashed sweet potato"),
        "oats" to listOf("quinoa flakes", "muesli", "buckwheat flakes"),
        "avocado" to listOf("hummus", "mashed peas", "guacamole"),
        "carrot" to listOf("parsnip", "sweet potato", "celery"),
        "onion" to listOf("shallot", "leek", "spring onion"),
        "lentils" to listOf("split peas", "chickpeas", "black beans")
    )

    fun substitutesFor(name: String): List<String> =
        substitutes[name.trim().lowercase()] ?: listOf("a similar ingredient you already have", "leave it out")

    fun substituteResponse(name: String) =
        SubstituteResponse(ingredient = name, substitutes = substitutesFor(name), message = "demo")

    // ── Camera scan ───────────────────────────────────────────────────────────

    private val scanResults = listOf(
        listOf("apple", "banana", "orange", "carrot"),
        listOf("yogurt", "cheese", "eggs", "butter"),
        listOf("bell pepper", "zucchini", "mushrooms", "onion"),
        listOf("salmon", "lemon", "asparagus", "garlic")
    )
    private var scanCount = 0

    /** The demo cannot recognise a real photo, so each scan returns a plausible haul. */
    fun nextScan(): List<String> = scanResults[scanCount++ % scanResults.size]

    // ── AI chat ───────────────────────────────────────────────────────────────

    fun chatReply(userText: String): String {
        val t = userText.lowercase()
        fun has(vararg words: String) = words.any { t.contains(it) }
        return when {
            has("hello", "hi ", "hey") || t.trim() in setOf("hi", "hey", "hello") ->
                "Hey! I'm Sprout. Ask me about meals, protein, hydration or what to cook with what's in your pantry."
            has("protein") ->
                "Good protein sources: eggs, Greek yogurt, chicken breast, salmon, lentils, chickpeas and tofu. " +
                    "A simple target is 20 to 30 g per meal. Try the Garlic Chicken Stir Fry or the Lentil Soup in Recipes."
            has("breakfast", "morning") ->
                "For a quick breakfast: Berry Overnight Oats (10 minutes, prep the night before) or a Veggie Omelette " +
                    "(15 minutes, 22 g protein). Both are in the Breakfast filter on the Recipes tab."
            has("dinner", "tonight", "cook", "make", "pantry", "fridge") ->
                "Check your pantry first: with pasta, tomato and garlic you can make Tomato Basil Pasta in 25 minutes. " +
                    "Open the Pantry tab and look at \"What can I make?\" to see recipes ranked by how little you need to buy."
            has("water", "hydrat", "drink") ->
                "Aim for around 8 glasses a day, more if you exercise. A glass with each meal and one between meals " +
                    "gets you most of the way. You can log glasses from the Fitness tab."
            has("calorie", "weight", "lose", "gain", "bmi") ->
                "Your daily calorie goal lives in Settings, and the Home screen shows how much is left today. " +
                    "For gradual weight change, a modest gap of 300 to 500 kcal from your usual intake is a common starting point."
            has("vegan", "vegetarian", "plant") ->
                "Plant-based favourites in the app: Hearty Lentil Soup, Quinoa Buddha Bowl and Tomato Basil Pasta. " +
                    "Set your diet preference in Settings and the recipe search filters to match."
            has("snack") ->
                "Easy snacks: Hummus and Veggie Sticks, Apple Almond Butter Bites or a Greek Yogurt Parfait. " +
                    "Each is under 250 kcal and takes about five minutes."
            has("thank") -> "Anytime! Happy cooking."
            else ->
                "Good question! This is the demo build, so I have pre-written answers. " +
                    "Try asking about protein, breakfast, dinner ideas, hydration, snacks or calories."
        }
    }
}

/** In-memory stand-in for Firebase in the DEMO build. State lives only while the app is open. */
object DemoStore {

    const val DEMO_UID = "demo-user"

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var signedIn: String? = null
    private val emailToUid = mutableMapOf<String, String>()
    private val profiles = mutableMapOf<String, User>()
    private val logs = mutableMapOf<String, MutableMap<String, DailyLog>>()
    private val pantry = mutableMapOf<String, MutableList<PantryItem>>()
    private val saved = mutableMapOf<String, MutableMap<Int, SavedRecipe>>()

    private val starterPantry = listOf(
        "eggs", "tomato", "spinach", "milk", "pasta", "garlic", "cheese", "banana",
        "olive oil", "chicken breast", "rice", "avocado", "onion", "lemon", "oats", "carrot"
    )

    private fun starterItems() = starterPantry.mapIndexed { i, n ->
        PantryItem(itemId = "demo-$i", name = n, quantity = (1 + i % 3).toFloat(), unit = "item", isAvailable = true)
    }.toMutableList()

    init {
        profiles[DEMO_UID] = User(
            firstName = "Sam", lastName = "Demo", email = "demo@smartfridge.app",
            age = 27, sex = "Male", weight = 72f, height = 178f, bloodType = "A+",
            allergies = "", dietPreference = "None", dailyCalorieGoal = 2200, dailyWaterGoal = 8
        )
        pantry[DEMO_UID] = starterItems()

        // Today is partly logged and the six days before it have history, so the
        // streak, weekly chart and diary all look lived-in on first launch.
        val history = listOf(
            2050f to 7, 2310f to 8, 1890f to 6, 2140f to 8, 1760f to 5, 2230f to 9
        )
        val m = mutableMapOf<String, DailyLog>()
        fun put(daysAgo: Int, cal: Float, water: Int) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val d = dateFmt.format(c.time)
            m[d] = DailyLog(d, cal, cal * 0.20f / 4f, cal * 0.50f / 4f, cal * 0.30f / 9f, water)
        }
        put(0, 520f, 3)
        history.forEachIndexed { i, (cal, water) -> put(i + 1, cal, water) }
        logs[DEMO_UID] = m

        saved[DEMO_UID] = mutableMapOf(
            2009 to savedFrom(2009), 2007 to savedFrom(2007), 2011 to savedFrom(2011)
        )
    }

    private fun savedFrom(id: Int): SavedRecipe {
        val d = DemoData.recipeById(id)!!
        fun n(name: String) = d.nutrition?.nutrients?.firstOrNull { it.name == name }?.amount ?: 0f
        return SavedRecipe(
            recipeId = id, title = d.title, imageUrl = d.image,
            instructions = d.analyzedInstructions.firstOrNull()?.steps?.joinToString("\n") { it.step }.orEmpty(),
            calories = n("Calories"), protein = n("Protein"), carbs = n("Carbohydrates"), fat = n("Fat")
        )
    }

    // The demo has no real accounts: any email and password signs in, which keeps the
    // first run to one tap. An email that was registered in this session gets its own profile.
    fun login(email: String): String {
        val uid = emailToUid[email.trim().lowercase()] ?: DEMO_UID
        signedIn = uid
        return uid
    }

    fun register(email: String): String {
        val uid = "demo-" + UUID.randomUUID().toString().take(8)
        emailToUid[email.trim().lowercase()] = uid
        pantry[uid] = starterItems()
        signedIn = uid
        return uid
    }

    fun profile(uid: String): User? = profiles[uid]
    fun saveProfile(uid: String, user: User) { profiles[uid] = user }

    fun logFor(uid: String, date: String): DailyLog? = logs[uid]?.get(date)
    fun saveLog(uid: String, log: DailyLog) { logs.getOrPut(uid) { mutableMapOf() }[log.date] = log }
    fun allLogs(uid: String): List<DailyLog> = logs[uid]?.values?.sortedByDescending { it.date } ?: emptyList()

    fun pantryFor(uid: String): List<PantryItem> = pantry[uid]?.filter { it.isAvailable }?.toList() ?: emptyList()

    fun addPantry(uid: String, item: PantryItem): String {
        val id = item.itemId.ifBlank { "p-" + UUID.randomUUID().toString().take(8) }
        val list = pantry.getOrPut(uid) { mutableListOf() }
        list.removeAll { it.itemId == id }
        list.add(item.copy(itemId = id))
        return id
    }

    fun removePantry(uid: String, itemId: String) { pantry[uid]?.removeAll { it.itemId == itemId } }
    fun clearPantry(uid: String) { pantry[uid]?.clear() }

    fun saveRecipe(uid: String, recipe: SavedRecipe) { saved.getOrPut(uid) { mutableMapOf() }[recipe.recipeId] = recipe }
    fun removeRecipe(uid: String, id: Int) { saved[uid]?.remove(id) }
    fun savedRecipes(uid: String): List<SavedRecipe> = saved[uid]?.values?.toList() ?: emptyList()
    fun isSaved(uid: String, id: Int): Boolean = saved[uid]?.containsKey(id) == true
}
